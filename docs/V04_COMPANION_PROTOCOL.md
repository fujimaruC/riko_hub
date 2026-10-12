# Laptop Companion protocol 1

Implementation contract for v0.4. All network/runtime verification belongs to the owner.

Protocol 1 remains presence-only. Additive authenticated v2 file/text/workspace routes are documented in [V05_HANDOFF_PROTOCOL.md](V05_HANDOFF_PROTOCOL.md), with **IMPLEMENTED / UNVERIFIED** status; statements below about absent transfer apply to v1.

## Discovery and transport

- DNS-SD: `_rikohub._tcp.local.` (Android service type `_rikohub._tcp.`).
- TXT: `id` (persistent UUID), `v` (`1`), `caps` (`presence`), `name` (bounded display name). TXT is untrusted discovery metadata, never a trust anchor.
- IPv4 private LAN only in this version. Owner selects the laptop's LAN bind address. No wildcard/public binding, cloud relay or port forwarding.
- HTTPS only, minimum TLS 1.2. Laptop has a persistent self-signed P-256/SHA-256 certificate. Android verifies its full DER SHA-256 fingerprint from an out-of-band pairing payload, including on reconnect. No global trust-all manager or plaintext fallback.
- Pairing payload: `riko1:<laptop UUID>:<64 lowercase hex certificate SHA-256>:<64 lowercase hex one-time secret>`. Obtain it directly from the owner's laptop terminal, never from mDNS or a network response. Valid for 120 seconds, one use. This longer key is the secure confirmation equivalent of a short code; it avoids a weak PIN and does not require a custom cryptographic protocol.

## HTTP JSON contract

JSON request/response bodies are at most 8 KiB. No redirects or cookies. All successful responses contain `protocol: 1`, `peer_id: <laptop UUID>`, `name: <laptop display name>`, `capabilities: ["presence"]`. HTTP 426 rejects protocol mismatch; POST bodies carry `protocol`, authenticated GET/DELETE requests use `X-Riko-Protocol: 1`.

- `GET /v1/hello`: protocol/identity/capabilities only. No trust granted.
- `POST /v1/pair`: `{protocol:1, pairing_secret:<64 hex>, device_id:<phone UUID>, device_name:<bounded name>}`. Consume a valid unexpired secret atomically. One pending request at a time. Owner must type `approve <request id>` in the laptop console, within 60 seconds and before the key expires. The phone waits for this response. On approval, persist the phone ID and SHA-256 of a fresh 256-bit token; return `token:<64 hex>`. Never persist plaintext token on laptop. Reject occupied trust/device IDs rather than replacing them silently. Maximum eight trusted phones.
- `GET /v1/status`: `Authorization: Bearer <token>` and protocol header. Validate token and return `status:"connected"`; updates last-seen presence. Phone validates pinned peer ID, protocol and capabilities before reporting Connected.
- `POST /v1/disconnect`: same bearer/header, empty JSON `{protocol:1}`; clear transient presence without revoking trust.
- `DELETE /v1/trust`: same bearer/header; revoke the requesting phone's trust immediately. Revoked token becomes invalid.

Errors: `{error:<stable code>}` with no secret or personal metadata. 400 invalid request, 401 unknown/revoked token, 403 bad/expired key or denied approval, 409 occupied pairing/trust, 413 oversized body, 426 incompatible protocol, 429 bounded resource/rate limit, 503 shutting down. Unknown routes/methods cannot access files or execute commands.

## Lifetime and storage

- Android supports one trusted laptop at a time. Trust record: laptop ID/name, pin, client token and phone UUID. Encrypt the record with Android Keystore AES-GCM in private preferences; fail closed if storage/key recovery fails. No Room/task or backup access.
- Laptop private key, certificate and trust hashes live in an owner-only 0700 data directory, files 0600; atomic writes. Hold a process lock to prevent two instances corrupting trust. Never log pairing key, bearer token, request body or headers. The explicit `pair` terminal command displays the ephemeral key only to the operator.
- Android discovery and heartbeat run only while the Companion screen is resumed. NSD resolution is serialized and bounded; release multicast lock/callbacks when paused. Use Wi-Fi network callbacks and bind HTTPS to the active Wi-Fi network. Do not send LAN traffic through cellular/VPN fallback.
- Foreground status heartbeat every 15 seconds. After failure use backoff 2/4/8/16/32/60 seconds, capped; terminal/authorization/pin/protocol failures require owner action. Network loss pauses retries. Discovery snapshots expire and scan windows stop after 20 seconds; refresh or network restoration starts another bounded scan. Resolved endpoints are transient, expire after 90 seconds and are discarded on network change; an endpoint is never a trust anchor.
- Laptop reports connected when an authenticated presence request arrived within 45 seconds. Display this as recent presence, not proof of a permanent socket. Stop/exit unregisters mDNS, cancels pending approval and shuts down listening sockets.
- Forget attempts remote revocation and always permits explicit local deletion. If remote revocation cannot be confirmed, clearly instruct the owner to run `revoke <phone id>` on laptop. Never claim remote revocation succeeded offline.

No files, clipboard, task records, remote shell or surveillance endpoints exist. Certificate rotation requires re-pairing. A pairing approved at the laptop but interrupted before the phone saves its token can leave orphan laptop trust; revoke it locally before retrying.
