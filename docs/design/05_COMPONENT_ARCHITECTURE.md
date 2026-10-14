# Component architecture and functionality preservation

This is a **native Compose architectural proposal**, not source implementation. Paths below refer to the inspected worktree, including existing uncommitted academic work. Keep the current ViewModel/repository boundaries and local persistence while changing presentation. No new framework or dependency is proposed.

Visual sources: A1 `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_43 AM-1.png`, A2 `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_45 AM-2.png`, A3 `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_46 AM-3.png`, A4 `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_48 AM-4.png` (all CANDIDATE); A5 `redesign-idea/Riko Hub_ Cozy Academic Homebase.png` (UNKNOWN). See [manifest](ASSET_MANIFEST.md) for visual inventory and [acceptance](06_VISUAL_ACCEPTANCE.md) for element-level conditions.

## Existing functionality that must survive

Paths in this table are under `app/src/main/java/id/riko/hub/` unless stated otherwise. Static source presence is evidence of implementation, not proof of working runtime behavior.

| Area | Actual source and state | Preservation obligation |
|---|---|---|
| Navigation | `navigation/Destinations.kt`, `RikoNavHost.kt`, `SpatialMap.kt`; routes Home, Tasks, Courses, Timeline, Vault, Sync, Settings | Every route remains reachable. Current five spatial/compass destinations exclude Courses and Settings; new shell must explicitly place them. No silent removal of spatial/compass-only preference or Back access. |
| Today/Home | `ui/home/HomeScreen.kt`, `HomeViewModel.kt`, `HomeContent.kt`, `BubbleField.kt`, `Bubble.kt`, `BubblePeekSheet.kt` | Local ranked tasks, progress, next obligation, quick add, peek/start/complete/delete, pinning, empty/error/retry. Bubble layout may change after approval; its task ranking/actions must survive. |
| Task Workspace | `ui/tasks/TasksScreen.kt`, `TasksViewModel.kt`, `TaskRow.kt`, `TaskLabels.kt`, `TaskExtensionFields.kt`; `ui/components/TaskEditorSheet.kt`, `TaskDeadlineFields.kt` | Search/category filters, deadline buckets, completed expansion, create/edit/status, soft-delete/undo, reminders, recurrence and checklist/extension fields. ORGANIZATION remains a task category even though Spaces has no Organization page. |
| Academics in progress | `ui/courses/CoursesScreen.kt`, `CoursesViewModel.kt`, `CourseEditorSheet.kt`; `ui/academic/AssignmentViewModel.kt`, `AssignmentEditorDialog.kt`, `CourseAssignmentsSection.kt` | Current untracked course/assignment work has real routes, course list/edit/archive/timetable and task-linked assignment lifecycle. Preserve data and access; related materials are still a placeholder. Do not describe all Academics as absent or declare this work released. |
| Plan/Timeline | `ui/timeline/TimelineScreen.kt`, `TimelineContent.kt`, `TimelineViewModel.kt`, `TimelineStopRow.kt`; `ui/components/EventEditorSheet.kt`; `domain/logic/TimelineBuilder.kt` | Week/day planning, real task deadlines, event create/edit/delete/undo and chronological behavior. Existing event kinds include CLASS, EXAM, MEETING, ACTIVITY, NOTE, OTHER. Pictured tabs do not create calendar/priority features. |
| Capture | Current Home quick add and shared `TaskEditorSheet.kt`; no Capture route in `Destinations.kt` | Keep task creation immediately available. A new Capture screen is a planned IA surface; photo, scan, audio and universal inbox need separate capability decisions. Vault upload remains an existing feature, even if new Capture routing is undecided. |
| Vault | `ui/vault/VaultScreen.kt`, `VaultViewModel.kt`, `VaultUiState.kt`; `data/remote/drive/DriveRepository.kt`, `DriveApi.kt`, `GoogleDriveAuthorization.kt` | Real Google Drive OAuth, browse/search/breadcrumb/load-more, open/download, upload/progress/cancel, retry/reconnect and dependency/error states. Do not regress to the v0.1 placeholder described in older docs. No assumed local mirror or total cloud storage telemetry. |
| Devices/Sync | `ui/sync/SyncScreen.kt`, `SyncViewModel.kt`, `SyncUiState.kt`; `data/remote/device/CompanionRepository.kt`, `HandoffRepository.kt`, `HandoffTransport.kt`, `CompanionProtocol.kt`, `HandoffModels.kt`; `companion/riko_companion/` | LAN trusted-laptop pairing, connect/forget, staged file send with explicit confirmation, incoming-offer save destination, text preview/consume, history/retry and workspace-session metadata. This is not general multi-device database sync, Bluetooth accessory management or battery telemetry. |
| Settings/backup | `ui/settings/SettingsScreen.kt`, `BackupSettings.kt`; `data/prefs/SettingsDataStore.kt`; `data/backup/LocalBackupService.kt` | SYSTEM/LIGHT/DARK themes, display name, SPATIAL_COMPASS/COMPASS_ONLY preference; local JSON export/import preview/merge, optional preference restoration. Automatic backup is not enabled by a design image. |
| Secondary task surfaces | `ui/widget/TaskWidgetProvider.kt`, `work/TaskReminders.kt`; repository mutation hook; `core/time/Clock.kt` | Reminders, widget refresh, recurrence generation and clock/zone-based deadlines keep the same data semantics. Launcher widget uses RemoteViews and does not inherit Compose effects automatically. |

