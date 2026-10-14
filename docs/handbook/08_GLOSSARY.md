# Glossary

Terms below are marked as **current** when they appear in implemented product/source paths, and **proposed** when they describe future direction. A term can be used in a specification before its full feature exists.

| Term | Meaning | Status |
|---|---|---|
| **Course Space** | A place to organize a course and its schedule, with broader assignment and material relationships envisioned for academic planning. Course records and weekly timetable references are present in the current working tree. | Current source: **IMPLEMENTED / UNVERIFIED**. Broader academic concept: **PLANNED**. |
| **Materials Vault** | The Android area for browsing and searching Google Drive materials, with explicit file selection for supported actions. | Source present: **IMPLEMENTED / UNVERIFIED**; live Drive QA remains pending. |
| **Universal Handoff** | The planned, consent-based way to move selected files and useful work context between Android and the Linux Companion. | Bounded LAN source exists, while broader v0.5 is **PARTIAL** and unverified. |
| **Companion** | The optional Linux Python application that exposes trusted local device presence and handoff capabilities to the Android app. | LAN and CLI source: **IMPLEMENTED / UNVERIFIED**. Remote networking: **PLANNED**. |
| **Mission Graph** | Proposed typed relationships and dependencies between tasks, courses, notes, files, and other work. It does not imply that a graph database or graph editor exists. | **PLANNED**. |
| **Evidence Ledger** | Proposed chronological record of meaningful work and supporting evidence, with provenance and corrections. It is not a claim of legal or cryptographic immutability. | **PLANNED**. |
| **Continuity Engine** | Proposed selected context and transfer state that help a person resume work on another device. It is not automatic replication of the app database. | **PLANNED**; only bounded handoff foundations are present. |
| **Deadline Radar** | Proposed academic view that explains urgency and workload around course deadlines. The existing task urgency logic is distinct and should not be silently replaced. | **PLANNED**. |
| **Offline-first** | Core local workflows remain useful without an internet connection; optional connected features can become unavailable while local work continues. It does not mean every remote file is cached locally. | Product principle; implemented behaviors vary by feature and require verification. |
| **Device pairing** | An explicit trust-establishment flow between Android and a Linux Companion, with out-of-band pairing information, laptop approval, certificate pinning, and revocation in the LAN source. | **IMPLEMENTED / UNVERIFIED**. Pairing is not authorization for arbitrary data access. |
| **Certificate pinning** | Checking that a Companion presents the exact certificate identity previously trusted by the phone. A changed certificate requires a deliberate re-pairing path. | Present in v0.4 source; runtime behavior is unverified. |
| **SAF** | Android Storage Access Framework: system-mediated file and destination selection that lets a user grant access to chosen files. | Used by current backup, Vault, and handoff flows where applicable. |
| **Room** | Android's local database library used for structured task and event data, with migrations managed explicitly. | Current Android implementation. Current v0.6 course/assignment changes are unverified. |
| **Task status** | The current task lifecycle states (`NOT_STARTED`, `IN_PROGRESS`, `DONE`). Academic assignment stages are a separate concept and must not be mapped silently. | Current task model. |
| **Academic lifecycle** | Proposed or developing states for assignments and academic work, distinct from the existing task lifecycle. | Phase-dependent; broader workflow **PLANNED**. |

**Related:** [Features](01_FEATURES.md) · [Architecture](03_ARCHITECTURE.md) · [Renaissance proposals](../renaissance/00_README.md)
