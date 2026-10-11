# 09 — Execution Backlog

**Status:** Planning only. Work items below are proposed sequencing and acceptance targets; inclusion is not implementation authorization. ADR recommendations are not approvals. Riko approval is required wherever explicitly stated. Preserve the shipped v0.2 task experience and the v0.4 protocol 1 contract while planning future work. See [03_MASTER_ROADMAP.md](03_MASTER_ROADMAP.md), [04_V04_REMOTE_COMPANION.md](04_V04_REMOTE_COMPANION.md), [05_V05_SYNC_AND_HANDOFF.md](05_V05_SYNC_AND_HANDOFF.md), [06_SIGNATURE_FEATURES.md](06_SIGNATURE_FEATURES.md), [07_ACADEMIC_AND_AUTOMATION.md](07_ACADEMIC_AND_AUTOMATION.md), and [08_ARCHITECTURE_AND_SECURITY.md](08_ARCHITECTURE_AND_SECURITY.md).

## Priority and sizing

P0 protects data safety, security, recovery and essential correct behavior. P1 serves frequent user workflows and continuity. P2 covers optional customization, advanced visualization and integrations. Size is relative complexity: S is localized; M crosses a few known seams; L crosses components or persistence; XL introduces a substantial protocol/state/migration surface. These are not schedule promises. Dependencies name item IDs; modules are likely touch points. Acceptance evidence is required for completion. Each item's version is its enclosing heading; R10 safety gates apply to earlier releases as well as v1.0.

## Recommended next work

R04-01 LAN baseline, R04-02 overlay decision, R04-03 endpoint/identity contract, R04-04 Linux listeners, then R04-05 Android VPN routing. This order does not approve a provider or remote access.

## v0.3 — Drive QA gate

### R03-01 — Owner live Drive QA

**Priority:** P0 parallel gate. **Outcome:** Riko records owner-account pass/fail evidence; live QA remains unknown until checked. **Depends:** none. **Modules:** existing Drive integration, setup notes, QA record. **Accept:** check sign-in, scope, browse/open, offline use, revocation, and failures on the owner account. **Risk:** expose credentials or Drive data; sanitize evidence. **Size:** M. **Approval:** Riko runs account checks.

## v0.4 — Remote companion overlay

### R04-01 — Capture existing LAN baseline

**Priority:** P0. **Outcome:** Riko records actual LAN discovery, pairing, presence, TLS pin and token behavior before remote changes. **Depends:** none. **Modules:** Python companion, Android companion client, existing v0.4 setup/protocol docs. **Accept:** owner executes candidate build/tests and a phone/laptop LAN session covering good pairing, denied/expired secret, wrong pin, revocation, pause/resume and restart; records failures and protocol 1 limits. This document performs none of those checks. **Risk:** server startup may be mistaken for Android acceptance. **Size:** M. **Approval:** Riko executes and accepts baseline evidence.

### R04-02 — Decide overlay provider and approval

**Priority:** P0. **Outcome:** ADR-01 recommends provider, account/control plane, relays, owner burden, and remote go/no-go. **Depends:** R04-01. **Modules:** decision record and owner setup documentation. **Accept:** compare overlay, WireGuard, and Headscale; document relay, availability, terms, maintenance, and exit. **Risk:** lock-in or assumed relay behavior. **Size:** M. **Approval:** **Riko approval required** before provider selection or remote implementation.

### R04-03 — Specify endpoint and identity contract

**Priority:** P0. **Outcome:** specify manual endpoint separately from laptop UUID and full DER SHA-256 pin, including DNS and route rules. **Depends:** R04-02; ADR-02. **Modules:** protocol docs, Android endpoint model, Linux listener configuration. **Accept:** addresses, membership, DNS, and mDNS never establish identity; same approved route for resolve/connect; no public fallback. **Risk:** route confusion or DNS rebinding. **Size:** M. **Approval:** Riko approves endpoint policy.

### R04-04 — Add explicit Linux LAN and overlay listeners

