# 07 — Academic Workflow, Knowledge Vault, and Automation

**Status:** v0.6 academic workflow, v0.7 knowledge features, and v0.8 local automation are proposals. All new entities and contracts are **PROPOSED**; they do not authorize implementation or alter current task behavior. See [03_MASTER_ROADMAP.md](03_MASTER_ROADMAP.md) and [09_EXECUTION_BACKLOG.md](09_EXECUTION_BACKLOG.md).

## v0.6 — Academic Course Spaces

Academic Course Spaces organize course work around existing tasks, deadlines, events, documents, materials, and evidence. Proposed Course and Assignment records can group work; Assignment has an academic state separate from existing TaskStatus. Its states are **received**, **planned**, **in progress**, **review**, **submitted**, and **completed**. Never silently map these six states onto the current NOT_STARTED/IN_PROGRESS/DONE task states. A linked Task keeps the current task lifecycle unless a separately reviewed migration and explicit mapping are approved.

A Course Space should answer: what is due, what is underway, what awaits review, what changed, and where are the supporting files? It includes assignment and lab dates, linked Events and Materials, user-entered work estimates, checklists/dependencies, workload views, and submission history. Unknown effort stays unknown. A course template is a versioned snapshot of starter assignments, checklist rows, suggested workload, and links; applying it previews the new records. Later template edits do not silently rewrite courses already created from it. Updating an existing course is a separate explicit previewed operation.

The proposed lab workflow has concrete stages: **received → planned → in progress → ready for review → submitted → feedback received/completed**. Stage history records the change and its source; it does not itself prove external submission. A submission ActivityRecord can distinguish “I marked this submitted” from a status confirmed by an authorized integration. Preparing, opening, or transferring a file is not submission.

**Primary story and daily example:**

1. A student receives “Database Lab 4,” adds it to the Database Systems Course, records its deadline and lab brief, and applies a template snapshot with steps: inspect schema, run queries, write report, review, submit.
2. The course view groups the assignment under its due date, shows its unknown workload until the student estimates hours, and links the next class Event and course Material.
3. The student records 90 minutes of estimated work and marks the assignment in progress. A proposed Deadline Radar view explains the existing `UrgencyCalculator` result for the linked Task; it does not invent a second urgency score. For example, a high-priority task due today remains ranked by existing urgency even when another course has a larger estimated workload. Workload totals are a separate planning measure.
4. Before a batch action creates next week's repeated lab checklist, the student previews every row. A failed write reports which entries succeeded and leaves the course in a consistent state; submission evidence is added only after the student confirms what happened.

Calendar/Timeline links show class Events and assignment deadlines together. Workload may be summarized by course and week from known estimates, with unestimated items counted separately as unknown. Do not display unknown as zero, predict grades, or let an aggregate workload reorder tasks behind the existing urgency policy.

**MVP:** Course and Assignment grouping; distinct academic state; deadline and course links; lab stages/history; optional workload estimate; checklists and accepted typed dependencies; template snapshot application; previewed batch planning; Calendar/Timeline links; links to Materials and submission evidence. Course browsing and local editing work offline.

**Advanced:** custom template editor, workload comparisons, batch course setup, and official LMS writes. Each LMS integration needs its own authorized API, permission scope, and confirmation flow. The manual record path remains available.

**Failure, performance, privacy:** missing material never hides the assignment. Bound course queries and keep local deadlines visible if calendar data is unavailable. Batch actions preview changes and either commit atomically or report per-item partial outcomes with a recovery route. Export and external submission are deliberate actions; failed uploads never appear submitted. Course data is private local data.

**Acceptance:** academic state never mutates TaskStatus; template application creates a reviewable snapshot and does not later change silently; deadline/history survives edits; unknown estimates remain explicit; proposed Radar uses existing urgency ordering and explains it; batch failures are recoverable; and user-recorded vs integration-confirmed submission is distinguishable offline.

## v0.7 — Living Knowledge Vault

The v0.7 knowledge layer connects notes, document references, materials, evidence, tasks, courses, and events. Mission Graph associations and dependency lists, backlinks, supported-text indexing, saved searches, Evidence Ledger history, selected evidence export, provenance, and guided orphan repair belong to this version. New note/document/link/event entities remain **PROPOSED**. Reuse Task and Event `syncId` identities where they already exist. A visual graph and extraction from additional file formats are optional P2 work; list navigation and bounded supported-text indexing are the default.

**Primary story:** From the lab assignment, the student opens its brief, a Note explaining the query, and prior Evidence. A saved search for “database lab reports” finds supported text offline. If the file was moved, the app reports the stale reference and offers relink rather than silently changing identity.

