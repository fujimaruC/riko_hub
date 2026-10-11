# 06 — Signature Features

**Status:** All entities, relationships, and contracts in this document are **PROPOSED**. No schema or release behavior is implied. Sequence and acceptance authority remain with [03_MASTER_ROADMAP.md](03_MASTER_ROADMAP.md), with implementation candidates tracked in [09_EXECUTION_BACKLOG.md](09_EXECUTION_BACKLOG.md).

## Mission Graph

The Mission Graph connects eight record types: **Task**, **Course**, **Event**, **Note**, **Document**, **Material**, **Evidence**, and **Milestone**. Existing Task and Event rows already have stable UUID `syncId` values; reuse those identities rather than add duplicate graph IDs. New record types need stable UUIDs. Relationship rows reference source and target UUIDs, their type, timestamps, and optional provenance. Room remains the store; no graph database is proposed.

There are two edge kinds:

- **Dependency**: only Task and Milestone records may block one another. Examples: “submit lab” Task depends on “finish analysis” Task; “course project complete” Milestone depends on “submit report” Task; one Milestone may depend on another Milestone. A dependency affects readiness and belongs to a directed acyclic graph.
- **Association**: all other relationships provide context and never block work. Examples include Task–Course (“belongs to”), Task–Event (“for class”), Task–Note (“explained by”), Task–Document (“uses brief”), Task–Material (“uses reading”), Task–Evidence (“supported by”), Course–Event (“meets at”), Course–Material (“contains”), Note–Document (“references”), and Evidence–Document (“is a copy of”). Associations may be cyclic.

**Primary story and daily example:**

1. Between classes, the student creates “Submit database lab” as a Task, gives it a deadline, and associates it with the Database Systems Course and the lab Event.
2. They add a dependency on “Finish query results,” associate the task with the lab brief Document and SQL reference Material, and link a Note explaining a query.
3. After work, they attach the report as Evidence, associate it with the submission task, and mark the dependency complete.
4. From the task, course, or evidence detail, accessible backlinks lead to the same records; nothing must be re-entered in separate lists.

Dependency insertion is checked in the same database transaction as the write. Reject self-links and any edge that closes a cycle, leaving stored edges unchanged. Serialize validation and writes; if a concurrent edit makes the second edge invalid, reject it and identify the conflict. Do not silently drop an edge or display stale blocker state.

The default is a readable linked-record list and backlinks, with relationship kind and blocker state announced in text. Users can open and remove links without a visual canvas. A graph view is an optional stretch feature.

**MVP:** one accepted vertical slice with stable identities, association and dependency links, backlinks, list navigation, and atomic cycle validation. Avoid implementing every type before a useful task/course workflow ships.

**Advanced:** filtered graph exploration and richer provenance after list navigation proves useful.

**Failure, performance, privacy:** bound related-record queries and paginate long lists; link failure must not hide the record itself. Associations never affect ready/blocked calculations. Keep links local unless explicitly selected for a transfer. The owner must decide whether deleting a target removes, marks, or retains its links; explain the effect before deletion and never leave an unexplained dangling link.

**Acceptance:** all eight types can appear in the defined link examples; Task/Event reuse their existing `syncId`; only Task/Milestone dependency edges block; association cycles are allowed; dependency self-links/cycles and concurrent conflicts fail atomically; backlinks resolve; and assistive technology can inspect and edit links without a graph canvas.

## Evidence Ledger

The Evidence Ledger is a chronological, searchable account of meaningful work events: draft attached, review requested, submitted, feedback received. It is continuity history, not immutable legal proof. A proposed ActivityRecord includes stable event ID, linked record IDs, event type, event and recorded times, actor/device where available, summary, and references to attachments or superseding events. Task and Event identities continue using existing `syncId` values.

**Primary story and daily example:**

1. After a student submits a lab through an external system, they add a “submitted” ActivityRecord to the assignment and attach the report reference.
2. When feedback arrives, they add a “feedback received” record and link the marked file and review Note.
3. Searching the assignment history shows the sequence and the files the student chose to retain.

Ordinary audit fields (event ID, original event/recorded time, type, and original summary) are not rewritten to make a correction. A correction is a new event that supersedes or corrects the earlier event and points to it. The UI shows both and explains the relation; this is an append-oriented user history, not a tamper-proof audit trail.

