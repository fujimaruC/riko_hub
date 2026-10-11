"""HTTPS protocol endpoint. The raw accept loop never performs TLS handshakes."""

from __future__ import annotations

import json
import http.client
import socket
import ssl
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

from .state import StateError

MAX_BODY = 8192
MAX_HEADERS = 8192
REQUEST_RECEIPT_SECONDS = 10


class LimitedHeaderReader:
    def __init__(self, stream):
        self.stream, self.used = stream, 0

    def readline(self, size=-1):
        remaining = MAX_HEADERS - self.used
        data = self.stream.readline(min(size, remaining + 1) if size >= 0 else remaining + 1)
        self.used += len(data)
        if self.used > MAX_HEADERS:
            raise http.client.LineTooLong("header section too large")
        return data


class BoundedTLSServer(HTTPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, address, handler, context, state, bind_and_activate=True):
        self.context, self.state = context, state
        self.slots = threading.BoundedSemaphore(8)
        self.lifecycle_lock = threading.Lock()
        self.closing = False
        self.connections = set()
        self.workers = set()
        self.deadlines = {}
        super().__init__(address, handler, bind_and_activate)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        thread = threading.Thread(target=self._serve_tls, args=(request, client_address), daemon=True)
        with self.lifecycle_lock:
            if self.closing:
                self.slots.release()
                self.shutdown_request(request)
                return
            self.workers = {worker for worker in self.workers if worker.is_alive()}
            self.connections.add(request)
            self.workers.add(thread)
            try:
                thread.start()
            except RuntimeError:
                self.connections.discard(request)
                self.workers.discard(thread)
                self.slots.release()
                self.shutdown_request(request)

    def _register_tls(self, raw, tls):
        with self.lifecycle_lock:
            if self.closing:
                return False
            self.connections.discard(raw)
            self.connections.add(tls)
            return True

    def _arm_deadline(self, connection):
        with self.lifecycle_lock:
            if self.closing or connection not in self.connections:
                return
            timer = threading.Timer(REQUEST_RECEIPT_SECONDS, self._expire_request, (connection,))
            timer.daemon = True
            self.deadlines[connection] = timer
            try:
                timer.start()
            except RuntimeError:
                self.deadlines.pop(connection, None)
                raise

    def _expire_request(self, connection):
        with self.lifecycle_lock:
            timer = self.deadlines.get(connection)
            if timer is None or connection not in self.connections:
                return
            self.connections.discard(connection)
        try:
            connection.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        try:
            connection.close()
        except OSError:
            pass
        finally:
            with self.lifecycle_lock:
                if self.deadlines.get(connection) is timer:
                    self.deadlines.pop(connection, None)

    def cancel_request_deadline(self, connection):
        with self.lifecycle_lock:
            timer = self.deadlines.pop(connection, None)
        if timer:
            timer.cancel()

    def _serve_tls(self, raw, client_address):
        tls = None
        try:
            raw.settimeout(5)
            tls = self.context.wrap_socket(raw, server_side=True, do_handshake_on_connect=False)
            if not self._register_tls(raw, tls):
                return
            tls.settimeout(5)
            tls.do_handshake()
            tls.settimeout(None)
            self._arm_deadline(tls)
            self.finish_request(tls, client_address)
        except (OSError, ssl.SSLError, TimeoutError):
            pass
        except Exception:
            # Do not expose tracebacks or request data to the terminal.
            pass
        finally:
            if tls is not None:
                try:
                    tls.close()
                except OSError:
                    pass
            self.shutdown_request(raw)
            with self.lifecycle_lock:
                self.connections.discard(raw)
                if tls is not None:
                    self.connections.discard(tls)
                    timer = self.deadlines.pop(tls, None)
                    if timer:
                        timer.cancel()
            self.slots.release()

    def server_close(self):
        with self.lifecycle_lock:
            self.closing = True
            connections = list(self.connections)
            workers = list(self.workers)
            timers = list(self.deadlines.values())
            self.connections.clear()
            self.deadlines.clear()
        for timer in timers:
            timer.cancel()
        for timer in timers:
            timer.join()
        for connection in connections:
            try:
                connection.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            try:
                connection.close()
            except OSError:
                pass
        super().server_close()
        current = threading.current_thread()
        for worker in workers:
            if worker is not current:
                worker.join()

