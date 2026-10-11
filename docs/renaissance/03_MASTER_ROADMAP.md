# Master roadmap: v0.4 to v1.0

Status: **PROPOSED**, except the current-state baseline explicitly identified below. Release numbers describe product scope, not dates or automatic authorization to implement it. Riko controls all execution evidence and release acceptance.

## Starting point and sequencing

v0.2 is owner-accepted. v0.3 source is checkpointed; OAuth setup and installation are owner-reported, while live Drive acceptance remains separate. v0.4 LAN companion source exists and a Linux startup is owner-reported. Android and cross-device acceptance remain unconfirmed. Remote v0.4 access is new work, not an enabled setting.

The critical path is: validate existing LAN behavior; decide remote transport and endpoint policy; implement and accept remote presence; negotiate transfer permissions/protocol; accept bounded bidirectional handoff; establish course and document identities; build useful links/history; add guarded automation; unify workflows; freeze and prove recovery for v1.0.

Course design and local note prototypes can proceed after their data decisions without waiting for remote networking. They must not make core offline planning depend on an overlay or Google account. Structured database synchronization is not on the essential file-transfer critical path. Live Drive QA is a parallel acceptance track, not a reason to repeat accepted widget work.

P0 protects data, trust, recovery and essential behavior. P1 improves frequent workflows. P2 adds optional customization or advanced views. A stretch goal never blocks a useful MVP unless it becomes an explicit accepted requirement.

## v0.4 — Anywhere Companion

**User problem:** a trusted laptop becomes unreachable when the phone leaves its LAN; current source also needs its first complete Android acceptance evidence.

**MVP:** preserve LAN discovery and add an independent manual private-IP/hostname path over an owner-approved private overlay. Retain persistent device trust, explicit pairing, certificate pinning, revoke/forget and reconnect. Show selected LAN/remote transport mode, recent authenticated status, last check and actionable failures. Handle Wi-Fi/mobile/VPN changes without treating a hostname or discovery entry as authorization. Tailscale is the leading initial candidate, with alternatives in [04](04_V04_REMOTE_COMPANION.md).

**Stretch:** easier pairing-code entry, additional saved endpoints for a trusted laptop, IPv6 and multiple laptops after the single-laptop flow is reliable. LAN and remote reachability in the MVP must not require public port forwarding.

**Technical prerequisites and dependencies:** existing Android/Linux compilation and owner QA; ADR-01 provider/relay/account decision; ADR-02 address/routing policy; endpoint configuration separate from trust identity; explicit Linux interface listeners sharing one state; Android VPN-aware routing. Existing exact protocol-1 identity/presence checks remain intact.

**Exclusions:** file transfer, clipboard, planner sync, arbitrary remote commands, remote desktop, public companion exposure and automatic approval. Remote initial pairing still requires the existing explicit laptop approval; pairing locally before travel is the default setup workflow.

**Acceptance:** Riko demonstrates LAN discovery, manual numeric and hostname connection, two genuinely different networks, wrong-pin/expired-secret/revoked-token rejection, network transition recovery and offline forget guidance. Connection labels describe selected transport and recent authentication, not an inferred physical distance or permanent socket. Core planning works without overlay enrollment.

**Risks:** Android 9 overlay-client support, VPN coexistence, physical-network binding, DNS routing, NAT/relay dependence, address churn and provider policy. A Linux listening message alone does not pass this gate.

**Release gates:** G04-A existing LAN accepted; G04-B owner transport decision and security design approved; G04-C remote matrix and revocation evidence accepted. Work items R04-01 through R04-07.

## v0.5 — Universal Handoff

**User problem:** device presence does not yet help move selected work or resume its context.

**MVP:** multiple-file selection, Android-to-Linux and Linux-to-Android transfer, explicit receiver destination/consent, bounded queue, progress, cancel, safe retry and recent history. Verify full-file checksums before declaring success. Resume only when source identity and destination durability can be established; otherwise explain restart. Send a read-only workspace context snapshot with task identity, selected file references and pending transfers. The Android client initiates both upload and download requests; no incoming phone listener is required.

**Stretch:** cooperative pause at chunk boundaries, explicit clipboard handoff with short retention, richer Linux context viewing and improved resumable-provider support. There is no hidden clipboard observer. Pause is unavailable when safe continuation cannot be guaranteed.

**Technical prerequisites and dependencies:** G04-C; ADR-03 transfer limits, capability grants and destination policy; a proposed version-2 protocol alongside existing version 1; durable queue/file manifests; source revalidation; storage reservations; safe temporary-file promotion; Android SAF provider behavior. [05](05_V05_SYNC_AND_HANDOFF.md) defines the proposed contract. Existing 8 KiB JSON limits must not become unlimited binary bodies.

**Exclusions:** copying SQLite databases, automatic task replication, unrestricted directory browsing, automatic opening/execution of received content and live simultaneous editing. Structured sync receives a separate design decision, not an implicit implementation commitment.

