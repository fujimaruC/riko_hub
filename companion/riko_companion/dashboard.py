"""Small curses dashboard for the Riko Hub laptop companion."""

from __future__ import annotations

import os
import unicodedata

import curses


MENU = ("Start Companion", "Connection Status", "Trusted Devices",
        "Network Settings", "Diagnostics", "Exit")
_COLOR = False


def clean(value) -> str:
    """Keep untrusted labels from injecting terminal control sequences."""
    value = str(value)
    return "".join(char if char.isprintable() and char != "\x1b" else " " for char in value)


def _clip(value, cells):
    output, used = [], 0
    for char in clean(value):
        size = 0 if unicodedata.combining(char) else (
            2 if unicodedata.east_asian_width(char) in ("W", "F") else 1)
        if size > cells:
            char, size = "?", 1
        if used + size > cells:
            break
        output.append(char)
        used += size
    return "".join(output)


def _wrap_cells(value, width):
    lines, line, used = [], [], 0
    for char in clean(value):
        size = 0 if unicodedata.combining(char) else (
            2 if unicodedata.east_asian_width(char) in ("W", "F") else 1)
        if size > width:
            char, size = "?", 1
        if size and used + size > width:
            if line:
                lines.append("".join(line))
            line, used = [], 0
        line.append(char)
        used += size
    if line or not lines:
        lines.append("".join(line))
    return lines


def _draw(stdscr, rows, selected=0, scroll=0, snapshot=None):
    stdscr.erase()
    height, width = stdscr.getmaxyx()
    if height < 1 or width < 1:
        return scroll
    usable = width - 1
    def put(y, value, attr=0):
        if 0 <= y < height and usable > 0:
            try:
                clipped = _clip(value, usable)
                stdscr.addnstr(y, 0, clipped, len(clipped), attr)
            except curses.error:
                pass

    color = curses.color_pair(1) if _COLOR else 0
    full_banner = width >= 42 and height >= 16
    if full_banner:
        put(0, "      ╭────◇────╮", color | curses.A_BOLD)
        put(1, "  ────┤ R I K O ├────  H U B", color | curses.A_BOLD)
        put(2, "      ╰────┬────╯   LAPTOP COMPANION · v0.4", color)
    elif height >= 3:
        put(0, "╭─◇ R I K O  H U B ◇──────────────────────────────╮", color | curses.A_BOLD)
        put(1, "│  ░▒▓  L A P T O P   C O M P A N I O N  ▓▒░ v0.4 │", color)
    elif rows is None and height == 2:
        put(0, "RIKO HUB · LAPTOP COMPANION", color | curses.A_BOLD)

    if rows is None:
        items = list(MENU)
        if height <= 2:
            put(height - 1, f"❯ {items[selected]}", curses.A_BOLD)
            stdscr.refresh()
            return scroll
        if full_banner or height >= 13:
            status_keys = (("Server", "server"), ("Local", "local"), ("LAN", "lan"),
                           ("Remote", "remote"), ("Trusted", "trusted"))
            status_start = 4 if full_banner else 2
            for index, (label, key) in enumerate(status_keys, status_start):
                put(index, f"{label:<8} {clean((snapshot or {}).get(key, 'Unknown'))}")
            menu_start = 10 if full_banner else 9
        else:
            menu_start = 3 if height >= 6 else 1
        footer_y = height - 1
        capacity = max(1, footer_y - menu_start)
        if selected < scroll:
            scroll = selected
        elif selected >= scroll + capacity:
            scroll = selected - capacity + 1
        scroll = max(0, min(scroll, max(0, len(items) - capacity)))
        for visible, item_index in enumerate(range(scroll, min(len(items), scroll + capacity))):
            marker = "❯" if item_index == selected else " "
            put(menu_start + visible, f" {marker}  {items[item_index]}",
                curses.A_BOLD if item_index == selected else 0)
        put(footer_y, "↑/↓ move  Enter select  q exit", curses.A_DIM)
    else:
        if height <= 2:
            flat = [line for row in rows for line in _wrap_cells(row, max(1, usable))]
            content = flat[min(scroll, max(0, len(flat) - 1))] if flat else ""
            put(0, content)
            if height == 2:
                put(1, "q/Esc back", curses.A_DIM)
            stdscr.refresh()
            return min(scroll, max(0, len(flat) - 1))
        content_start = 3 if height >= 5 else 1
        footer_y = height - 1
        available = max(1, footer_y - content_start)
        flat = []
        for row in rows:
            flat.extend(_wrap_cells(row, max(1, usable)))
        scroll = max(0, min(scroll, max(0, len(flat) - available)))
        for offset, line in enumerate(flat[scroll:scroll + available]):
            put(content_start + offset, line)
        put(footer_y, "↑/↓ scroll  r refresh  q back", curses.A_DIM)
    stdscr.refresh()
    return scroll


def _init_colors():
    global _COLOR
    _COLOR = False
    try:
        if (os.environ.get("NO_COLOR") is None and os.environ.get("TERM") != "dumb" and
                getattr(curses, "has_colors", lambda: False)()):
            curses.start_color()
            curses.use_default_colors()
            curses.init_pair(1, curses.COLOR_CYAN, -1)
            _COLOR = True
    except curses.error:
        _COLOR = False


def _menu(stdscr, snapshot, handlers):
    _init_colors()
    try:
        curses.curs_set(0)
    except curses.error:
        pass
    stdscr.keypad(True)
    selected = scroll = 0
    while True:
        scroll = _draw(stdscr, None, selected, scroll, snapshot)
        key = stdscr.getch()
        if key in (ord("q"), 27):
            return "exit"
        if key in (curses.KEY_UP, ord("k")):
            selected = (selected - 1) % len(MENU)
        elif key in (curses.KEY_DOWN, ord("j")):
            selected = (selected + 1) % len(MENU)
        elif key in (curses.KEY_ENTER, 10, 13):
            action = MENU[selected]
            if action == "Exit":
                return "exit"
            if action == "Start Companion":
                return "start"
            if action == "Network Settings":
                return "settings"
            rows = handlers[action]()
            if rows is not None:
                _detail(stdscr, action, rows, handlers)


def _detail(stdscr, title, rows, handlers):
    scroll = 0
    while True:
        scroll = _draw(stdscr, [title, "─" * 20, *rows], scroll=scroll)
        key = stdscr.getch()
        if key in (ord("q"), 27):
            return
        if key in (curses.KEY_UP, ord("k")):
            scroll = max(0, scroll - 1)
        elif key in (curses.KEY_DOWN, ord("j")):
            scroll += 1
        elif key in (ord("r"), ord("R")):
            rows = handlers[title]()
            scroll = 0


def run(snapshot, handlers):
    """Return an action after curses has restored the user's terminal."""
    if not os.isatty(0) or not os.isatty(1):
        return None
    if os.environ.get("TERM") == "dumb":
        return "fallback"
    try:
        return curses.wrapper(_menu, snapshot, handlers)
    except curses.error:
        print("Terminal does not support the interactive dashboard; use rikohub status.")
        return None
