# Implementation phases — plan only

**Do not execute these phases in this task.** Riko exclusively performs builds, tests, lint and runtime verification. Future implementation needs a separately authorized task and the approvals below. Every component name is a proposal from [05_COMPONENT_ARCHITECTURE.md](05_COMPONENT_ARCHITECTURE.md); visual checks refer to [06_VISUAL_ACCEPTANCE.md](06_VISUAL_ACCEPTANCE.md).

## Source prerequisites

| ID | Exact repository-relative source | Current approval |
|---|---|---|
| A1 | `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_43 AM-1.png` | CANDIDATE |
| A2 | `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_45 AM-2.png` | CANDIDATE |
| A3 | `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_46 AM-3.png` | CANDIDATE |
| A4 | `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_48 AM-4.png` | CANDIDATE |
| A5 | `redesign-idea/Riko Hub_ Cozy Academic Homebase.png` | UNKNOWN; Today/Plan panel correspondence proposed |

No required image is currently finally APPROVED. Each phase needs owner acceptance of its source scope **or an explicit written replacement proposal**, recorded in the manifest/deviation register. Owner-confirmed non-negotiable requirements still apply. Do not block useful specification work on missing assets; do block claims that an unapproved visual has become authoritative. Original full screenshots remain unchanged.

Files listed below are under `app/src/main/java/id/riko/hub/` unless another root is named. New names describe possible files, not created source. Data files are preservation dependencies, not permission to change data models in visual phases.

## Phase 1 — Design tokens and custom visual primitives

- **Required approved images:** accepted A5 palette/surface regions or owner-approved token sheet; accepted Workspace contour proposal for the lava primitive. A1/A2 may be analogues only if their role is accepted.
- **Components:** semantic visual tokens, `OrganicSurface`, tactile state treatments, illustration sizing/asset policy.
- **Dependencies:** approval ledger; current light/dark/user preferences; existing `ui/theme/` conventions; D04/D06/D07 decisions.
- **Likely files:** `ui/theme/Theme.kt`, `RikoColors.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Motion.kt`, `HomeTokens.kt`; proposed `ui/components/OrganicSurface.kt`; later approved resources in `app/src/main/res/`.
- **Acceptance:** V01/V05/V06/V07; documented numeric tokens are deliberate choices; contours adapt without clipping; no new framework, network art or legacy-palette substitution.
- **Risks:** conceptual glow/texture may be expensive; wrong contrast after sampling art; overly generic component API; old token document conflicts.
- **Unresolved owner decisions:** exact palette/fonts/radii/gutters, light theme, illustration/mascot approval and source, permitted texture/glow simplification, Workspace silhouette.

## Phase 2 — Animated global navigation

- **Required approved images:** accepted dock region/variant from A5 or explicit chosen A1–A4 variant. No mixing incompatible tab sets without approval.
- **Components:** `RikoNavigationDock`, selected organic contour, destination labels, focus/back/gesture alternatives.
- **Dependencies:** phase 1, explicit route/name/order/central-action and compass preference decisions under D03; reachability map for Courses/Settings/Sync/Tasks.
- **Likely files:** `navigation/Destinations.kt`, `RikoNavHost.kt`, `SpatialMap.kt`; `ui/compass/CompassChip.kt`, `SpatialNavLayer.kt`; `ui/components/DestinationLabel.kt`, `ScreenFrame.kt`; `ui/RikoHub.kt`; proposed dock file.
- **Acceptance:** V02/V06/V07, M1; custom active contour and labeled selection; rapid input settles; Back and existing navigation preferences are retained or explicitly migrated with approval; no lost routes.
- **Risks:** six-target variant cannot fit narrow screens; scroll/swipe conflict; focus/insets; existing dirty navigation work.
- **Unresolved owner decisions:** final five-area proposal versus other variants, Today/Home and Plan/Timeline names, Link meaning, central plus behavior, persistent Tasks/Courses/Sync access, spatial navigation coexistence.

## Phase 3 — Today dashboard and Task Workspace

