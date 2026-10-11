# Project Renaissance

Status: **PROPOSED product direction; repository-grounded audit.** Prepared on 2026-10-11. This suite does not implement or approve future features.

Riko Hub's proposed direction is a private, offline-first, cross-device personal command center. Preserve the accepted Android planner and extend its useful workflows incrementally. The checked-out branch is `feature/v04-laptop-companion`, with HEAD at `44d6c67`; the v0.4 implementation includes uncommitted and untracked files. This is an audit of that working state, not just the commit.

## Audience and reading order

Riko uses this suite to choose scope, approve external dependencies and accept releases. Implementers use the contracts and backlog. Reviewers use the evidence distinctions, security boundaries and release gates.

1. [01 Current state audit](01_CURRENT_STATE_AUDIT.md): what exists, its evidence, gaps and historical specification differences.
2. [02 Product vision](02_PRODUCT_VISION.md): users, workflows, identity and non-goals.
3. [03 Master roadmap](03_MASTER_ROADMAP.md): version boundaries, dependencies and release gates.
4. [04 Remote Companion](04_V04_REMOTE_COMPANION.md): the proposed extension from LAN presence to remote private connectivity.
5. [05 Sync and handoff](05_V05_SYNC_AND_HANDOFF.md): proposed file transport and explicit boundaries around structured synchronization.
6. [06 Signature features](06_SIGNATURE_FEATURES.md): Mission Graph, Evidence Ledger and Continuity Engine.
7. [07 Academic and automation](07_ACADEMIC_AND_AUTOMATION.md): course workflows, knowledge connections and bounded local automation.
8. [08 Architecture and security](08_ARCHITECTURE_AND_SECURITY.md): current foundations, proposed boundaries and recovery.
9. [09 Execution backlog](09_EXECUTION_BACKLOG.md): stable work IDs, dependencies, acceptance criteria and owner verification checklist.

Start implementation planning with documents 01, 03, 04 and 09. Consult 05–08 when the corresponding prerequisite gate has passed.

## Status conventions

| Label | Meaning |
|---|---|
| IMPLEMENTED | Relevant source exists in the inspected working tree. This does not prove compilation or runtime success. |
| VERIFIED | A precisely scoped check has recorded evidence, including who performed it, environment and result. Never infer this from a test file. |
| OWNER-REPORTED | Riko supplied a result or acceptance statement. Preserve its scope; do not invent independent evidence. |
| PROPOSED | A design or backlog item requiring later implementation and verification. |
| DEFERRED | Deliberately excluded from the release MVP, with a stated prerequisite or reason. |
| UNKNOWN / NEEDS VERIFICATION | Available source or evidence cannot establish the claim. |

For runtime evidence, record build identity, device/OS, topology, steps, result, date and a redacted evidence reference. A server startup is not a successful Android pairing test. A successful build is not a security review. Owner acceptance of v0.2 does not automatically accept v0.3 or v0.4.

## Document precedence

Actual source establishes current implementation. Riko's latest report establishes owner-reported progress. [STATUS](../STATUS.md), [current companion protocol](../V04_COMPANION_PROTOCOL.md) and [setup guide](../V04_LAPTOP_COMPANION_SETUP.md) provide historical implementation context. This suite proposes the future direction; it does not silently rewrite the existing wire protocol or Room schema.

The old [roadmap](../04_ROADMAP.md) remains historical. Its v0.5 timestamp-only conflict candidate is not the recommended structured-sync design. The new remote requirement also expands the old LAN-only v0.4 scope. These differences are explicit in documents 01 and 04; unrelated documents were not modified.

## Scope and approval

P0 means data safety, security, recovery and essential correctness. P1 means frequent useful workflows. P2 means customization, optional integrations and advanced visualization. Complexity estimates are relative, not delivery promises.

Roadmap approval does not authorize package installation, account enrollment, networking changes, personal-data transfer or every future implementation. Decision IDs in document 09 identify owner choices. Tailscale is a leading optional transport candidate, not a permanent mandatory account dependency. Local planning and LAN use must remain available without an overlay account.

Riko exclusively executes builds, lint, tests and device verification. This documentation session performed source inspection and lightweight text/preservation checks only. It did not start services or modify application code. No AI integration is included anywhere in the proposed product.