**MVP:** typed associations/dependencies with transactional DAG rules, accessible backlinks, bounded indexing for supported text, saved searches, searchable chronological ActivityRecords, corrections that supersede earlier records, selected evidence export, provenance, and report/repair for missing references.

**Advanced:** visual graph, wider format extraction, related-content suggestions, and custom index controls.

**Failure, performance, privacy:** indexing is bounded and rebuildable; core records stay available if it fails. Index only selected/local authorized content. Export includes only selected records/files. Missing links are repairable without silent deletion; owner-approved retention governs attachment references and deletion.

**Acceptance:** supported text searches offline; saved filters reopen; dependency cycles fail atomically; associations can be cyclic; correction preserves the prior event; selected export is scoped; orphan repair retains stable identity; and indexing failure does not block task/course work.

## v0.8 — Local Automation

Local automation is a small, deterministic rule runner, not an AI agent. It uses a versioned JSON grammar with typed allowlists. A rule has stable `ruleId`, `version`, `enabled`, one trigger, bounded conditions, an action, and a per-rule permission. The trigger is a named local domain event or a scheduled local time. A condition uses an allowlisted record field, typed comparison operator, and value; a rule may combine a bounded list with `all` or `any`. Unknown keys, field/operator pairs, value types, actions, or target types fail validation before save. No script, expression evaluator, or network fetch is accepted.

An illustrative rule is:

```json
{
  "version": 1,
  "ruleId": "6a7347f0-312d-4309-854e-0a2be43d3823",
  "enabled": false,
  "trigger": { "event": "task.deadlineReached" },
  "conditions": {
    "all": [
      { "field": "task.status", "op": "equals", "value": "NOT_STARTED" },
      { "field": "task.category", "op": "equals", "value": "ACADEMIC" }
    ]
  },
  "action": { "type": "showLocalReminder", "target": "triggerTask" }
}
```

The exact names are proposed. The editor shows a plain-language preview of matched records, intended action, permission, schedule, and possible side effects. Dry run reads inputs and displays prospective results without mutating data, scheduling a reminder, or sending a file. A saved rule remains disabled until the user reviews and enables it.

Stable source event IDs and deterministic action IDs deduplicate replay. Write an outbox/job in the same Room transaction as the source event; enqueue/retry must be idempotent. Track the causal chain when one action emits another event, and stop after a per-run action budget or repeated event/action pair. Show loop/budget stops in local run history. Schedule policy stores the intended local time and zone, recomputes after timezone/DST change, uses Android's inexact scheduling, and defines whether a missed run is skipped, run once on next opportunity, or asks. Never replay an unbounded backlog or promise an exact alarm.

Rule templates are local examples the user copies and reviews; applying a template does not enable it. Event IDs identify source changes, while action IDs identify one intended effect. Integrate with the existing reminder repository so a rule does not create a duplicate notification for the same task/deadline event. Each action checks its own permission. Rules cannot upload files, send clipboard contents, or broaden access autonomously. External transfer and LMS actions require a separate interactive confirmation and are excluded from the initial local-action MVP.

**Primary story:** A student selects an overdue-task reminder template, reviews the matched academic task, checks the local notification permission, dry-runs it, then enables it. If Android delays the job, history says it is pending/delayed; the existing reminder path is not duplicated. A repeated event produces no second action.

**MVP:** documented JSON grammar and allowlist; preview/dry run; disabled-by-default rules; per-rule permission; stable event/action IDs; transactional outbox; idempotent bounded execution; loop and battery/work budget; run history; local reversible actions; explicit schedule zone and missed-run policy.

**Advanced:** richer rule composition and reviewed permission-scoped integrations after local execution and recovery are accepted.

**Failure, performance, privacy:** malformed rules fail before persistence. A crash between commit and enqueue leaves a durable job; retry cannot duplicate an effect. Bound job count, event-chain depth, frequency, payload size, and retention to protect battery and storage. Rules and history remain local unless explicitly exported. Local updates and reminders offer undo/cancel where safe. Irreversible external actions require a preview and confirmation and may only offer a compensating action, not claim reversal.

**Acceptance:** reject malformed or unlisted grammar; dry run has no side effects; duplicate delivery yields one action; crash/retry preserves the job without duplicate notifications; loops stop within budget; timezone/DST and missed-run behavior follow the saved policy; permission is checked for each action; and external or destructive effects require interactive confirmation.

See [06_SIGNATURE_FEATURES.md](06_SIGNATURE_FEATURES.md) for linked records and activity history, [05_V05_SYNC_AND_HANDOFF.md](05_V05_SYNC_AND_HANDOFF.md) for transfer boundaries, and [08_ARCHITECTURE_AND_SECURITY.md](08_ARCHITECTURE_AND_SECURITY.md) for security constraints.
