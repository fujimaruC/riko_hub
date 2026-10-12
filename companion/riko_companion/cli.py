"""User-facing CLI; server lifecycle and persistent state live in lifecycle."""

from __future__ import annotations

import argparse
import ipaddress
import signal
import sys
from pathlib import Path

MENU_ITEMS = ("Start Companion", "Connection Status", "Trusted Devices",
              "Network Settings", "Handoff Help", "Diagnostics", "Exit")


def _parser():
    parser = argparse.ArgumentParser(prog="rikohub", description="Riko Hub Laptop Companion")
    parser.add_argument("--data-dir", type=Path, default=Path("~/.local/share/riko-hub-companion"),
                        help="companion data directory (also accepted after a command)")
    commands = parser.add_subparsers(dest="command")
    for name, help_text in (("start", "start the foreground companion server"),
                            ("status", "show companion and connection status"),
                            ("devices", "list trusted devices"),
                            ("doctor", "run read-only diagnostics")):
        sub = commands.add_parser(name, help=help_text)
        sub.add_argument("--data-dir", type=Path, default=argparse.SUPPRESS,
                         help="companion data directory")
        if name == "start":
            sub.add_argument("--bind", help="RFC1918 IPv4 listener address")
            sub.add_argument("--port", type=int)
            sub.add_argument("--name", help="companion display name")
    handoff = commands.add_parser("handoff", help="configure the owner-controlled handoff area")
    handoff.add_argument("--data-dir", type=Path, default=argparse.SUPPRESS,
                         help="companion data directory")
    handoff_commands = handoff.add_subparsers(dest="handoff_command", required=True)
    configure = handoff_commands.add_parser("configure", help="enable file and text handoff")
    configure.add_argument("--data-dir", type=Path, default=argparse.SUPPRESS,
                           help="companion data directory")
    configure.add_argument("--directory", type=Path, required=True,
                           help="dedicated empty private transfer directory")
    configure.add_argument("--retention-days", type=int, default=7, choices=range(1, 31),
                           help="retain completed transfer data for 1 to 30 days")
    return parser


def _backend():
    # Keep --help usable before optional server dependencies are installed.
    from . import lifecycle
    return lifecycle


def _safe(value):
    return "".join(c if c.isprintable() and c != "\x1b" else " " for c in str(value))


def _status(data_dir, probe=False):
    try:
        return _backend().status(data_dir, probe=probe)
    except Exception:
        return {"server": "Unknown", "listener": "Unknown", "local": "Not checked",
                "lan": "Not checked", "remote": "Not configured", "trusted": "Unknown",
                "failure": "Status could not be read safely"}


def _status_lines(status):
    labels = (("server", "Server"), ("listener", "Listener"), ("local", "Local"),
              ("lan", "LAN"), ("remote", "Remote"), ("trusted", "Trusted"),
              ("failure", "Known failure"))
    return [f"{label}: {_safe(status.get(key, 'Unknown'))}" for key, label in labels
            if status.get(key) not in (None, "", "None")]


def _report_error(action):
    print(f"rikohub: {action} failed; details were suppressed for safety", file=sys.stderr)


def _server_running(status):
    return "running" in str(status.get("server", "")).lower()


def _choose_settings(data_dir, args):
    backend = _backend()
    settings = backend.load_settings(data_dir)
    bind = args.bind if args.bind is not None else settings.get("bind")
    port = args.port if args.port is not None else settings.get("port", 8765)
    name = args.name if args.name is not None else settings.get("name", "Riko Laptop")
    if bind is None:
        raise ValueError("No listener address is configured")
    address = ipaddress.ip_address(backend.private_address(bind))
    if type(port) is not int or not 1 <= port <= 65535:
        raise ValueError("Invalid listener port")
    return str(address), port, backend.display_name(name)


def _confirm_start(bind, port):
    print(f"This starts the Companion on {bind}:{port} for devices on your LAN.")
    try:
        answer = input(f"Type {bind} to confirm: ").strip()
    except EOFError:
        return False
    return answer == bind


