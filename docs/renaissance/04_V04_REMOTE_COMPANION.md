# 04 — v0.4 Remote Companion Access

**Status:** Proposed extension to the v0.4 laptop companion. It does not describe shipped or verified remote behavior. The existing implementation supports foreground LAN discovery, pairing, and recent presence only. See [V04 companion setup](../V04_LAPTOP_COMPANION_SETUP.md) and [protocol 1](../V04_COMPANION_PROTOCOL.md) for the current contract.

## Goal and boundary

Remote access would let the owner's phone reach their Linux companion while away from the local network, for example to check recent presence or start a later explicit handoff. LAN use and local planning remain possible without a cloud account. The proposed remote service uses a private overlay, with no public companion listener or router port forwarding. Hosted overlay enrollment may require an account; it must never become an account requirement for the core app. Remote shell and unattended file access are excluded.

The leading candidate is an owner-selected private overlay such as Tailscale. It is a candidate, not a permanent vendor commitment. The owner must decide whether its vendor, account, control plane, DERP relays, client software, terms, and availability fit. Tailscale separates its coordination/control plane from encrypted peer traffic; restrictive networks can require encrypted relay paths. Do not promise direct-only connections, cloudless behavior, or guaranteed free operation. See [control/data planes](https://tailscale.com/docs/concepts/control-data-planes) and [connection types](https://tailscale.com/docs/reference/connection-types).

### Connectivity alternatives

| Candidate | Useful property | Cost or limitation | Initial recommendation |
|---|---|---|---|
| Hosted Tailscale | Managed enrollment and peer routing; manual private IP or MagicDNS endpoint. | External account/control-plane dependency; possible relays; client and policy compatibility require owner checks. | Optional first transport if ADR-01 is approved. |
| Self-managed WireGuard | Owner controls peer keys, configuration and routing; see the [official setup model](https://www.wireguard.com/quickstart/). | Owner must solve endpoint reachability, routing and key rotation; no promise that two peers behind restrictive NAT connect unaided. | Alternative for an owner already willing to administer networking. No public companion port is required. |
| Headscale with compatible clients | Owner-operated alternative coordination service. | Requires reachable control-plane hosting, maintenance and possibly relay infrastructure. This is not a zero-infrastructure option. | Evaluate only if hosted coordination is unacceptable; see [setup requirements](https://headscale.net/stable/usage/getting-started/). |

These choices change transport provisioning, not companion authorization. No new Riko cloud relay is proposed. A WireGuard or Headscale installation is separate owner-approved work, not a requirement imposed by this documentation.

## Endpoint and route model

Keep two concepts separate:

- **Manual host endpoint:** the operator-selected numeric address or approved MagicDNS host and port used to reach a listener.
- **Trusted peer identity:** the existing persistent laptop UUID and full DER SHA-256 certificate pin, supplied out of band during pairing and checked on every HTTPS connection.

A DNS name, private address, overlay address, mDNS record, or tailnet membership is only a route to a candidate endpoint. None identifies the laptop. Resolve a MagicDNS name and connect over the same approved overlay route; do not resolve on one path and silently connect on another. A DNS response containing a private address does not prove peer identity and is not enough to prevent rebinding. The pinned certificate and protocol peer UUID remain authoritative.

The current code accepts RFC1918 IPv4 only in Android discovery and request validation, and in the Linux CLI bind parser. Android calls `Network.openConnection` on the active Wi-Fi `Network`; explicitly bound traffic uses that network rather than the default VPN route. See [Network.openConnection](https://developer.android.com/reference/android/net/Network) and [VPN routing](https://developer.android.com/develop/connectivity/vpn). Remote mode therefore needs an explicit default/VPN-aware transport path and a separately selected manual endpoint; it must not reuse the physical-Wi-Fi route by accident. Keep TLS pin validation and bearer authentication identical on every route. Do not fall back to a public endpoint or bypass a required overlay. An approved overlay may use cellular as its underlying network only under the owner's metered-data policy.

The Linux companion should expose explicit LAN and overlay listeners in the same process, sharing trust state and the existing process lock. Bind each listener only to an explicitly selected interface address; never bind a wildcard or public interface. A shared TLS identity and peer authorization policy must apply to both listeners. mDNS remains LAN-only. Failure to advertise mDNS must not disable an independently configured overlay listener, and overlay failure must not break usable LAN service. Startup and shutdown should report each listener and advertisement outcome separately.

The existing RFC1918 filters reject 100.64.0.0/10, used by Tailscale addresses. This shared CGNAT range is not RFC1918; see [Tailscale address allocation](https://tailscale.com/docs/concepts/tailscale-ip-addresses). An approved overlay mode may accept it only with an explicitly configured overlay route. A numeric address is the initial endpoint MVP; [MagicDNS](https://tailscale.com/docs/features/magicdns) is a hostname convenience requiring compatible VPN DNS resolution. Never infer approval from an address being private or CGNAT. IPv6 is deferred until classification, binding, route selection and pinning are specified and tested together.

## Current modules and proposed records

Current source anchors are Android `CompanionProtocol.isPrivateIpv4`, `CompanionRepository.setActive/startScan/request/requestBlocking/matchesPin` and Python `__main__.private_address/run`, `server.BoundedTLSServer/Handler`, `state.CompanionState` and `storage.Storage`. The existing server has one bind and startup-coupled mDNS. The existing phone requires a discovered cached endpoint. Neither side has remote endpoint configuration.

Proposed `PeerEndpoint` fields: stable endpoint ID, trusted peer UUID, selected mode (`LAN` or `OVERLAY`), normalized host, port, optional label, enabled state and last-success timestamp. Store it separately from the encrypted trust record. Endpoint edits do not edit the certificate pin, generate a new token or silently replace a peer. Persist only configuration and bounded diagnostic timestamps, not a history of movements or networks. The existing one-trusted-laptop limit can remain while that laptop has multiple explicitly approved endpoints.

Accept a bare numeric host or hostname and a separately validated port; reject embedded credentials, schemes, paths, queries and fragments. Normalize hostnames, bound length, and validate every resolved address against the selected endpoint policy. Use the same route for resolution and sockets, and keep the chosen address fixed for that attempt. A changed DNS answer starts a new validated attempt. The client must not use redirects or turn a manual host field into arbitrary authenticated URL access.

## Lifecycle and failure states

Proposed states: `DISABLED`, `CONFIGURED`, `WAITING_NETWORK`, `CONNECTING`, `CONNECTED_RECENTLY`, `RETRY_WAIT`, `OWNER_ACTION_REQUIRED`. Show transport mode separately from connection state. Remote means the selected overlay path, not proof that devices are geographically apart.

On resume, validate configuration and network policy, resolve the endpoint, establish pinned TLS and verify protocol/UUID before an authenticated status check. Keep current foreground-only presence, 15-second checks and bounded backoff as the starting policy. On pause, cancel attempts and monitoring. Network generation changes invalidate old DNS/socket results before reconnecting; do not show a stale success from a prior network.

| Failure | User feedback and recovery |
|---|---|
| Overlay unavailable or another VPN conflicts | “Jaringan privat belum tersedia.” Wait or let the owner choose LAN explicitly; never bypass overlay. |
| DNS failure | “Nama laptop tidak ditemukan.” Retry approved DNS or edit to the known private numeric endpoint. |
| Listener/firewall/policy denial | “Laptop belum dapat dijangkau.” Show host/port and bounded non-secret guidance; do not claim to know which firewall denied it. |
| Certificate/peer mismatch | Stop retries requiring new trust; offer explicit forget/re-pair guidance. Never accept the new pin from the failing endpoint. |
| Revoked token or unsupported version | Stop automatic retries; require reauthorization or a compatible companion. |
| Temporary timeout | Mark disconnected and use capped backoff while resumed; show last successful check. |
| Local trust removed but remote revoke unknown | Confirm only local deletion; instruct laptop-side revocation, separately from overlay policy. |

Sleep/offline laptops remain unavailable; no wake-on-LAN or automatic laptop startup is promised. Enrollment and new peer establishment may depend on external coordination availability. Test provider behavior rather than promising an existing connection always survives an outage.

## Responsibilities, threat model and implementation stages

Android owns endpoint entry, protected trust, VPN-aware connection selection, permission/metered-data controls and truthful UI. Linux owns explicit listeners, shared TLS/trust state, local approval/revocation and bounded request handling. The owner provisions overlay enrollment, grants and OS firewall rules. The app cannot reliably inspect arbitrary overlay administration policy; policy enforcement belongs to that overlay, with owner setup checks as evidence.

Threats include spoofed discovery/DNS, mistaken endpoint entry, stolen tokens, unintended public binding, a broader-than-intended overlay policy and route changes during a request. Require pin/UUID validation before transmitting the secret or bearer token, retain request bounds, and keep logs free of secrets. A compromised phone or laptop OS can expose its own data; the overlay and TLS do not solve endpoint compromise.

Implement the stages in order:

1. R04-01: owner verifies existing LAN build, trust and lifecycle behavior without adding remote complexity.
2. R04-02/03: approve provider/address policy and specify endpoint persistence plus route-selection seams. Confirm Android 9 client support and VPN/DNS behavior in an owner-run feasibility check; document a blocker if safe routing cannot be established.
3. R04-04: add explicit optional LAN/overlay listeners in one process. LAN mDNS failure must not take down an approved remote listener. Share identity and trust; do not start competing processes against one locked store.
4. R04-05/06: add manual numeric/hostname configuration, VPN-aware route selection, generation guards and failure UI. Retain protocol 1 and all application-level checks.
5. R04-07: owner verifies different networks, invalid identity/token, denied policy, laptop restart, network transitions and LAN fallback chosen explicitly.

Setup assumes approved clients on both devices, membership in the intended overlay, a narrowly allowed companion port, a compatible route/DNS path and a running laptop service. `192.168.1.49:8765` is a reported previous LAN startup only. It is neither a remote endpoint nor a permanent default.

## Pairing and authorization

Prefer pairing while phone and laptop share the LAN, using the existing 120-second one-use out-of-band secret, certificate fingerprint, and explicit laptop console approval. After both devices join the overlay, the owner enters the manual endpoint and the app validates the same laptop UUID and pin before using it. Discovery is not required for a manually configured endpoint.

If the first pairing happens remotely, require both owner approvals: the existing explicit phone action and laptop-console approval over the pinned TLS session. An unattended remote approval mode is out of scope. Keep the approval window at 60 seconds and pairing secret at 120 seconds unless a later security review justifies a change. Do not weaken the payload or invent a short numeric PIN.

Overlay access and companion authorization are independent gates. A peer needs both permission to route through the overlay and a valid companion bearer token. Use least-privilege grants for only the owner's devices and companion port. A new tailnet's default policy can allow broad connectivity; configure explicit narrow policy rather than assume default deny. See [grants](https://tailscale.com/docs/reference/syntax/grants) and [ACL defaults](https://tailscale.com/docs/features/access-control/acls). Revoke companion trust and overlay access separately. Explain which layer was revoked when the other is unavailable.

## Owner decisions and acceptance

Before implementation, the owner must decide: overlay vendor and account/control-plane/DERP acceptance; whether cellular data may be used and how metered use is shown; behavior when another VPN or work profile conflicts; endpoint editing and persistence; and recovery when a device leaves the overlay. Do not imply that installing an overlay automatically provides the companion's authorization.

A remote slice is acceptable only when the user explicitly enables remote mode, selects an approved interface and endpoint, the listener is bound only to that interface, and both overlay policy and companion auth are checked. LAN pairing and presence continue when no cloud account or overlay exists. mDNS traffic and remote manual traffic use their intended routes. Wrong peer UUID, certificate pin, protocol, or revoked token fails closed on both routes. No automatic public fallback occurs. Startup, shutdown, and partial listener failures remain understandable and independently recoverable.

## References

- [Tailscale IP addresses](https://tailscale.com/docs/concepts/tailscale-ip-addresses) and [MagicDNS](https://tailscale.com/docs/features/magicdns).
- [Tailscale control and data planes](https://tailscale.com/docs/concepts/control-data-planes) and [connection types](https://tailscale.com/docs/reference/connection-types).
- [Tailscale grants](https://tailscale.com/docs/reference/syntax/grants) and [ACL access control](https://tailscale.com/docs/features/access-control/acls).
- [Android `Network`](https://developer.android.com/reference/android/net/Network) and [Android VPN guide](https://developer.android.com/develop/connectivity/vpn).
- [Headscale getting started](https://headscale.net/stable/usage/getting-started/).

Related proposals: [product vision](02_PRODUCT_VISION.md), [signature features](06_SIGNATURE_FEATURES.md), [sync and handoff](05_V05_SYNC_AND_HANDOFF.md), and [architecture and security](08_ARCHITECTURE_AND_SECURITY.md).
