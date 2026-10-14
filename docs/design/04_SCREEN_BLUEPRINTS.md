# Screen Blueprints (Conceptual)

These blueprints map the proposed direction to screen intent and content hierarchy. They are not claims that every pictured feature exists or has been approved. Existing product capabilities and persistence are authoritative; do not populate these layouts with invented sample statistics or connectivity. Candidate art references are in the [asset manifest](ASSET_MANIFEST.md).

## Shared shell

Provide a native app shell with a clear destination title, a selected navigation item, safe-area-aware content, and access to existing settings and sync functions. The bottom navigation order is unresolved: the images disagree, and the owner has not selected one. Keep the navigation model configurable in design until that decision is made. Preserve existing Sync and settings access even though they are absent from some concepts; do not imply that a new Link destination replaces either one.

## Today — Focus Deck

**Status:** CANDIDATE; Today concept provisionally approved.

Answer “what matters now?” with the data and behavior currently surfaced through task bubbles: ranked tasks, snapshot/progress, next obligation, pin/focus state, and quick add. Bubble geometry is not automatically a redesign requirement. A5 proposes an orange focus hero below date and connection pills, then paired summary panels and short task rows. Treat the connection pill as a proposal only and show it only when grounded in actual state. Preserve access to the full task view. Loading, no-task, and stale/offline states should keep quick add available and clearly distinguish absent data from zero progress. Do not copy the board's displayed name, due time, mood, or focus duration as real data.

## Task Workspace

**Status:** Owner-approved requirements: animated lava-like organic header and an expanding task surface. Source image: missing/UNKNOWN. The owner authorized us to propose the screen architecture; this proposal does not approve a missing image.

Give the task list a clear title, category filters, search, and scannable time-grouped rows. Preserve existing task CRUD, status, soft delete with undo, reminders, recurrence, checklists/extensions, and completed-task expansion. The organic hero/header anchors the workspace; the task surface expands when the user requests more working room. The resting form should remain usable and legible, and expansion must expose a clear way to return to the previous layout. Include empty search/no-task, loading, and save failure feedback with retry where supported; never let a decorative header obscure those states. Exact header geometry and animation parameters remain unspecified; follow the current task data model.

## Plan — Time Map / Timeline

**Status:** CANDIDATE; creative Timeline concept provisionally approved.

Show time in chronological order with the existing week/day planning and event/task CRUD. A5 proposes a seven-day strip above left-aligned time labels, a sinuous multicolor path with dots, and offset stop blocks. Keep the text and selected state legible independently of the path. This mapping remains a visual proposal pending owner acceptance. Include empty-day/week and loading states, and preserve save-failure feedback and retry where supported. Do not assume the pictured schedule, projects, locations, or event count exists.

## Spaces entry — Portal Rooms

**Status:** A5 UNKNOWN; individual Spaces art A1–A4 CANDIDATE. The owner confirmed the four Spaces are Academics, Projects, Personal, and Devices. Order, availability, and final layout remain open. The owner authorized a proposed mapping of A5's Today and Plan panels to the provisionally approved concepts; acceptance of this visual mapping is still pending.

Use an entry surface to show the four owner-confirmed names with distinct, understandable identities. A5 proposes illustrated portal tiles: an academic desk scene, purple project station, cozy personal room, and device desk. The specific illustration, colors, order, availability, and card interaction remain proposals. Show an unavailable or empty room honestly rather than implying a live module. Provide a loading state if the portal's content is loaded asynchronously.

## Academics Space

**Reference:** A1, CANDIDATE. A broad orange wave sits behind the focus panel; below it are narrow course tiles, a date/deadline row, paired study/quick-action panels, and recent material rows. These are possible groupings, not proof that every pictured module exists. Use only current academic data and actions, with clear pathways to their source views. Course/assignment routes remain uncommitted; show loading, empty, offline, and source failure states only where those sources are actually connected.

## Projects Space

**Reference:** A2, CANDIDATE. Its split hero pairs a featured project summary with a checklist, followed by wide progress-and-tag rows, a sprint strip, and offset milestone/activity panels. Keep this dense layout responsive and collapse panels when space or font scale requires. Git activity, percentages, sync, and handoff are only examples unless backed by actual sources. Empty, loading, offline, and activity-source failure states should explain what is unavailable.

## Personal Space

**Reference:** A3, CANDIDATE. A moonlit window, sleeping cat, and plants create a tall calm hero; a wide journal panel sits below paired content groups. Health, sleep, mood, finance, medication, and habit modules each require explicit owner approval and product/data-source review before inclusion. Do not add their collection or display based on this image. If approved modules load external or protected data, provide clear permission denial, empty, offline, and failure states.

## Devices Space

**Reference:** A4, CANDIDATE. The screen pairs two device cards with a visible connector, then places a queue below, transfer and clipboard panels side by side, and trust/history sections full width. Map only current LAN pairing, trusted laptop, staged send, incoming save, text transfer/history, retry, and workspace session behavior. Provide permission-denied, unpaired, offline, loading, empty queue, and failed/retry states as applicable. The pictured hardware names, battery percentages, and transfer statuses are examples, not product state.

## Capture — Drop Zone

**Status:** A5 UNKNOWN. Preserve only capture actions that already exist.

Current quick add creates a task. Keep that action direct and easy to reach. A5's asymmetric drop zone, floating file/image/audio symbols, and concise prompt are visual composition only; do not present scan, microphone or photo as implemented, or imply that a standalone Capture upload flow already exists. File upload does exist in Vault; routing that existing action through Capture requires an explicit design decision and must preserve its picker/auth/progress/cancel behavior. The broader Capture destination is planned, with visual approval UNKNOWN. Show task-save feedback and failure/retry; request permissions only for a capability that is actually implemented, with a clear denial path.

## Vault — Knowledge Shelf

**Status:** A5 UNKNOWN. Preserve the current vault/materials behavior.

Preserve the existing Google Drive OAuth flow and supported browse, search, upload, download, cancel, and retry behavior. A5 suggests a compact shelf header, search/filter controls, and short file rows; retain this practical density and keep names/source detail accessible. Show loading, empty results, offline/provider failure, upload/download progress, cancellation, and retry results according to the existing connector behavior. Explain OAuth permission denial or expiry and provide the supported recovery action. Do not imply storage capacity or cloud status without actual data.

## Sync and settings continuity

Keep existing Sync and settings destinations reachable in the final navigation model and flows. Preserve the current LAN pairing/trusted laptop flow, staged send, incoming save, text transfer/history, retry, and workspace sessions; do not frame Sync as general data synchronization. Keep current settings for theme, display name, navigation style, JSON backup/import preview and merge, preference restore, widget, and reminders. The supplied screens do not determine new layouts for these functions. Do not encode “connected” or successful transfer in decorative color without a confirmed state.

## Open design decisions

The owner still needs to confirm A5 approval and acceptance of its proposed Today/Plan visual mapping, select navigation order, and decide candidate Space layouts. The underlying feature and data inventory is documented elsewhere in the project; these blueprints preserve those source behaviors and do not define new product capabilities.
