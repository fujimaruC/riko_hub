# v0.4 Laptop Companion

This version provides discovery, explicit pairing and recent connection presence. The owner performs every build, test, lint and device/network check. No successful runtime verification is claimed.

## Architecture

`companion/riko_companion/` is a foreground Python console service for Linux, with platform TLS and a small JSON HTTPS endpoint. `zeroconf` advertises DNS-SD and `cryptography` creates the persistent certificate; neither is an Android dependency. Android uses platform NSD, Wi-Fi network callbacks, HTTPS, Android Keystore and the existing Sync route/theme. No task database or Drive repository is exposed to the companion. See [protocol 1](V04_COMPANION_PROTOCOL.md) for exact messages and trust boundaries.

## Linux setup (owner commands)

Use Python 3.11 or later. Select the laptop's private IPv4 address on the same Wi-Fi/LAN as the phone. No root privileges, Avahi daemon or automatic boot service is required. A firewall/router may need to permit UDP 5353 multicast and the selected TCP port on this private network only; do not disable the firewall or expose the port to the internet.

```bash
cd /home/riko/Projects/riko_hub/companion
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m riko_companion --bind 192.168.1.20 --port 8765 --name 'Riko Laptop'
```

Replace `192.168.1.20` with the actual laptop LAN address. The service binds only that address, not every interface. A DHCP address change requires stopping and restarting with the new address; the persistent certificate/identity is retained. Keep its default owner-only data directory in `~/.local/share/riko-hub-companion/`; a separate `--data-dir` can isolate disposable owner QA. Do not copy private keys or trust data into Git.

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

## Primary API references

- [Android NSD](https://developer.android.com/develop/connectivity/wifi/use-nsd), [NsdManager multicast behavior](https://developer.android.com/reference/android/net/nsd/NsdManager).
- [Android 17 local-network permission](https://developer.android.com/privacy-and-security/local-network-permission).
- [Python TLS](https://docs.python.org/3/library/ssl.html), [zeroconf API](https://python-zeroconf.readthedocs.io/en/stable/api.html), [PyCA X.509](https://cryptography.io/en/stable/x509/tutorial/).
