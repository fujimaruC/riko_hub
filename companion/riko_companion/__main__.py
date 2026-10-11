"""Backward-compatible foreground Companion entry point."""

from __future__ import annotations

import signal
import sys

from .lifecycle import run


def _interrupt(_signal, _frame):
    raise KeyboardInterrupt


def _safe_error(value):
    return "".join(character if character.isprintable() and character not in "\x1b\x7f" else "?"
                   for character in str(value))[:240]


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, _interrupt)
    try:
        run()
    except (OSError, RuntimeError, ValueError) as error:
        print(f"Riko Hub companion: {_safe_error(error)}", file=sys.stderr)
        raise SystemExit(1)
