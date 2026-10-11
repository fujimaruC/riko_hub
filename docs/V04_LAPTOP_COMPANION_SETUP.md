# v0.4 Laptop Companion

This version provides discovery, explicit pairing and recent connection presence. The owner performs every build, test, lint and device/network check. No successful runtime verification is claimed.

## Architecture

`companion/riko_companion/` is a foreground Python console service for Linux, with platform TLS and a small JSON HTTPS endpoint. `zeroconf` advertises DNS-SD and `cryptography` creates the persistent certificate; neither is an Android dependency. Android uses platform NSD, Wi-Fi network callbacks, HTTPS, Android Keystore and the existing Sync route/theme. No task database or Drive repository is exposed to the companion. See [protocol 1](V04_COMPANION_PROTOCOL.md) for exact messages and trust boundaries.

## Linux setup and dashboard (owner commands)

Use Python 3.11 or later. Arch Linux marks system Python as externally managed, so install only into the project's virtual environment. No root privileges, Avahi daemon or automatic boot service is required.

```bash
cd /home/riko/Projects/riko_hub/companion
python3 -m venv .venv
.venv/bin/python -m pip install -e .
mkdir -p "$HOME/.local/bin"
ln -s "$PWD/.venv/bin/rikohub" "$HOME/.local/bin/rikohub"
```

The symlink command fails safely if that name already exists; inspect the existing file before replacing it. For Fish, add `~/.local/bin` using this separate Fish command:

```fish
fish_add_path -U ~/.local/bin
```

Open a new shell and check `rikohub --help`. The symlink targets this checkout's `.venv`, which must remain in place. To refresh the editable install after code changes, run `.venv/bin/python -m pip install -e .` from `companion/`.

Run `ip -brief -4 address`, then select the actual private IPv4 address assigned to the laptop on the same Wi-Fi/LAN as the phone. Do not copy a sample address. The dashboard's Network Settings asks for confirmation before saving LAN exposure, and `start --bind ADDRESS` asks before starting. The server rejects wildcard and public addresses and binds only the selected private interface. A firewall/router may need to permit UDP 5353 multicast and the selected TCP port on that private network; do not disable the firewall or expose the port to the internet.

Run `rikohub` for the interactive dashboard. Running status is detected through the lock in the selected Companion data directory, so it does not claim to find servers using another data directory. The standalone `rikohub status` checks local TCP reachability when a process is detected; phone/LAN end-to-end reachability remains unverified even when that TCP probe succeeds. Remote transport is not implemented. `rikohub status`, `rikohub devices`, and `rikohub doctor` provide direct views. Trusted-device output includes IDs and safe names only, never tokens or private keys.

To start from the shell, run `rikohub start --bind ACTUAL_PRIVATE_IPV4` after replacing the placeholder with the address just inspected. The server remains in the foreground so pairing approvals and trusted-device revocation continue to work in the same terminal. There is no background `stop` command or arbitrary PID handling. Use the server prompt's `quit` or Ctrl+C to shut it down and return to the shell. If a DHCP change changes the address, restart with the new address; the persistent certificate/identity is retained. Keep the default owner-only data directory at `~/.local/share/riko-hub-companion/`; `--data-dir` can isolate disposable QA data. Do not copy private keys or trust data into Git.

The legacy entry point is retained and still requires an explicit bind. Run it from `companion/`:

```bash
cd /home/riko/Projects/riko_hub/companion
.venv/bin/python -m riko_companion --bind ACTUAL_PRIVATE_IPV4
```

When started from the dashboard, `quit` or Ctrl+C stops the foreground server and returns to the dashboard. When using `rikohub start` directly, it returns to the shell. There is no background process to stop.

Use `help` for console commands, `status` for trust and recent presence, `pair` to display a fresh two-minute pairing payload, `approve <request id>` or `deny <request id>` for pending approval, `revoke <phone id>` to revoke a phone, and `quit`/Ctrl-C to stop. The module entry point also routes SIGTERM through shutdown cleanup. Do not record pairing payloads in logs, screenshots or shared terminals. The payload is intentionally long: it carries the full certificate fingerprint and a random 256-bit secret, avoiding an insecure short PIN. No QR scanner is included in this slice.

## Pairing and daily use

1. Start the companion on the private LAN. Open Riko Hub's existing Sync screen, now containing Laptop Companion. Grant local-network permission if requested on Android 17; Android 9 has no such runtime prompt.
2. Refresh discovery. A nearby device row is an untrusted announcement, not an approved connection. If nothing appears, confirm both devices share a network without access-point/client isolation.
3. Choose the laptop and enter the payload obtained directly from its local `pair` command. Confirm the pairing action on the phone. The phone pins the exact certificate before transmitting the one-time secret.
4. At the laptop, approve only the request you initiated. Within the approval window, the service returns a new client token over pinned TLS. Both sides save minimal trust state. Interrupted approval can leave an orphan laptop record: revoke it before trying again.
5. A valid authenticated status response changes the phone to Connected. Foreground presence checks run every 15 seconds; network failures use capped backoff. The laptop reports recent presence for 45 seconds after an authenticated check. Leaving or backgrounding Sync stops Android discovery/monitoring. Reopen Sync or use reconnect to resume.
6. Forget requires explicit confirmation. The phone attempts remote revocation and clears local trust. If remote revocation was not confirmed, run `revoke <phone id>` on the laptop. `status` shows the relevant phone ID; no bearer token is shown.

