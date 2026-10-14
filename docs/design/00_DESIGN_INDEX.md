# Riko Hub — Visual Fidelity Lock

Documentation contract dated 2026-10-13. **No redesign implementation is authorized by this document.** Riko exclusively performs builds, tests, lint and runtime verification. This pass permits source/image inspection, Markdown design documentation and static consistency review only.

## Evidence and repository state

- Inspected branch: `feature/v04-laptop-companion`; HEAD: `947b057f230c8083512e7553ce9a5aab7de89507`.
- The worktree already contained tracked edits and untracked academic/course code, tests, handbook documents and reference images. Source descriptions include that worktree; they are not claims about a clean or released commit.
- Requested `design-idea/` does not exist. The owner explicitly confirmed using **`redesign-idea/`** instead. Original image names and locations are preserved.
- Five PNGs were discovered there and all five were visually inspected. Their dimensions were read from the files. No dedicated Task Workspace reference was found in this set.
- The owner asked the architect to make the mapping proposal. This authorizes architectural proposals; it does not confirm image approval. Exact asset approvals are still pending.
- Structural review used the codebase-memory index, followed by source inspection because the index predates dirty academic changes and has partial coverage of `HomeScreen.kt`. No runtime behavior was verified.
- Static consistency review checks the ten required documents, source/link existence, approval boundaries, traceability fields and phase prerequisites. A content-hash comparison protects all pre-existing worktree files and the five images; it is not an app test.

## Read order

| Document | Purpose |
|---|---|
| [Asset manifest](ASSET_MANIFEST.md) | Every image, inspection evidence, proposed page association and approval uncertainty |
| [01 Visual contract](01_VISUAL_CONTRACT.md) | Authority, owner requirements and rules against silent simplification |
| [02 Tokens and geometry](02_TOKENS_AND_GEOMETRY.md) | Observed visual relationships versus proposed implementation values |
| [03 Motion specification](03_MOTION_SPECIFICATION.md) | Meaningful motion, lifecycle limits and accessible fallbacks |
| [04 Screen blueprints](04_SCREEN_BLUEPRINTS.md) | Distinct page composition, honest content and states |
| [05 Component architecture](05_COMPONENT_ARCHITECTURE.md) | Native Compose proposals and preservation of existing functionality |
| [06 Visual acceptance](06_VISUAL_ACCEPTANCE.md) | Image-to-component traceability and future owner verification |
| [07 Implementation phases](07_IMPLEMENTATION_PHASES.md) | Nine incremental phases, dependencies and approval gates |
| [08 Deviation register](08_DEVIATION_REGISTER.md) | Missing decisions, feasibility risks and owner-approved exceptions |

## Status vocabulary

| Status | Meaning |
|---|---|
| APPROVED | Explicitly confirmed by the owner; scope of confirmation must be recorded |
| CANDIDATE | Under consideration, including provisional concept approval pending final acceptance |
| REFERENCE | Inspiration only, explicitly designated as such |
| REJECTED | Explicitly discarded by the owner |
| UNKNOWN | Approval cannot be determined |

**Approval belongs to a specific asset, panel or requirement, not automatically to its neighbors.** No image in the inspected set has explicit final APPROVED status. A1–A4 are CANDIDATE; A5 is UNKNOWN. There is no evidence that any image is REJECTED or explicitly REFERENCE. Today and creative Timeline concepts are provisionally approved, recorded as CANDIDATE with that qualification; the exact correspondence to A5 is proposed.

Owner-confirmed textual requirements (APPROVED at requirement level): warm/playful/mature character; readable density; page-specific identities; organic composition; tactile controls; custom animated navigation; meaningful motion; preferred Graphite & Tangerine foundation; animated organic lava-like Task Workspace header; expanding task surfaces; four Spaces areas; lightweight native Android and offline-first core; feature preservation. Exact colors, measurements, curves, illustration ownership and motion tuning are not thereby approved.

## Source key

All paths are repository relative. The manifest owns the complete inventory; IDs are convenience labels, never replacements for source paths.

| ID | Actual file | Proposed association | Asset status |
|---|---|---|---|
| A1 | [redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_43 AM-1.png](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_43%20AM-1.png) | Academics | CANDIDATE |
| A2 | [redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_45 AM-2.png](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_45%20AM-2.png) | Projects | CANDIDATE |
| A3 | [redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_46 AM-3.png](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_46%20AM-3.png) | Personal | CANDIDATE |
| A4 | [redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_48 AM-4.png](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_48%20AM-4.png) | Devices | CANDIDATE |
| A5 | [redesign-idea/Riko Hub_ Cozy Academic Homebase.png](../../redesign-idea/Riko%20Hub_%20Cozy%20Academic%20Homebase.png) | Today, Plan, Capture, Vault, Spaces entry panels | UNKNOWN |

## Authority and stop boundary

The owner's explicit instructions control this contract. Existing `docs/01_UX_UI_SPEC.md`, `docs/07_DESIGN_TOKENS.md`, and `docs/handbook/07_DESIGN_DIRECTION.md` describe earlier/current direction and remain untouched. Their ivory/pastel defaults, compass arrangement and exact values must not silently substitute for the new direction. This contract separates the new owner requirements from those historical details without declaring unapproved images final.

Before future implementation, record owner decisions in the manifest and deviation register, then apply the traceability gate. If an element has no confirmed source, retain the explicit textual requirement and seek approval of a written/visual proposal. Do not pretend a missing reference exists. Stop this task after the ten Markdown documents and static review.
