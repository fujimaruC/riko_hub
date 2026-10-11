"""Owner-only identity and trust storage with symlink and process-lock checks."""

from __future__ import annotations

import json
import os
import stat
import tempfile
import uuid
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID


class Storage:
    def __init__(self, directory: Path):
        self.directory = directory.expanduser()
        self._ensure_directory()

    def _ensure_directory(self):
        self.directory.mkdir(mode=0o700, parents=True, exist_ok=True)
        if self.directory.is_symlink() or not self.directory.is_dir():
            raise RuntimeError("data directory must be a real directory")
        os.chmod(self.directory, 0o700)

    def _regular(self, name: str):
        path = self.directory / name
        try:
            mode = path.lstat().st_mode
        except FileNotFoundError:
            return path
        if not stat.S_ISREG(mode):
            raise RuntimeError(f"unsafe storage path: {name}")
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            if not stat.S_ISREG(os.fstat(fd).st_mode):
                raise RuntimeError(f"unsafe storage path: {name}")
            os.fchmod(fd, 0o600)
        finally:
            os.close(fd)
        return path

    @contextmanager
    def process_lock(self):
        import fcntl
        path = self._regular("companion.lock")
        fd = os.open(path, os.O_CREAT | os.O_RDWR | getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            os.fchmod(fd, 0o600)
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            yield
        except BlockingIOError as exc:
            raise RuntimeError("another companion is already running") from exc
        finally:
            os.close(fd)

    def _atomic_write(self, name: str, data: bytes):
        target = self._regular(name)
        fd, temporary = tempfile.mkstemp(prefix=".write-", dir=self.directory)
        try:
            os.fchmod(fd, 0o600)
            with os.fdopen(fd, "wb") as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, target)
            directory_fd = os.open(self.directory, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0))
            try:
                os.fsync(directory_fd)
            finally:
                os.close(directory_fd)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)

    def _read(self, name: str) -> bytes:
        path = self._regular(name)
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            if not stat.S_ISREG(os.fstat(fd).st_mode):
                raise RuntimeError(f"unsafe storage path: {name}")
            with os.fdopen(fd, "rb", closefd=False) as stream:
                return stream.read(os.fstat(fd).st_size + 1)
        finally:
            os.close(fd)

    def identity(self, name: str):
        key_path, cert_path = self._regular("identity-key.pem"), self._regular("identity-cert.pem")
        if key_path.exists() != cert_path.exists():
            raise RuntimeError("incomplete identity; remove both identity files to regenerate")
        if not key_path.exists():
            key = ec.generate_private_key(ec.SECP256R1())
            now = datetime.now(timezone.utc)
            subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Riko Hub Companion")])
            cert = (x509.CertificateBuilder().subject_name(subject).issuer_name(subject)
                    .public_key(key.public_key()).serial_number(x509.random_serial_number())
                    .not_valid_before(now).not_valid_after(now + timedelta(days=3650))
                    .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
                    .sign(key, hashes.SHA256()))
            self._atomic_write("identity-key.pem", key.private_bytes(serialization.Encoding.PEM,
                              serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
            self._atomic_write("identity-cert.pem", cert.public_bytes(serialization.Encoding.PEM))
        key = serialization.load_pem_private_key(self._read("identity-key.pem"), password=None)
        cert = x509.load_pem_x509_certificate(self._read("identity-cert.pem"))
        return key, cert

    def load_trust(self):
        path = self._regular("trusted-phones.json")
        if not path.exists():
            self._atomic_write("trusted-phones.json", b"{}")
            return {}
        data = json.loads(self._read("trusted-phones.json"))
        if not isinstance(data, dict) or len(data) > 8:
            raise RuntimeError("invalid trust store")
        for device_id, entry in data.items():
            try:
                if str(uuid.UUID(device_id)) != device_id:
                    raise ValueError
                if (not isinstance(entry, dict) or not isinstance(entry.get("device_name"), str) or
                        len(entry["device_name"]) > 64 or not isinstance(entry.get("token_hash"), str) or
                        len(entry["token_hash"]) != 64 or any(c not in "0123456789abcdef" for c in entry["token_hash"])):
                    raise ValueError
            except (ValueError, TypeError):
                raise RuntimeError("invalid trust store") from None
        return data

    def save_trust(self, trust: dict):
        self._atomic_write("trusted-phones.json", json.dumps(trust, separators=(",", ":"), sort_keys=True).encode())