**Priority:** P0. **Outcome:** selected LAN and overlay listeners share process trust state but operate independently. **Depends:** R04-02, R04-03; ADR-02. **Modules:** Python listener startup, TLS, trust state, process lock, mDNS. **Accept:** selected-address binds only; reject wildcard/public; report listeners separately; mDNS failure does not stop overlay. **Risk:** exposure or ambiguous partial startup. **Size:** L. **Approval:** owner validates host policy.

### R04-05 — Route Android manual endpoint over approved VPN

**Priority:** P0. **Outcome:** manually configured remote endpoint uses approved VPN route with TLS pin and UUID checks. **Depends:** R04-03, R04-04; ADR-02. **Modules:** Android companion transport, endpoint validation, trust store. **Accept:** verify VPN routing, wrong pin/UUID, and unavailable VPN; no Wi-Fi-only path or fallback; LAN still works. **Risk:** Android-version route differences. **Size:** M. **Approval:** Riko validates on target devices.

### R04-06 — Expose remote mode, errors, and revocation

**Priority:** P1. **Outcome:** expose mode, endpoint, failures, and separate app-trust/overlay revocation. **Depends:** R04-03–05. **Modules:** Compose settings/status, Android trust state, Linux trust commands/docs. **Accept:** truthful network/revocation state; explicit retry; no fallback or silent re-pair. **Risk:** UI may imply both layers were revoked. **Size:** M. **Approval:** Riko approves user wording and recovery flow.

### R04-07 — Owner remote acceptance gate

**Priority:** P0 release gate after implementation. **Outcome:** owner verifies approved topology and rollback. **Depends:** R04-01–06. **Modules:** acceptance checklist, supported Android/Linux devices. **Accept:** Riko checks TLS/pairing failures, cancel/revoke/restart, topologies, network changes, and LAN regression; explain all failures. **Risk:** lab may not reflect owner networks. **Size:** M. **Approval:** Riko only; no agent may mark this verified.

## v0.5 — Explicit handoff

### R05-01 — Define protocol v2, grants, and limits

**Priority:** P1. **Outcome:** protocol 2 handoff contract preserves protocol 1 presence behavior. **Depends:** R04-01; ADR-03. **Modules:** protocol docs, Python API, Android client. **Accept:** specify negotiation, scoped grants, quotas, chunks, destinations, retention, idempotent status, and errors; reject unknown/oversized input. **Risk:** excessive file authority or ambiguity. **Size:** L. **Approval:** security and owner review before implementation.

### R05-02 — Android send to Linux receive

**Priority:** P1. **Outcome:** explicit Android file selection and upload to an approved Linux destination. **Depends:** R05-01 and accepted R04 transport for remote use. **Modules:** Android SAF picker, transfer repository/queue, Python receiver. **Accept:** preview file/size/destination; check permission; safe path; hash before commit; failures preserve source. **Risk:** path escape, overwrite, full storage. **Size:** L. **Approval:** user confirms each transfer.

### R05-03 — Linux send to Android SAF destination

**Priority:** P1. **Outcome:** Android pulls a selected Linux file into a user-chosen SAF destination. **Depends:** R05-01. **Modules:** Android SAF/document provider, Python catalog/API. **Accept:** no pushed paths; user selects source/destination; partial output is incomplete and safely removable/retryable. **Risk:** SAF provider write differences. **Size:** L. **Approval:** user confirms each transfer.

### R05-04 — Durable transfer lifecycle

**Priority:** P1. **Outcome:** durable transfer queue, progress, cancel/retry/resume, integrity, and history. **Depends:** R05-02, R05-03. **Modules:** additive Room transfer tables, WorkManager/repository, Linux staging. **Accept:** durable acknowledgements; no duplicate commit; recoverably report hash mismatch, disk full, cancel race, and uncertain commit. **Risk:** migration/crash may strand staged data. **Size:** XL. **Approval:** migration/recovery design reviewed.

### R05-05 — Read-only workspace context snapshot

**Priority:** P1. **Outcome:** selected stable record IDs and document references form a versioned read-only manifest. **Depends:** R05-01, R05-04. **Modules:** existing task `syncId`, transfer manifest/queue, companion viewer. **Accept:** snapshot only selected Task fields, timestamp/content fingerprint, selected file manifest and pending transfers; preview matches payload; compare fields rather than only timestamps; no SQLite copy/live editing; reject unsupported versions. Courses, notes and graph links may enrich a later version but do not block v0.5. **Risk:** stale task/file references. **Size:** L. **Approval:** owner approves selected context.

