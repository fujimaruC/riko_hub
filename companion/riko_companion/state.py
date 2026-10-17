"""Pure pairing and trust state for the Riko Hub laptop companion."""

from __future__ import annotations

import hashlib
import hmac
import re
import secrets
import threading
import time
import uuid
from dataclasses import dataclass

HEX64 = re.compile(r"[0-9a-f]{64}\Z")
MAX_TRUSTED = 8
PAIR_TTL = 120
APPROVAL_TTL = 60


class StateError(Exception):
    def __init__(self, code: str):
        super().__init__(code)
        self.code = code


@dataclass
class PairingKey:
    secret: str
    expires: float
    used: bool = False


@dataclass
class PendingApproval:
    request_id: str
    device_id: str
    device_name: str
    expires: float
    event: threading.Event
    decision: str | None = None


class CompanionState:
    def __init__(self, peer_id: str, name: str, cert_pin: str, trust: dict,
                 persist, monotonic=time.monotonic, handoff=None):
        self.peer_id, self.name, self.cert_pin = peer_id, name, cert_pin
        self.trust, self.persist, self.monotonic = trust, persist, monotonic
        self.handoff = handoff
        self.lock = threading.RLock()
        self.pairing: PairingKey | None = None
        self.pending: PendingApproval | None = None
        self.presence: dict[str, float] = {}
        self.shutting_down = False

    def create_pairing_key(self) -> str:
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            if self.pending:
                raise StateError("occupied")
            self.pairing = PairingKey(secrets.token_hex(32), self.monotonic() + PAIR_TTL)
            return f"riko1:{self.peer_id}:{self.cert_pin}:{self.pairing.secret}"

    def begin_pairing(self, secret: str, device_id: str, device_name: str):
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            pair = self.pairing
            if (not pair or pair.used or self.monotonic() >= pair.expires or
                    not HEX64.fullmatch(secret) or not hmac.compare_digest(secret, pair.secret)):
                raise StateError("bad_pairing_key")
            pair.used = True
            try:
                uuid.UUID(device_id)
            except (ValueError, AttributeError):
                raise StateError("invalid_request") from None
            device_id = str(uuid.UUID(device_id))
            if (not device_name or len(device_name) > 64 or len(device_name.encode("utf-8")) > 255 or
                    any(not c.isprintable() for c in device_name)):
                raise StateError("invalid_request")
            if self.pending or device_id in self.trust or len(self.trust) >= MAX_TRUSTED:
                raise StateError("occupied")
            request = PendingApproval(secrets.token_hex(4), device_id, device_name,
                                      min(self.monotonic() + APPROVAL_TTL, pair.expires), threading.Event())
            self.pending = request
            return request

    def decide(self, request_id: str, approve: bool) -> bool:
        with self.lock:
            pending = self.pending
            if (self.shutting_down or not pending or pending.request_id != request_id or
                    pending.decision is not None or self.monotonic() >= pending.expires):
                return False
            pending.decision = "approve" if approve else "deny"
            pending.event.set()
            return True

    def finish_pairing(self, pending: PendingApproval, timeout: float = APPROVAL_TTL):
        pending.event.wait(max(0, min(timeout, pending.expires - self.monotonic())))
        with self.lock:
            if self.shutting_down or self.pending is not pending:
                raise StateError("approval_denied")
            self.pending = None
            if pending.decision != "approve" or self.monotonic() >= pending.expires:
                raise StateError("approval_denied")
            if pending.device_id in self.trust or len(self.trust) >= MAX_TRUSTED:
                raise StateError("occupied")
            token = secrets.token_hex(32)
            entry = {"device_name": pending.device_name, "token_hash": hashlib.sha256(token.encode()).hexdigest()}
            self.trust[pending.device_id] = entry
            try:
                self.persist(self.trust)
            except Exception:
                del self.trust[pending.device_id]
                raise
            return token

    def _device_for_token(self, token: str) -> str:
        if not isinstance(token, str) or not HEX64.fullmatch(token):
            raise StateError("unauthorized")
        digest = hashlib.sha256(token.encode()).hexdigest()
        for device_id, entry in self.trust.items():
            if hmac.compare_digest(digest, entry["token_hash"]):
                return device_id
        raise StateError("unauthorized")

    def authenticated(self, token: str) -> str:
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            device_id = self._device_for_token(token)
            self.presence[device_id] = self.monotonic()
            return device_id

    def disconnect(self, token: str):
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            self.presence.pop(self._device_for_token(token), None)

    def revoke(self, token: str):
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            device_id = self._device_for_token(token)
            old = self.trust.pop(device_id)
            seen = self.presence.pop(device_id, None)
            try:
                self.persist(self.trust)
            except Exception:
                self.trust[device_id] = old
                if seen is not None:
                    self.presence[device_id] = seen
                raise
            if self.handoff is not None:
                try:
                    self.handoff.revoke(device_id)
                except Exception:
                    pass
            return device_id

    def revoke_id(self, device_id: str) -> bool:
        with self.lock:
            if self.shutting_down:
                raise StateError("shutting_down")
            if device_id not in self.trust:
                return False
            old = self.trust.pop(device_id)
            seen = self.presence.pop(device_id, None)
            try:
                self.persist(self.trust)
            except Exception:
                self.trust[device_id] = old
                if seen is not None:
                    self.presence[device_id] = seen
                raise
            if self.handoff is not None:
                try:
                    self.handoff.revoke(device_id)
                except Exception:
                    pass
            return True

    def status(self) -> str:
        with self.lock:
            return "connected" if any(self.monotonic() - seen < 45 for seen in self.presence.values()) else "disconnected"

    def phone_status(self):
        with self.lock:
            now = self.monotonic()
            return [(device_id, entry["device_name"],
                     "connected" if now - self.presence.get(device_id, float("-inf")) < 45 else "disconnected")
                    for device_id, entry in self.trust.items()]

    def stop(self):
        with self.lock:
            self.shutting_down = True
            if self.pending:
                self.pending.decision = "deny"
                self.pending.event.set()
