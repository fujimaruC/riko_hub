# 08 — Architecture and Security

**Status:** Constraints and proposed design for future Renaissance work. This does not authorize implementation or claim a security review has passed. Keep Kotlin, Compose, Room, coroutines/Flow, manual `AppContainer`, and the Python TLS companion. See [Android architecture](../02_ANDROID_ARCHITECTURE.md), [current v0.4 setup](../V04_LAPTOP_COMPANION_SETUP.md), [remote proposal](04_V04_REMOTE_COMPANION.md), and [handoff proposal](05_V05_SYNC_AND_HANDOFF.md).

## Architectural constraints

### Current architecture, not proposed replacement

The Android app has one `:app` module, minimum SDK 26, compile/target SDK 37 and current version metadata 0.4.0/400. `AppContainer` manually constructs services. `RikoDatabase` is Room version 2 with `MIGRATION_1_2`; Task/Event entities use local numeric IDs plus unique `syncId` values. `SettingsDataStore` holds preferences. Companion trust uses separate private preferences encrypted by Android Keystore AES-GCM. Room data is not shown in source as an application-encrypted database; do not describe Keystore protection of the trust record as encryption of all planner data.

`TaskRepository`/`EventRepository` in `data/repository/Repositories.kt` own local mutations. `LocalBackupService` exports/previews/imports an explicit bounded JSON task/event/settings document. `TaskReminders` and native widget services receive committed mutation/import callbacks. Drive authorization/REST lives in `data/remote/drive`; local companion networking lives in `data/remote/device`. Neither is a task-sync implementation.

The Python process currently combines one explicitly selected LAN listener, DNS-SD and a local console. `server.py` handles bounded HTTPS, `state.py` handles pairing/presence/trust, and `storage.py` persists identity and token hashes. The owner-reported startup and source inspection do not establish cross-device security acceptance. Detailed evidence is in [01](01_CURRENT_STATE_AUDIT.md).

### Proposed boundaries within existing modules

| Boundary | Responsibility | Must not own |
|---|---|---|
| Android domain/repositories | Task/event/course invariants, links, ledger and durable local actions. | Network transport policy or Google tokens. |
| Android companion transport | Endpoint validation, LAN/VPN-aware routing, protocol checks and authenticated operations. | Direct Room mutation or arbitrary filesystem access. |
| Protected trust store | Pin/token lifecycle and explicit reset/revoke semantics. | Transfer history, backups or general planner content. |
| Transfer repository | Explicit selection, grants, queue, staged bytes, integrity and receiver completion. | Hidden file discovery or structured-record merge. |
| Document/knowledge repository | Stable references, owned copies, provenance, bounded indexing and deletion. | Treating a URI/path as portable identity. |
| Local automation | Committed domain events, approved actions, durable dedupe and compensation. | Arbitrary scripts, silent uploads or broader authority than the rule. |
| Python companion | Shared TLS/trust across selected listeners, explicit prepared file offers, approved staging and context presentation. | Reading Android databases, campus credentials or unrelated laptop files. |

Introduce only seams required by the next vertical workflow. Package-level interfaces for endpoint policy and routing may be justified by the existing combined `CompanionRepository`; a multi-module rewrite, new graph service or dependency-injection framework is not. Desktop context starts as a lightweight read-only view; full Linux planner editing requires a separate structured-sync design.

Preserve the single Android app module and existing manual dependency container. Do not add Hilt or rewrite the UI architecture to support a companion feature. Keep Android local work independent of network availability. Room remains the source of truth for local records. Future companion queues may use focused Room tables and additive migrations from the current schema version; never ship a database copy/merge protocol. Any migration must preserve user data and have explicit upgrade and recovery behavior.

Keep the Python service owner-run. LAN and approved overlay listeners may share process, TLS identity, trust store, and process lock, but each binds to a selected address. Never bind wildcard or public interfaces. mDNS is LAN-only and must not control overlay startup; remote access uses an explicit endpoint and approved interface.

Use stable IDs for relationships across devices. Android `Uri` values and Linux filesystem paths are local handles, not portable record identity. A future `DocumentReference` should describe a user-owned source or link; a future `BlobFileRecord` may describe an ingested copy with measured size and checksum. Do not conflate the two or assume an absolute path can be opened on another device. Future outbox or transfer-ledger rows may coordinate durable work with Room transactions, but are proposals requiring a separate schema and lifecycle decision.