### R05-06 — Optional clipboard transfer

**Priority:** P2 deferred. **Outcome:** optional one-item clipboard transfer after separate approval. **Depends:** R05-01 and security review. **Modules:** Android clipboard API, Linux clipboard adapter, transfer history. **Accept:** explicit selection; no monitoring/overwrite; show expiry and destination. **Risk:** sensitive content and platform limits. **Size:** M. **Approval:** separate Riko approval required; not part of initial handoff.

### R05-07 — Structured sync decision only

**Priority:** P2 deferred; **ADR only**. **Outcome:** ADR decides whether sync is justified and defines causality, conflicts, tombstones, deletion, and recovery before implementation. **Depends:** R05-04; ADR-04. **Modules:** decision/data lifecycle only. **Accept:** no SQLite copy, silent merge, or sync implementation. **Risk:** data loss from undefined conflicts/deletion. **Size:** M. **Approval:** Riko approves any future sync proposal.

## v0.6 — Academic spaces

### R06-01 — Courses, assignments, lifecycle, and migrations

**Priority:** P1. **Outcome:** local courses and assignments with distinct academic lifecycle. **Depends:** accepted signature feature model; ADR-05. **Modules:** Room entities/DAOs/migrations, domain and Compose course screens. **Accept:** academic states never silently mutate TaskStatus; additive migration preserves v0.2 data; offline browse. **Risk:** duplicate semantics or migration loss. **Size:** XL. **Approval:** Riko approves lifecycle mapping before schema work.

### R06-02 — Reuse Radar for workload, deadlines, and history

**Priority:** P1. **Outcome:** course view groups work using existing `UrgencyCalculator` ordering; a Deadline Radar presentation remains proposed. **Depends:** R06-01. **Modules:** existing urgency/deadline logic, course queries and UI. **Accept:** no competing ranking; unknown workload stays unknown; preserve date/timezone rules; distinguish recorded/confirmed submission. **Risk:** false precision or ranking divergence. **Size:** M. **Approval:** product review of presentation.

### R06-03 — Lab submission evidence

**Priority:** P1. **Outcome:** assignments link reports, materials, and minimal submission/review ActivityRecords. **Depends:** R06-01. **Modules:** Room activity/attachment references, course detail. **Accept:** failed transfer is not submission; define evidence ownership/deletion; history works offline. R07-03 later extends the records into the unified Evidence Ledger. **Risk:** prepared file mistaken for submission. **Size:** M. **Approval:** retention decision under ADR-05.

### R06-04 — Templates, batch preparation, and calendar links

**Priority:** P1 MVP. **Outcome:** basic course template snapshots, previewed batch planning, and Calendar/Timeline links. **Depends:** R06-01–03. **Modules:** course template snapshots, per-item Room transactions, Timeline links and selection UI. **Accept:** preview every change; commit each item transactionally and report per-item partial success/failure for safe retry; preserve deadlines on cancellation/failure; authorize external actions. Complex custom templates remain P2. **Risk:** duplicate work or partial updates. **Size:** M. **Approval:** separate integration approval for external actions.

## v0.7 — Living Knowledge Vault

### R07-01 — Notes, document references, provenance

**Priority:** P1. **Outcome:** link local notes and document references to work. **Depends:** R05-01; ADR-05. **Modules:** Room/domain document reference model, Compose detail UI. **Accept:** distinguish reference/copy; URI/path is not portable identity; show provenance offline. **Risk:** stale reference mistaken for available file. **Size:** M. **Approval:** data retention decision.

### R07-02 — Mission Graph typed links and DAG

**Priority:** P1. **Outcome:** accessible lists/backlinks for associations and dependencies. **Depends:** R06-01, R07-01; ADR-05. **Modules:** Room relation table, repository transaction, link UI. **Accept:** transactionally reject dependency self/cycles, including concurrent writes; associations may cycle; graph view optional. **Risk:** inconsistent blockers. **Size:** L. **Approval:** relation and delete semantics approved before migration.

