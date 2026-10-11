# Riko Hub Linux companion

A small Python 3.11+ companion for the v0.4 laptop protocol. It listens only on an explicitly selected RFC1918 IPv4 address, serves HTTPS, and advertises _rikohub._tcp.local. on that interface. It has no root requirement, system service, or automatic startup.

From this directory, install requirements.txt in an isolated environment, then run:

    python -m riko_companion --bind 192.168.1.20

Options are --port (default 8765), --name (default Riko Laptop), and --data-dir (default ~/.local/share/riko-hub-companion). Keep the data directory private. The first run creates a persistent self-signed P-256 certificate and laptop identity. The companion accepts at most eight trusted phones.

At the companion prompt, use pair and provide the displayed payload directly to the phone. The laptop prints a request ID when a phone asks to pair; approve or deny that request by ID. status reports recent authenticated presence. revoke <phone-uuid> removes a phone's trust. quit or Ctrl+C stops the listener and service advertisement.

The service exposes only the protocol's hello, pair, status, disconnect, and trust-revocation routes. It does not serve files or provide shell or task endpoints. Certificate rotation requires pairing again. Pair approval followed by a lost response can leave a trusted phone entry whose token the phone never saved; remove it with revoke <phone-uuid> before retrying.

The test source is in tests/. Runtime, dependency, build, and network validation must be performed by the repository owner.