## Security and limitations

- TLS 1.2 or newer encrypts all traffic. The out-of-band full SHA-256 certificate pin authenticates the laptop; a random per-phone bearer token authenticates the phone. This is not mutual-certificate TLS. No trust-on-first-use, network-provided pin, cloud relay, plaintext fallback or custom encryption exists.
- The laptop stores only token hashes. Android encrypts the trust record with Keystore AES-GCM in private app storage. Compromised local operating systems/root access remain outside the trust model. Loss/replacement of the certificate or Android Keystore key requires re-pairing.
- One trusted laptop is supported on the phone; up to eight phones on the laptop. Private IPv4 and Wi-Fi are supported. IPv6-only networks, VPN routing, automatic interface migration, background presence and Windows are outside this version.
- Discovery names/IDs are untrusted and bounded. Pins and authenticated protocol/peer checks decide identity. Router multicast filtering can prevent discovery even when TLS is otherwise reachable.
- Protocol 1 advertises only `presence`; unsupported versions/capabilities fail with an actionable message. Connected means a recent successful authenticated status check, not a persistent stream.
- Foreground heartbeat, timeout/backoff, TLS compatibility, Android NSD behavior and process/lifecycle races need owner execution. This source implementation is not a security certification.
- No file transfer, clipboard, remote shell, remote control, task synchronization or private document access is implemented.

## Owner verification commands (not executed by the agent)

```bash
cd /home/riko/Projects/riko_hub
JAVA_HOME=/opt/android-studio/jbr rtk gradlew :app:assembleDebug --console plain
JAVA_HOME=/opt/android-studio/jbr rtk gradlew :app:testDebugUnitTest --tests 'id.riko.hub.data.remote.device.*' --console plain
cd companion
.venv/bin/python -m unittest discover -s tests -v
```

The build/test commands intentionally belong to the owner. The agent does not install dependencies, start the server, create runtime keys, invoke Gradle, execute tests, run lint or use ADB.

## Owner device/network checklist

- Build and install using the existing signing identity; preserve application data. Check normal navigation and Indonesian labels/large fonts.
- Test no Wi-Fi, empty discovery, refresh, multiple announcements, leaving Sync during discovery, network switch and screen/background cleanup. Deny/revoke Android 17 local-network permission if that platform is available.
- Test the real Linux/Android vertical flow: invalid/expired payload, cancelled/denied approval, valid approval, replay rejection, wrong certificate pin, unsupported protocol and wrong peer ID. Capture only non-sensitive evidence.
- Confirm presence after pairing, laptop stop/start with the same identity, Wi-Fi loss/restoration and capped reconnect. Confirm Connected is not shown after failed authentication.
- Test local forget while online and offline; confirm offline wording requests laptop revocation. Revoke on the laptop and verify the old phone token is rejected. Check simultaneous requests, pause/resume during pairing approval and trust persistence, and interrupted pairing/storage failures. A corrupt local trust record must fail closed and offer an explicit reset; laptop-side revocation may still be needed.
- Verify console shutdown/EOF/Ctrl-C removes mDNS and closes the endpoint. Start a second instance with the same data directory and confirm it refuses the lock. Confirm no task/database content crosses the connection.

### CLI acceptance checklist

From any directory in a new shell, run `rikohub`, then check `rikohub --help`, `rikohub status`, `rikohub devices`, and `rikohub doctor`. Also verify `.venv/bin/python -m riko_companion --bind ACTUAL_PRIVATE_IPV4` from `companion/` retains the foreground flow. Resize the terminal to a narrow width and short height; repeat with `NO_COLOR=1 rikohub` and confirm the menu remains readable and exits cleanly. Pipe `rikohub` output in a non-interactive shell and confirm it prints a status summary without starting the service. While one foreground server is active, run `rikohub start --bind ACTUAL_PRIVATE_IPV4` in a second terminal and confirm it reports the existing process without launching another. Complete a pairing, restart using the same data directory, and confirm trusted metadata remains visible. Check that status, devices and diagnostics output contain no tokens or private keys; the `pair` command intentionally displays its temporary payload directly to the owner, which must not be captured or shared. Confirm Ctrl+C returns to the dashboard or shell cleanly. These are owner-run acceptance checks; no test, build, lint, install or runtime command was executed while preparing this change.

## Primary API references

- [Android NSD](https://developer.android.com/develop/connectivity/wifi/use-nsd), [NsdManager multicast behavior](https://developer.android.com/reference/android/net/nsd/NsdManager).
- [Android 17 local-network permission](https://developer.android.com/privacy-and-security/local-network-permission).
- [Python TLS](https://docs.python.org/3/library/ssl.html), [zeroconf API](https://python-zeroconf.readthedocs.io/en/stable/api.html), [PyCA X.509](https://cryptography.io/en/stable/x509/tutorial/).
