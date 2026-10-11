import hashlib
import io
import socket
import threading
from email.message import Message
import unittest
import uuid
from unittest.mock import Mock, patch

from riko_companion.server import BoundedTLSServer, Handler, REQUEST_RECEIPT_SECONDS
from riko_companion.state import CompanionState, StateError


class Clock:
    def __init__(self):
        self.now = 10.0

    def __call__(self):
        return self.now


class CompanionStateTests(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        self.saved = []
        self.state = CompanionState(str(uuid.uuid4()), "Riko Laptop", "a" * 64, {},
                                   lambda trust: self.saved.append(dict(trust)), self.clock)

    def start_request(self):
        payload = self.state.create_pairing_key()
        secret = payload.rsplit(":", 1)[1]
        return self.state.begin_pairing(secret, str(uuid.uuid4()), "Phone")

    def test_pairing_key_expires_and_is_one_use(self):
        payload = self.state.create_pairing_key()
        secret = payload.rsplit(":", 1)[1]
        self.clock.now += 120
        with self.assertRaises(StateError):
            self.state.begin_pairing(secret, str(uuid.uuid4()), "Phone")
        request = self.start_request()
        self.assertTrue(self.state.decide(request.request_id, False))
        self.assertFalse(self.state.decide(request.request_id, True))
        with self.assertRaises(StateError):
            self.state.begin_pairing(self.state.pairing.secret, str(uuid.uuid4()), "Phone")
        with self.assertRaises(StateError):
            self.state.finish_pairing(request)

    def test_approval_deadline_is_checked_when_deciding(self):
        request = self.start_request()
        self.clock.now += 60
        self.assertFalse(self.state.decide(request.request_id, True))
        with self.assertRaises(StateError):
            self.state.finish_pairing(request, timeout=0)

    def test_approval_persists_hash_and_token_can_be_revoked(self):
        request = self.start_request()
        self.assertTrue(self.state.decide(request.request_id, True))
        token = self.state.finish_pairing(request)
        phone_id = request.device_id
        self.assertEqual(hashlib.sha256(token.encode()).hexdigest(),
                         self.state.trust[phone_id]["token_hash"])
        self.assertNotEqual(token, self.state.trust[phone_id]["token_hash"])
        self.assertEqual(phone_id, self.state.authenticated(token))
        self.assertEqual("connected", self.state.status())
        self.assertEqual(phone_id, self.state.revoke(token))
        with self.assertRaises(StateError):
            self.state.authenticated(token)
        self.assertEqual({}, self.state.trust)

    def test_trust_limit_rejects_ninth_device(self):
        self.state.trust.update({str(uuid.uuid4()): {"device_name": "Phone", "token_hash": "0" * 64}
                                 for _ in range(8)})
        payload = self.state.create_pairing_key()
        with self.assertRaises(StateError) as error:
            self.state.begin_pairing(payload.rsplit(":", 1)[1], str(uuid.uuid4()), "Phone")
        self.assertEqual("occupied", error.exception.code)

    def test_revoke_restores_memory_if_persistence_fails(self):
        request = self.start_request()
        self.state.decide(request.request_id, True)
        token = self.state.finish_pairing(request)
        self.state.persist = lambda _trust: (_ for _ in ()).throw(OSError("disk"))
        with self.assertRaises(OSError):
            self.state.revoke(token)
        self.assertEqual(request.device_id, self.state.authenticated(token))

    def test_shutdown_cancels_pending_and_blocks_new_key(self):
        request = self.start_request()
        self.state.stop()
        self.assertFalse(self.state.decide(request.request_id, True))
        with self.assertRaises(StateError):
            self.state.finish_pairing(request, timeout=0)
        with self.assertRaises(StateError):
            self.state.create_pairing_key()

    def test_status_includes_phone_uuid_for_local_revocation(self):
        request = self.start_request()
        self.state.decide(request.request_id, True)
        self.state.finish_pairing(request)
        self.assertEqual([(request.device_id, "Phone", "disconnected")], self.state.phone_status())

    def test_protocol_header_is_string_and_json_protocol_is_integer(self):
        handler = object.__new__(Handler)
        handler.headers = Message()
        handler.responses = []
        handler._send = lambda status, body: handler.responses.append((status, body))
        handler.headers["X-Riko-Protocol"] = "1"
        self.assertTrue(handler._protocol())
        handler.headers.replace_header("X-Riko-Protocol", "2")
        self.assertFalse(handler._protocol())
        self.assertEqual(426, handler.responses[-1][0])
        self.assertTrue(handler._protocol({"protocol": 1}))
        self.assertFalse(handler._protocol({"protocol": True}))

    def test_json_body_limit_and_duplicate_lengths(self):
        handler = object.__new__(Handler)
        handler.server = Mock()
        handler.connection = object()
        handler.headers = Message()
        handler.headers["Content-Length"] = "8193"
        handler.rfile = io.BytesIO(b"")
        with self.assertRaises(StateError) as error:
            handler._read_json()
        self.assertEqual("too_large", error.exception.code)
        handler.headers = Message()
        handler.headers["Content-Length"] = "2"
        handler.headers["Content-Length"] = "2"
        with self.assertRaises(StateError) as error:
            handler._read_json()
        self.assertEqual("invalid_request", error.exception.code)
        self.assertEqual(2, handler.server.cancel_request_deadline.call_count)
        handler.server.cancel_request_deadline.assert_called_with(handler.connection)

    def test_authenticated_state_operations_reject_shutdown(self):
        phone_id, token = str(uuid.uuid4()), "b" * 64
        self.state.trust[phone_id] = {"device_name": "Phone", "token_hash": hashlib.sha256(token.encode()).hexdigest()}
        self.state.stop()
        for operation in (lambda: self.state.authenticated(token), lambda: self.state.disconnect(token),
                          lambda: self.state.revoke(token), lambda: self.state.revoke_id(phone_id)):
            with self.assertRaises(StateError) as error:
                operation()
            self.assertEqual("shutting_down", error.exception.code)


class FakeSocket:
    def __init__(self):
        self.shutdown_mode = None
        self.closed = False

    def shutdown(self, mode):
        self.shutdown_mode = mode

    def close(self):
        self.closed = True


class FakeTimer:
    made = []

    def __init__(self, interval, function, args):
        self.interval, self.function, self.args = interval, function, args
        self.cancelled = False
        FakeTimer.made.append(self)

    def start(self):
        pass

    def cancel(self):
        self.cancelled = True

    def join(self):
        pass


class ServerLifecycleTests(unittest.TestCase):
    def make_server(self):
        server = object.__new__(BoundedTLSServer)
        server.lifecycle_lock = threading.Lock()
        server.closing = False
        server.connections = set()
        server.workers = set()
        server.deadlines = {}
        return server

    def test_request_deadline_is_absolute_ten_seconds(self):
        FakeTimer.made.clear()
        server, connection = self.make_server(), FakeSocket()
        server.connections.add(connection)
        with patch("riko_companion.server.threading.Timer", FakeTimer):
            server._arm_deadline(connection)
        timer = FakeTimer.made[-1]
        self.assertEqual(REQUEST_RECEIPT_SECONDS, timer.interval)
        timer.function(*timer.args)
        self.assertEqual(socket.SHUT_RDWR, connection.shutdown_mode)
        self.assertTrue(connection.closed)
        self.assertNotIn(connection, server.connections)

    def test_server_close_cancels_deadlines_closes_sockets_and_joins_workers(self):
        class Worker:
            joined = False

            def join(self):
                self.joined = True

        server, connection, timer, worker = self.make_server(), FakeSocket(), FakeTimer(1, lambda: None, ()), Worker()
        server.connections.add(connection)
        server.deadlines[connection] = timer
        server.workers.add(worker)
        with patch("riko_companion.server.HTTPServer.server_close", lambda _server: None):
            server.server_close()
        self.assertTrue(server.closing)
        self.assertTrue(timer.cancelled)
        self.assertTrue(connection.closed)
        self.assertTrue(worker.joined)


if __name__ == "__main__":
    unittest.main()