### R07-03 — Evidence Ledger corrections and privacy

**Priority:** P1. **Outcome:** record, correct, export, and delete history/evidence links. **Depends:** R06-01, R07-01; ADR-05. **Modules:** ledger table, attachment references, export. **Accept:** stable IDs, correction provenance, retention/deletion rules; checksums are not legal proof. **Risk:** false immutability claim or retained deleted files. **Size:** L. **Approval:** retention and export policy required.

### R07-04 — Search, saved search, orphan repair

**Priority:** P1 MVP. **Outcome:** bounded offline text indexing, saved searches, and missing-reference repair. **Depends:** R06-01, R07-01–03. **Modules:** Room FTS, search UI, integrity report. **Accept:** index only supported bounded text; respect deletion visibility; report/repair orphans without silent deletion; continue core work if indexing fails. Extended format extraction and visual graph remain P2. **Risk:** stale index or exposed result. **Size:** M. **Approval:** confirm indexed fields and retention.

## v0.8 — Personal Automation Engine

### R08-01 — Automation grammar, preview, permissions

**Priority:** P1. **Outcome:** validate and preview versioned trigger-condition-action rules. **Depends:** R06-01, R07-01; ADR-06. **Modules:** rule parser/model, Compose editor, permission checks. **Accept:** typed allowlist; reject unknowns; require review; dry run has no effects. **Risk:** excessive rule authority. **Size:** L. **Approval:** Riko approves authority and permitted actions.

### R08-02 — Durable execution, dedupe, loop and battery budget

**Priority:** P1. **Outcome:** local jobs run within explicit resource and causal limits. **Depends:** R08-01. **Modules:** Room outbox/jobs, WorkManager, event IDs, execution history. **Accept:** durable enqueue; idempotent retry/dedupe; bound loops, work, missed runs; disclose inexact timing. **Risk:** notification loops or battery drain. **Size:** XL. **Approval:** policy decisions in ADR-06.

### R08-03 — History, compensation, templates

**Priority:** P1 MVP. **Outcome:** inspect effects, undo safe reversible local actions and copy reviewed templates. **Depends:** R08-01, R08-02. **Modules:** run history, compensation handlers, template catalog. **Accept:** report applied/skipped/failed effects; preserve later edits during undo; template copies stay disabled; validate imports. Advanced templates and external compensation are P2. **Risk:** external state may not be reversible. **Size:** M. **Approval:** each new action reviewed separately.

## v0.9 — Unified Workspace

### R09-01 — Capture palette and global dashboard

**Priority:** P1. **Outcome:** quick local capture and dashboard connect work and deadlines. **Depends:** R06-01, R07-01, existing navigation. **Modules:** Compose navigation/dashboard, repository entry points. **Accept:** offline accessible capture reuses task/deadline behavior. **Risk:** competing prioritization. **Size:** L. **Approval:** UX review.

### R09-02 — Continuity, import/export, restore

**Priority:** P1. **Outcome:** resume selected context and recover data via explicit export/import/restore. **Depends:** R05-04, R05-05, migration recovery; ADR-03. **Modules:** manifest, backup/export format, validation and restore UI. **Accept:** preview; validate before mutation; failed restore preserves source; no DB-copy sync. **Risk:** duplicate/destructive restore. **Size:** XL. **Approval:** restore policy and destructive confirmation approved.

### R09-03 — Official LMS/extensions decision

**Priority:** P2 deferred; **decision only**. **Outcome:** assess official LMS APIs, permissions, and support cost. **Depends:** R06-03; ADR-07. **Modules:** research and product decision. **Accept:** no scraping, credential collection, or implementation; retain manual flow. **Risk:** policy/API changes. **Size:** M. **Approval:** separate Riko approval per integration.

## v1.0 — Reliable Release

### R10-01 — Migration and recovery

**Priority:** P0 before any schema-changing release. **Outcome:** owner-verified upgrade, interruption recovery, backup and restore for candidate Room changes. **Depends:** each proposed/implemented migration and its archive contract, before that release's acceptance. **Modules:** Room migrations, backup/recovery flow, upgrade test source. **Accept:** preserve v0.2 data; cover new relationships/attachments; explain/recover failures; no destructive release fallback. Repeat this gate for each schema release, not only v1.0. **Risk:** user data loss. **Size:** L. **Approval:** owner executes checks and accepts recovery behavior.