**Acceptance:** selected disposable files transfer in both directions with matching hashes; filename collisions preserve existing files; interrupted/retried requests do not duplicate completed transfers; cancel/commit races produce truthful results; insufficient storage and revoked permission stop safely; unrelated files and task data remain inaccessible. Context can open an available file or explain a missing/stale reference without silently transferring it.

**Risks:** unknown SAF lengths, revoked URI grants, non-seekable sources, remote disconnects, provider non-atomic writes, disk exhaustion, changed source files and stale context. Queue history must not retain file content indefinitely.

**Release gates:** G05-A one-file vertical path and protocol rejection; G05-B both directions, queue and interruption/recovery; G05-C context and privacy acceptance. Clipboard is separately opt-in. R05-07 only specifies the future structured-sync model; it does not block accepted file transfer.

## v0.6 — Academic Command Center

**User problem:** assignments, labs, course materials and submission proof are fragmented across places and states.

**MVP:** Course Spaces; typed assignments/labs/exams/projects/recurring activities; explicit lifecycle from received to completed; course-linked materials; existing checklists plus bounded child-task/dependency relations; Deadline Radar explaining existing urgency; workload by course; basic lab report stages; submission links/evidence; deadline-change history; Calendar/Timeline links. Provide template instantiation and previewed batch actions for repetitive work.

**Stretch:** richer custom templates, advanced workload comparisons and bulk course setup. Estimates stay optional; missing estimates must be visible rather than counted as zero effort.

**Technical prerequisites and dependencies:** ADR-05 course/link/history entities, stable IDs and lifecycle mapping; reviewed Room migrations; backup export/import coverage for new records; reusable document references; a small typed dependency relation compatible with the later Mission Graph. Existing three-state TaskStatus must not be silently reinterpreted as the six-state academic lifecycle.

**Exclusions:** unofficial LMS scraping, credential collection, automatic submission, grade prediction and a campus administration system. Remote access is optional; course planning must work offline.

**Acceptance:** create a course and assignment from a template; plan and complete its checklist; record a changed deadline without losing history; inspect urgency reasons and unknown workload estimates; attach submission evidence; distinguish submitted from completed; undo or explain partial batch outcomes. Calendar edits and task recurrence remain consistent. Restore reproduces course/task/material relationships.

**Risks:** migration loss, duplicate recurrence generation, ambiguous submission state, dangling links, cycle handling and conflicting deadline edits.

**Release gates:** G06-A schema/backup/lifecycle fixtures; G06-B one real academic workflow with disposable data; G06-C accessible lists and preservation of accepted task/reminder behavior.

## v0.7 — Living Knowledge Vault

**User problem:** notes and documents are stored but disconnected from the work they support.

**MVP:** a local note/document library, tags and collections, typed bidirectional links and backlinks among tasks, courses, notes and materials; practical Mission Graph dependency lists; local text indexing for supported text only; saved searches/filters; chronological Evidence Ledger; explicit evidence-bundle export; provenance and file-version metadata; detection and guided repair of broken links and orphaned documents.

**Stretch:** optional visual graph exploration, broader format extraction and richer related-content suggestions based on explicit links and deterministic rules. No AI dependency. Unsupported binary formats remain searchable by metadata without pretending their contents were indexed.

**Technical prerequisites and dependencies:** stable entity and file references from v0.6/v0.5; ADR-05 deletion/retention rules; bounded local indexing with rebuild paths; schema and backup updates; ledger corrections/export format; checksums as content identifiers, not legal attestations. Reuse v0.6 deadline/action records rather than create competing histories.

**Exclusions:** a graph database server, mandatory node-canvas editing, blockchain, certified immutability, automatic cloud ingestion and unrestricted document crawling.

**Acceptance:** navigate from an assignment to a note/material and back; reject dependency cycles while permitting associative cycles; locate supported offline text; correct a ledger entry while showing history; export a selected evidence bundle; delete personal records/attachments under an explicit policy; repair a missing file without changing its identity silently. Core tasks remain usable when indexing fails.

**Risks:** indexing cost on older hardware, polymorphic link integrity, stale file handles, privacy leakage in exports, misleading provenance and accidental deletion of shared attachments.

**Release gates:** G07-A link integrity and list navigation; G07-B bounded indexing and ledger recovery/export; G07-C storage, accessibility and deletion checks. A polished graph canvas is not a gate.

## v0.8 — Personal Automation Engine

**User problem:** repetitive academic actions consume attention and are easy to miss.

**MVP:** a typed trigger/condition/action grammar, templates, local scheduled actions, task/deadline/material/course triggers, notification routing, checklist/template creation and recurring workflows. Every rule has a preview/dry run, explicit allowed actions, execution history, deduplication, loop/rate limits and a disable switch. Provide undo or compensating actions only where meaningful.

**Stretch:** richer rule composition and permission-scoped optional handoff actions after transfer consent behavior is established. Rules cannot silently acquire new authority after an update.

