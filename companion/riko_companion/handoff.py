"""Owner-controlled, authenticated file and explicit handoff storage."""

from __future__ import annotations

import hashlib
import json
import os
import re
import stat
import time
import uuid
from pathlib import Path

from .state import StateError

CHUNK_BYTES = 262144
FILE_BYTES = 67108864
STORE_BYTES = 268435456
TEXT_BYTES = 2048
MAX_SESSIONS = 32
MAX_ACTIVE_PER_PEER = 1
MAX_TEXTS = 8
TEXT_TTL = 300
APPROVAL_TTL = 600
TRANSFER_TTL = 86400
HEX64 = re.compile(r"[0-9a-f]{64}\Z")
UUID_RE = re.compile(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\Z")
MARKER = b"riko-handoff-v1\n"


def _uuid(value):
    if not isinstance(value, str) or not UUID_RE.fullmatch(value):
        raise StateError("invalid_request")
    return value


def _safe_text(value, maximum, allow_empty=False):
    if not isinstance(value, str):
        raise StateError("invalid_request")
    try:
        encoded_size = len(value.encode("utf-8"))
    except UnicodeEncodeError:
        raise StateError("invalid_request") from None
    if ((not allow_empty and not value) or len(value) > maximum or encoded_size > maximum or
            any(not c.isprintable() or c in "\x1b\x7f" for c in value)):
        raise StateError("invalid_request")
    return value


def _handoff_text(value):
    if not isinstance(value, str) or not value:
        raise StateError("invalid_request")
    try:
        if len(value.encode("utf-8")) > TEXT_BYTES:
            raise StateError("too_large")
    except UnicodeEncodeError:
        raise StateError("invalid_request") from None
    if any(c not in "\n\t" and (not c.isprintable() or c in "\x1b\x7f") for c in value):
        raise StateError("invalid_request")
    return value


def _regular(path):
    try:
        info = path.lstat()
    except FileNotFoundError:
        return False
    if (not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or
            stat.S_IMODE(info.st_mode) & 0o077):
        raise StateError("storage_failure")
    return True


class Handoff:
    def __init__(self, root: Path, retention_days=7, clock=time.time):
        self.root = Path(root).expanduser()
        self.retention_days = retention_days
        self.clock = clock
        self.root_fd = None
        self._open_root()
        try:
            self.grants = self._read_json(self.root / "grants.json", dict)
            self.workspaces = self._read_json(self.root / "workspaces.json", dict)
            if any(not UUID_RE.fullmatch(key) or not isinstance(value, dict) or
                   set(value) != {"token_hash", "created_at"} or
                   not isinstance(value["token_hash"], str) or not HEX64.fullmatch(value["token_hash"]) or
                   type(value["created_at"]) is not int for key, value in self.grants.items()):
                raise RuntimeError("invalid handoff grants")
            if any(not self._valid_workspace(value) or key != value["session_id"]
                   for key, value in self.workspaces.items()):
                raise RuntimeError("invalid workspace metadata")
            self.sessions = {}
            self.texts = {}
            self._load_sessions()
            self._recover()
        except StateError:
            self.close()
            raise RuntimeError("unsafe handoff storage") from None
        except Exception:
            self.close()
            raise
        self._expire_texts()

    def _open_root(self):
        try:
            info = self.root.lstat()
        except FileNotFoundError:
            raise RuntimeError("handoff root is not configured") from None
        if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) & 0o077:
            raise RuntimeError("handoff root is unsafe")
        marker = self.root / ".riko-handoff"
        if not _regular(marker):
            raise RuntimeError("handoff root marker is invalid")
        fd = os.open(marker, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            if os.read(fd, len(MARKER) + 1) != MARKER:
                raise RuntimeError("handoff root marker is invalid")
        finally:
            os.close(fd)
        self.root_fd = os.open(self.root, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) |
                               getattr(os, "O_NOFOLLOW", 0))

    @staticmethod
    def _read_json(path, expected):
        if not _regular(path):
            return {}
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            info = os.fstat(fd)
            if stat.S_IMODE(info.st_mode) & 0o077 or info.st_size > 262144:
                raise RuntimeError("unsafe handoff metadata")
            with os.fdopen(fd, "rb", closefd=False) as stream:
                value = json.loads(stream.read(info.st_size + 1))
        except (ValueError, UnicodeDecodeError, json.JSONDecodeError):
            raise RuntimeError("invalid handoff metadata") from None
        finally:
            os.close(fd)
        if not isinstance(value, expected):
            raise RuntimeError("invalid handoff metadata")
        return value

    def close(self):
        if self.root_fd is not None:
            os.close(self.root_fd)
            self.root_fd = None

    def _write_json(self, filename, value):
        payload = json.dumps(value, separators=(",", ":"), sort_keys=True).encode()
        if len(payload) > 262144:
            raise StateError("resource_limit")
        temp = self.root / f".write-{uuid.uuid4()}"
        fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(payload)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temp, self.root / filename)
            os.fsync(self.root_fd)
        finally:
            try:
                temp.unlink()
            except FileNotFoundError:
                pass

    def _session_dir(self, transfer_id, create=False):
        transfer_id = _uuid(transfer_id)
        path = self.root / transfer_id
        if create:
            os.mkdir(path, 0o700)
        try:
            info = path.lstat()
        except FileNotFoundError:
            raise StateError("not_found") from None
        if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) & 0o077:
            raise StateError("storage_failure")
        return path

    def _manifest_path(self, transfer_id):
        return self._session_dir(transfer_id) / "transfer.json"

    def _save(self, transfer):
        transfer["updated_at"] = int(self.clock() * 1000)
        path = self._manifest_path(transfer["transfer_id"])
        temp = path.parent / f".manifest-{uuid.uuid4()}"
        fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(json.dumps(transfer, separators=(",", ":"), sort_keys=True).encode())
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temp, path)
            dirfd = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) |
                            getattr(os, "O_NOFOLLOW", 0))
            try:
                os.fsync(dirfd)
            finally:
                os.close(dirfd)
        finally:
            try:
                temp.unlink()
            except FileNotFoundError:
                pass
        self.sessions[transfer["transfer_id"]] = transfer

    def _load_sessions(self):
        for entry in self.root.iterdir():
            if entry.name in (".riko-handoff", "grants.json", "workspaces.json"):
                continue
            if self._generated_temp(entry.name, ".write-"):
                if not _regular(entry):
                    raise RuntimeError("unsafe temporary handoff metadata")
                entry.unlink()
                continue
            if not UUID_RE.fullmatch(entry.name):
                raise RuntimeError("unexpected entry in handoff root")
            if not stat.S_ISDIR(entry.lstat().st_mode):
                raise RuntimeError("unexpected entry in handoff root")
            self._session_dir(entry.name)
            manifest_path = entry / "transfer.json"
            if not manifest_path.exists() and not any(entry.iterdir()):
                raise RuntimeError("incomplete handoff session directory")
            transfer = self._read_json(entry / "transfer.json", dict)
            if transfer.get("transfer_id") != entry.name or not self._valid_manifest(transfer):
                raise RuntimeError("invalid transfer manifest")
            for child in entry.iterdir():
                if self._generated_temp(child.name, ".manifest-"):
                    if not _regular(child):
                        raise RuntimeError("unsafe temporary transfer manifest")
                    child.unlink()
                elif child.name not in {"transfer.json", "partial", "content", transfer["result_name"]}:
                    raise RuntimeError("unexpected entry in transfer directory")
                elif child.name != "transfer.json" and not _regular(child):
                    raise RuntimeError("unsafe transfer content")
            self.sessions[entry.name] = transfer
        if len(self.sessions) > MAX_SESSIONS:
            raise RuntimeError("handoff session limit exceeded")

    @staticmethod
    def _generated_temp(name, prefix):
        return name.startswith(prefix) and UUID_RE.fullmatch(name[len(prefix):]) is not None

    @staticmethod
    def _valid_manifest(item):
        try:
            if set(item) != {"transfer_id", "peer_id", "direction", "state", "display_name", "mime_type",
                             "size_bytes", "sha256", "offset", "created_at", "expires_at", "updated_at",
                             "error", "result_name"}:
                return False
            _uuid(item["transfer_id"])
            _uuid(item["peer_id"])
            return (item["direction"] in ("phone_to_laptop", "laptop_to_phone") and
                    item["state"] in ("awaiting_approval", "offered", "ready", "transferring",
                                      "completed", "cancelled", "failed") and
                    isinstance(item["display_name"], str) and len(item["display_name"]) <= 128 and
                    isinstance(item["mime_type"], str) and len(item["mime_type"]) <= 127 and
                    type(item["size_bytes"]) is int and 0 <= item["size_bytes"] <= FILE_BYTES and
                    isinstance(item["sha256"], str) and HEX64.fullmatch(item["sha256"]) and
                    type(item["offset"]) is int and 0 <= item["offset"] <= item["size_bytes"] and
                    type(item["created_at"]) is int and type(item["expires_at"]) is int and
                    type(item["updated_at"]) is int and
                    (item["error"] is None or item["error"] in {"approval_denied", "approval_expired",
                     "cancelled", "expired", "interrupted_restart", "integrity_mismatch", "offer_failed", "offer_changed"}) and
                    isinstance(item["result_name"], str) and bool(re.fullmatch(
                        r"[A-Za-z0-9][A-Za-z0-9._ -]{0,127}", item["result_name"])))
        except (KeyError, TypeError, StateError):
            return False

    def _recover(self):
        now = int(self.clock() * 1000)
        for transfer in self.sessions.values():
            if transfer["direction"] == "phone_to_laptop" and transfer["state"] in (
                    "awaiting_approval", "ready", "transferring"):
                result = self._session_dir(transfer["transfer_id"]) / transfer["result_name"]
                if self._matches_file(result, transfer["size_bytes"], transfer["sha256"]):
                    transfer["state"], transfer["offset"], transfer["error"] = "completed", transfer["size_bytes"], None
                    self._unlink_generated(transfer["transfer_id"], "partial")
                    self._save(transfer)
                elif _regular(result):
                    transfer["state"], transfer["error"] = "failed", "integrity_mismatch"
                    self._unlink_generated(transfer["transfer_id"], "partial")
                    self._save(transfer)
                else:
                    transfer["state"] = "cancelled"
                    transfer["error"] = "interrupted_restart"
                    self._unlink_generated(transfer["transfer_id"], "partial")
                    self._save(transfer)
            elif transfer["expires_at"] <= now and transfer["state"] not in ("completed", "cancelled", "failed"):
                self._cancel_generated(transfer)
            elif transfer["direction"] == "laptop_to_phone" and transfer["state"] in (
                    "offered", "ready", "transferring"):
                content = self._session_dir(transfer["transfer_id"]) / "content"
                if not self._matches_file(content, transfer["size_bytes"], transfer["sha256"]):
                    self._unlink_generated(transfer["transfer_id"], "content")
                    transfer["state"], transfer["error"] = "failed", "offer_failed"
                    self._save(transfer)

    @staticmethod
    def _obj(item):
        return {key: item.get(key) for key in ("transfer_id", "direction", "state", "display_name", "mime_type",
                "size_bytes", "sha256", "offset", "created_at", "error", "available")}

    def _available(self, item):
        if item["direction"] == "phone_to_laptop":
            return item["state"] == "completed" and _regular(
                self._session_dir(item["transfer_id"]) / item["result_name"])
        return item["state"] in ("offered", "ready", "transferring", "completed") and _regular(
            self._session_dir(item["transfer_id"]) / "content")

    def _view(self, item):
        self._expire_transfer(item)
        result = dict(item)
        result["available"] = self._available(item)
        return self._obj(result)

    @staticmethod
    def _matches_file(path, size, digest):
        if not _regular(path):
            return False
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        actual = hashlib.sha256()
        total = 0
        try:
            info = os.fstat(fd)
            if info.st_size != size:
                return False
            with os.fdopen(fd, "rb", closefd=False) as stream:
                while chunk := stream.read(CHUNK_BYTES):
                    total += len(chunk)
                    actual.update(chunk)
        finally:
            os.close(fd)
        return total == size and actual.hexdigest() == digest

    def grant(self, peer_id, token_hash):
        peer_id = _uuid(peer_id)
        if not isinstance(token_hash, str) or not HEX64.fullmatch(token_hash):
            raise StateError("unauthorized")
        if peer_id not in self.grants or self.grants[peer_id]["token_hash"] != token_hash:
            previous = self.grants.get(peer_id)
            for transfer in list(self.sessions.values()):
                if transfer["peer_id"] == peer_id and transfer["state"] not in ("completed", "cancelled", "failed"):
                    self._cancel_generated(transfer)
            if previous and previous["token_hash"] != token_hash:
                for key, item in list(self.texts.items()):
                    if item["peer_id"] == peer_id:
                        del self.texts[key]
                self.workspaces = {key: value for key, value in self.workspaces.items()
                                   if value["peer_id"] != peer_id}
                self._write_json("workspaces.json", self.workspaces)
            self.grants[peer_id] = {"token_hash": token_hash, "created_at": int(self.clock() * 1000)}
            try:
                self._write_json("grants.json", self.grants)
            except Exception:
                if previous is None:
                    self.grants.pop(peer_id, None)
                else:
                    self.grants[peer_id] = previous
                raise

    def deny_peer(self, peer_id):
        peer_id = _uuid(peer_id)
        self.grants.pop(peer_id, None)
        for key, item in list(self.texts.items()):
            if item["peer_id"] == peer_id:
                del self.texts[key]
        for transfer in list(self.sessions.values()):
            if transfer["peer_id"] == peer_id and transfer["state"] not in ("completed", "cancelled", "failed"):
                self._cancel_generated(transfer)
        self._write_json("grants.json", self.grants)

    def require_peer(self, peer_id, trusted):
        peer_id = _uuid(peer_id)
        current = trusted.get(peer_id)
        grant = self.grants.get(peer_id)
        if not current:
            self.deny_peer(peer_id)
            raise StateError("unauthorized")
        if not grant:
            raise StateError("forbidden")
        if current.get("token_hash") != grant.get("token_hash"):
            self.deny_peer(peer_id)
            raise StateError("unauthorized")
        return peer_id

    def revoke(self, peer_id):
        self.deny_peer(peer_id)
        for key, text in list(self.texts.items()):
            if text["peer_id"] == peer_id:
                del self.texts[key]
        self.workspaces = {key: value for key, value in self.workspaces.items() if value["peer_id"] != peer_id}
        self._write_json("workspaces.json", self.workspaces)

    def _active(self, peer_id):
        for item in self.sessions.values():
            if item["peer_id"] == peer_id:
                self._expire_transfer(item)
        return [x for x in self.sessions.values() if x["peer_id"] == peer_id and
                x["state"] in ("awaiting_approval", "offered", "ready", "transferring")]

    def _expire_transfer(self, item):
        if (item["state"] in ("awaiting_approval", "offered", "ready", "transferring") and
                item["expires_at"] <= int(self.clock() * 1000)):
            self._cancel_generated(item, "expired")

    def _usage(self):
        return sum(x["size_bytes"] for x in self.sessions.values()
                   if x["state"] != "cancelled")

    def _new(self, peer_id, transfer_id, name, mime, size, digest, direction, state):
        transfer_id = _uuid(transfer_id)
        if transfer_id in self.sessions:
            previous = self.sessions[transfer_id]
            if (previous["peer_id"], previous["display_name"], previous["mime_type"],
                    previous["size_bytes"], previous["sha256"], previous["direction"]) == (
                    peer_id, name, mime, size, digest, direction):
                return previous
            raise StateError("conflict")
        if len(self.sessions) >= MAX_SESSIONS or self._active(peer_id):
            raise StateError("resource_limit")
        if self._usage() + size > STORE_BYTES:
            raise StateError("resource_limit")
        if os.statvfs(self.root).f_bavail * os.statvfs(self.root).f_frsize < size:
            raise StateError("resource_limit")
        transfer = {"transfer_id": transfer_id, "peer_id": peer_id, "direction": direction,
                    "state": state, "display_name": name, "mime_type": mime, "size_bytes": size,
                    "sha256": digest, "offset": 0, "created_at": int(self.clock() * 1000),
                    "expires_at": int((self.clock() + (APPROVAL_TTL if state == "awaiting_approval" else TRANSFER_TTL)) * 1000),
                    "updated_at": int(self.clock() * 1000), "error": None,
                    "result_name": self._result_name(name)}
        self._session_dir(transfer_id, create=True)
        self._save(transfer)
        return transfer

    def create_upload(self, peer_id, body):
        fields = {"protocol", "transfer_id", "display_name", "mime_type", "size_bytes", "sha256"}
        if set(body) != fields or type(body.get("protocol")) is not int or body["protocol"] != 2:
            raise StateError("invalid_request")
        name = _safe_text(body["display_name"], 128)
        mime = _safe_text(body["mime_type"], 127)
        size = body["size_bytes"]
        digest = body["sha256"]
        if type(size) is not int or not 0 <= size <= FILE_BYTES or not isinstance(digest, str) or not HEX64.fullmatch(digest):
            raise StateError("invalid_request")
        item = self._new(peer_id, body["transfer_id"], name, mime, size, digest,
                         "phone_to_laptop", "awaiting_approval")
        if size == 0 and item["state"] != "completed" and not _regular(
                self._session_dir(item["transfer_id"]) / "partial"):
            path = self._session_dir(item["transfer_id"]) / "partial"
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
            os.fsync(fd)
            os.close(fd)
            dirfd = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) |
                            getattr(os, "O_NOFOLLOW", 0))
            try:
                os.fsync(dirfd)
            finally:
                os.close(dirfd)
        return self._view(item)

    @staticmethod
    def _result_name(name):
        basename = name.rsplit("/", 1)[-1].replace("\\", "_")
        safe = re.sub(r"[^A-Za-z0-9._ -]", "_", basename).lstrip(".").strip()[:128]
        if safe in {"transfer.json", "content", "partial", "result"}:
            safe = "received-" + safe
        return safe if safe and safe not in (".", "..") else "file"

    def list_transfers(self, peer_id):
        return [self._view(x) for x in list(self.sessions.values()) if x["peer_id"] == peer_id][:32]

    def transfer(self, peer_id, transfer_id):
        item = self.sessions.get(_uuid(transfer_id))
        if not item or item["peer_id"] != peer_id:
            raise StateError("not_found")
        self._expire_transfer(item)
        return item

    def decide(self, transfer_id, approve):
        item = self.sessions.get(_uuid(transfer_id))
        if not item or item["direction"] != "phone_to_laptop" or item["state"] != "awaiting_approval":
            return False
        if item["expires_at"] <= int(self.clock() * 1000):
            self._cancel_generated(item, "approval_expired")
            return False
        item["state"] = "ready" if approve else "cancelled"
        if not approve:
            self._cancel_generated(item, "approval_denied")
        else:
            item["expires_at"] = int((self.clock() + TRANSFER_TTL) * 1000)
        self._save(item)
        return True

    def offer_file(self, peer_id, source):
        source = Path(source).expanduser()
        try:
            info = source.lstat()
        except OSError:
            raise StateError("invalid_request") from None
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or info.st_size > FILE_BYTES:
            raise StateError("invalid_request")
        name = _safe_text(source.name, 128)
        digest = hashlib.sha256()
        transfer_id = str(uuid.uuid4())
        item = self._new(peer_id, transfer_id, name, "application/octet-stream", info.st_size,
                         "0" * 64, "laptop_to_phone", "offered")
        target = self._session_dir(transfer_id) / "content"
        try:
            src_fd = os.open(source, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        except OSError:
            self._cancel_generated(item, "offer_failed")
            raise StateError("invalid_request") from None
        try:
            dst_fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
        except Exception:
            os.close(src_fd)
            self._cancel_generated(item, "offer_failed")
            raise
        opened_source = os.fstat(src_fd)
        if (not stat.S_ISREG(opened_source.st_mode) or opened_source.st_uid != os.getuid() or
                opened_source.st_size != info.st_size):
            os.close(src_fd)
            os.close(dst_fd)
            self._cancel_generated(item, "offer_changed")
            raise StateError("conflict")
        total = 0
        try:
            with os.fdopen(src_fd, "rb") as src, os.fdopen(dst_fd, "wb") as dst:
                while chunk := src.read(CHUNK_BYTES):
                    total += len(chunk)
                    if total > FILE_BYTES or total > info.st_size:
                        raise StateError("too_large")
                    digest.update(chunk)
                    dst.write(chunk)
                dst.flush()
                os.fsync(dst.fileno())
        except Exception:
            self._cancel_generated(item, "offer_failed")
            raise
        if total != info.st_size:
            self._cancel_generated(item, "offer_changed")
            raise StateError("conflict")
        item["sha256"] = digest.hexdigest()
        item["offset"] = 0
        self._save(item)
        return self._view(item)

    def offers(self, peer_id):
        return [self._view(x) for x in self.sessions.values()
                if x["peer_id"] == peer_id and x["direction"] == "laptop_to_phone" and
                x["state"] in ("offered", "ready", "transferring")][:32]

    def consent(self, peer_id, transfer_id, body):
        if body != {"protocol": 2}:
            raise StateError("invalid_request")
        item = self.transfer(peer_id, transfer_id)
        if item["direction"] != "laptop_to_phone":
            raise StateError("conflict")
        if item["state"] == "offered":
            item["state"] = "ready"
            item["expires_at"] = int((self.clock() + TRANSFER_TTL) * 1000)
            self._save(item)
        elif item["state"] not in ("ready", "transferring", "completed"):
            raise StateError("conflict")
        return self._view(item)

    def _chunk_file(self, peer_id, transfer_id, direction):
        item = self.transfer(peer_id, transfer_id)
        if item["direction"] != direction:
            raise StateError("conflict")
        if item["state"] not in ("ready", "transferring"):
            raise StateError("conflict")
        filename = "partial" if direction == "phone_to_laptop" else "content"
        return item, self._session_dir(transfer_id) / filename

    def write_chunk(self, peer_id, transfer_id, offset, chunk, digest):
        item, path = self._chunk_file(peer_id, transfer_id, "phone_to_laptop")
        if len(chunk) > CHUNK_BYTES or hashlib.sha256(chunk).hexdigest() != digest:
            raise StateError("invalid_request")
        if type(offset) is not int or offset < 0 or offset + len(chunk) > item["size_bytes"]:
            raise StateError("invalid_request")
        if offset < item["offset"]:
            if offset + len(chunk) == item["offset"] and _regular(path):
                fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
                try:
                    os.lseek(fd, offset, os.SEEK_SET)
                    prior = os.read(fd, len(chunk))
                finally:
                    os.close(fd)
                if prior == chunk:
                    return item["offset"]
            raise StateError("conflict")
        if offset != item["offset"]:
            raise StateError("conflict")
        if len(chunk) == 0 and item["size_bytes"] != 0:
            raise StateError("invalid_request")
        fd = os.open(path, os.O_RDWR | os.O_CREAT | getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            opened = os.fstat(fd)
            if not stat.S_ISREG(opened.st_mode) or opened.st_uid != os.getuid() or opened.st_size != offset:
                raise StateError("storage_failure")
            stream = os.fdopen(fd, "r+b")
            fd = -1
            with stream:
                stream.seek(offset)
                stream.write(chunk)
                stream.flush()
                os.fsync(stream.fileno())
            dirfd = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) |
                            getattr(os, "O_NOFOLLOW", 0))
            try:
                os.fsync(dirfd)
            finally:
                os.close(dirfd)
        except OSError:
            raise StateError("storage_failure") from None
        finally:
            if fd >= 0:
                os.close(fd)
        item["offset"] += len(chunk)
        item["state"] = "transferring"
        self._save(item)
        return item["offset"]

    def read_chunk(self, peer_id, transfer_id, offset, length):
        item, path = self._chunk_file(peer_id, transfer_id, "laptop_to_phone")
        if (type(offset) is not int or type(length) is not int or offset < 0 or
                not 1 <= length <= CHUNK_BYTES or offset + length > item["size_bytes"]):
            raise StateError("invalid_request")
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            opened = os.fstat(fd)
            if not stat.S_ISREG(opened.st_mode) or opened.st_uid != os.getuid() or opened.st_size != item["size_bytes"]:
                raise StateError("storage_failure")
            os.lseek(fd, offset, os.SEEK_SET)
            data = os.read(fd, length)
        finally:
            os.close(fd)
        if len(data) != length:
            raise StateError("storage_failure")
        item["state"] = "transferring"
        item["offset"] = max(item["offset"], offset + len(data))
        self._save(item)
        return data

    def commit(self, peer_id, transfer_id, body):
        if (not isinstance(body, dict) or set(body) != {"protocol", "sha256", "size_bytes"} or
                type(body["protocol"]) is not int or body["protocol"] != 2):
            raise StateError("invalid_request")
        item = self.transfer(peer_id, transfer_id)
        if body["sha256"] != item["sha256"] or body["size_bytes"] != item["size_bytes"]:
            raise StateError("conflict")
        if item["state"] == "completed":
            return self._view(item)
        if item["direction"] == "laptop_to_phone":
            if item["state"] not in ("ready", "transferring"):
                raise StateError("conflict")
            item["state"] = "completed"
        else:
            if item["state"] not in ("ready", "transferring") or item["offset"] != item["size_bytes"]:
                raise StateError("conflict")
            partial = self._session_dir(transfer_id) / "partial"
            fd = os.open(partial, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
            digest = hashlib.sha256()
            total = 0
            try:
                info = os.fstat(fd)
                if not stat.S_ISREG(info.st_mode) or info.st_size != item["size_bytes"]:
                    raise StateError("conflict")
                with os.fdopen(fd, "rb", closefd=False) as stream:
                    while chunk := stream.read(CHUNK_BYTES):
                        total += len(chunk)
                        digest.update(chunk)
            finally:
                os.close(fd)
            if total != item["size_bytes"] or digest.hexdigest() != item["sha256"]:
                self._unlink_generated(transfer_id, "partial")
                item["state"], item["error"] = "failed", "integrity_mismatch"
                self._save(item)
                raise StateError("integrity_mismatch")
            target = self._session_dir(transfer_id) / item["result_name"]
            try:
                os.link(partial, target, follow_symlinks=False)
            except FileExistsError:
                raise StateError("conflict") from None
            os.unlink(partial)
            item["state"] = "completed"
        item["offset"] = item["size_bytes"]
        item["error"] = None
        self._save(item)
        return self._view(item)

    def cancel(self, peer_id, transfer_id, body):
        if body != {"protocol": 2}:
            raise StateError("invalid_request")
        item = self.transfer(peer_id, transfer_id)
        if item["state"] == "completed":
            return self._view(item)
        self._cancel_generated(item)
        return self._view(item)

    def _unlink_generated(self, transfer_id, filename):
        path = self._session_dir(transfer_id) / filename
        if _regular(path):
            path.unlink()

    def _cancel_generated(self, item, reason="cancelled"):
        if item["state"] != "completed":
            for filename in ("partial", "content"):
                self._unlink_generated(item["transfer_id"], filename)
            item["state"] = "cancelled"
            item["error"] = reason
            self._save(item)

    def create_text(self, peer_id, value):
        value = _handoff_text(value)
        self._expire_texts()
        if len(self.texts) >= MAX_TEXTS:
            raise StateError("resource_limit")
        text_id = str(uuid.uuid4())
        self.texts[text_id] = {"text_id": text_id, "peer_id": peer_id, "text": value,
                               "created_at": int(self.clock() * 1000), "expires": self.clock() + TEXT_TTL,
                               "direction": "phone_to_laptop"}
        return {key: self.texts[text_id][key] for key in ("text_id", "created_at", "direction")}

    def list_texts(self, peer_id):
        self._expire_texts()
        return [{"text_id": x["text_id"], "created_at": x["created_at"],
                 "size_bytes": len(x["text"].encode()), "direction": x["direction"]}
                for x in self.texts.values() if x["peer_id"] == peer_id]

    def receive_text(self, peer_id, body):
        if set(body) != {"protocol", "text"} or type(body["protocol"]) is not int or body["protocol"] != 2:
            raise StateError("invalid_request")
        return self.create_text(peer_id, body["text"])

    def offer_text(self, peer_id, value):
        result = self.create_text(peer_id, value)
        self.texts[result["text_id"]]["direction"] = "laptop_to_phone"
        return result

    def get_text(self, peer_id, text_id):
        self._expire_texts()
        item = self.texts.get(_uuid(text_id))
        if not item or item["peer_id"] != peer_id or item["direction"] != "laptop_to_phone":
            raise StateError("not_found")
        return {"text_id": item["text_id"], "text": item["text"], "created_at": item["created_at"],
                "direction": item["direction"]}

    def consume_text(self, peer_id, text_id, body):
        if body != {"protocol": 2}:
            raise StateError("invalid_request")
        self._expire_texts()
        item = self.texts.get(_uuid(text_id))
        if not item or item["peer_id"] != peer_id:
            raise StateError("not_found")
        del self.texts[text_id]

    def inbox_text(self):
        self._expire_texts()
        return [{"text_id": x["text_id"], "created_at": x["created_at"],
                 "size_bytes": len(x["text"].encode())} for x in self.texts.values()
                if x["direction"] == "phone_to_laptop"]

    def show_inbox_text(self, text_id):
        self._expire_texts()
        item = self.texts.get(_uuid(text_id))
        if not item or item["direction"] != "phone_to_laptop":
            raise StateError("not_found")
        return item["text"]

    def consume_inbox_text(self, text_id):
        item = self.texts.get(_uuid(text_id))
        if item and item["direction"] == "phone_to_laptop":
            del self.texts[text_id]

    def _expire_texts(self):
        now = self.clock()
        for key, item in list(self.texts.items()):
            if item["expires"] <= now:
                del self.texts[key]

    @staticmethod
    def _valid_workspace(item):
        try:
            if set(item) != {"session_id", "peer_id", "title", "created_at", "task", "materials", "transfer_ids"}:
                return False
            _uuid(item["session_id"])
            _uuid(item["peer_id"])
            _safe_text(item["title"], 128)
            if type(item["created_at"]) is not int or item["created_at"] < 0:
                return False
            task = item["task"]
            if task is not None:
                if not isinstance(task, dict) or set(task) != {"sync_id", "title", "deadline", "updated_at", "fingerprint"}:
                    return False
                _uuid(task["sync_id"])
                _safe_text(task["title"], 128)
                if task["deadline"] is not None and (type(task["deadline"]) is not int or task["deadline"] < 0):
                    return False
                if type(task["updated_at"]) is not int or task["updated_at"] < 0:
                    return False
                if not isinstance(task["fingerprint"], str) or not HEX64.fullmatch(task["fingerprint"]):
                    return False
            if (not isinstance(item["materials"], list) or len(item["materials"]) > 8 or
                    not isinstance(item["transfer_ids"], list) or len(item["transfer_ids"]) > 8):
                return False
            for transfer_id in item["transfer_ids"]:
                _uuid(transfer_id)
            for material in item["materials"]:
                if not isinstance(material, dict) or set(material) != {"title", "reference", "transfer_id"}:
                    return False
                _safe_text(material["title"], 128)
                reference = _safe_text(material["reference"], 512)
                if not (reference.startswith("https://") or re.fullmatch(r"[A-Za-z0-9._:-]{1,512}", reference)):
                    return False
                if material["transfer_id"] is not None:
                    _uuid(material["transfer_id"])
            return True
        except (KeyError, TypeError, StateError):
            return False

    def save_workspace(self, peer_id, body):
        required = {"protocol", "session_id", "title", "created_at", "task", "materials", "transfer_ids"}
        if set(body) != required or type(body["protocol"]) is not int or body["protocol"] != 2:
            raise StateError("invalid_request")
        session_id = _uuid(body["session_id"])
        title = _safe_text(body["title"], 128)
        created = body["created_at"]
        if type(created) is not int or created < 0:
            raise StateError("invalid_request")
        task = body["task"]
        if task is not None:
            if not isinstance(task, dict) or set(task) != {"sync_id", "title", "deadline", "updated_at", "fingerprint"}:
                raise StateError("invalid_request")
            _uuid(task["sync_id"])
            _safe_text(task["title"], 128)
            if task["deadline"] is not None and (type(task["deadline"]) is not int or task["deadline"] < 0):
                raise StateError("invalid_request")
            if type(task["updated_at"]) is not int or task["updated_at"] < 0:
                raise StateError("invalid_request")
            if not isinstance(task["fingerprint"], str) or not HEX64.fullmatch(task["fingerprint"]):
                raise StateError("invalid_request")
        materials = body["materials"]
        transfer_ids = body["transfer_ids"]
        if not isinstance(materials, list) or len(materials) > 8 or not isinstance(transfer_ids, list) or len(transfer_ids) > 8:
            raise StateError("invalid_request")
        for ref in transfer_ids:
            if self.transfer(peer_id, ref)["state"] in ("cancelled", "failed"):
                pass
        clean_materials = []
        for material in materials:
            if not isinstance(material, dict) or set(material) != {"title", "reference", "transfer_id"}:
                raise StateError("invalid_request")
            material_title = _safe_text(material["title"], 128)
            reference = _safe_text(material["reference"], 512)
            if not (reference.startswith("https://") or re.fullmatch(r"[A-Za-z0-9._:-]{1,512}", reference)):
                raise StateError("invalid_request")
            ref_id = material["transfer_id"]
            if ref_id is not None:
                self.transfer(peer_id, ref_id)
            clean_materials.append({"title": material_title, "reference": reference, "transfer_id": ref_id})
        item = {"session_id": session_id, "peer_id": peer_id, "title": title, "created_at": created,
                "task": task, "materials": clean_materials, "transfer_ids": [_uuid(x) for x in transfer_ids]}
        existing = self.workspaces.get(session_id)
        if existing is not None:
            if existing == item:
                return item
            raise StateError("conflict")
        self.workspaces[session_id] = item
        self._write_json("workspaces.json", self.workspaces)
        return item

    def list_workspaces(self, peer_id):
        results = []
        for item in self.workspaces.values():
            if item["peer_id"] != peer_id:
                continue
            result = dict(item)
            result.pop("peer_id", None)
            result["available"] = all(
                transfer_id in self.sessions and self.sessions[transfer_id]["peer_id"] == peer_id and
                self._available(self.sessions[transfer_id]) for transfer_id in item["transfer_ids"])
            result["materials"] = [dict(x, available=(x["transfer_id"] is None or
                (x["transfer_id"] in self.sessions and self.sessions[x["transfer_id"]]["peer_id"] == peer_id and
                 self._available(self.sessions[x["transfer_id"]])))) for x in item["materials"]]
            results.append(result)
        results.sort(key=lambda x: x["created_at"], reverse=True)
        truncated = len(results) > 7
        results = results[:7]
        while results and len(json.dumps({"workspaces": results}, separators=(",", ":")).encode()) > 60000:
            results.pop()
            truncated = True
        return {"workspaces": results, "truncated": truncated}

    def list_workspaces_for_console(self):
        result = []
        for item in self.workspaces.values():
            if item["peer_id"] not in self.grants:
                continue
            result.append({"session_id": item["session_id"], "title": item["title"],
                           "created_at": item["created_at"],
                           "available": all(ref in self.sessions and self._available(self.sessions[ref])
                                            for ref in item["transfer_ids"])})
        result.sort(key=lambda x: x["created_at"], reverse=True)
        return result[:7]

    def cleanup(self):
        now = int(self.clock() * 1000)
        cutoff = now - self.retention_days * 86400000
        removed = 0
        for transfer in self.sessions.values():
            self._expire_transfer(transfer)
        for transfer_id, item in list(self.sessions.items()):
            if item["created_at"] < cutoff and item["state"] in ("completed", "cancelled", "failed"):
                directory = self._session_dir(transfer_id)
                allowed = {"transfer.json", "content", "partial", item["result_name"]}
                for child in directory.iterdir():
                    if child.name not in allowed:
                        raise StateError("storage_failure")
                    if child.name == item["result_name"] and not self._matches_file(
                            child, item["size_bytes"], item["sha256"]):
                        raise StateError("storage_failure")
                    if child.name == "content" and not self._matches_file(
                            child, item["size_bytes"], item["sha256"]):
                        raise StateError("storage_failure")
                    if _regular(child):
                        child.unlink()
                    elif stat.S_ISDIR(child.lstat().st_mode):
                        raise StateError("storage_failure")
                directory.rmdir()
                del self.sessions[transfer_id]
                removed += 1
        self.workspaces = {key: value for key, value in self.workspaces.items()
                           if value["created_at"] >= cutoff}
        self._write_json("workspaces.json", self.workspaces)
        return removed