class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "RikoHub"
    sys_version = ""

    def log_message(self, _format, *_args):
        return

    def parse_request(self):
        if len(self.raw_requestline) > 2048:
            self._send(400, {"error": "invalid_request"})
            return False
        original = self.rfile
        self.rfile = LimitedHeaderReader(original)
        try:
            parsed = super().parse_request()
        finally:
            self.rfile = original
        if not parsed:
            return False
        headers = list(self.headers.raw_items())
        if len(headers) > 64 or sum(len(k) + len(v) + 4 for k, v in headers) > 8192:
            self._send(400, {"error": "invalid_request"})
            return False
        if self.command != "POST":
            self.server.cancel_request_deadline(self.connection)
        return True

    @property
    def state(self):
        return self.server.state

    def _send(self, status, payload):
        body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(body)
        self.close_connection = True

    def _success(self, **extra):
        return {"protocol": 1, "peer_id": self.state.peer_id, "name": self.state.name,
                "capabilities": ["presence"], **extra}

    def _protocol(self, body=None):
        raw = self.headers.get("X-Riko-Protocol") if body is None else body.get("protocol")
        valid = raw == "1" if body is None else type(raw) is int and raw == 1
        if not valid:
            self._send(426, {"error": "protocol_mismatch"})
            return False
        return True

    def _read_json(self):
        lengths = self.headers.get_all("Content-Length", [])
        if len(lengths) != 1 or self.headers.get_all("Transfer-Encoding"):
            self.server.cancel_request_deadline(self.connection)
            raise StateError("invalid_request")
        raw_length = lengths[0]
        if not raw_length.isascii() or not raw_length.isdecimal():
            self.server.cancel_request_deadline(self.connection)
            raise StateError("invalid_request")
        if raw_length != "0" and raw_length.startswith("0"):
            self.server.cancel_request_deadline(self.connection)
            raise StateError("invalid_request")
        if len(raw_length) > 5:
            self.server.cancel_request_deadline(self.connection)
            raise StateError("too_large")
        length = int(raw_length)
        if length > MAX_BODY:
            self.server.cancel_request_deadline(self.connection)
            raise StateError("too_large")
        raw = self.rfile.read(length)
        self.server.cancel_request_deadline(self.connection)
        if len(raw) != length:
            raise StateError("invalid_request")
        try:
            def unique_object(pairs):
                result = {}
                for key, value in pairs:
                    if key in result:
                        raise ValueError("duplicate JSON key")
                    result[key] = value
                return result
            value = json.loads(raw, object_pairs_hook=unique_object)
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError):
            raise StateError("invalid_request") from None
        if not isinstance(value, dict):
            raise StateError("invalid_request")
        return value

    def _auth(self):
        if not self._protocol():
            return None
        header = self.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            raise StateError("unauthorized")
        return header[7:]

    def _route(self, method):
        if self.state.shutting_down:
            self._send(503, {"error": "shutting_down"})
            return False
        if self.path not in {"/v1/hello", "/v1/pair", "/v1/status", "/v1/disconnect", "/v1/trust"}:
            self._send(404, {"error": "not_found"})
            return False
        if (method, self.path) not in {
            ("GET", "/v1/hello"), ("POST", "/v1/pair"), ("GET", "/v1/status"),
            ("POST", "/v1/disconnect"), ("DELETE", "/v1/trust"),
        }:
            self._send(404, {"error": "not_found"})
            return False
        return True

    def _error(self, error):
        status = {
            "invalid_request": 400, "unauthorized": 401, "bad_pairing_key": 403,
            "approval_denied": 403, "occupied": 409, "too_large": 413,
            "resource_limit": 429, "shutting_down": 503,
        }.get(error.code, 400)
        self._send(status, {"error": error.code})

    def do_GET(self):
        try:
            if not self._route("GET"):
                return
            if self.path == "/v1/hello":
                self._send(200, self._success())
                return
            token = self._auth()
            if token is not None:
                self.state.authenticated(token)
                self._send(200, self._success(status="connected"))
        except StateError as error:
            self._error(error)

    def do_POST(self):
        try:
            if not self._route("POST"):
                return
            body = self._read_json()
            if not self._protocol(body):
                return
            if self.path == "/v1/pair":
                required = {"protocol", "pairing_secret", "device_id", "device_name"}
                if set(body) != required or not all(isinstance(body[k], str) for k in required - {"protocol"}):
                    raise StateError("invalid_request")
                pending = self.state.begin_pairing(body["pairing_secret"], body["device_id"], body["device_name"])
                print(f"Pair request {pending.request_id} from {safe_terminal(pending.device_name)}; type approve or deny {pending.request_id}", flush=True)
                token = self.state.finish_pairing(pending)
                self._send(200, self._success(token=token))
                return
            if self.path == "/v1/disconnect":
                if set(body) != {"protocol"}:
                    raise StateError("invalid_request")
                token = self._auth()
                if token is not None:
                    self.state.disconnect(token)
                    self._send(200, self._success())
        except StateError as error:
            self._error(error)
        except Exception:
            self._send(503, {"error": "storage_failure"})

    def do_DELETE(self):
        try:
            if not self._route("DELETE"):
                return
            token = self._auth()
            if token is not None:
                self.state.revoke(token)
                self._send(200, self._success())
        except StateError as error:
            self._error(error)
        except Exception:
            self._send(503, {"error": "storage_failure"})

    def _unsupported(self):
        self._send(404, {"error": "not_found"})

    do_PUT = _unsupported
    do_PATCH = _unsupported
    do_OPTIONS = _unsupported
    do_HEAD = _unsupported


def safe_terminal(value):
    return "".join(char if char.isprintable() and char not in "\x1b\x7f" else "?" for char in value)[:64]
