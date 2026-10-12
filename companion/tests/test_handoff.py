"""Source-level tests for the owner-controlled handoff store and console."""

import contextlib
import hashlib
import io
import tempfile
import threading
from types import SimpleNamespace
import unittest
import uuid
from pathlib import Path
from unittest import mock

from riko_companion.handoff import CHUNK_BYTES, Handoff
from riko_companion.lifecycle import _console_handoff
from riko_companion.state import StateError


class Clock:
    def __init__(self):
        self.now = 1000.0

    def __call__(self):
        return self.now


class HandoffTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / "store"
        self.root.mkdir(mode=0o700)
        marker = self.root / ".riko-handoff"
        marker.write_bytes(b"riko-handoff-v1\n")
        marker.chmod(0o600)
        self.clock = Clock()
        self.store = Handoff(self.root, clock=self.clock)
        self.peer = str(uuid.uuid4())
        self.other = str(uuid.uuid4())
        self.token = "a" * 64
        self.store.grant(self.peer, self.token)

    def tearDown(self):
        self.store.close()
        self.temp.cleanup()

    def upload(self, data=b"payload", transfer_id=None):
        transfer_id = transfer_id or str(uuid.uuid4())
        result = self.store.create_upload(self.peer, {
            "protocol": 2, "transfer_id": transfer_id, "display_name": "sample.bin",
            "mime_type": "application/octet-stream", "size_bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
        })
        return transfer_id, result, data

    def test_storage_lifecycle_approval_chunks_and_completed_file(self):
        transfer_id, item, data = self.upload()
        self.assertEqual(item["state"], "awaiting_approval")
        self.assertTrue(self.store.decide(transfer_id, True))
        self.assertEqual(self.store.write_chunk(self.peer, transfer_id, 0, data,
                                                hashlib.sha256(data).hexdigest()), len(data))
        completed = self.store.commit(self.peer, transfer_id, {
            "protocol": 2, "sha256": item["sha256"], "size_bytes": len(data)})
        self.assertEqual(completed["state"], "completed")
        self.assertEqual((self.root / transfer_id / item["display_name"]).read_bytes(), data)

    def test_peer_isolation_and_grant_revocation(self):
        transfer_id, _, _ = self.upload()
        with self.assertRaises(StateError):
            self.store.transfer(self.other, transfer_id)
        self.store.deny_peer(self.peer)
        self.assertNotIn(self.peer, self.store.grants)
        self.assertEqual(self.store.sessions[transfer_id]["state"], "cancelled")

    def test_incoming_offer_requires_and_records_consent(self):
        source = Path(self.temp.name) / "laptop.txt"
        source.write_bytes(b"from laptop")
        item = self.store.offer_file(self.peer, source)
        self.assertEqual(item["state"], "offered")
        with self.assertRaises(StateError):
            self.store.read_chunk(self.peer, item["transfer_id"], 0, 1)
        accepted = self.store.consent(self.peer, item["transfer_id"], {"protocol": 2})
        self.assertEqual(accepted["state"], "ready")
        with self.assertRaises(StateError):
            self.store.consent(self.other, item["transfer_id"], {"protocol": 2})
        self.store.cancel(self.peer, item["transfer_id"], {"protocol": 2})
        self.assertEqual(source.read_bytes(), b"from laptop")

    def test_chunk_size_digest_and_offset_are_bounded(self):
        transfer_id, item, _ = self.upload(b"x" * (CHUNK_BYTES + 1))
        with self.assertRaises(StateError):
            self.store.write_chunk(self.peer, transfer_id, 0, b"x", hashlib.sha256(b"x").hexdigest())
        self.store.decide(transfer_id, True)
        huge = b"x" * (CHUNK_BYTES + 1)
        with self.assertRaises(StateError):
            self.store.write_chunk(self.peer, transfer_id, 0, huge, hashlib.sha256(huge).hexdigest())
        with self.assertRaises(StateError):
            self.store.write_chunk(self.peer, transfer_id, 0, b"x", "0" * 64)
        with self.assertRaises(StateError):
            self.store.write_chunk(self.peer, transfer_id, item["size_bytes"], b"xx", hashlib.sha256(b"xx").hexdigest())

    def test_cancelled_transfer_is_terminal_and_fresh_id_requires_approval(self):
        old_id, _, _ = self.upload()
        self.store.cancel(self.peer, old_id, {"protocol": 2})
        new_id, fresh, _ = self.upload()
        self.assertNotEqual(old_id, new_id)
        self.assertEqual(fresh["state"], "awaiting_approval")
        self.assertFalse(self.store.decide(old_id, True))
        self.assertTrue(self.store.decide(new_id, True))

    def test_text_expires_at_ttl(self):
        item = self.store.offer_text(self.peer, "hello")
        self.assertEqual(len(self.store.list_texts(self.peer)), 1)
        self.clock.now += 301
        self.assertEqual(self.store.list_texts(self.peer), [])
        with self.assertRaises(StateError):
            self.store.get_text(self.peer, item["text_id"])

    def test_workspace_validation_and_console_metadata(self):
        session_id = str(uuid.uuid4())
        body = {"protocol": 2, "session_id": session_id, "title": "Session", "created_at": 10,
                "task": None, "materials": [{"title": "Notes", "reference": "notes.md", "transfer_id": None}],
                "transfer_ids": []}
        self.store.save_workspace(self.peer, body)
        self.assertEqual(self.store.workspace_for_console(session_id)["title"], "Session")
        with self.assertRaises(StateError):
            self.store.save_workspace(self.peer, dict(body, title="bad\x1b[31m"))

    def test_console_cancel_preserves_completed_data_and_displays_workspace_metadata(self):
        state = SimpleNamespace(handoff=self.store, lock=threading.RLock())
        transfer_id, _, _ = self.upload()
        with (mock.patch("builtins.input", return_value=""),
              contextlib.redirect_stdout(io.StringIO()) as output):
            _console_handoff(state, ["cancel", transfer_id])
        self.assertEqual(self.store.sessions[transfer_id]["state"], "cancelled")
        complete_id, item, data = self.upload()
        self.store.decide(complete_id, True)
        self.store.write_chunk(self.peer, complete_id, 0, data, item["sha256"])
        self.store.commit(self.peer, complete_id, {"protocol": 2, "sha256": item["sha256"], "size_bytes": len(data)})
        with contextlib.redirect_stdout(io.StringIO()):
            _console_handoff(state, ["cancel", complete_id])
        self.assertEqual(self.store.sessions[complete_id]["state"], "completed")
        session_id = str(uuid.uuid4())
        body = {"protocol": 2, "session_id": session_id, "title": "safe\x1b[2Jtitle", "created_at": 10,
                "task": None, "materials": [], "transfer_ids": []}
        # Storage rejects control characters, so test the renderer using a valid title and hostile reference.
        body["title"] = "safe"
        body["materials"] = [{"title": "label", "reference": "https://example.test/a", "transfer_id": None}]
        self.store.save_workspace(self.peer, body)
        with contextlib.redirect_stdout(output):
            _console_handoff(state, ["workspace", session_id])
        self.assertIn("reference=https://example.test/a", output.getvalue())

    def test_console_sanitizes_hostile_workspace_metadata(self):
        session_id = str(uuid.uuid4())
        handoff = mock.Mock()
        handoff.workspace_for_console.return_value = {
            "session_id": session_id, "peer_id": self.peer, "title": "bad\x1b[31m", "created_at": 1,
            "task": None, "materials": [{"title": "ok", "reference": "ref\x1b[2J", "transfer_id": None}],
            "transfer_ids": [],
        }
        state = SimpleNamespace(handoff=handoff, lock=threading.RLock())
        with contextlib.redirect_stdout(io.StringIO()) as output:
            _console_handoff(state, ["workspace", session_id])
        rendered = output.getvalue()
        self.assertNotIn("\x1b", rendered)
        self.assertIn("bad?[31m", rendered)
        self.assertIn("reference=ref?[2J", rendered)

