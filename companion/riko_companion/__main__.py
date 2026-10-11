from __future__ import annotations

import argparse
import hashlib
import ipaddress
import signal
import socket
import ssl
import sys
import threading
import uuid
from pathlib import Path

from cryptography.hazmat.primitives import serialization
from zeroconf import ServiceInfo, Zeroconf

from .server import BoundedTLSServer, Handler, safe_terminal
from .state import CompanionState
from .storage import Storage


def private_address(value):
    address = ipaddress.ip_address(value)
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
    parser.add_argument("--data-dir", type=Path, default=Path("~/.local/share/riko-hub-companion"))
    args = parser.parse_args(argv)
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    return args


def run(argv=None):
    args = arguments(argv)
    storage = Storage(args.data_dir)
    with storage.process_lock():
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
        state = CompanionState(peer_id, args.name, pin, trust, storage.save_trust)
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
        try:
            zeroconf = Zeroconf(interfaces=[args.bind])
            server = BoundedTLSServer((args.bind, args.port), Handler, context, state)
            serving = threading.Thread(target=server.serve_forever, daemon=True)
            zeroconf.register_service(info)
            registered = True
            serving.start()
            started = True
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


def console(state):
    while not state.shutting_down:
        try:
            line = input("companion> ")
        except EOFError:
            return
        command, _, argument = line.strip().partition(" ")
        if command == "help":
            print("pair | approve <request id> | deny <request id> | status | revoke <phone uuid> | quit")
        elif command == "pair":
            try:
                print(state.create_pairing_key(), flush=True)
            except Exception:
                print("pairing key unavailable while a request is pending or service is stopping")
        elif command == "approve":
            print("approved" if state.decide(argument.strip(), True) else "no such pending request")
        elif command == "deny":
            print("denied" if state.decide(argument.strip(), False) else "no such pending request")
        elif command == "status":
            print(state.status())
            for phone_id, phone_name, presence in state.phone_status():
                print(f"{phone_id}  {safe_terminal(phone_name)}  {presence}")
        elif command == "revoke":
            device_id = argument.strip()
            try:
                device_id = str(uuid.UUID(device_id))
                try:
                    print("revoked" if state.revoke_id(device_id) else "phone is not trusted")
                except Exception:
                    print("could not persist trust change")
            except ValueError:
                print("invalid phone UUID")
        elif command == "quit":
            return
        elif command:
            print("unknown command; type help")


def _interrupt(_signal, _frame):
    raise KeyboardInterrupt


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, _interrupt)
    try:
        run()
    except (OSError, RuntimeError, ValueError) as error:
        print(f"Riko Hub companion: {safe_terminal(str(error))}", file=sys.stderr)
        raise SystemExit(1)
