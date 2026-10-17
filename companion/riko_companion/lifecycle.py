"""Companion startup, process state and read-only laptop diagnostics."""

from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import os
import socket
import ssl
import shlex
import stat
import threading
import uuid
from pathlib import Path

from .storage import DEFAULT_DATA_DIR, Storage
from .state import StateError


def private_address(value):
    try:
        address = ipaddress.ip_address(value)
    except ValueError:
        raise argparse.ArgumentTypeError("bind must be an RFC1918 IPv4 address") from None
    if address.version != 4 or not any(address in ipaddress.ip_network(net) for net in
                                      ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")):
        raise argparse.ArgumentTypeError("bind must be an RFC1918 IPv4 address")
    return str(address)


def display_name(value):
    if (not value or len(value) > 64 or len(value.encode("utf-8")) > 255 or
            any(not c.isprintable() or c in "\x1b\x7f" for c in value)):
        raise argparse.ArgumentTypeError("name must be 1 to 64 safe printable characters")
    return value


def arguments(argv=None):
    parser = argparse.ArgumentParser(description="Riko Hub LAN companion")
    parser.add_argument("--bind", required=True, type=private_address)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--name", type=display_name, default="Riko Laptop")
    parser.add_argument("--data-dir", type=Path, default=DEFAULT_DATA_DIR)
    args = parser.parse_args(argv)
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    return args


def _handoff_config_valid(value):
    if not isinstance(value, dict) or set(value) != {"directory", "retention_days"}:
        return False
    directory, days = value["directory"], value["retention_days"]
    return (isinstance(directory, str) and Path(directory).is_absolute() and
            type(days) is int and 1 <= days <= 30)


def configure_handoff(data_dir, directory, retention_days=7):
    root = Path(directory).expanduser().absolute()
    data_root = Path(data_dir).expanduser().resolve()
    home = Path.home().resolve()
    if (type(retention_days) is not int or not 1 <= retention_days <= 30 or
            root.resolve(strict=False) != root or root in (Path("/"), home, data_root) or
            data_root in root.parents):
        raise ValueError("unsafe handoff directory or retention policy")
    root.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    existed = True
    try:
        root.mkdir(mode=0o700)
        existed = False
    except FileExistsError:
        pass
    info = root.lstat()
    if (not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or
            stat.S_IMODE(info.st_mode) & 0o077):
        raise ValueError("handoff directory must be a private owner directory")
    marker = root / ".riko-handoff"
    if existed and not marker.exists() and any(root.iterdir()):
        raise ValueError("handoff directory must be empty or already managed")
    try:
        fd = os.open(marker, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
    except FileExistsError:
        try:
            marker_info = marker.lstat()
        except OSError:
            raise ValueError("handoff directory marker is invalid") from None
        if (not stat.S_ISREG(marker_info.st_mode) or marker_info.st_uid != os.getuid() or
                stat.S_IMODE(marker_info.st_mode) & 0o077 or marker.is_symlink() or
                marker.read_bytes() != b"riko-handoff-v1\n"):
            raise ValueError("handoff directory marker is invalid")
        allowed = {".riko-handoff", "grants.json", "workspaces.json"}
        for item in root.iterdir():
            if (item.name not in allowed and not _is_handoff_session(item.name, item) and
                    not _is_handoff_temp(item.name, ".write-")):
                raise ValueError("handoff directory contains unexpected files")
    else:
        with os.fdopen(fd, "wb") as stream:
            stream.write(b"riko-handoff-v1\n")
            stream.flush()
            os.fsync(stream.fileno())
    store = Storage(data_dir)
    with store.process_lock():
        store._atomic_write("handoff.json", json.dumps(
            {"directory": str(root), "retention_days": retention_days}, separators=(",", ":")).encode())
    return root


def handoff_status(data_dir=DEFAULT_DATA_DIR):
    config = Storage(data_dir, read_only=True).read_json("handoff.json", _handoff_config_valid)
    if not config:
        return "Not configured"
    return f"Configured at {config['directory']} · {config['retention_days']}-day retention"


def _is_handoff_session(name, path):
    try:
        return (str(uuid.UUID(name)) == name and stat.S_ISDIR(path.lstat().st_mode) and
                path.lstat().st_uid == os.getuid() and not (stat.S_IMODE(path.lstat().st_mode) & 0o077))
    except (ValueError, OSError):
        return False


def _is_handoff_temp(name, prefix):
    try:
        return name.startswith(prefix) and str(uuid.UUID(name[len(prefix):])) == name[len(prefix):]
    except ValueError:
        return False


def run(argv=None):
    args = arguments(argv)
    # These optional runtime dependencies are not needed by dashboard help or diagnostics.
    from cryptography.hazmat.primitives import serialization
    from zeroconf import ServiceInfo, Zeroconf
    from .server import BoundedTLSServer, Handler
    from .state import CompanionState

    storage = Storage(args.data_dir)
    with storage.process_lock():
        previous_runtime = storage._regular("companion-runtime.json")
        if previous_runtime.exists():
            previous_runtime.unlink()
        peer_id_path = storage._regular("laptop-id")
        if peer_id_path.exists():
            peer_id = storage._read("laptop-id").decode("ascii").strip()
            uuid.UUID(peer_id)
        else:
            peer_id = str(uuid.uuid4())
            storage._atomic_write("laptop-id", peer_id.encode("ascii"))
        _, cert = storage.identity(args.name)
        cert_file = storage.directory / "identity-cert.pem"
        pin = hashlib.sha256(cert.public_bytes(serialization.Encoding.DER)).hexdigest()
        trust = storage.load_trust()
        handoff_config = storage.read_json("handoff.json", _handoff_config_valid)
        handoff = None
        if handoff_config:
            from .handoff import Handoff
            handoff = Handoff(Path(handoff_config["directory"]), handoff_config["retention_days"])
            handoff.cleanup()
        state = CompanionState(peer_id, args.name, pin, trust, storage.save_trust, handoff=handoff)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        context.load_cert_chain(str(cert_file), str(storage.directory / "identity-key.pem"))
        service_type = "_rikohub._tcp.local."
        service_name = f"Riko Hub Companion {peer_id}._rikohub._tcp.local."
        info = ServiceInfo(service_type, service_name, addresses=[socket.inet_aton(args.bind)],
                           port=args.port, properties={b"id": peer_id.encode(), b"v": b"1",
                           b"caps": b"presence", b"name": args.name.encode("utf-8")})
        server = None
        zeroconf = None
        serving = None
        registered = False
        started = False
        metadata_written = False
        try:
            zeroconf = Zeroconf(interfaces=[args.bind])
            server = BoundedTLSServer((args.bind, args.port), Handler, context, state, handoff=handoff)
            serving = threading.Thread(target=server.serve_forever, daemon=True)
            zeroconf.register_service(info)
            registered = True
            serving.start()
            started = True
            storage._atomic_write("companion-runtime.json", json.dumps(
                {"version": 1, "bind": args.bind, "port": args.port, "name": args.name},
                separators=(",", ":")).encode("utf-8"))
            metadata_written = True
            print(f"Riko Hub companion listening on {args.bind}:{args.port}; type help for commands.", flush=True)
            console(state)
        except KeyboardInterrupt:
            pass
        finally:
            state.stop()
            if server is not None:
                if started:
                    server.shutdown()
                server.server_close()
            if started:
                serving.join()
            if zeroconf is not None:
                try:
                    if registered:
                        zeroconf.unregister_service(info)
                finally:
                    zeroconf.close()
            if metadata_written:
                runtime_path = storage._regular("companion-runtime.json")
                if runtime_path.exists():
                    runtime_path.unlink()
            if handoff is not None:
                handoff.close()


def console(state):
    from .server import safe_terminal
    while not state.shutting_down:
        try:
            line = input("companion> ")
        except EOFError:
            return
        try:
            parts = shlex.split(line)
        except ValueError:
            print("invalid command quoting")
            continue
        command = parts[0] if parts else ""
        if command == "help":
            print("pair | approve <request id> | deny <request id> | status | revoke <phone uuid> | handoff help | quit")
        elif command == "pair":
            try:
                print(state.create_pairing_key(), flush=True)
            except Exception:
                print("pairing key unavailable while a request is pending or service is stopping")
        elif command == "approve":
            print("approved" if len(parts) == 2 and state.decide(parts[1], True) else "no such pending request")
        elif command == "deny":
            print("denied" if len(parts) == 2 and state.decide(parts[1], False) else "no such pending request")
        elif command == "status":
            print(state.status())
            for phone_id, phone_name, presence in state.phone_status():
                print(f"{phone_id}  {safe_terminal(phone_name)}  {presence}")
        elif command == "revoke":
            try:
                if len(parts) != 2:
                    raise ValueError
                device_id = str(uuid.UUID(parts[1]))
                try:
                    print("revoked" if state.revoke_id(device_id) else "phone is not trusted")
                except Exception:
                    print("could not persist trust change")
            except ValueError:
                print("invalid phone UUID")
        elif command == "handoff":
            _console_handoff(state, parts[1:])
        elif command == "quit":
            return
        elif command:
            print("unknown command; type help")


def _terminal_text(value):
    return "".join(c if c in "\n\t" or (c.isprintable() and c not in "\x1b\x7f") else "?"
                   for c in value)


def _console_handoff(state, args):
    from .server import safe_terminal
    handoff = state.handoff
    if not args or args[0] == "help":
        print("handoff allow <phoneUUID> | deny-peer <phoneUUID> | list | approve <transferUUID> | deny <transferUUID> | cancel <transferUUID> | offer <phoneUUID> <path> | text <phoneUUID> | texts | show <textUUID> | workspaces | workspace <sessionUUID> | cleanup")
        return
    if handoff is None:
        print("Handoff is not configured. Run rikohub handoff configure.")
        return
    action = args[0]
    try:
        if action == "allow" and len(args) == 2:
            peer = str(uuid.UUID(args[1]))
            with state.lock:
                trust = state.trust.get(peer)
                if not trust:
                    raise ValueError
                token_hash = trust["token_hash"]
            if input(f"Allow handoff for trusted phone {peer}? Type yes: ").strip() != "yes":
                print("unchanged")
                return
            with state.lock:
                current = state.trust.get(peer)
                if not current or current["token_hash"] != token_hash:
                    raise StateError("unauthorized")
                handoff.grant(peer, token_hash)
            print("handoff enabled for trusted phone")
        elif action == "deny-peer" and len(args) == 2:
            peer = str(uuid.UUID(args[1]))
            with state.lock:
                handoff.deny_peer(peer)
            print("handoff access removed")
        elif action == "list" and len(args) == 1:
            with state.lock:
                items = [x for x in handoff.sessions.values()]
                for item in items[-32:]:
                    view = handoff._view(item)
                    print(f"{view['transfer_id']}  {safe_terminal(view['display_name'])}  {view['direction']}  {view['state']}  {view['offset']}/{view['size_bytes']} bytes  available={view['available']}")
                    if view["available"] and view["direction"] == "phone_to_laptop":
                        print(f"  stored: {_terminal_text(str(handoff._session_dir(view['transfer_id']) / item['result_name']))}")
                if not items:
                    print("no transfers")
        elif action in ("approve", "deny") and len(args) == 2:
            with state.lock:
                result = handoff.decide(args[1], action == "approve")
            print("updated" if result else "no pending transfer")
        elif action == "cancel" and len(args) == 2:
            transfer_id = str(uuid.UUID(args[1]))
            with state.lock:
                item = handoff.sessions.get(transfer_id)
                if item is None:
                    raise StateError("not_found")
                handoff._cancel_generated(item)
            print("cancelled" if item["state"] == "cancelled" else "completed files are retained")
        elif action == "offer" and len(args) >= 3:
            peer = str(uuid.UUID(args[1]))
            with state.lock:
                handoff.require_peer(peer, state.trust)
                item = handoff.offer_file(peer, " ".join(args[2:]))
            print(f"offered {safe_terminal(item['display_name'])} as {item['transfer_id']}")
        elif action == "text" and len(args) == 2:
            peer = str(uuid.UUID(args[1]))
            with state.lock:
                handoff.require_peer(peer, state.trust)
                token_hash = state.trust[peer]["token_hash"]
            value = input("Text to offer (not stored on disk): ")
            if len(value.encode("utf-8")) > 2048:
                print("text exceeds 2048 UTF-8 bytes")
                return
            if input("Send this text to the trusted phone? Type yes: ").strip() != "yes":
                print("text offer cancelled")
                return
            with state.lock:
                current = state.trust.get(peer)
                if not current or current["token_hash"] != token_hash:
                    raise StateError("unauthorized")
                handoff.require_peer(peer, state.trust)
                item = handoff.offer_text(peer, value)
            print(f"text offer {item['text_id']} created")
        elif action == "texts" and len(args) == 1:
            with state.lock:
                items = handoff.inbox_text()
            for item in items:
                print(f"{item['text_id']}  {item['created_at']}  {item['size_bytes']} bytes")
            if not items:
                print("no pending text")
        elif action == "show" and len(args) == 2:
            text_id = str(uuid.UUID(args[1]))
            if input("Display and consume this received text? Type yes: ").strip() != "yes":
                print("text unchanged")
                return
            with state.lock:
                value = handoff.show_inbox_text(text_id)
                print(_terminal_text(value))
                handoff.consume_inbox_text(text_id)
        elif action == "workspaces" and len(args) == 1:
            with state.lock:
                items = handoff.list_workspaces_for_console()
            for item in items:
                print(f"{item['session_id']}  {safe_terminal(item['title'])}  available={item['available']}")
            if not items:
                print("no workspace handoffs")
        elif action == "workspace" and len(args) == 2:
            session_id = str(uuid.UUID(args[1]))
            with state.lock:
                item = handoff.workspace_for_console(session_id)
            print(f"{item['session_id']}  {_terminal_text(item['title'])}  created={item['created_at']}")
            if item["task"]:
                task = item["task"]
                deadline = task["deadline"] if task["deadline"] is not None else "none"
                print(f"  task: {_terminal_text(task['title'])}  sync_id={task['sync_id']}  deadline={deadline}  updated={task['updated_at']}  fingerprint={task['fingerprint']}")
            for material in item["materials"]:
                print(f"  material: {_terminal_text(material['title'])}  reference={_terminal_text(material['reference'])}  transfer={material['transfer_id'] or 'none'}")
            print("  transfers: " + (", ".join(item["transfer_ids"]) if item["transfer_ids"] else "none"))
        elif action == "cleanup" and len(args) == 1:
            if input("Remove expired and retained generated handoff data? Type yes: ").strip() != "yes":
                print("cleanup cancelled")
                return
            with state.lock:
                removed = handoff.cleanup()
            print(f"removed {removed} expired transfer sessions")
        else:
            print("unknown handoff command; type handoff help")
    except (ValueError, StateError):
        print("handoff request is invalid, unavailable, or not authorized")
    except Exception:
        print("handoff action failed; details suppressed")


def _valid_settings(value):
    if not isinstance(value, dict) or set(value) != {"bind", "port", "name"}:
        return False
    try:
        if value["bind"] is not None and private_address(value["bind"]) != value["bind"]:
            return False
        display_name(value["name"])
    except (argparse.ArgumentTypeError, TypeError, AttributeError):
        return False
    return isinstance(value["port"], int) and not isinstance(value["port"], bool) and 1 <= value["port"] <= 65535


def load_settings(data_dir=DEFAULT_DATA_DIR):
    store = Storage(data_dir, read_only=True)
    value = store.read_json("network.json", _valid_settings)
    return value if value is not None else {"bind": None, "port": 8765, "name": "Riko Laptop"}


def save_settings(data_dir, bind, port, name):
    value = {"bind": bind, "port": port, "name": name}
    if not _valid_settings(value):
        raise ValueError("invalid network settings")
    store = Storage(data_dir)
    with store.process_lock():
        store._atomic_write("network.json", json.dumps(value, separators=(",", ":")).encode("utf-8"))


def status(data_dir=DEFAULT_DATA_DIR, probe=False):
    try:
        store = Storage(data_dir, read_only=True)
        server, runtime = store.probe_process()
        try:
            settings = load_settings(data_dir)
            settings_error = False
        except (OSError, RuntimeError, ValueError):
            settings, settings_error = {"bind": None, "port": 8765, "name": "Riko Laptop"}, True
        failures = ["Could not read network settings"] if settings_error else []
        if server.startswith("Running"):
            listener = (f"{runtime['bind']}:{runtime['port']}" if runtime
                        else "Unknown (no runtime metadata)")
            local = "Not checked"
            if probe and runtime:
                try:
                    with socket.create_connection((runtime["bind"], runtime["port"]), timeout=0.4):
                        local = "TCP reachable (TLS/auth not checked)"
                except OSError:
                    local = "TCP unreachable"
                    failures.append("Active listener TCP connection failed")
        else:
            listener = (f"Not listening; configured {settings['bind']}:{settings['port']}"
                        if settings["bind"] else "Not configured")
            local = "Not checked"
        try:
            trusted = f"{len(store.devices())} devices"
        except (OSError, RuntimeError, ValueError):
            trusted = "Unknown"
            failures.append("Could not safely read trusted-device metadata")
        return {"server": server, "listener": listener, "local": local,
                "lan": "Phone reachability not verified", "remote": "Not implemented",
                "trusted": trusted, "failure": "; ".join(failures) if failures else "None known"}
    except (OSError, RuntimeError, ValueError):
        return {"server": "Unknown", "listener": "Unknown", "local": "Not checked",
                "lan": "Phone reachability not verified", "remote": "Not implemented",
                "trusted": "Unknown", "failure": "Could not safely inspect Companion state"}


def devices(data_dir=DEFAULT_DATA_DIR):
    return Storage(data_dir, read_only=True).devices()


def doctor(data_dir=DEFAULT_DATA_DIR):
    result = [f"Python: {__import__('sys').version.split()[0]}"]
    try:
        import importlib.util
        result.extend(f"{package} module: {'found (import not checked)' if importlib.util.find_spec(package) else 'missing'}"
                      for package in ("riko_companion", "cryptography", "zeroconf"))
    except (ImportError, ValueError):
        result.append("Package availability: Unknown")
    try:
        store = Storage(data_dir, read_only=True)
        result.append(f"Configuration directory: {'safe and available' if store.directory.exists() else 'not created'}")
    except (OSError, RuntimeError, ValueError):
        result.append("Configuration directory: unavailable or unsafe")
        store = None
    if store is not None:
        try:
            result.append(f"Certificate: {store.certificate_status()}")
        except ImportError:
            result.append("Certificate: Cannot inspect; cryptography is missing")
        except (OSError, RuntimeError, ValueError):
            result.append("Certificate: Unknown (unsafe or unreadable state)")
    current = status(data_dir)
    result.append(f"Companion: {current['server']}")
    result.append(f"Listener: {current['listener']}")
    result.append("LAN: Phone reachability not verified")
    result.append("Remote transport: Not implemented")
    return result