def _start(data_dir, args):
    status = _status(data_dir)
    if _server_running(status):
        print("Companion is already running. Use its original terminal for pairing approvals.")
        return
    try:
        bind, port, name = _choose_settings(data_dir, args)
    except Exception:
        print("No valid private listener is configured. Use Network Settings or start --bind.")
        return 1
    if not sys.stdin.isatty() or not sys.stdout.isatty() or not _confirm_start(bind, port):
        print("Start cancelled; LAN exposure requires confirmation.")
        return 1
    argv = ["--bind", bind, "--port", str(port), "--name", name, "--data-dir", str(data_dir)]
    try:
        _backend().run(argv)
    except KeyboardInterrupt:
        print("Companion stopped.")
    except Exception:
        _report_error("server start")
        return 1
    return 0


def _terminate(_signal, _frame):
    raise SystemExit(143)


def _settings(data_dir):
    backend = _backend()
    try:
        current = backend.load_settings(data_dir)
        print("Network Settings (private IPv4 only; blank keeps the current value, 'clear' disables listener)")
        def ask(prompt):
            value = input(prompt).strip()
            if value.lower() in ("q", "\x1b"):
                raise KeyboardInterrupt
            return value
        bind_input = ask(f"Bind address [{current.get('bind') or 'unset'}] (q cancels): ")
        port_input = ask(f"Port [{current.get('port', 8765)}]: ")
        name_input = ask(f"Name [{current.get('name', 'Riko Laptop')}]: ")
        bind = current.get("bind") if not bind_input else (None if bind_input.lower() == "clear" else bind_input)
        port = int(port_input) if port_input else current.get("port", 8765)
        name = name_input or current.get("name", "Riko Laptop")
        if bind is not None:
            address = backend.private_address(bind)
            if ask(f"Allow LAN devices to reach {address}? Type yes to save: ") != "yes":
                print("Settings unchanged.")
                return
            name = backend.display_name(name)
        backend.save_settings(data_dir, bind, port, name)
        print("Settings saved.")
    except (EOFError, KeyboardInterrupt):
        print("Settings unchanged.")
    except Exception:
        _report_error("settings update")


def _dashboard(data_dir):
    try:
        from . import dashboard
    except ImportError:
        dashboard = None
    def fallback():
        print("Riko Hub · Laptop Companion")
        while True:
            for index, item in enumerate(MENU_ITEMS, 1):
                print(f"{index}. {item}")
            try:
                choice = input("Select a number, q or Esc to exit: ").strip()
            except (EOFError, KeyboardInterrupt):
                return
            if choice.lower() in ("q", "\x1b"):
                return
            try:
                index = int(choice) - 1
                if not 0 <= index < len(MENU_ITEMS):
                    raise ValueError
            except ValueError:
                print("Choose one of the listed numbers.")
                continue
            item = MENU_ITEMS[index]
            if item == "Exit":
                return
            if item == "Start Companion":
                result = _start(data_dir, argparse.Namespace(bind=None, port=None, name=None))
            elif item == "Network Settings":
                _settings(data_dir)
                result = 0
            elif item == "Connection Status":
                print("\n".join(_status_lines(_status(data_dir, probe=True))))
                result = 0
            elif item == "Trusted Devices":
                print("\n".join(_device_lines(data_dir)))
                result = 0
            elif item == "Handoff Help":
                print("\n".join(_handoff_help_lines(data_dir)))
                result = 0
            else:
                print("\n".join(_diagnostic_lines(data_dir)))
                result = 0
            if result == 1:
                print("This action did not complete.")
            try:
                input("Press Enter to return to the menu...")
            except (EOFError, KeyboardInterrupt):
                return

    while True:
        snapshot = _status(data_dir)
        if dashboard is None:
            fallback()
            return
        try:
            action = dashboard.run(snapshot, {
                "Connection Status": lambda: _status_lines(_status(data_dir, probe=True)),
                "Trusted Devices": lambda: _device_lines(data_dir),
                "Handoff Help": lambda: _handoff_help_lines(data_dir),
                "Diagnostics": lambda: _diagnostic_lines(data_dir),
            })
        except KeyboardInterrupt:
            return
        except (ImportError, OSError):
            fallback()
            return
        if action in ("fallback", None):
            fallback()
            return
        if action == "start":
            result = _start(data_dir, argparse.Namespace(bind=None, port=None, name=None))
            if not _pause("Press Enter to return to the dashboard..."):
                return
        elif action == "settings":
            _settings(data_dir)
            if not _pause("Press Enter to return to the dashboard..."):
                return
        else:
            return


