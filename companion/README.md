# Riko Hub Linux Companion

The Python 3.11+ Companion provides the v0.4 laptop protocol on an explicitly selected private IPv4 address. It serves HTTPS and advertises `_rikohub._tcp.local.` on that interface. It does not require root, install a system service, or start automatically.

## Install the user command on Arch Linux

Run these commands from the repository checkout. They install into the project's virtual environment and create a user-local command without touching Arch's system Python:

```bash
cd /home/riko/Projects/riko_hub/companion
python3 -m venv .venv
.venv/bin/python -m pip install -e .
mkdir -p "$HOME/.local/bin"
ln -s "$PWD/.venv/bin/rikohub" "$HOME/.local/bin/rikohub"
```

The `ln -s` step intentionally fails if `~/.local/bin/rikohub` already exists; inspect or remove that entry yourself before retrying. In Fish, add the directory to the user path in a separate Fish command:

```fish
fish_add_path -U ~/.local/bin
```

Open a new shell (or source the relevant shell configuration), then run `rikohub --help`. The symlink points into this checkout's `.venv`; keep the checkout and virtual environment in place. To update the install after source changes, run `.venv/bin/python -m pip install -e .` again from `companion/`.

## Use

Run `rikohub` for the interactive dashboard, or `rikohub status`, `rikohub devices`, and `rikohub doctor` for direct views. Running status is detected through the lock in the selected Companion data directory, so it makes no claim about services using other data directories. `rikohub status` checks local TCP reachability when a process is detected, but phone/LAN end-to-end reachability always remains unverified and TCP does not verify TLS. `rikohub start --bind ADDRESS` starts the foreground server after confirming the selected private LAN address. Obtain the laptop's current addresses with `ip -brief -4 address` and use the actual RFC1918 address on the phone's LAN; the dashboard's Network Settings can save it after explicit confirmation. Wildcard and public binds are rejected. The server prompt remains in the terminal so pairing approvals and revocation stay available.

Start also accepts `--port` (default 8765) and `--name` (default `Riko Laptop`). Every command accepts `--data-dir`, defaulting to `~/.local/share/riko-hub-companion`; no bind address is selected automatically.

At the server prompt, `help` lists commands. Use `pair` and give the displayed one-time payload directly to the phone; approve or deny the request ID shown when it arrives. `status` shows recent authenticated presence and `revoke <phone-uuid>` removes trust. `quit` or Ctrl+C stops the foreground server, returning to the dashboard when started there or to the shell when started directly. Do not copy pairing payloads into logs, screenshots, or shared terminals.

The original foreground entry point remains supported. Run from `companion/` so the module is importable:

```bash
cd /home/riko/Projects/riko_hub/companion
.venv/bin/python -m riko_companion --bind ACTUAL_PRIVATE_IPV4
```

The service accepts only the protocol's hello, pair, status, disconnect, and trust-revocation routes. It does not serve files or provide shell/task endpoints. The laptop stores token hashes, not bearer tokens. Certificate rotation requires pairing again. Pair approval followed by a lost response can leave a trusted phone record whose token the phone did not save; remove it with `revoke <phone-uuid>` before retrying.

## Verification

The owner runs tests and runtime checks:

```bash
cd /home/riko/Projects/riko_hub/companion
.venv/bin/python -m unittest discover -s tests -v
```

The dashboard is a lightweight standard-library curses UI. Remote transport is not implemented; phone/LAN end-to-end reachability always remains unverified. Local TCP reachability is reported separately and does not prove TLS or phone connectivity. There is no background stop command, remote management endpoint, or autostart service. See [the v0.4 setup and owner checklist](../docs/V04_LAPTOP_COMPANION_SETUP.md) for installation and acceptance details.