## Identity, secrets, and authorization

The existing trust model is the baseline: HTTPS TLS 1.2 or newer, a persistent self-signed certificate, full DER SHA-256 pin delivered out of band, laptop UUID validation, and a random per-phone bearer token. The phone stores its trust record encrypted with Android Keystore AES-GCM in app-private preferences; Linux stores private keys and trust data in owner-only storage. Never log pairing payloads, tokens, request bodies, or authorization headers. Fail closed if identity or protected storage cannot be validated.

A manual endpoint, DNS answer, RFC1918 address, CGNAT address, mDNS record, or overlay membership is a route, not identity. TLS pin and peer UUID validate identity. DNS resolution and connection must use the same approved route to prevent rebinding/path confusion. Overlay access and companion bearer-token authorization are independent requirements. Least-privilege overlay grants should limit access to named devices and the companion port; broad default tailnet connectivity does not replace app authorization. Revoke device access in overlay policy and peer trust separately, and report partial revocation truthfully.

Do not enable transfer capability by extending protocol 1's current exact `['presence']` capabilities. Negotiate a distinct version/contract, check authorization on every route and operation, bind each file capability to a peer and selected action, and require per-transfer user consent. Do not use a single global permission as a broad file grant. The Android device initiates both upload and download connections. Never add remote shell, arbitrary path access, automatic file launch, background clipboard capture, or public fallback as incidental features.

## Threat and failure review

| Threat or failure | Required response |
|---|---|
| Spoofed mDNS or changed DNS endpoint | Treat discovery as untrusted; require matching pinned certificate and UUID. |
| Stolen bearer token or revoked phone | Require TLS and peer token on every protected endpoint; revoke server trust immediately and separately remove overlay access. Avoid token logs and backups. |
| Public or unintended listener | Bind only selected interface addresses; reject wildcard/public bind; show active listener addresses at startup. |
| DNS rebinding or route switch | Resolve and connect using the same explicit overlay-aware path; identity still comes from pin plus UUID. No automatic route fallback. |
| Malicious or oversized request | Bound headers, body, concurrency, queue, path, metadata, and timeouts; reject malformed input before mutation. Preserve protocol 1's 8 KiB JSON bound. |
| Path traversal, symlink escape, overwrite | Use configured roots, safe generated names, no arbitrary client paths, controlled staging, and explicit replacement approval. |
| Partial write, crash, disk full, cancel/commit race | Persist only durable acknowledgements; verify full hash before completion; make commit/status idempotent and expose uncertain outcomes. |
| Database or clock conflict | Defer structured sync until stable IDs, causal versions, tombstones, merge rules, migrations, skew handling, and backup recovery are specified. |
| Lost Android Keystore key or Linux certificate | Fail closed and require an explicit reset/re-pair flow; never silently trust a replacement identity. |

A security review should include route/interface behavior on the supported Android versions, TLS pin and UUID failures, permission and revocation paths, malformed manifests/chunks, storage exhaustion, lifecycle interruption, and backup/restore behavior. Testing a happy-path transfer alone is insufficient. Record the reviewed threat model and accepted limits before release.

## Storage, backup, and release

### Local-first records, files and portability

The phone remains authoritative for its local planner until an approved multi-writer model exists. A received context snapshot is not a replacement task row. Treat copied files as owned copies with measured size/hash; treat Drive IDs, URLs and SAF URIs as references whose access can expire. Material describes a learning resource, Document describes a concrete document/version, and an attachment relation explains its use. Do not create separate unmanaged copies merely because one document supports several tasks.

Use existing Task/Event `syncId` values for cross-domain links. New entities receive stable IDs; local Room keys remain local. Link and domain mutations require consistent transactions and deletion checks. Indexes are derived and rebuildable. A failing index must not lose source text, block task writes or imply a document was deleted. Bounded pagination, lazy queries and chunked cancellable indexing are the default on older devices.