- **Required approved images:** accepted A5 Today panel (currently UNKNOWN despite provisional Today concept) or confirmed alternative; a dedicated Workspace image or owner-accepted written/visual proposal for header and expanding task surface.
- **Components:** `TodayFocusDeck`, `TaskWorkspaceSurface`, animated lava header, existing task editor, task rows and supported summaries.
- **Dependencies:** phases 1–2; current Home ranking/pin logic and Tasks state; D02/D04/D05; task mutation/reminder/widget hooks; existing course-linked assignments.
- **Likely files:** `ui/home/HomeScreen.kt`, `HomeContent.kt`, `BubbleField.kt`, `Bubble.kt`, `BubblePeekSheet.kt`, `HomeViewModel.kt`; `ui/tasks/TasksScreen.kt`, `TaskRow.kt`, `TaskLabels.kt`, `TaskExtensionFields.kt`, `TasksViewModel.kt`; `ui/components/TaskEditorSheet.kt`, `TaskDeadlineFields.kt`; shared primitives. ViewModel changes only as presentation needs, preserving repository semantics.
- **Acceptance:** V03/V04/V08 and task preservation ledger; dominant orange focus hierarchy; same task expands with continuity; default lava is animated; filters/search/checklists/recurrence/deadlines/status/undo remain available; failed edits preserve input.
- **Risks:** art-backed summary fiction; loss of ranking/filter/scroll state; expansion/IME clash; recurrence/reminder/widget regression; copying reduced task fields from board.
- **Unresolved owner decisions:** exact Today source, focus/session/mood summary scope, Workspace expansion trigger/state, default lava rhythm/loop, header art.

## Phase 4 — Creative Timeline / Plan

- **Required approved images:** accepted A5 Plan panel or confirmed separate creative Timeline source; accepted dense/large-text time-map adaptation.
- **Components:** `CreativeTimeMap`, week/day controls, time gutter, stop markers, real event/task cards and editors.
- **Dependencies:** phases 1–2; `TimelineBuilder` chronology and real events; D02/D08/D09; phase 3 task editor integration if shared.
- **Likely files:** `ui/timeline/TimelineScreen.kt`, `TimelineContent.kt`, `TimelineStopRow.kt`, `TimelineViewModel.kt`; `ui/components/EventEditorSheet.kt`; `domain/logic/TimelineBuilder.kt` only if an approved presentation requirement needs an adapter, preserving chronology.
- **Acceptance:** V09/M4; winding path and offset time blocks remain distinctive, no generic primary task-list replacement; actual week/day/task/event CRUD stays intact; ordered TalkBack semantics and dense data do not overlap.
- **Risks:** Canvas anchors drift under font scaling/list virtualization; fictional breaks/focus sessions; unimplemented pictured mode tabs; older timeline refinements conflict with new concept.
- **Unresolved owner decisions:** time range/granularity, Timeline/Priorities/Calendar tab scope, path colors/domain mapping, dense adaptation, relationship to earlier approved timeline refinement.

## Phase 5 — Spaces entry page

- **Required approved images:** accepted A5 portal panel or owner-approved portal alternative; four names are already owner-confirmed.
- **Components:** four `SpacePortal` surfaces and honest capability/availability messaging.
- **Dependencies:** phases 1–2; approved route structure and feature boundaries; D05/D06.
- **Likely files:** proposed `ui/spaces/SpacesScreen.kt`; `navigation/Destinations.kt`, `RikoNavHost.kt`; shared portal component; approved local illustration resources.
- **Acceptance:** V10; Academics/Projects/Personal/Devices unmistakably distinct; portal room composition retained if approved; current Courses and Sync remain reachable; no unavailable module presented as live.
- **Risks:** navigation dead ends, duplicated category concepts, heavy raster assets, inaccessible image-only titles.
- **Unresolved owner decisions:** portal approval, order, illustrated scenes/mascot, unavailable-module handling, whether detail pages share or vary portal artwork.

## Phase 6 — Academics, Projects, Personal, Devices

- **Required approved images:** **separate** acceptance of A1, A2, A3 and A4 or separate replacement proposals. Approval of one Space never approves all four.
- **Components:** Academics wave focus/course/deadline hierarchy; Projects split hero/wide rows/sprint rhythm; Personal restful hero and approved modules; Devices connected desk/pair/queue/transfer/trust/history composition.
- **Dependencies:** phases 1–2 and 5; existing Courses/assignments and Sync/Handoff; task category data; D05/D06/D10/D11. Each subpage can proceed only after its own gate.
- **Likely files:** `ui/courses/`, `ui/academic/` and proposed Academics wrapper; proposed Projects/Personal presentation directories; `ui/sync/SyncScreen.kt`, `SyncViewModel.kt`, `SyncUiState.kt` and proposed Devices wrapper; `navigation/`; resources. `data/repository/CourseRepository.kt`, `AssignmentRepository.kt`, `data/remote/device/`, `companion/riko_companion/` are dependencies, not automatic change scope.
- **Acceptance:** V11–V14/V07; visibly distinct page compositions; only real course/task/handoff data; explicit transfer confirmation/incoming-save/trust flows preserved; no fabricated progress, health, finance, Apple device/battery or Git feed data.
- **Risks:** candidate sections imply new databases/features; in-progress academic schema/backup collisions; general sync claim exceeds LAN handoff; loss of full Sync routes; Personal layout after unsupported modules omitted.
- **Unresolved owner decisions:** each candidate's accepted visual scope; Projects/Personal MVP modules and substitutions; study metrics/material linking; battery/trust-device limitations; connection/transfer wording and grouping. Any missing module that changes composition requires a recorded approved adaptation.

