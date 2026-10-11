"""Source-level tests for the user-facing command and terminal renderer."""

import argparse
import contextlib
import io
import os
import signal
import tomllib
import unittest
from pathlib import Path
from unittest import mock

from riko_companion import cli, dashboard, lifecycle


class CliTests(unittest.TestCase):
    def test_help_does_not_load_optional_runtime(self):
        with mock.patch.object(cli, "_backend", side_effect=AssertionError("loaded")):
            with contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(SystemExit) as raised:
                    cli.main(["--help"])
        self.assertEqual(raised.exception.code, 0)

    def test_unknown_command_is_rejected(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as raised:
                cli.main(["not-a-command"])
        self.assertEqual(raised.exception.code, 2)

    def test_non_tty_no_argument_path_reports_status_without_starting(self):
        stdin, stdout = mock.Mock(isatty=lambda: False), mock.Mock(isatty=lambda: False)
        status = {"server": "Stopped", "listener": "Not configured", "trusted": "0 devices"}
        with (mock.patch.object(cli.sys, "stdin", stdin),
              mock.patch.object(cli.sys, "stdout", stdout),
              mock.patch.object(cli, "_status", return_value=status) as read_status,
              mock.patch.object(cli, "_dashboard") as open_dashboard):
            self.assertEqual(cli.main([]), 0)
        read_status.assert_called_once()
        open_dashboard.assert_not_called()

    def test_sigterm_handler_covers_dashboard_and_is_restored(self):
        original = object()
        installed = []
        stdin, stdout = mock.Mock(isatty=lambda: True), mock.Mock(isatty=lambda: True)
        def dashboard_call(_data_dir):
            self.assertIs(installed[-1], cli._terminate)
        with (mock.patch.object(cli.signal, "getsignal", return_value=original),
              mock.patch.object(cli.signal, "signal", side_effect=lambda _sig, handler: installed.append(handler)),
              mock.patch.object(cli.sys, "stdin", stdin),
              mock.patch.object(cli.sys, "stdout", stdout),
              mock.patch.object(cli, "_dashboard", side_effect=dashboard_call)):
            self.assertEqual(cli.main([]), 0)
        self.assertEqual(installed, [cli._terminate, original])

    def test_sigterm_handler_uses_non_keyboard_exit_code(self):
        with self.assertRaises(SystemExit) as raised:
            cli._terminate(signal.SIGTERM, None)
        self.assertEqual(raised.exception.code, 143)

    def test_already_running_start_never_calls_server(self):
        backend = mock.Mock()
        with (mock.patch.object(cli, "_status", return_value={"server": "Running (process lock held)"}),
              mock.patch.object(cli, "_backend", return_value=backend),
              mock.patch("builtins.print")):
            cli._start(Path("/unused"), argparse.Namespace(bind=None, port=None, name=None))
        backend.run.assert_not_called()

    def test_public_and_wildcard_listener_addresses_are_rejected(self):
        for bind in ("0.0.0.0", "8.8.8.8", "::1"):
            backend = mock.Mock(load_settings=lambda _path: {
                "bind": None, "port": 8765, "name": "Riko"},
                private_address=lifecycle.private_address,
                display_name=lifecycle.display_name)
            with self.subTest(bind=bind), mock.patch.object(cli, "_backend", return_value=backend):
                with self.assertRaises(argparse.ArgumentTypeError):
                    cli._choose_settings(Path("/unused"), argparse.Namespace(
                        bind=bind, port=None, name=None))

    def test_installed_console_entry_points_to_cli_main(self):
        project = Path(__file__).parents[1] / "pyproject.toml"
        with project.open("rb") as stream:
            metadata = tomllib.load(stream)
        self.assertEqual(metadata["project"]["scripts"]["rikohub"], "riko_companion.cli:main")

    def test_status_requests_local_tcp_probe(self):
        store = mock.Mock()
        store.probe_process.return_value = ("Running (process lock held)", {
            "bind": "192.168.1.25", "port": 8765, "name": "Riko"})
        store.devices.return_value = []
        with (mock.patch.object(lifecycle, "Storage", return_value=store),
              mock.patch.object(lifecycle, "load_settings", return_value={
                  "bind": "192.168.1.25", "port": 8765, "name": "Riko"}),
              mock.patch("riko_companion.lifecycle.socket.create_connection") as connect):
            result = lifecycle.status(Path("/unused"), probe=True)
        connect.assert_called_once_with(("192.168.1.25", 8765), timeout=0.4)
        self.assertEqual(result["local"], "TCP reachable (TLS/auth not checked)")


class DashboardTests(unittest.TestCase):
    class Screen:
        def __init__(self, size, keys=()):
            self.size, self.keys, self.writes = size, iter(keys), []

        def getmaxyx(self):
            return self.size

        def erase(self):
            pass

        def addnstr(self, y, x, value, limit, attr):
            self.writes.append((y, x, value, limit, attr))

        def refresh(self):
            pass

        def keypad(self, _enabled):
            pass

        def getch(self):
            return next(self.keys)

    def test_menu_navigation_reaches_exit(self):
        screen = self.Screen((12, 48), [dashboard.curses.KEY_DOWN] * 5 + [10])
        with (mock.patch.object(dashboard, "_init_colors"),
              mock.patch.object(dashboard.curses, "has_colors", return_value=False),
              mock.patch.object(dashboard.curses, "curs_set")):
            result = dashboard._menu(screen, {}, {})
        self.assertEqual(result, "exit")

    def test_menu_navigation_handles_two_line_terminal(self):
        screen = self.Screen((2, 8), [dashboard.curses.KEY_DOWN] * 5 + [10])
        with (mock.patch.object(dashboard, "_init_colors"),
              mock.patch.object(dashboard.curses, "has_colors", return_value=False),
              mock.patch.object(dashboard.curses, "curs_set")):
            result = dashboard._menu(screen, {}, {})
        self.assertEqual(result, "exit")
        self.assertTrue(all(0 <= y < 2 for y, *_rest in screen.writes))

    def test_narrow_screen_writes_stay_inside_reported_bounds(self):
        screen = self.Screen((4, 12))
        with mock.patch.object(dashboard.curses, "has_colors", return_value=False):
            dashboard._draw(screen, ["a long detail row that wraps", "second"], scroll=0)
        self.assertTrue(screen.writes)
        self.assertTrue(all(0 <= y < 4 and limit <= 11 for y, _x, _v, limit, _a in screen.writes))
        self.assertTrue(all(len(value) <= limit for _y, _x, value, limit, _a in screen.writes))

    def test_cell_wrapper_preserves_wide_characters(self):
        lines = dashboard._wrap_cells("甲乙丙丁", 3)
        self.assertEqual("".join(lines), "甲乙丙丁")
        self.assertTrue(all(sum(2 if ord(char) > 255 else 1 for char in line) <= 3 for line in lines))

    def test_tiny_detail_keeps_content_visible(self):
        screen = self.Screen((2, 12))
        dashboard._draw(screen, ["Important detail"])
        self.assertIn((0, 0, "Important d", 11, 0), screen.writes)
        self.assertTrue(any(y == 1 and "back" in value for y, _x, value, _limit, _attr in screen.writes))

    def test_combining_marks_do_not_truncate_visible_detail(self):
        screen = self.Screen((4, 4))
        dashboard._draw(screen, ["a\u0301bc"])
        self.assertIn((1, 0, "a\u0301bc", 4, 0), screen.writes)

    def test_no_color_disables_color_initialization(self):
        with (mock.patch.dict(os.environ, {"NO_COLOR": "1", "TERM": "xterm"}),
              mock.patch.object(dashboard.curses, "has_colors", return_value=True),
              mock.patch.object(dashboard.curses, "start_color") as start,
              mock.patch.object(dashboard.curses, "init_pair") as init_pair):
            dashboard._init_colors()
        start.assert_not_called()
        init_pair.assert_not_called()
