# v0.5 LAN Universal Handoff

Status: **IMPLEMENTED / UNVERIFIED** source. This is the bounded LAN MVP contract, not acceptance of the broader roadmap. Only Riko runs builds, tests, lint, installations and runtime verification. None were performed during this recovery session.

## Scope and trust

Protocol 1 discovery, pairing and presence remain unchanged. Additive `/v2` handoff routes use the same pinned HTTPS connection and paired-device bearer token. A separate laptop-owner grant is tied to that phone's current token hash. Re-pairing or revocation invalidates the grant. Every transfer belongs to one peer; another transfer UUID grants no access. No client-supplied arbitrary filesystem path is accepted.

Android retains one trusted laptop, selected through existing Sync pairing/connection controls. Replacing it requires forgetting and pairing again. Handoff is foreground-only over Wi-Fi/LAN. Separate-network transport, Tailscale, public endpoints, background continuation and full Room synchronization are not implemented.

## Wire contract

JSON requests contain `protocol: 2`; successful JSON responses contain `protocol: 2` and the laptop `peer_id`. Requests use the protocol header set by `CompanionRepository`. Operations recheck authentication, grant and current connection identity.

| Operation | Route | Effect |
|---|---|---|
| Negotiate | `GET /v2/hello` | Authenticated capabilities, limits, `handoff_allowed` |
| History / offers | `GET /v2/transfers`, `GET /v2/offers` | Current peer only |
| Inspect | `GET /v2/transfers/{id}` | Current peer only |
| Offer phone file | `POST /v2/transfers` | Manifest; waits for laptop owner approval |
| Accept laptop file | `POST /v2/transfers/{id}/consent` | Explicit phone acceptance after fresh SAF destination selection |
| Upload chunk | `PUT /v2/transfers/{id}/chunks?offset=N` | Binary body and `X-Riko-Chunk-SHA256` |
| Download chunk | `GET /v2/transfers/{id}/chunks?offset=N&length=N` | Approved offer; binary response and chunk digest |
| Finish | `POST /v2/transfers/{id}/commit` | Total size and full SHA-256 |
| Cancel | `POST /v2/transfers/{id}/cancel` | Removes generated incomplete data; preserves source files |
| Send text | `POST /v2/text` | Deliberate action; no clipboard observation |
| Text list / preview | `GET /v2/texts`, `GET /v2/texts/{id}` | Metadata list; explicit laptop-to-phone payload preview |
| Consume text | `POST /v2/texts/{id}/consume` | Explicit receiver action |
| Workspace snapshots | `POST /v2/workspaces`, `GET /v2/workspaces` | Read-only metadata; no database/task mutation |

Fixed limits: 64 MiB per file, 256 MiB Linux store reservation, 256 KiB chunks, 8 KiB JSON requests, 64 KiB JSON responses, 2 KiB UTF-8 text, eight pending text entries, 32 retained transfer sessions, one active transfer per peer. Text expires after five minutes and is not persisted. Upload approval expires after ten minutes; other active transfers after 24 hours. Generated-file retention is configurable from 1–30 days, default seven. Workspace lists return at most seven entries and can report truncation. These values supersede larger *proposed* roadmap limits for this implementation.

## Consent, storage and recovery

Uploads stage an explicitly selected SAF document privately. The phone user reviews metadata and sends; the laptop owner approves before chunks are accepted. Chunks are bounded and hashed. Linux verifies the complete file before publishing its generated result inside the owner-controlled transfer directory. Selected source files are never moved or deleted.

Laptop offers copy an owner-selected file into controlled storage. Android explicitly accepts, downloads into private staging, verifies the full digest, then writes and reads back a fresh SAF destination. Providers do not guarantee atomic writes or deletion; cleanup failures must identify possible partial output. A lost completion response can leave a verified local file with uncertain remote completion; refresh before retrying.

Cancel, screen pause, connection loss and process interruption do not promise background continuation. Upload retry resumes a valid active transfer; a cancelled/failed upload starts with a fresh UUID and fresh laptop approval. Downloads can retry only while the offer remains available; a terminal cancelled offer needs a new laptop offer. Local phone staging/history cleanup and laptop retention cleanup are separate from user-selected destination documents.

## User surfaces and limits

The existing Android Sync screen exposes manual handoff refresh, file preview/send, destination acceptance, progress/cancel/retry, recent history, explicit text send/preview/consume and workspace metadata. Text drafts/previews clear on pause. No broad storage permission or automatic clipboard read/write is added.

The Linux dashboard provides handoff guidance; management remains in the single running foreground console. There is no second listener or unauthenticated control channel. Workspace creation remains a repository/protocol capability; task/material selection and a workspace compose UI are deferred.

## Owner verification gate

Use [the setup guide](V04_LAPTOP_COMPANION_SETUP.md). Check v1 compatibility; wrong pin/token/peer and revoked grant rejection; approve/deny/expire; both file directions including empty files and Unicode names; corrupt/oversized/out-of-order chunks; cancel/retry; process restart; SAF cancel/rotation/provider failure; disk full; text expiry; workspace metadata; and cleanup preserving sources/destinations. Added test source is **UNVERIFIED**, not passing evidence.