A checksum or version metadata can flag that attachment bytes changed, but cannot certify origin, authorship, or submission. References and copies are distinct. If multiple records refer to one locally stored attachment, deletion must account for reference count: remove the user's selected reference, and delete the underlying bytes only when no retained reference needs them under the chosen policy. An already exported copy cannot be recalled by deleting the local source. Before implementation, the owner must choose attachment/history retention and deletion behavior and state clearly what deletion does and does not remove. For new ledger records, user-confirmed deletion permanently removes the local row; attachment bytes are removed only after no retained local record references them. Existing Task/Event deletion keeps its current soft-delete contract unless separately migrated. A previous export is a separate copy and cannot be revoked by deleting the local source.

**MVP:** add and browse a chronological ActivityRecord, search history by linked record and supported text, link existing attachment references, correct by superseding, and export only user-selected history/evidence. Export is part of the MVP because selected evidence needs a user-controlled way out; it is not automatic archival.

**Advanced:** attachment version comparison, retention presets, and richer export layouts.

**Failure, performance, privacy:** page long histories; if an attachment is unavailable or changed, retain the event and offer relink/inspect actions. Local deletion must be explicit and accurately describe shared-reference and prior-export limits. Transfers include ledger events only when selected. No automatic upload or legal-proof claim.

**Acceptance:** history can be browsed and searched offline; correction preserves the original event and points to it; selected export includes only approved items; attachment checksums are labeled as change detection; reference-aware deletion does not remove bytes still referenced under the approved policy; and retention/deletion consequences are disclosed before use.

## Continuity Engine

The initial v0.5 continuity contract is intentionally small: a versioned read-only snapshot of selected Task records using their existing `syncId`, title, deadline and captured `updatedAt`, a fingerprint of the canonical selected task fields, selected file manifest, and pending transfer IDs/status. The fingerprint detects changed snapshot content; it is not a causal revision or authorship proof. It is not a copy of Room, not a general export of all records, and not live editing. Course, Note, Event, and graph context can be added as optional later enrichment without blocking the v0.5 snapshot.

The user selects the tasks and files and previews exactly what leaves the phone. Only selected metadata is shared. Each transfer capability is granted to the paired peer for the selected operation; the global companion token does not by itself authorize all Room rows or arbitrary files. The Linux companion needs only a read-only context viewer for the MVP; a simple CLI/list view is sufficient, and a heavy desktop UI is optional. For Linux-to-phone handoff, Android initiates a pull into a user-selected SAF destination; Linux never opens an inbound Android listener.

**Primary story and daily example:**

1. The student selects “Submit database lab,” confirms its title/deadline, picks the report and lab brief, and reviews pending transfers before leaving campus.
2. The paired Linux companion shows that selected snapshot in a read-only list and receives only the approved files.
3. At home, the student asks Android to pull the report back into a chosen destination; the phone previews the source and confirms the write.
4. On return to the phone, compare the captured task fields/fingerprint with current local fields and ask what to do if they differ. `updatedAt` is a timestamp hint, not the sole conflict or freshness test. Local Room remains authoritative; never auto-merge edits.

A changed or stale file is marked changed and can be skipped or reselected. If a file is missing, let the user choose a replacement or relink; never transfer it automatically. Pending transfers remain visible while offline and can be retried or canceled. Define expiry/TTL before implementation; expiration must cancel stale authority and report what metadata remains.

**MVP:** versioned minimal Task snapshot, file manifest and pending-transfer status; explicit per-item preview and grants; Android-initiated upload and pull; read-only Linux viewer; and visible pending/completed/failed/canceled/expired state.

**Advanced:** optional Course/Note/Event context, richer destination mapping, resuming large transfers, and selective return snapshots after separate acceptance.

**Failure, performance, privacy:** bounded transfer work and metadata; unavailable devices do not block local editing. Unsupported versions fail clearly. A source or revision mismatch prompts the user; local data wins unless they explicitly choose a supported action. Verify received file hashes, and preserve the source after failure. Pairing, endpoint identity, and authorization remain governed by [08_ARCHITECTURE_AND_SECURITY.md](08_ARCHITECTURE_AND_SECURITY.md).

**Acceptance:** snapshot includes only selected Task fields, timestamp/fingerprint, selected file manifest, and pending transfers; Linux can inspect it read-only without a heavy UI; Android initiates both directions; changed task fields prompt even when timestamps match; missing/changed files can be skipped or relinked without auto-transfer; offline pending work can be retried/canceled/expired; and neither side copies or merges SQLite.

See [05_V05_SYNC_AND_HANDOFF.md](05_V05_SYNC_AND_HANDOFF.md) for protocol boundaries and [09_EXECUTION_BACKLOG.md](09_EXECUTION_BACKLOG.md) for sequencing.
