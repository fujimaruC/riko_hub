"""Owner-only identity, configuration and trust storage."""

from __future__ import annotations

import json
import ipaddress
import os
import stat
import tempfile
import uuid
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from pathlib import Path

DEFAULT_DATA_DIR = Path("~/.local/share/riko-hub-companion")


class Storage:
    def __init__(self, directory: Path, read_only: bool = False):
        self.directory = Path(directory).expanduser()
        self.read_only = read_only
        if read_only:
            self._validate_directory()
        else:
            self._ensure_directory()

    def _validate_directory(self, check_permissions=True):
        try:
            info = self.directory.lstat()
        except FileNotFoundError:
            return
        if (not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or
                (check_permissions and stat.S_IMODE(info.st_mode) & 0o077)):
            raise RuntimeError("data directory is not a safe owner-only directory")

    def _ensure_directory(self):
        self.directory.mkdir(mode=0o700, parents=True, exist_ok=True)
        self._validate_directory(check_permissions=False)
        os.chmod(self.directory, 0o700)

    def _regular(self, name: str):
        path = self.directory / name
        try:
            info = path.lstat()
        except FileNotFoundError:
            return path
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid():
            raise RuntimeError(f"unsafe storage path: {name}")
        if self.read_only and stat.S_IMODE(info.st_mode) & 0o077:
            raise RuntimeError(f"unsafe storage permissions: {name}")
        fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        try:
            opened = os.fstat(fd)
            if not stat.S_ISREG(opened.st_mode) or opened.st_uid != os.getuid():
                raise RuntimeError(f"unsafe storage path: {name}")
            if self.read_only and stat.S_IMODE(opened.st_mode) & 0o077:
                raise RuntimeError(f"unsafe storage permissions: {name}")
            if not self.read_only:
                os.fchmod(fd, 0o600)
        finally:
            os.close(fd)
        return path

    def _safe_exists(self, name: str):
        path = self.directory / name
        try:
            info = path.lstat()
        except FileNotFoundError:
            return False
        if (not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or
                (self.read_only and stat.S_IMODE(info.st_mode) & 0o077)):
            raise RuntimeError(f"unsafe storage path: {name}")
        return True

    @contextmanager
    def process_lock(self):
        if self.read_only:
            raise RuntimeError("read-only storage cannot acquire a process lock")
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

    def probe_process(self):
        """Return (state, runtime metadata); metadata is trusted only under a held lock."""
        import fcntl
        path = self._regular("companion.lock")
        if not path.exists():
            return "Stopped", None
        try:
            fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        except FileNotFoundError:
            return "Stopped", None
        try:
            opened = os.fstat(fd)
            if (not stat.S_ISREG(opened.st_mode) or opened.st_uid != os.getuid() or
                    (self.read_only and stat.S_IMODE(opened.st_mode) & 0o077)):
                raise RuntimeError("unsafe storage path: companion.lock")
            try:
                fcntl.flock(fd, fcntl.LOCK_SH | fcntl.LOCK_NB)
            except BlockingIOError:
                try:
                    metadata = self.read_json("companion-runtime.json", _runtime_valid)
                except (OSError, RuntimeError, ValueError):
                    metadata = None
                return "Running (process lock held)", metadata
            return "Stopped", None
        finally:
            os.close(fd)

    def _atomic_write(self, name: str, data: bytes):
        if self.read_only:
            raise RuntimeError("read-only storage cannot write")
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
            info = os.fstat(fd)
            if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid():
                raise RuntimeError(f"unsafe storage path: {name}")
            if self.read_only and stat.S_IMODE(info.st_mode) & 0o077:
                raise RuntimeError(f"unsafe storage permissions: {name}")
            with os.fdopen(fd, "rb", closefd=False) as stream:
                return stream.read(info.st_size + 1)
        finally:
            os.close(fd)

    def read_json(self, name: str, validator):
        path = self._regular(name)
        if not path.exists():
            return None
        try:
            value = json.loads(self._read(name))
        except (json.JSONDecodeError, UnicodeDecodeError):
            raise RuntimeError(f"invalid {name}") from None
        if not validator(value):
            raise RuntimeError(f"invalid {name}")
        return value

    def identity(self, name: str):
        from cryptography import x509
        from cryptography.hazmat.primitives import hashes, serialization
        from cryptography.hazmat.primitives.asymmetric import ec
        from cryptography.x509.oid import NameOID
        key_path, cert_path = self._regular("identity-key.pem"), self._regular("identity-cert.pem")
        if key_path.exists() != cert_path.exists():
            raise RuntimeError("incomplete identity; remove both identity files to regenerate")
        if not key_path.exists():
            if self.read_only:
                raise RuntimeError("identity is not configured")
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

    def certificate_status(self):
        from cryptography import x509
        cert_exists = self._safe_exists("identity-cert.pem")
        key_exists = self._safe_exists("identity-key.pem")
        if not cert_exists and not key_exists:
            return "Not configured"
        if cert_exists != key_exists:
            return "Incomplete identity"
        try:
            cert = x509.load_pem_x509_certificate(self._read("identity-cert.pem"))
            now = datetime.now(timezone.utc)
            before = cert.not_valid_before_utc
            after = cert.not_valid_after_utc
            return "Valid" if before <= now <= after else "Expired or not yet valid"
        except (ValueError, TypeError):
            return "Invalid certificate"

    def load_trust(self):
        data = self.read_json("trusted-phones.json", _trust_valid)
        if data is None:
            if not self.read_only:
                self._atomic_write("trusted-phones.json", b"{}")
            return {}
        return data

    def devices(self):
        return [(device_id, "".join(c if c.isprintable() and c not in "\x1b\x7f" else "?"
                                    for c in entry["device_name"])[:64])
                for device_id, entry in self.load_trust().items()]

    def save_trust(self, trust: dict):
        if not _trust_valid(trust):
            raise RuntimeError("invalid trust store")
        self._atomic_write("trusted-phones.json", json.dumps(trust, separators=(",", ":"), sort_keys=True).encode())


def _trust_valid(data):
    if not isinstance(data, dict) or len(data) > 8:
        return False
    for device_id, entry in data.items():
        try:
            if not isinstance(device_id, str) or str(uuid.UUID(device_id)) != device_id:
                return False
            if (not isinstance(entry, dict) or not isinstance(entry.get("device_name"), str) or
                    len(entry["device_name"]) > 64 or not isinstance(entry.get("token_hash"), str) or
                    len(entry["token_hash"]) != 64 or any(c not in "0123456789abcdef" for c in entry["token_hash"])):
                return False
        except (ValueError, TypeError, AttributeError):
            return False
    return True


def _runtime_valid(value):
    if (not isinstance(value, dict) or set(value) != {"version", "bind", "port", "name"} or
            value.get("version") != 1 or not isinstance(value.get("bind"), str) or
            not isinstance(value.get("port"), int) or isinstance(value.get("port"), bool) or
            not 1 <= value["port"] <= 65535 or not isinstance(value.get("name"), str) or
            not 1 <= len(value["name"]) <= 64 or
            any(not c.isprintable() or c in "\x1b\x7f" for c in value["name"])):
        return False
    try:
        address = ipaddress.ip_address(value["bind"])
        return address.version == 4 and any(address in ipaddress.ip_network(net) for net in
                                            ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"))
    except ValueError:
        return False