def _pause(prompt):
    try:
        input(prompt)
        return True
    except (EOFError, KeyboardInterrupt):
        return False


def _device_lines(data_dir):
    try:
        devices = _backend().devices(data_dir)
        return [f"{_safe(name)} · {_safe(device_id)}" for device_id, name in devices] or ["No trusted devices"]
    except Exception:
        return ["Trusted device metadata could not be read safely"]


def _handoff_help_lines(data_dir):
    try:
        status = _safe(_backend().handoff_status(data_dir))
    except Exception:
        status = "Status could not be read safely"
    return [f"Handoff: {status}", "Configure: rikohub handoff configure --directory ~/Riko-Handoff",
            "Start the Companion, then allow this phone with: handoff allow <phoneUUID>",
            "Live console: handoff list | approve <transferUUID> | deny <transferUUID> | cancel <transferUUID>",
            "Live console: handoff offer <phoneUUID> <path> | text <phoneUUID> | texts | show <textUUID>",
            "Workspaces: handoff workspaces | workspace <sessionUUID>",
            "Received files stay in the configured directory; cancel leaves completed files in place."]


def _diagnostic_lines(data_dir):
    try:
        return [_safe(line) for line in _backend().doctor(data_dir)] or ["No diagnostic findings"]
    except Exception:
        return ["Diagnostics could not be completed safely"]


def main(argv=None):
    parser = _parser()
    args = parser.parse_args(argv)
    data_dir = args.data_dir.expanduser()
    previous_handler = signal.getsignal(signal.SIGTERM)
    signal.signal(signal.SIGTERM, _terminate)
    try:
        try:
            if args.command is None:
                if not sys.stdin.isatty() or not sys.stdout.isatty():
                    print("Riko Hub Laptop Companion")
                    print("\n".join(_status_lines(_status(data_dir))))
                    print("Run 'rikohub --help' for commands; an interactive terminal is required for the dashboard.")
                    return 0
                _dashboard(data_dir)
            elif args.command == "start":
                return _start(data_dir, args)
            elif args.command == "status":
                print("\n".join(_status_lines(_status(data_dir, probe=True))))
            elif args.command == "devices":
                print("\n".join(_device_lines(data_dir)))
            elif args.command == "doctor":
                print("\n".join(_diagnostic_lines(data_dir)))
            elif args.command == "handoff" and args.handoff_command == "configure":
                if not sys.stdin.isatty() or not sys.stdout.isatty():
                    print("Handoff configuration requires an interactive terminal.")
                    return 1
                selected = args.directory.expanduser().absolute()
                print("This configures a dedicated owner-only handoff directory.")
                if input(f"Type the full path {selected} to confirm: ").strip() != str(selected):
                    print("Handoff configuration cancelled.")
                    return 1
                try:
                    root = _backend().configure_handoff(data_dir, selected, args.retention_days)
                    print(f"Handoff configured at {root} with {args.retention_days}-day retention.")
                except Exception:
                    _report_error("handoff configuration")
                    return 1
        except KeyboardInterrupt:
            print("\nCancelled.")
        return 0
    finally:
        signal.signal(signal.SIGTERM, previous_handler)


if __name__ == "__main__":
    raise SystemExit(main())
