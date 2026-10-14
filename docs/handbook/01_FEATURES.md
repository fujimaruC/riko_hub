# Feature catalog

This catalog describes the product visible in the current repository and the next planned capabilities. Source presence is not the same as owner acceptance or verified runtime behavior. The status labels below are intentionally conservative; check [project status](../STATUS.md) for the latest evidence.

## Status labels

| Label | Meaning |
|---|---|
| **OWNER ACCEPTED** | The Project Owner accepted this scope. This does not promise every device or environment behaves identically. |
| **IMPLEMENTED / UNVERIFIED** | Relevant source is present, but the behavior has not completed the required owner verification or acceptance. |
| **PARTIAL** | Some of the capability exists; the complete intended workflow does not. |
| **PLANNED** | A documented future direction without a claim of implementation. |
| **DEFERRED** | Deliberately outside the current accepted scope or postponed pending a decision or prerequisite. |

## v0.2 — Local planner foundation

| Feature | User problem and behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Tasks and personal planning | Capture and organize work with local task records, priorities, deadlines, checklists, estimates, recurrence, and task views. | `ui/tasks/`, `domain/model/Models.kt`, `domain/logic/`, `data/local/`, `data/repository/Repositories.kt` | **OWNER ACCEPTED** | Academic grouping and connected context are planned for later versions; avoid changing established task semantics silently. |
| Daily view and Timeline | See a useful daily focus and arrange tasks/events over time. | `ui/home/`, `ui/timeline/`, `domain/logic/FocusSelector.kt`, `TimelineBuilder.kt`, `UrgencyCalculator.kt` | **OWNER ACCEPTED** | Better continuity and course-linked context are future work. |
| Local reminders | Receive reminders for relevant tasks, subject to Android scheduling and notification behavior. | `work/TaskReminders.kt`, task repository hooks | **OWNER ACCEPTED** | Improve clarity around delayed or denied notification delivery; platform timing is not guaranteed. |
| Native task widget | Glance at selected tasks and a clock from the Android launcher. | `ui/widget/`, `domain/logic/TaskWidgetPresentation.kt` | **OWNER ACCEPTED** | Launcher and device-specific rendering remain environment-dependent. |
| Manual local backup | Explicitly export, preview, and import supported task/event/settings data. | `data/backup/LocalBackupService.kt`, `ui/settings/BackupSettings.kt` | **OWNER ACCEPTED** | Continue to extend formats only with reviewed migration, restore, and compatibility behavior. |

## v0.3 — Materials Vault

| Feature | User problem and behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Google Drive browsing and search | Find folders and supported files without losing the current folder context. | `data/remote/drive/`, `ui/vault/` | **IMPLEMENTED / UNVERIFIED** | Live owner QA for authorization, browsing, account changes, and provider behavior remains tracked. |
| Explicit upload and file opening | Choose files and destinations deliberately; open supported documents through a local viewer or validated Google link. | `DriveRepository.kt`, `GoogleDriveAuthorization.kt`, `VaultViewModel.kt` | **IMPLEMENTED / UNVERIFIED** | Durable background/resumable transfer, a general offline library, and broader Drive workflows are not part of the verified feature set. |

## v0.4 — Linux Companion

| Feature | User problem and behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Local network pairing and presence | Pair a phone with an explicitly approved Linux laptop, then show recent authenticated presence on the LAN. | `data/remote/device/CompanionProtocol.kt`, `CompanionRepository.kt`, `ui/sync/`, `companion/riko_companion/` | **IMPLEMENTED / UNVERIFIED** | Owner build, network, security, and device acceptance are still required. See [protocol details](../V04_COMPANION_PROTOCOL.md). |
| Linux CLI and dashboard | Start and inspect the foreground Companion and its trusted devices from Linux. | `companion/riko_companion/cli.py`, `dashboard.py`, `__main__.py` | **IMPLEMENTED / UNVERIFIED** | Packaging and live behavior require owner setup and verification. |
| Remote or overlay access | Reach a paired laptop from another network through an owner-approved private transport. | Proposed Android routing and Linux listener changes | **PLANNED** | This is the central v0.4 roadmap addition. **Remote networking is not implemented.** |

## v0.5 — Universal Handoff

| Feature | User problem and behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Explicit file handoff | Move selected files between the paired phone and laptop with preview, consent, progress, cancellation, retry, and history. | `data/remote/device/Handoff*.kt`, `ui/sync/`, `companion/riko_companion/handoff.py` | **IMPLEMENTED / UNVERIFIED** | Current source describes a bounded LAN workflow; owner compilation and device acceptance remain pending. Broader release scope is **PARTIAL**. |
| Explicit text handoff | Review a text payload before sending it through the paired workflow. | `HandoffRepository.kt`, `SyncScreen.kt`, `companion/riko_companion/handoff.py` | **IMPLEMENTED / UNVERIFIED** | Keep payload size and retention bounded; this is an explicit action, not clipboard monitoring. |
| Read-only work context | Review selected workspace metadata and pending transfer state on the Companion. | `HandoffRepository.kt`, `dashboard.py` | **PARTIAL** | Richer context, durable queues, structured sync, and broader task/material composition remain future work or deferred. |
| Clipboard transfer and simultaneous editing | Move clipboard contents or edit shared records live across devices. | — | **DEFERRED** | These require distinct consent, retention, conflict, and security decisions. |

## v0.6 — Academic Command Center

| Feature | User problem and behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Course Spaces, metadata, and timetable slots | Keep a course and its weekly schedule together for local planning. Current working tree includes course creation/editing, archive/restore, ordering, and timetable references. | `domain/model/Course.kt`, `data/local/`, `data/repository/CourseRepository.kt`, `ui/courses/` | **IMPLEMENTED / UNVERIFIED** | Phase 1 is source-complete in the working tree; migrations, backup round trips, accessibility, and owner verification remain pending. |
| Course-linked assignments and richer academic planning | Group lifecycle-tracked assignments with checklists, course materials, submission evidence, workload views, and Deadline Radar. | `domain/model/AcademicAssignment.kt`, `domain/logic/AssignmentRules.kt`, `ui/academic/` | **PARTIAL** | Current source has assignment foundations, but broader academic scope is planned; later phases require explicit approval. See [Course Spaces](../V06_COURSE_SPACES.md) and [assignment lifecycle](../V06_ASSIGNMENT_LIFECYCLE.md). |
| LMS writes, grade prediction, automatic submission | Submit work or fetch institutional state without user action. | — | **DEFERRED** | No LMS scraping, credential collection, or automatic submission is promised. |

## v0.7 and v0.8 — Proposals

| Feature | User problem and intended behavior | Related modules | Status | Planned enhancement |
|---|---|---|---|---|
| Living Knowledge Vault | Link notes, documents, tasks, courses, and evidence; search supported local text and repair stale references. | Future local knowledge modules | **PLANNED** | Start with useful lists, bounded indexing, backlinks, and explicit export. A visual graph and AI are not prerequisites. |
| Personal Automation Engine | Reduce repeated local actions with deterministic, previewable rules and run history. | Future local rule runner; existing reminder boundaries | **PLANNED** | Begin with bounded local actions, clear permissions, dry runs, and duplicate/loop protection. No arbitrary scripts or mandatory AI. |

## How to read this catalog

The labels reflect repository notes and source inventory; this handbook did not execute builds, tests, lint, or runtime checks. The current working tree also contains untracked v0.6 source and modified status notes. A contributor should check the actual branch and tree before treating any unverified entry as release-ready.

**Related:** [Roadmap](02_ROADMAP.md) · [Architecture](03_ARCHITECTURE.md) · [Current status](../STATUS.md)
