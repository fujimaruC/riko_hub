# v0.6 Phase 2 — Academic Assignment Lifecycle

The owner authorized Phase 2 after the Phase 1 source handoff. Work continues on `feature/v04-laptop-companion` at `947b057f230c8083512e7553ce9a5aab7de89507`, preserving the uncommitted Course Spaces implementation. Builds, tests, lint, device checks, dependency installation, and Git operations remain owner-controlled.

## Scope and task compatibility

An academic assignment is optional metadata attached to one existing Planner task. It does not duplicate the task, checklist, deadline, priority, estimate, or reminder. Any saved task can use the academic workflow with or without a course. Ordinary tasks require no academic record and retain their existing behavior.

The six academic states are Received, Planned, In Progress, Review, Submitted, and Completed. Academic state and generic TaskStatus are independent. Neither Submitted nor academic Completed completes the Planner task, generates a recurring successor, or changes a checklist. Generic task completion and reopening likewise leave the academic state unchanged. The UI explains this distinction and presents both states.

Assignments can begin at a manually selected state to support existing work. Before submission or completion, manual state changes permit progress, skipped steps, and corrections. Submitted permits Review, In Progress, or Completed. Completed permits reopening to In Progress. Same-state edits are allowed. The explicit Reopen action uses In Progress.

Entering Submitted records the latest manual submission timestamp. Reopening retains that timestamp and the optional submission reference. A later submission replaces the latest timestamp; this phase does not implement a submission event ledger. These fields describe the user's record, not confirmation by a university, Drive, Companion, or LMS. Completed without formal submission is allowed.

The repository treats a null submission-reference argument as omitted and retains the existing reference. An explicitly blank edited reference clears it. Status-only reopening therefore preserves submission details.

The academic editor saves separately from unsaved task fields. Save task changes in the existing task editor to change the current deadline or checklist. No new subtask or dependency system is introduced; this phase uses the existing checklist capabilities.

Course detail displays its related assignments and opens their existing task editor. Archived courses retain existing associations; new associations require an active course. Clearing a course association leaves the assignment and history intact. Materials, SAF/Drive integration, lab templates, Deadline Radar, the academic dashboard, and a Home summary remain outside this phase.

## Deadline preservation

Enrollment records the persisted task's deadline and date-only/time flag as the original deadline, including an originally missing deadline. Earlier edits made before enrollment cannot be reconstructed from the existing database.

Once enrolled, every deadline or date-only/time change through the shared TaskRepository write path appends a history row in the same transaction as the task change. This covers edits from Tasks, Home, Timeline, and the course task editor. Removing a deadline also records a change. The original deadline is never replaced. A failed transaction preserves both task and assignment data.

History stores previous and new values with timestamps and stable identities. It is read-only in this phase. Generic status or checklist edits without a deadline change add no deadline-history row. Recurrence successors remain ordinary tasks; academic enrollment and submission data are not copied automatically.

If a user manually enrolls a generated successor, recurrence undo and redo preserve that work. Reopening its parent cannot hide the enrolled assignment; completing its parent again cannot restore an enrolled successor that the user explicitly deleted. The same guards protect restored assignment records.

## Database and deletion

The actual starting source database version is **3**, following Phase 1. Phase 2 advances it to **4** using additive `MIGRATION_3_4`, preserving the earlier migration chain and all task, event, course, and timetable rows. No destructive fallback is used.

New tables:

- `academic_assignments`: local numeric primary key, unique stable `syncId`, unique `taskId`, nullable `courseId`, academic state, submission fields, original deadline fields, and creation/update timestamps.
- `academic_deadline_changes`: local numeric primary key, unique stable `syncId`, assignment owner, previous/new deadline fields, and change timestamp.

Task deletion through the existing UI remains a soft deletion: assignment data and history survive and are hidden from normal assignment lists until task restoration. A physical task deletion cascades to its assignment; a physical assignment deletion cascades only to its owned deadline history. A physical course deletion sets the optional course association to null. No assignment deletion or physical task/course deletion is exposed by this phase.

Numeric keys remain local. Backups carry stable task, course, assignment, and history identities and resolve them to local keys during transactional import. Missing or malformed relationships fail validation rather than silently creating detached records.

Authentic Room schema exports for versions **3 and 4** require the owner's builds. Agents do not generate schemas or fabricate identity hashes. Migration tests require those generated assets.

## Backup compatibility

New exports use JSON format **3**, adding assignments and nested deadline history. Formats **1 and 2** remain accepted and leave existing academic records intact. Task records keep the original three-state status schema. Course records retain the Phase 1 representation.

Exports include assignment data for soft-deleted tasks so restoration remains possible. Preview and import retain the existing local-wins policy, include assignment/history counts, and reject stale previews when academic data changes. Conflicting task or assignment ownership prevents importing incompatible academic records. Records and history count toward the existing bounded export/import limits.

Archives remain explicit plaintext JSON, not file archives or encrypted backups. No service credentials or external submission authority are exported. Older app versions cannot import format 3; upgrade the receiving app and retain prior backups.

## Owner verification and phase boundary

Focused source tests cover lifecycle separation, transitions, submission/reopen behavior, course association, deadline preservation, rollback, task deletion/restoration, migrations, backup compatibility, and UI behavior. Test source does not establish compilation or runtime correctness.

Riko must generate and inspect the Room schemas, build, execute the focused tests, and verify disposable database upgrades before accepting runtime behavior. Verify enrollment without a course, archived-course handling, all lifecycle transitions, Submitted while the task remains unfinished, generic completion without academic changes, reopening with retained submission data, null and date-only deadline changes, every shared task editor, backup conflicts and stale previews, offline behavior, and accessibility on the supported phone.

No builds, Gradle tasks, tests, lint, ADB, application launches, services, dependency installation, commits, pushes, merges, or branch changes are executed by agents. Stop after Phase 2 and wait for the owner's approval before Phase 3.