**Technical prerequisites and dependencies:** ADR-06 permission and execution budgets; durable event IDs and transactional outbox for committed domain changes; idempotent actions; causation chains; scheduler integration with existing WorkManager/reminders; local-time/DST policy and missed-run handling. Rules consume accepted events, not transient UI state.

**Exclusions:** arbitrary scripts, remote shell, exact execution-time guarantees, unrestricted background services, silent uploads, hidden clipboard access and AI integrations.

**Acceptance:** dry-run a deadline rule, approve it, replay its trigger without duplicate work, inspect why it ran or skipped, stop a loop, disable it, and undo a reversible local action. Battery restrictions or denied notifications show delayed/blocked status without a false success. Existing reminder delivery is not duplicated by rule scheduling.

**Risks:** event storms, recursive templates, unbounded logs, duplicate notifications, timezone shifts and confusing compensation with reversal of an external effect.

**Release gates:** G08-A grammar/permission/dry-run contract; G08-B crash/retry/idempotency and loop controls; G08-C owner battery and delayed-execution checks. No rule may bypass a P0 safety gate for convenience.

## v0.9 — Unified Workspace

**User problem:** individually useful systems still require too many disconnected steps.

**MVP:** a configurable but bounded dashboard, Universal Quick Capture, command palette, global search, task/resource navigation and cross-device continuity. Support user-defined workflows through existing approved automation, richer import/export, restore previews, activity history and recoverable edits. All navigation has visible, accessible alternatives to gestures and graphs.

**Stretch:** official LMS adapters using approved APIs or allowed imports, and optional extensions with strict capability boundaries. These are independent opt-in releases; absence of an authorized campus API is a reason to defer, not to scrape credentials.

**Technical prerequisites and dependencies:** stable capture/search/link APIs, versioned context envelopes, explicit stale/missing-file behavior, archive coverage for all local entities and files, and a decision about any structured synchronization. A context snapshot alone must not advertise bidirectional database editing.

**Exclusions:** forcing every feature into Home, a second design system, unlimited dashboard widgets, simultaneous collaborative editing, mandatory cloud accounts and untrusted executable plugins in the app process.

**Acceptance:** capture an item offline, organize it into a course/task, find its material, resume selected context on the other device and recover a mistaken edit. Import/export round trips retain supported relationships and report unsupported fields. Optional integrations remain disabled without explicit authorization.

**Risks:** UI overload, command discoverability, duplicate capture, stale context, archive format drift and extension permission creep.

**Release gates:** G09-A coherent daily workflow; G09-B restore-preview and continuity recovery; G09-C accessible onboarding and optional-integration isolation. Structured sync stays deferred if its separate model/recovery gate has not passed.

## v1.0 — Reliable Release

**User problem:** accumulated features are useful only if personal data and daily workflows remain dependable.

**MVP:** stabilize accepted scope. Prove migrations and recoverability, review security, measure battery/storage/memory behavior, confirm offline operation, accessibility and Android 9 compatibility, improve actionable errors, finalize onboarding and maintainable documentation, and establish release/signing/upgrade procedures.

**Stretch:** small usability corrections justified by acceptance evidence. No new product subsystem is a v1.0 stretch goal.

**Technical prerequisites and dependencies:** all included feature gates accepted; inventory of retained data/credentials/permissions; representative upgrade fixtures and backups; supported-version matrix; explicit recovery path when downgrade is unsupported. Optional unaccepted features are disabled or deferred instead of delaying reliability indefinitely.

**Exclusions:** expanding scope to meet a marketing feature count, silently raising minimum Android support, destructive migration fallbacks and claiming a security certification from source review.

**Acceptance:** owner-approved upgrade/restore exercises preserve records and relationships; revoked devices cannot use accepted capabilities; offline workflows remain available; large text, TalkBack and non-gesture navigation work; measured device budgets are documented; storage exhaustion and interruption recover truthfully; release notes state known limits and support policy.

**Risks:** insufficient real-device coverage, compatibility debt, uncontrolled attachments/logs, missing archive members and assumptions about external providers.

**Release gates:** G10-A feature freeze and migration/recovery evidence; G10-B independent security review and owner compatibility/performance evidence; G10-C signed-release readiness, onboarding/support documents and explicit Riko acceptance. v1.0 is a reliability milestone, not permission to add features endlessly.

## Cross-release controls

- Every migration adds archive/import and recovery coverage before its release is accepted. Raw Room replication is never a shortcut.
- Every external side effect requires an explicit capability and truthful failure/compensation semantics.
- A provider outage must not block local planning. Drive, overlay networking and file handoff have separate availability states.
- Current UI, urgency and reminder behavior remain the compatibility baseline. Proposals requiring changes must identify the user-visible difference.
- All gates are initially **PENDING** in this suite. Riko records executions; source inspection and documentation completion do not pass a release gate.

Implementation order, stable IDs, approval decisions and the owner checklist are in [09 Execution backlog](09_EXECUTION_BACKLOG.md).