## Proposed component boundaries

Names here are proposed names, not claims that classes already exist. Use existing shared components where they fit; avoid a general-purpose design engine.

| Proposed component | Visual association and behavior | Compose approach | Existing integration surface |
|---|---|---|---|
| `RikoVisualTokens` | Graphite/Tangerine roles and shared readable geometry; A5 brand/palette area UNKNOWN | Extend semantic colors/shape/spacing/type locals after owner token approval; no image-pixel constants | `ui/theme/Theme.kt`, `RikoColors.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `HomeTokens.kt` |
| `OrganicSurface` | Scalable designed contours; owner-required Workspace lava; candidate A1/A2 orange waves | Cached normalized cubic paths drawn with `Canvas`/`drawWithCache`; stable rectangular semantic/hit layer over decorative shape | `ui/components/` (new proposed primitive), existing `ScreenFrame.kt` |
| `RikoNavigationDock` | Warm active contour, labeled icon targets; A5 bottom dock UNKNOWN | Custom `Row`/layout + path behind selected item; native selectable semantics, focus, insets and interruptible transition | `ui/compass/CompassChip.kt`, `SpatialNavLayer.kt`, `navigation/RikoNavHost.kt`, `SpatialMap.kt`, `Destinations.kt`, `ui/components/DestinationLabel.kt` |
| `TodayFocusDeck` | Orange dominant focus card, task summaries; A5 Today UNKNOWN | Real task-derived state, stable task-ID callbacks, surface emphasis and concise row hierarchy | Existing Home ViewModel/snapshot; preserve rank/pin/actions |
| `TaskWorkspaceSurface` | Expanding surface beneath animated lava header; source missing | One stateful task workspace; coordinated bounds/height transition and expansion state, `LazyColumn` keyed by task ID, shared editor; no unrelated replacement screen | `ui/tasks/`, `ui/components/TaskEditorSheet.kt`, `TaskDeadlineFields.kt` |
| `CreativeTimeMap` | Sinuous path, dots, time labels and offset event blocks; A5 Plan UNKNOWN | Chronological semantic rows with measured anchors; Canvas connector behind content; virtualized sections and real week/day state | `ui/timeline/`, `domain/logic/TimelineBuilder.kt`; keep builder semantics |
| `SpacePortal` and four distinct compositions | A5 entry UNKNOWN; A1–A4 detail CANDIDATE | Shared portal interaction only; each page has its own content layout. Bundle approved optimized artwork, no network fetch for decoration | New presentation surfaces/routes proposed; Academics wraps existing Courses; Devices reuses Sync state |
| `CaptureSurface` | Irregular drop zone and supported actions; A5 Capture UNKNOWN | Task creation first; distinguish planned from available actions. Use native file picker for any approved existing upload routing | Shared task editor; Drive flow only if explicitly routed |
| `VaultShelfHeader` + file rows | Shelf/search hierarchy; A5 Vault UNKNOWN | Decorative header separate from accessible search and real file row actions | Existing Vault state, auth and repository |
| `MotionPolicy` | Common default/reduced-motion rules | Extend existing motion preference; finite transitions plus bounded owner-approved header animation, pause when not visible | `ui/theme/Motion.kt` and screen lifecycle |

## Data and integration boundaries

`RikoHubApp.kt` and `AppContainer.kt` construct Room, repositories, DataStore, local backup, reminders/widgets, Drive and Companion/Handoff. Local source-of-truth remains in `data/local/RikoDatabase.kt`, DAOs, `data/repository/Repositories.kt`, `CourseRepository.kt` and `AssignmentRepository.kt`; UI consumes existing Flow/StateFlow through ViewModels. The inspected dirty database is v4 with academic entities/migrations. A visual-only phase must not change schema, backup format or transfer protocol to reproduce mockup content.

Offline-first means local tasks/events/courses/preferences can be read and edited without connectivity. Drive and LAN handoff necessarily expose separate authorization, connection and failure states. It does not promise that remote files or an absent laptop work offline. Never replace a transport status with a decorative green dot, simulate sent files or auto-send on navigation.

New Projects/Personal aggregation is a proposal over existing task categories; project objects, Git feeds, health tracking, finance, journals and analytics are not established by the inspected source or images. Devices is a proposed home for existing Sync capabilities. The owner must approve scope and reachability before routes migrate. Domain/category identity and underlying task data must remain distinct from the four Spaces names.

## Performance and accessibility design constraints

- Avoid per-frame recomposition of full lists; animate drawing/transform state locally and cache paths/brushes against size changes. Bound contour complexity and simultaneous animated regions.
- Do not ship the whole generated phone screenshot as UI or depend on costly real-time blur to imitate its lighting. Optimize approved illustration regions as local assets, preserving identity; any material visual reduction goes through the deviation register.
- Draw connectors behind readable controls. Decorative Canvas has no independent reading order; native semantic controls carry actual data, actions and state.
- Key items and preserve scroll, filter, expansion and edit state across navigation/configuration. Back and focus restoration must remain predictable.
- Support native minimum target sizes, large text, TalkBack, switch/keyboard access and theme/reduced-motion preferences. Candidate dense columns reflow instead of shrinking type.
- No performance or accessibility pass is claimed here. Riko performs all implementation-time measurement and runtime verification.