Current JSON backups are explicit plaintext exports, not encrypted vaults or complete file archives. Warn about sensitive content at export and let the user choose a destination. Future portable archives need a versioned manifest, supported record schema, relationship IDs, explicit attachment inclusion and checksums. Exclude OAuth tokens, bearer tokens, private keys, clipboard payloads and live execution authority. Encryption of future archives requires an established reviewed format/key-recovery decision; this suite does not invent one.

### Migration and recovery model

Allocate future Room versions only when each migration is approved. Retain exported schemas and fixtures covering existing v1/v2 data. Plan schema, backfill, indexes and backup-format compatibility together; do not use destructive fallback. Additive-first is a preference, not permission to ignore renamed fields or changed invariants. Unsupported imports must fail before mutation, with a preview explaining missing fields or versions.

Separate durable domain commit from optional scheduling/rendering effects. A future outbox can record an event atomically with a local mutation; retries must not generate duplicate recurrence successors, ledger entries or rule actions. Backup restoration must disable imported automation until reviewed and invalidate/reconcile ephemeral transfer sessions rather than trust old acknowledged offsets. Restore never recreates peer authority from an exported token.

| Recovery case | Proposed safe result |
|---|---|
| Migration/import interrupted | Preserve a known recoverable source; transactional apply or explicit staged recovery, with owner-tested fixtures. |
| Missing or changed attachment | Keep the record/reference, show unavailable/version-changed status, offer explicit relink or replacement. |
| Keystore or laptop identity lost | Fail closed; require new pairing and revoke orphan trust where possible. Never downgrade to plaintext. |
| Queue restored or process restarted | Reconcile peer status, source identity and destination grants before resuming; completed bytes are not inferred from stale local rows. |
| Index/log budget exceeded | Pause derived work and offer pruning/rebuild; preserve primary records and explain lost history if deletion is chosen. |
| Concurrent future record changes | Preserve competing revisions for review; do not let wall-clock timestamps silently choose a winner. |

### Permission and privacy policy

Discovery uses only the existing network/multicast permissions appropriate to the OS; remote design must review VPN/local-network behavior without adding unnecessary broad storage access. Documents use explicit SAF selection and bounded persisted grants where available. Notifications use current platform permission checks. Optional clipboard and automation permissions are visible actions/capabilities, not perpetual access implied by pairing.

File/context consent is per action and destination, including a preview of metadata leaving the device. External uploads and LMS adapters require separate user intent and authorized APIs; no credential scraping, surveillance or automatic planner upload is permitted. Explain retention for files, references, transfer history, ledger and rule logs separately. Deleting an export or received copy on another device is not guaranteed by local deletion; display that limit.

Endpoint compromise, an authorized user intentionally exporting sensitive content and external copies outside app control remain outside the guarantees. TLS and overlay encryption protect transport, not a compromised endpoint. Security acceptance requires owner execution and review of these specific limits.

Exclude companion credentials from Android backup. Keystore-encrypted values cannot be recovered if their key is missing; detect this and provide local reset plus laptop revoke guidance. Linux keys and trust hashes remain owner-only and outside source control and exports. Give transfer history separate retention and delete controls.

A future Room schema change must be additive where practical, preserve existing task/event rows, export an updated schema, and include an explicit migration and recovery plan. Keep credentials in a dedicated protected store rather than Room. A transfer ledger can contain state, peer ID, manifest hash, durable offset, and bounded metadata; it should not contain secret material or file contents. Backup policy for that ledger must be decided before implementation because restoring stale transfer offsets can misreport durable data.

Before release, verify database upgrades, corruption/reset behavior, online/offline use, revocation at each layer, LAN-only use without an account, and approved overlay routing. Confirm logs exclude secrets. Release notes must distinguish implementation from proposal and avoid promises of direct paths, cloudless operation, free/unlimited relay, or offline revocation.

Related product and data proposals: [02 Product Vision](02_PRODUCT_VISION.md), [06 Signature Features](06_SIGNATURE_FEATURES.md), and [07 Academic and Automation](07_ACADEMIC_AND_AUTOMATION.md). Sequencing and implementation authority belong to the [master roadmap](03_MASTER_ROADMAP.md) and [execution backlog](09_EXECUTION_BACKLOG.md). The [historical testing/release plan](../09_TESTING_AND_RELEASE.md) remains context; current manual backup exists and all new release execution belongs to Riko.