### R10-02 — Security review

**Priority:** P0 before transfer or remote acceptance. **Outcome:** review trust boundaries, grants, routes, inputs, storage and revocation. **Depends:** candidate R04-03–06 before R04-07; repeat for candidate R05-01–04 before G05 acceptance. **Modules:** Android transport, Python API, file handling, secret storage. **Accept:** review source and owner-run evidence for invalid trust, malformed/oversized input, path escape, full storage, cancel, partial revocation and secret redaction. **Risk:** file exposure or wrong peer. **Size:** L. **Approval:** security sign-off and Riko acceptance required.

### R10-03 — Performance, offline, accessibility, Android 9

**Priority:** P0 release gate. **Outcome:** demonstrate responsive, accessible offline use from minimum API 26 (Android 8); Android 9 remains an explicit acceptance target. **Depends:** implemented accepted features. **Modules:** Room query paths, Compose UI, network state, API 26 and Android 9 devices. **Accept:** test large lists, TalkBack/text, reduced motion, network/storage failure, and both API 26 and Android 9; record devices/limits. **Risk:** emulator-only confidence or older-device regression. **Size:** L. **Approval:** Riko performs device acceptance.

### R10-04 — Release, onboarding, documentation freeze

**Priority:** P0 after gates. **Outcome:** release with accurate onboarding and documented supported behavior. **Depends:** R03-01, all included G04–G09 gates, and R10-01–03. Deferred optional features are excluded explicitly. **Modules:** release configuration, onboarding, docs, changelog. **Accept:** feature freeze; Riko records release checks; docs separate shipped/proposed/unknown; explain rollback and support limits. **Risk:** unverified behavior presented as shipped. **Size:** M. **Approval:** Riko owns final release decision.

## ADR register — recommendations, not approvals

- **ADR-01 — Transport provider, relay, account:** compare candidates, relays/control planes, owner burden, availability, and exit. Choose only after Riko reviews privacy and operations.
- **ADR-02 — Endpoint and routing:** decide address families, overlay ranges, interface, DNS, and Android VPN routing. Recommend explicit-interface IPv4 first; defer IPv6 until route and identity tests cover it.
- **ADR-03 — Transfer limits:** define per-peer/action grants, destinations, quotas, staging cleanup, and retention. Recommend narrow grants, explicit selection, and bounded quotas.
- **ADR-04 — Sync conflicts/deletion:** define causality, merge UX, tombstones, retention, and recovery. Defer implementation until handoff proves need and recovery is testable.
- **ADR-05 — Academic/graph/ledger data:** settle IDs, task mapping, edge deletion, attachment ownership, corrections, export, and retention. Recommend additive Room tables; no legal-proof claims.
- **ADR-06 — Automation authority:** define actions, permissions, execution/missed-run budgets, and compensation. Recommend bounded inexact schedules and reversible local actions first.
- **ADR-07 — LMS/extensions:** assess official APIs, permissions, policy, and support per integration. Keep manual flow unless Riko approves a specific extension.

## Owner acceptance checklist — intentionally unchecked

No verification is claimed. Riko records device, OS, network, result, and evidence for each check.

- [ ] Build, unit tests, instrumentation tests, and lint on the accepted release candidate.
- [ ] Supported Android 9 device: TalkBack, large text, reduced motion.
- [ ] Companion TLS: invalid pin/UUID/secret, cancel, revoke, restart, re-pair.
- [ ] Network: LAN/overlay, transitions, no Wi-Fi, VPN/DNS failure, metered connection.
- [ ] Policy: invalid interface, no public/wildcard bind, partial revocation, denied authorization.
- [ ] Transfer: full storage, oversized input, interruption, cancel/commit race, retry, bad hash, destination conflict.
- [ ] Upgrade v0.2 data; interrupt migration; backup/restore; failed restore preserves source.
- [ ] Confirm offline capture, browsing, and deadlines after upgrade.
