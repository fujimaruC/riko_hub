"""Read-only and secret-redaction checks for Companion storage."""

import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from riko_companion import lifecycle
from riko_companion.storage import Storage


class ReadOnlyStorageTests(unittest.TestCase):
    def test_missing_data_directory_is_not_created_by_read_only_listing(self):
        with tempfile.TemporaryDirectory() as temporary:
            data_dir = Path(temporary) / "not-created"
            with (mock.patch.object(Path, "mkdir", side_effect=AssertionError("mkdir")),
                  mock.patch("riko_companion.storage.os.chmod", side_effect=AssertionError("chmod")),
                  mock.patch.object(Storage, "_atomic_write", side_effect=AssertionError("write"))):
                self.assertEqual(Storage(data_dir, read_only=True).devices(), [])
            self.assertFalse(data_dir.exists())

    def test_devices_expose_safe_metadata_without_token_hash(self):
        device_id = "01234567-89ab-cdef-0123-456789abcdef"
        token_hash = "a" * 64
        with tempfile.TemporaryDirectory() as temporary:
            data_dir = Path(temporary)
            os.chmod(data_dir, 0o700)
            (data_dir / "trusted-phones.json").write_text(json.dumps({device_id: {
                "device_name": "Phone\u001b[31m", "token_hash": token_hash}}), encoding="utf-8")
            os.chmod(data_dir / "trusted-phones.json", 0o600)
            devices = Storage(data_dir, read_only=True).devices()
        self.assertEqual(devices, [(device_id, "Phone?[31m")])
        self.assertNotIn(token_hash, repr(devices))

    def test_unlocked_stale_runtime_metadata_does_not_claim_running(self):
        with tempfile.TemporaryDirectory() as temporary:
            data_dir = Path(temporary)
            os.chmod(data_dir, 0o700)
            (data_dir / "companion.lock").touch(mode=0o600)
            (data_dir / "companion-runtime.json").write_text(json.dumps({
                "version": 1, "bind": "192.168.1.25", "port": 8765, "name": "Riko"}),
                encoding="utf-8")
            os.chmod(data_dir / "companion-runtime.json", 0o600)
            result = Storage(data_dir, read_only=True).probe_process()
        self.assertEqual(result, ("Stopped", None))

    def test_held_process_lock_reports_only_its_valid_runtime_metadata(self):
        with tempfile.TemporaryDirectory() as temporary:
            data_dir = Path(temporary) / "companion"
            store = Storage(data_dir)
            runtime = {"version": 1, "bind": "192.168.1.25", "port": 8765, "name": "Riko"}
            runtime_path = data_dir / "companion-runtime.json"
            runtime_path.write_text(json.dumps(runtime), encoding="utf-8")
            os.chmod(runtime_path, 0o600)
            with store.process_lock():
                result = Storage(data_dir, read_only=True).probe_process()
        self.assertEqual(result, ("Running (process lock held)", runtime))

    def test_status_does_not_probe_listener_unless_requested(self):
        with tempfile.TemporaryDirectory() as temporary:
            data_dir = Path(temporary) / "not-created"
            with mock.patch("riko_companion.lifecycle.socket.create_connection") as connect:
                result = lifecycle.status(data_dir)
        connect.assert_not_called()
        self.assertEqual(result["server"], "Stopped")
        self.assertFalse(data_dir.exists())