## Phase 7 — Capture and Vault

- **Required approved images:** accepted A5 Capture and Vault panels or separately accepted alternatives.
- **Components:** irregular Capture action surface; existing task editor; Vault shelf/search/file rows with actual OAuth/transfer states.
- **Dependencies:** phases 1–2; Capture routing/product capability scope; existing Drive authorization/repository; D05/D06.
- **Likely files:** proposed `ui/capture/CaptureScreen.kt`; existing shared task editor; `ui/vault/VaultScreen.kt`, `VaultUiState.kt`, `VaultViewModel.kt`; navigation and approved resources. `data/remote/drive/` remains provider boundary.
- **Acceptance:** V15/V16; task quick add stays direct; only supported actions active; real upload/download/cancel/retry/reconnect/breadcrumb/load-more preserved; no fake storage, file names or counts.
- **Risks:** pretending universal capture exists; camera/microphone permissions for unsupported actions; cloud requirement mislabeled offline; document picker/IME/focus interruptions.
- **Unresolved owner decisions:** Capture actions versus future roadmap, upload entry placement, category/recent/storage scope, shelf illustration, offline remote-file messaging.

## Phase 8 — Motion refinement and accessibility

- **Required approved images:** previously accepted screen regions plus accepted motion storyboards/written specs; a still image alone cannot approve animation.
- **Components:** M1–M7 where approved, motion policy, focus restoration, semantic targets, responsive adapters, contrast and tactile states.
- **Dependencies:** phases 1–7 that have passed their own owner gates; Riko-supplied device/accessibility evidence; D07/D08/D09; approved default/reduced-motion distinction.
- **Likely files:** `ui/theme/Motion.kt`; shared components/nav; only relevant page drawing/layout/semantics files; localized labels in `app/src/main/res/values/`. Widget receives a separate RemoteViews review if affected.
- **Acceptance:** V01–V16 as applicable; usable with animations disabled; organic lava resting silhouette and time-map retained; input never waits for animation; paused background effects; TalkBack/large text/48dp/contrast targets reviewed by Riko; no invented performance pass.
- **Risks:** decorative motion drains battery, jank on slower devices, wrong focus order from Canvas, large text truncation, theme contrast conflict.
- **Unresolved owner decisions:** exact rhythm/easing/looping, acceptable designed reduced-motion/static states, device/performance budget, light theme and artwork simplification boundaries.

## Phase 9 — Owner visual acceptance

- **Required approved images:** finalized manifest for every shipping visual or explicit owner-approved written replacement; no UNKNOWN scope silently accepted.
- **Components:** whole screen families, custom navigation, Workspace continuity, chronological map and preserved product flows.
- **Dependencies:** relevant phases accepted; Riko screenshot/motion/accessibility/offline/performance evidence; no unresolved blocking deviation; implementation revision identified.
- **Likely files:** documentation updates only in this gate: manifest, acceptance records, deviation register and approved token/motion specs. Any fixes require a scoped implementation task first.
- **Acceptance:** owner decision records specific traceability IDs/screens/evidence/date; source image scopes confirmed; functional preservation ledger reviewed; all shipping deviations accepted or corrected. Approval of screenshots does not retroactively validate untested runtime paths.
- **Risks:** review of only a happy-path screenshot, inconsistent image scopes, final generic simplification presented as optimization, silent approval upgrade.
- **Unresolved owner decisions:** final sign-off for each screen/component and explicit acceptance/rejection of residual deviations.

## Sequencing and containment

Proposed sequence: tokens → navigation → Today/Workspace → Plan → Spaces entry → four independently gated Spaces → Capture/Vault → motion/accessibility → owner acceptance. Carry accessibility and offline constraints through every phase; phase 8 is refinement, not permission to defer basic usability. Slice each phase by approved screen/component. Preserve dirty work and coordinate file ownership before any future implementation. Nothing in this plan authorizes commits, branch switches, dependency installs or source changes now.
