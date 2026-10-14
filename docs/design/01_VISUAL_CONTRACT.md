# Visual contract

## Authority and approval scope

The [index](00_DESIGN_INDEX.md) records the repository baseline and status definitions. [ASSET_MANIFEST.md](ASSET_MANIFEST.md) records image approval. Owner-confirmed requirements are binding; every Compose technique and screen mapping below is an **architectural proposal** until accepted. A filename suffix, timestamp, attractive render or existing code pattern cannot confer approval.

Source set: `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_43 AM-1.png` (A1, Academics, CANDIDATE), `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_45 AM-2.png` (A2, Projects, CANDIDATE), `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_46 AM-3.png` (A3, Personal, CANDIDATE), `redesign-idea/ChatGPT Image Oct 13, 2026, 08_17_48 AM-4.png` (A4, Devices, CANDIDATE), and `redesign-idea/Riko Hub_ Cozy Academic Homebase.png` (A5, overview board, UNKNOWN). These paths replace the absent requested directory by owner clarification, without moving files.

## Binding owner requirements

| Requirement | Preserve in implementation | Forbidden silent substitution |
|---|---|---|
| Graphite & Tangerine foundation | Dark graphite surface relationships, warm orange emphasis and readable warm light text; final numeric palette still undecided | Reusing the older ivory/pastel palette as the new approved foundation |
| Warm, playful, mature | Friendly organic forms and expressive hierarchy, with restrained ornament around useful information | Decorative density that hides deadlines, infantilizing copy or a generic corporate card system |
| Dense but readable | Focus, title, deadline, progress and next action clearly ranked | Tiny unreadable metadata to match a compressed mockup |
| Page-specific identity | Each page has its own composition and pacing inside shared semantics | Four Spaces pages reduced to identical card grids |
| Organic composition | Designed curved boundaries and silhouette vocabulary | Replacing all organic shapes with standard rounded rectangles |
| Tactile controls | Legible selected, pressed, focused and disabled states, responsive feedback | Flat indistinguishable controls or visual effects that delay input |
| Custom animated navigation | Purpose-built navigation with meaningful selected-state transition and non-gesture access | Stock Material `NavigationBar` used as the final visual design |
| Task Workspace | Animated organic lava-like header and task surfaces that expand with spatial continuity | Static rectangular header or navigating an expanding surface into an unrelated generic page |
| Creative Timeline | A readable creative time-map, connected chronologically to actual tasks/events | A generic task list as the primary replacement |
| Spaces information architecture | Academics, Projects, Personal, Devices | Treating old task categories as four completed Spaces implementations |
| Lightweight native Android | Compose/native primitives, bounded drawing, lifecycle-aware motion | WebView UI, continuous full-screen effects or unnecessary animation packages |
| Offline-first, functionality preserved | Local work remains usable offline; actual Drive/LAN integrations retain honest dependency states | Network-gating task editing or removing real features absent from generated art |

Today and creative Timeline have **provisional concept approval**, represented as CANDIDATE pending final visual acceptance. This does not approve every widget or sample in A5.

## Visual evidence to preserve if its scope is approved

These are concrete observations, not additional APPROVED requirements. Approval of a panel should specify which of these become locked; until then preserve them in proposals rather than simplify them without discussion.

- **A5 Today:** orange main-focus deck beneath illustrated greeting; compact date/connection pills; subordinate paired summary surfaces; concise task rows; orange organic edge above custom dock. Translate focus, deadline and progress from real task data; mood/session figures are unsupported unless separately scoped.
- **A5 Plan:** compact seven-day strip and three mode pills; left time labels; sinuous multicolor path with small luminous stop dots; offset horizontal activity blocks. Preserve the time-map's distinct visual grammar while making order and times accessible. Do not infer real calendar/priority modes solely from illustrated tabs.
- **A5 Capture:** large irregular drop-zone silhouette with floating content-type symbols and small capture tiles; quick-text field below. This is not evidence of scan, voice, camera or inbox functionality.
- **A5 Vault:** shelf/books header, search, category tiles and compact recent-file rows. Numbers, files and storage usage are conceptual. Match existing Drive capabilities and explicit provider states.
- **A5 Spaces:** four tall illustrated portal tiles in two rows: academic desk, purple project workstation, warm personal room and device desk. Candidate detail-page art need not use the same scene, but the entry should preserve the portal idea if approved.
- **A1 Academics:** orange flowing header, black cat at laptop beside books/plants, quote capsule, broad focus card with a wave-shaped internal region, narrow course tiles, date-led deadlines and compact materials rows. Study-time visualization is a concept, not a product requirement.
- **A2 Projects:** larger typography with light-to-orange title, bright lava composition and laptop cat; split focus/milestone hero; wide project rows with icon blocks, tags and progress; weekly sprint strip; paired milestone/activity panels. Sample Git events and progress numbers are fictional.
- **A3 Personal:** restful room with moon/planet window, pendant lighting and sleeping cat; violet rest ring paired with multicolor mood curve; wide finance area; habit circles, reminders and illustrated quick-journal surface. These metric systems are not implemented functionality by visual implication.
- **A4 Devices:** desk illustration with phone/laptop/lamp, two outlined device panels joined by a short connector, queue rows, quick-transfer and clipboard panels, trust list and activity rows. Online state, battery readings, Apple names and peripherals are fictional content.

## Navigation conflict is unresolved

A5 shows Today / Plan / Capture / Vault / Spaces. A1 reorders Spaces; A2 includes Link and omits Plan; A3 uses Home and Link while omitting Capture; A4 adds a central plus alongside five labels. These cannot all be authoritative route specifications. Proposed common shell: A5's five areas, with Task Workspace nested from Today and Sync capabilities accessible through Devices. **This remains a proposal**, including tab order, names, central-plus behavior, back behavior and preservation of the existing compass setting. Record the owner's decision under D03 before navigation implementation.

No gesture may be the only way to reach a destination, expand a task, complete an item or return. A custom dock may use Material interaction/semantics helpers without inheriting stock navigation visuals.

## Contract change procedure

1. Identify traceability ID and actual source path/panel, and quote the affected owner requirement.
2. Record proposed change, reason, before/after behavior, functionality impact, accessibility and performance implications in [08_DEVIATION_REGISTER.md](08_DEVIATION_REGISTER.md).
3. Supply a reviewable proposal. If fidelity cannot be achieved, mark the deviation as requiring owner approval; do not substitute silently.
4. Only explicit owner acceptance changes status/scope. Preserve original assets and prior decisions in the register.
5. Update acceptance criteria and phase prerequisites together. Implementation convenience alone is not approval.

Reduced-motion presentation is a designed accessibility fallback: it retains organic silhouettes, selection and task continuity. It is not permission to replace the default lava header with a rectangle or remove the primary creative timeline.
