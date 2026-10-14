# Roadmap

The roadmap describes user outcomes and release direction. It is not a delivery promise: source work, owner-approved scope, and release acceptance are separate. The [current status page](../STATUS.md) records the latest implementation notes.

## Where things stand

| Version | Current evidence | What that means |
|---|---|---|
| v0.2 | **OWNER ACCEPTED** | The local planner foundation and widget were accepted. |
| v0.3 | **IMPLEMENTED / UNVERIFIED** | Materials Vault source is present; live Drive QA remains outstanding. |
| v0.4 | **IMPLEMENTED / UNVERIFIED** for LAN source; remote access is **PLANNED** | Pairing/presence and Linux CLI source exist. Remote networking and owner acceptance are absent. |
| v0.5 | **PARTIAL** | Bounded LAN handoff source is present and unverified; broader Universal Handoff remains incomplete. |
| v0.6 | **IMPLEMENTED / UNVERIFIED** course and assignment source; broader scope is **PLANNED** | Current working-tree changes have not been owner-verified or released. |

## Upcoming direction

### v0.4 — Anywhere Companion

**User benefit:** a trusted Linux laptop can remain reachable when the phone leaves the home or campus LAN, without exposing a public listener or making remote access a prerequisite for local planning.

The committed Companion foundation is LAN pairing and recent presence. The future remote addition needs an owner-approved private transport, endpoint routing that preserves device identity, and clear transport/status feedback. Discovery or an address alone must never grant trust. Remote access is **PLANNED**, not implemented.

**Release boundary:** explicit pairing and revocation remain; no remote shell, remote desktop, cloud relay, or public exposure is included. See [v0.4 roadmap specification](../renaissance/03_MASTER_ROADMAP.md#v04--anywhere-companion), [protocol](../V04_COMPANION_PROTOCOL.md), and [backlog](../renaissance/09_EXECUTION_BACKLOG.md).

### v0.5 — Universal Handoff

**User benefit:** move a chosen file and enough context to continue work, while knowing what leaves the phone and where it goes.

The current bounded LAN source includes transfer in both directions, explicit selection/consent, SHA-256 chunk and whole-file checks, transfer state, and workspace metadata viewing. Future enhancements include multi-file queues, stronger interruption recovery, richer task/material context, and clear handling for missing or changed files. The broader version remains **PARTIAL**; source presence does not establish these release gates.

Structured database replication, copying SQLite, hidden clipboard monitoring, and simultaneous editing are outside this scope. See [v0.5 handoff specification](../renaissance/05_V05_SYNC_AND_HANDOFF.md), [protocol](../V05_HANDOFF_PROTOCOL.md), and [backlog](../renaissance/09_EXECUTION_BACKLOG.md).

### v0.6 — Academic Command Center

**User benefit:** understand course commitments in one place, including what's due, what's underway, and which materials or evidence support it.

Course Spaces Phase 1 source currently covers local course records and weekly timetable references. Assignment models, storage, editing and deadline-change source are also present in the working tree. These are **IMPLEMENTED / UNVERIFIED** and not a v0.6 release. The wider Academic Command Center remains **PLANNED**: links to tasks and materials, optional workload views, lab stages, submission evidence, Timeline context, and previewed batch planning.

Keep academic status separate from the existing task status unless a reviewed mapping is explicitly adopted. Unknown effort must remain visible as unknown. See [v0.6 course specification](../V06_COURSE_SPACES.md), [assignment lifecycle](../V06_ASSIGNMENT_LIFECYCLE.md), and [Renaissance roadmap](../renaissance/03_MASTER_ROADMAP.md#v06--academic-command-center).

### v0.7 — Living Knowledge Vault

**User benefit:** find the note, file, or earlier evidence connected to a task without relying on memory or duplicating content.

The proposal links tasks, courses, notes, materials, and evidence with practical lists and backlinks. Local text indexing should be bounded to supported formats, and broken file references should be repairable. A visual graph is optional; AI is not required. This work is **PLANNED**. See [signature feature proposals](../renaissance/06_SIGNATURE_FEATURES.md) and [academic and automation direction](../renaissance/07_ACADEMIC_AND_AUTOMATION.md).

### v0.8 — Personal Automation Engine

**User benefit:** let predictable local events trigger small, understandable actions, such as preparing a checklist or showing a reminder.

The proposal calls for typed rules, previews or dry runs, explicit permissions, bounded execution, duplicate protection, and history. It is a deterministic local rule system, not an AI agent or script runner. This work is **PLANNED**. See [automation proposal](../renaissance/07_ACADEMIC_AND_AUTOMATION.md).

### v0.9 — Unified Workspace

**User benefit:** move between capture, planning, materials, search, and device handoff with fewer repeated steps.

Potential scope includes bounded dashboard customization, quick capture, command palette, global search, and better continuity across devices. The exact information architecture depends on the ongoing design exploration and accepted foundations. This work is **PLANNED**; no final design or release scope is approved here. See [Renaissance roadmap](../renaissance/03_MASTER_ROADMAP.md#v09--unified-workspace).

### v1.0 — Stable Release

**User benefit:** rely on the accepted product for daily personal work and recover confidently from upgrades or mistakes.

The proposed release focus is reliability: migrations and restore, security review, offline behavior, accessibility, device compatibility, actionable errors, onboarding, and sustainable release procedures. v1.0 is **PLANNED** and is not a claim that release gates have been met. See [Renaissance roadmap](../renaissance/03_MASTER_ROADMAP.md#v10--reliable-release).

## How scope becomes real

The version names organize work; they do not approve every feature listed under them. The Project Owner reviews architectural, security, and design choices. Each feature needs a clear user outcome, data and failure boundaries, and appropriate verification before it can be called accepted.

**Related:** [Feature catalog](01_FEATURES.md) · [Design direction](07_DESIGN_DIRECTION.md) · [Renaissance specifications](../renaissance/00_README.md)
