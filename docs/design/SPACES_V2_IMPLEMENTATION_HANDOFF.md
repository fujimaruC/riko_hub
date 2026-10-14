# Spaces V2 — Implementation Handoff
*Creative Director → Codex · 13 Oktober 2024*

> **Document authority.** This handoff is grounded in the four V2 HTML mockups
> (`redesign-idea/academics-v2.html`, `projects-v2.html`, `personal-v2.html`,
> `devices-v2.html`), the existing Android source, and the established design
> contract in `docs/design/`. The V2 HTML files supersede A1–A4 as the visual
> source of truth for their respective Spaces pages, per Project Owner direction.
> The broader design contract in `01_VISUAL_CONTRACT.md` and `06_VISUAL_ACCEPTANCE.md`
> remains in force; this document adds page-specific specification rather than
> replacing existing contracts.
>
> **Scope boundary.** This is a design specification, not implementation. No
> Kotlin, no Gradle, no builds. The existing architecture, database schema, and
> existing Sync/Courses sources are preserved without change.

---

## Table of Contents

1. [A — Visual Tokens](#a--visual-tokens)
2. [B — Composition per Page](#b--composition-per-page)
3. [C — Component Inventory](#c--component-inventory)
4. [D — Icon Replacement Plan](#d--icon-replacement-plan)
5. [E — Motion Contract](#e--motion-contract)
6. [F — Interaction Contract](#f--interaction-contract)
7. [G — Design Fidelity Rules](#g--design-fidelity-rules)
8. [Acceptance Checklists](#acceptance-checklists)
9. [Codex Implementation Order](#codex-implementation-order)

---

## A — Visual Tokens

### A.1 Color roles — mapping V2 HTML to existing Kotlin

The existing `RikoColors.kt` and `Color.kt` define the semantic color system.
V2 HTML mockups use named CSS variables. The mapping below connects them to
existing Kotlin tokens. Do **not** add new color constants before checking this
table; every V2 color has a live counterpart.

| V2 HTML variable | Existing Kotlin equivalent | Hex (dark) | Semantic role |
|---|---|---|---|
| `--graphite-900` | `DarkScheme.background` | `#17181F` | App canvas |
| `--graphite-800` | `DarkScheme.surface` | `#1F2029` | First card layer |
| `--graphite-700` | `DarkScheme.surfaceVariant` / `surfaceContainer` | `#2A2B37` | Second card / row |
| `--graphite-600` | `DarkScheme.outline` (hover fill) | `#3A3B49` | Hover / pressed surface |
| `--graphite-500` | — | `#403C38` | Dividers, subtle strokes |
| `--tangerine` | `DarkRikoColors.projects.color` (closest) | `#E8742A` ¹ | Primary accent, Tangerine |
| `--tangerine-dim` | `DarkRikoColors.projects.container` | `#C25F18` | Tangerine pressed / deep |
| `--text-primary` | `DarkScheme.onBackground` | `#F1ECE2` | Primary text |
| `--text-secondary` | `DarkScheme.onSurfaceVariant` | `#A39EAD` | Supporting text |
| `--text-muted` | (alpha-dimmed onSurfaceVariant) | `#7A7470` | Metadata |
| `--jade` | `DarkRikoColors.personal.color` | `#82DDBC` | Status positive, Academics |
| `--ultramarine` | `DarkRikoColors.organization.color` | `#82C6F4` | Projects/Devices accent |
| `--magenta` | `DarkRikoColors.urgent.color` | `#FF907F` | Urgency, deadline |
| `--connected` (Devices) | `DarkRikoColors.personal.color` (green) | `#3ED87A`² | Live connection dot |
| `--amber` | `DarkRikoColors.projects.color` | `#F7CD68` | Projects variant |
| `--warm-cream` (Personal) | `DarkRikoColors.peach.color` | `#FFC19B` | Personal accent |
| `--sage` (Personal) | `DarkRikoColors.personal.color` (de-saturated) | `#6A8A6E` | Routine chip |

> ¹ **Tangerine gap**: the existing `#E8742A` is not a named constant in
> `Color.kt` or `RikoColors.kt`. The `projects.color` at `#F7CD68` is amber/gold.
> **Owner decision needed**: confirm `#E8742A` as the canonical Tangerine and add
> it as `SpacesTokens.tangerine` or equivalent before Phase 6 implementation.
>
> ² `#3ED87A` is brighter than the existing jade tokens; it should remain a
> Devices-specific semantic constant (`DevicesTokens.connectedGreen`) and never
> bleed into other pages.

### A.2 Tangerine accent — page-level overrides

Each Space uses Tangerine as its primary accent but pairs it with a domain color:

| Space | Primary accent | Domain pair | Usage |
|---|---|---|---|
| Academics | Tangerine | Jade (`--jade`) | Course satellites, progress |
| Projects | Tangerine | Ultramarine + Amber | Per-project color bars |
| Personal | Tangerine | Warm cream / sage | Journal send, routine done |
| Devices | Tangerine | Connected green | Bridge when live |

### A.3 Typography — mapping V2 to `Type.kt`

Fonts in the project: **Nunito** (UI) and **Fredoka** (display). No new fonts needed.

| V2 usage | `RikoTypography` role | Size · Weight |
|---|---|---|
| Page title (`Akademik`, `Workshop`, `Personal`, `Perangkat`) | `displayMedium` | 28sp · SemiBold · Fredoka |
| Section header (`Fokus Hari Ini`, `Semua Proyek`) | `titleSmall` | 15sp · Bold · Nunito |
| Card title (course name, project name) | `titleMedium` | 17sp · Bold · Nunito |
| Body rows (task name, material name) | `bodyMedium` | 14sp · Medium · Nunito |
| Metadata (course code, time, size) | `labelSmall` | 11sp · SemiBold · Nunito |
| Badge text (deadline label, tags) | `labelMedium` | 12sp · SemiBold · Nunito |
| Action buttons / "Lihat semua" | `labelLarge` | 14sp · Bold · Nunito |
| Status label (`Besok`, `Konseptual`) | `labelMedium` | 12sp · SemiBold · Nunito |

> Minimum floor: `labelSmall` (11sp). No metadata may go below 11sp.
> All metadata must reflow, not shrink, under font scaling ≥ 130%.

### A.4 Spacing — mapping V2 to `Spacing.kt`

| V2 usage | `Spacing` token | dp |
|---|---|---|
| Horizontal screen gutter | `space5` | 20dp |
| Gap between sections | `space5` | 20dp |
| Gap between rows in a list | `space2` | 8dp |
| Card internal padding | `space3`–`space4` | 12–16dp |
| Chip internal padding | `space2`×`space3` | 8×12dp (h×v) |
| Icon button size | `compass` | 40dp |
| Touch minimum | `touch` | 48dp |
| Orbit canvas height (Academics) | `illustration` | 144dp (or up to 200dp) |

### A.5 Shapes — mapping V2 to `RikoShapes`

| V2 usage | `RikoShapes` token | dp |
|---|---|---|
| Phone frame / page wrapper | n/a | system edge |
| Large hero cards (Focus, Active Project) | `tall` | 28dp |
| Standard cards (Project rows, Materials) | `wide` | 24dp |
| Small chips (deadline, badge, tag) | `pill` | 50% |
| Icon buttons (nav, action) | `pill` | 50% |
| Quick action buttons | `medium` | 16dp |
| Course satellite nodes | `small` | 12dp |
| Orbit core (Academics) | `wide` | 24dp |
| Journal surface | `tall` | 28dp |
| Bottom nav bar | top-only `sheet` | 32dp top |

### A.6 Textures, gradients, and overlays

These are visual primitives that must be reproduced in Compose `Canvas` or
`Brush`. None requires external assets.

| Surface | Technique | V2 definition |
|---|---|---|
| **Workshop hero texture** (Projects) | `Canvas.drawWithCache`, two `Brush.linearGradient` nets | Vertical + horizontal lines, 24dp grid, `rgba(255,255,255,0.015)` each |
| **Journal lined paper** (Personal) | `Canvas.drawLines()` in `onDraw`-equivalent | 22dp row height, `rgba(200,184,154,0.05)` stroke |
| **Orbit rings** (Academics) | SVG-style `Path.addOval()`, `PathEffect.dashPathEffect([4,6])` | Two ellipses, `rgba(232,116,42,0.10)` + `rgba(255,255,255,0.05)` |
| **Ambient blob / glow** (all headers) | `drawWithCache`, `Brush.radialGradient`, `BlurMaskFilter` | Page-specific; see B sections below |
| **Transfer progress shimmer** (Devices) | `Brush.linearGradient` animated offset | `rgba(255,255,255,0.4)` bar sweep |
| **Connection bridge line** (Devices) | `Canvas.drawLine()` + animated dot via `PathMeasure` | `rgba(62,216,122,0.3)` when live; `rgba(120,116,112,0.2)` when offline |

---

## B — Composition per Page

### B.1 Academics — The Living Orrery

**Source:** `redesign-idea/academics-v2.html`

#### Layout regions (top → bottom, scrollable)

```
┌─────────────────────────────────┐
│  StatusBar (system)             │
│  BackRow + PageTitle            │  sticky: no
│  OrbitCanvas  ≈ 200dp           │  non-scrollable decoration
│  SectionLabel "Fokus Hari Ini"  │
│  FocusCard (hero)               │
│  SectionLabel "Deadline & UTS"  │
│  DeadlineChipRow (h-scroll)     │
│  Divider                        │
│  SectionLabel "Materi Terbaru"  │
│  MaterialList (3 rows max vis.) │
│  Divider                        │
│  SectionLabel "Aksi Cepat"      │
│  QuickActionGrid (2×2)          │
│  BottomNav                      │  sticky
└─────────────────────────────────┘
```

#### Orbit canvas specification

- **Canvas size:** full width × 200dp. Not scrollable. Sits between the page header and the Focus card.
- **Center core:** 88×88dp rounded rect (24dp radius) with tangerine ambient fill. Contains the active course icon (44dp) + name (10sp Bold) + code (9sp). Positioned at canvas center.
- **Satellite nodes:** 4 nodes, 56×56dp, at fixed angular positions relative to center:
  - Top-left: ~135° (Math/Diskrit)
  - Top-right: ~45° (Programming)
  - Bottom-left: ~225° (English)
  - Bottom-right: ~315° (Statistics or 4th course)
- **Orbit ellipses:** Two `Path.addOval()`, centered on canvas, drawn with dashed stroke behind everything.
- **Adding more courses:** When the user has 5–8 courses, reduce satellite size to 44dp and add an outer ring. Above 8 courses, show 4 satellites + "…+N" chip at the bottom-right position. Never crowd the canvas; always preserve the core as the largest element.
- **Orbit tap:** Tap any satellite → it animates to the core position (200ms, `standard` easing), the old core fades and slides to the tapped position. The Focus card below updates to reflect the newly selected course.

#### Focus card specification

- Background: `Brush.linearGradient` from `tangerine.copy(alpha=0.14)` → `graphite-800`. **No flat color.**
- Border: `1.5.dp` `tangerine.copy(alpha=0.25)`, increases to `0.45` on press.
- Pulse dot: 6×6dp circle, `tangerine`, `alpha` animated 1.0→0.5→1.0 every 2000ms.
- Progress bar: 5dp height, `Brush.linearGradient(tangerine-dim → tangerine → #F4A050)`.
- "Next action" row: `rgba(255,255,255,0.04)` fill, 10dp radius, chevron circle (26dp, tangerine fill).
- **Data binding:** drive entirely from `CourseRepository` + `AssignmentRepository`. No hardcoded content.

#### Deadline chips

- Horizontal `LazyRow`. Three urgency tiers:
  - `urgent` (≤1 day): `magenta.copy(0.08)` fill, `magenta.copy(0.4)` border.
  - `soon` (2–5 days): `tangerine.copy(0.06)` fill, `tangerine.copy(0.3)` border.
  - `ok` (>5 days): `graphite-700` fill, subtle border.
- Minimum chip width: 160dp. Date box: 40×44dp.
- Empty state: single chip "Tidak ada deadline minggu ini" in `ok` style.

#### Scroll behavior

- `LazyColumn`, orbit canvas is a `stickyHeader` above Focus card — NO, it should be a regular item (first item) so it scrolls away as the user goes deeper.
- Section headers are **not** sticky; this page breathes with gentle vertical scroll.
- Bottom nav is sticky via `Scaffold`.

#### Responsive adaptation (< 360dp width)

- Orbit canvas: reduce satellite size to 48dp, reduce outer ellipse radius proportionally.
- QuickActionGrid: collapse to single column (2×1 + 2×1 stacked).
- DeadlineChipRow: chips may be narrower but minimum 140dp.

#### Empty states

| Region | Empty state |
|---|---|
| Orbit | Show core with "Belum ada mata kuliah" label, no satellites |
| Focus card | "Tidak ada kelas hari ini" with a subdued calendar icon |
| Deadlines | Single chip "Tidak ada deadline" |
| Materials | "Belum ada materi terbaru" row with upload CTA |

#### Loading state

All regions show shimmer skeletons using `Brush.linearGradient` animated offset.
Orbit shows a pulsing core and 4 empty satellite rings.

---

### B.2 Projects — The Maker's Bench

**Source:** `redesign-idea/projects-v2.html`

#### Layout regions

```
┌─────────────────────────────────┐
│  StatusBar (system)             │
│  HeaderRow (breadcrumb + add)   │  sticky: no
│  PageTitle "Workshop"           │
│  ActiveProjectHero              │  full-width, ~180–220dp tall
│  SectionLabel + FilterChipRow   │
│  ProjectList (variable)         │
│  SprintWeekStrip                │
│  IdeaBoard (stickies h-scroll)  │
│  [spacer 90dp]                  │
│  BottomNav                      │  sticky
└─────────────────────────────────┘
```

#### Active project hero

- **Surface texture:** `drawWithCache` — two `DrawScope.drawLines()` passes, one horizontal (y-step = 24dp) and one vertical (x-step = 24dp), stroke width 0.5dp, color `White.copy(alpha=0.015)`. Cache invalidates on size change only.
- **Ambient glow:** `Brush.radialGradient` at top-right corner, `tangerine.copy(0.30)` → transparent, radius 90dp, `BlurMaskFilter(40f, NORMAL)`.
- **Pulse label:** same 6dp animated dot as Academics focus card.
- **Milestone track:** 4-item column inside a `Column` inside a `Box` with `rgba(0,0,0,0.2)` fill inner panel. States: `done` (jade filled check), `active` (tangerine border, empty), `pending` (dim border, empty).
- **Progress bar:** 6dp height, animated fill, `tangerine-dim → tangerine → #F4A050`.
- **Data:** Active project = highest-priority PROJECTS-category task group. Milestone = sub-tasks linked to that task group. No Git data, no external sync claims.

#### Project list color identities

Each project card has a 3dp left-edge accent derived from its stored color or hashed from its ID. The card background is a very faint tint of that color (`copy(alpha=0.04)`). Available accent colors: jade, ultramarine, amber, magenta, tangerine. Assigned deterministically via `HomeTokens.variant(projectId)`.

#### Filter chips

- `LazyRow`, `RikoShapes.pill`, border `graphite-600`, active state: `tangerine.copy(0.15)` fill + `tangerine.copy(0.4)` border + `tangerine` text.
- Filters: `Aktif`, `Semua`, `Selesai`, `Arsip` — these map to actual task status filters on the underlying PROJECTS category.

#### Sprint week strip

- 7 columns, equal width. Each column: day name (`labelSmall`), number button (30dp × 30dp, 10dp radius), status dot (5dp).
- Today: `tangerine` filled number button, white text.
- Has-work days: `graphite-600` fill. Future/empty: transparent.
- Status dot color = the color of the most-due project for that day. Gray if no project.
- **Data:** derives from task deadlines in the PROJECTS category for the current 7-day window.

#### Idea board

- `LazyRow` of sticky note `Box` composables. Each sticky: 110dp × 80dp minimum, `RikoShapes.small`, per-note rotation via `graphicsLayer { rotationZ = ... }` derived from index (±2°).
- Colors: cycle through tangerine, amber, jade for visual variety.
- Final item: dashed-border add chip, 80×80dp.
- **Data:** maps to a lightweight "Quick Note" or memo entity associated with the active project. If no such entity exists yet, this section is collapsed to just the add chip.

#### Responsive adaptation

- Hero at width < 360dp: milestone track collapses to just 3 items + "…+N".
- Project rows: tags wrap to a second line rather than truncating.
- Sprint strip: day labels may abbreviate to single letter.

#### Empty states

| Region | Empty state |
|---|---|
| Active project | "Belum ada proyek aktif. Buat proyek baru +" |
| Project list | Full-width card with "Mulai proyek pertamamu" |
| Sprint strip | Show skeleton days with no dots |
| Idea board | Just the add chip |

---

### B.3 Personal — Moonlit Room

**Source:** `redesign-idea/personal-v2.html`

#### Layout regions

```
┌─────────────────────────────────┐
│  GardenHeader  ≈ 220dp          │  illustration; status bar overlaid
│  Content scrolls below          │
│  GreetingStrip                  │
│  SectionLabel "Niatku Hari Ini" │
│  TaskList (personal tasks)      │
│  Divider                        │
│  SectionLabel "Jurnal Cepat"    │
│  JournalSurface                 │
│  SectionLabel "Pengingat"       │
│  ReminderList                   │
│  Divider                        │
│  RoutineChips (optional)        │
│  Divider                        │
│  SectionLabel "Catatan Pribadi" │
│  NotesGrid (2-col + wide card)  │
│  [spacer 90dp]                  │
│  BottomNav                      │  sticky
└─────────────────────────────────┘
```

#### Garden header illustration

- **Not a raster image.** Implemented entirely in `Canvas` (Compose `drawWithCache`).
- Background: `Brush.linearGradient` from `#1A1008` (top-left) → `#221508` (mid) → `#0F1A12` (lower) → `#0A100A` (bottom).
- **Blob layer 1 (sage):** `Path` with irregular ellipse, `Brush.radialGradient(sage.copy(0.25) → transparent)`, `BlurMaskFilter(20f, NORMAL)`, positioned bottom-left.
- **Blob layer 2 (tangerine glow):** positioned top-right, `tangerine.copy(0.18)`, `BlurMaskFilter(25f, NORMAL)`.
- **Moon:** `drawCircle`, 30dp radius, `Brush.radialGradient(#D4A030 → #8A6018)`, alpha 0.5, positioned top-right area, `BlurMaskFilter(15f, NORMAL)`.
- **Stars:** 6–8 `drawCircle` calls at deterministic positions (seeded by date), 1dp radius, `White.copy(0.6)`, alpha animated slowly between 0.3–1.0 on staggered timers (see E section).
- **Plant silhouettes:** two `ImageVector` or `Icon` composables (🌿 / 🪴 fallback) positioned bottom-left and bottom-right of the header, with `colorFilter = ColorFilter.tint(Color(0xFF3A3A3A))` to turn them into dark silhouettes.
- **Status bar overlay:** the system status bar sits above the illustration; use `WindowInsets.statusBars` padding so time/icons remain legible over the dark illustration.
- **Page title text:** positioned `Alignment.BottomStart` within the header `Box`, bottom-left, always over a dark enough region. If contrast drops below 4.5:1, add a `Brush.verticalGradient(transparent → Black.copy(0.4))` scrim at the bottom 60dp of the header.

> **FORBIDDEN:** Do not use a raster PNG for this header. The illustration must
> be reproduced as native Canvas so it adapts to any screen width, respects
> reduced motion, and never requires a network fetch.

#### Journal surface

- Background: `Brush.linearGradient(warmCream.copy(0.08) → tangerine.copy(0.04))`.
- Border: `1.dp` `warmCream.copy(0.12)`.
- **Lined paper:** `drawWithCache` — horizontal lines every 22dp, stroke 0.5dp, `warmCream.copy(0.05)`.
- Prompt text: 14sp italic, `warmCream.copy(0.75)`.
- Input area: `TextField` with `colors = TextFieldDefaults.colors(unfocusedContainerColor = Black.copy(0.15), ...)`, min height 48dp.
- Send button: 30dp circle, `tangerine` fill.
- **Data:** maps to a `Journal` or `QuickNote` entity keyed to today's date. No history shown here; "Lihat semua" navigates to journal list. If no journal entity type exists, use a `Task` of a special JOURNAL category with a text field.

#### Personal task list

- Data source: Tasks with category `PERSONAL`.
- Each row: 22dp check circle (not a square), animated fill to jade when tapped (M6 motion).
- Circle border color: tangerine (`urgent`), sage (`routine`), or subtle (`general`).
- **No mood, sleep, health, financial, or analytics data.** These are explicitly out of scope.

#### Reminders

- Source: `TaskReminders` (existing). Show only today's active reminders.
- Status: done = jade filled circle; pending = empty bordered circle.

#### Routine chips (optional section)

- Each chip: icon + label + optional ✓. Tap toggles done/undone state for today.
- This is purely task-completion tracking using a sub-type of the PERSONAL category. No gamification metrics, streaks, or scores.
- Section is collapsible if the user has no routines configured.

#### Notes grid

- 2-column grid. Last item may span full width if its content is longer.
- Cards are `graphite-700/800` with faint domain tint.
- **Data:** Tasks with category PERSONAL and a note body, or a dedicated `Note` entity if available in the schema.

#### Forbidden modules

The following pictured in the original A3 reference are **explicitly excluded**:
- Sleep & Rest panel
- Mood tracking / mood curves
- Keuangan Pribadi (finance tracking)
- Any metric expressed as a percentage score for personal well-being

---

### B.4 Devices — The Living Bridge

**Source:** `redesign-idea/devices-v2.html`

#### Layout regions

```
┌─────────────────────────────────┐
│  StatusBar (system)             │
│  HeaderRow (back + connect btn) │  sticky: no
│  PageTitle "Perangkat"          │
│  BridgeZone                     │  ≈ 100dp, not scrollable
│  SectionLabel "Handoff Queue"   │
│  HandoffSection                 │
│  SectionLabel "Kirim Cepat"     │
│  TransferGrid (2-col)           │
│  ClipboardConceptualCard        │
│  Divider                        │
│  SectionLabel "Perangkat Terpercaya" │
│  TrustedSection                 │
│  DisclaimerCard                 │
│  SectionLabel "Aktivitas Terbaru" │
│  ActivityList                   │
│  [spacer 90dp]                  │
│  BottomNav                      │  sticky
└─────────────────────────────────┘
```

#### Bridge zone — connection state machine

The Bridge Zone is the most identity-defining element of this page. It has **four distinct visual states** all driven by `SyncUiState.companion`:

| State | Bridge line | Dot animation | Panel borders | Label |
|---|---|---|---|---|
| **Connected** | `connectedGreen.copy(0.3)` stroke, 2dp | 3 dots flow left→right, staggered 600ms | Phone: tangerine `0.4`; Laptop: green `0.25` | "Terhubung" + green dot |
| **Disconnected** | `graphite-500` stroke, 1dp dashed | No animation | Both: subtle `graphite-500` | "Tidak terhubung" |
| **Connecting** | tangerine stroke, `PathEffect.dashPathEffect`, animated offset | Single dot oscillates back and forth | Both: tangerine `0.2` | "Menghubungkan…" spinner |
| **Error** | `magenta.copy(0.4)` stroke, 2dp | No dot animation | Both: `magenta.copy(0.2)` | "Gagal terhubung" + retry icon |

**Dot animation (Connected state only):**
- 3 `drawCircle` calls, 3.5dp radius, `connectedGreen`, using `PathMeasure.getPosTan()`.
- Each dot: 1800ms cycle, staggered by 600ms, `FastOutSlowIn` easing.
- **Reduced motion:** dots disappear entirely. Bridge shows only a static `connectedGreen` line + text label.

**Device panels:**
- Phone panel: always "Perangkat ini · Android". Never shows iOS device name.
- Laptop panel: sourced from `CompanionSnapshot.deviceName` + OS. Defaults to "Laptop · Linux" if name is unavailable.
- Battery: hidden unless the companion explicitly sends battery data. Do not invent battery percentages.

#### Handoff queue

- Data from `HandoffSnapshot`. Show up to 3 items; "Lihat semua" navigates to full list.
- Each item: file/content icon, name (truncated to 1 line), device origin + time, "Buka di Laptop →" action button.
- The action button triggers the existing `HandoffRepository` open-on-device flow.
- Empty state: "Tidak ada item handoff. Buka file di HP dan pilih 'Buka di Laptop'."

#### Transfer section

- **Active transfer card:** shows real transfer progress from `CompanionRepository`. Progress bar uses actual byte counts. Shimmer only while transfer is in flight.
- **Send type grid:** File, Foto, Link, Teks — triggers the existing staged-file send flow. These are real actions; tapping them opens the appropriate system picker.
- All four buttons are live and functional (existing Sync capability).

#### Clipboard Bersama card

- **Always labeled "🔬 Konseptual"** with a clearly readable badge.
- Text: "Fitur ini belum tersedia. Direncanakan untuk versi mendatang."
- The card is read-only. No tap action. No misleading "Tersinkron" indicator.
- **Design regression to avoid:** do not show a green "Tersinkron" label next to this card, even as a placeholder. The existing A4 reference showed this; it is explicitly excluded.

#### Disclaimer card (inside TrustedSection)

- Text: "Koneksi jarak jauh (di luar jaringan lokal) belum didukung. Transfer hanya berfungsi saat kedua perangkat berada di jaringan yang sama."
- Style: subdued, 11sp, `text-muted`, info icon. Not an error; it's an honest capability statement.

#### Trusted devices

- Source: existing `CompanionRepository` trusted-device list.
- Show: device name + OS. No Apple devices, no AirPods, no iPad unless the Sync system explicitly supports them.
- No battery percentage unless the companion protocol provides it.
- Trust status: "Terhubung" (connected now) or "Dipercaya" (known, not connected).

#### Offline / disconnected state

- Bridge is dark (see state machine above).
- Handoff queue shows stale data with a "Terakhir dilihat: X lalu" label if data is cached; otherwise the empty state.
- Transfer grid buttons are disabled with a tooltip: "Perangkat tidak terhubung".
- Activity list shows cached history normally.

---

## C — Component Inventory

### C.1 Shared Spaces primitives (reusable across all four pages)

| Proposed component name | Purpose | Re-use scope |
|---|---|---|
| `SpacesPageHeader` | Back button, page title (Fredoka), subtitle, action icons | All 4 pages |
| `SpacesSectionHeader` | Section icon + label + optional "Lihat semua" | All 4 pages |
| `SpacesDivider` | `1.dp` divider, `White.copy(0.04)`, `Spacing.space5` horizontal padding | All 4 pages |
| `SpacesIconButton` | 36dp circle, `graphite-700`, border, icon | All 4 pages |
| `SpacesProgressBar` | Animated fill bar, configurable height 4–6dp, gradient brush | Academics, Projects |
| `SpacesBadge` | Pill badge: urgency tiers, conceptual, done | All 4 pages |
| `SpacesBottomNavSpacer` | 90dp spacer before `BottomNav` | All 4 pages |

### C.2 Page-unique components (must NOT be generalized away)

These are the identity elements of each page. Generalizing them into a uniform "SpacesCard" destroys page personality.

| Component | Page | Why it must stay unique |
|---|---|---|
| `OrbitCanvas` | Academics only | Custom `Canvas` draw; orbital spatial metaphor is the page identity |
| `AcademicsOrbitCore` | Academics only | Active course + animated pulse; not a generic card |
| `AcademicsFocusCard` | Academics only | Unique gradient + tangerine border + next-action row |
| `AcademicsDeadlineChip` | Academics only | Urgency tiers, date box anatomy |
| `ProjectsHeroSurface` | Projects only | Grid texture, ambient glow, milestone track inside |
| `ProjectsMilestoneTrack` | Projects only | 4-state indicators inside the hero |
| `ProjectsStickyNote` | Projects only | Rotated sticky note with per-color fill |
| `ProjectsIdeaBoard` | Projects only | Horizontal LazyRow of sticky notes + add chip |
| `PersonalGardenHeader` | Personal only | Canvas illustration; 220dp; stars + blobs + moon |
| `PersonalJournalSurface` | Personal only | Lined paper texture + prompt + input |
| `PersonalRoutineChip` | Personal only | Done/undone toggle, icon, non-gamified |
| `DevicesBridgeZone` | Devices only | State machine, animated dots via PathMeasure |
| `DevicesDevicePanel` | Devices only | Compact device identity card |
| `DevicesTransferProgress` | Devices only | Real-time shimmer progress bar |
| `DevicesConceptualCard` | Devices only | Always-labeled unimplemented feature card |

### C.3 Shared with existing codebase

These already exist; Spaces pages consume them without modification:

| Existing component | Consumed by |
|---|---|
| `TaskEditorSheet` | Personal (task creation), Projects (task creation) |
| `TaskRow` | Personal task list, Projects task list |
| `CourseEditorSheet` | Academics (add/edit course) |
| `CourseAssignmentsSection` | Academics (deadline data) |
| `ScreenFrame` | Wraps each Spaces page content |

---

## D — Icon Replacement Plan

The V2 HTML mockups use Unicode emoji and text symbols as placeholder icons.
The following table maps each important symbol to its intended vector icon.
Prefer **Material Symbols** (already available in Material 3) or equivalents
from the Lucide family if a Material symbol is missing.

> **Convention for this table:**
> `MaterialSymbols.*` = Material Symbols Outlined weight 400
> `Icons.Outlined.*` = Material Icons (fallback if not in Symbols)

### D.1 Navigation icons

| Emoji / symbol used | Intended icon | Material Symbols name |
|---|---|---|
| 🏠 Today | Home | `MaterialSymbols.Home` |
| 📅 Plan | Calendar | `MaterialSymbols.CalendarMonth` |
| ⬡ Spaces | Grid / Hub | `MaterialSymbols.Dashboard` or custom hexagon |
| + Capture | Add Circle | `MaterialSymbols.AddCircle` |
| 🗄 Vault | Archive | `MaterialSymbols.Shelves` |

### D.2 Academics icons

| Emoji / symbol | Intended icon | Note |
|---|---|---|
| 🎓 Page icon | `MaterialSymbols.School` | Page header |
| ⚛️ Fisika orbit core | `MaterialSymbols.Science` | Per-course icon, physics |
| √x Matematika satellite | `MaterialSymbols.Calculate` | Per-course icon, math |
| 💻 Pemrograman satellite | `MaterialSymbols.Laptop` | Per-course icon, CS |
| 🇬🇧 Bahasa satellite | `MaterialSymbols.Translate` | Per-course icon, language |
| 🎯 Fokus section | `MaterialSymbols.TrackChanges` | Section header icon |
| 📅 Deadline section | `MaterialSymbols.EventBusy` | Section header |
| 📂 Materi section | `MaterialSymbols.FolderOpen` | Section header |
| ⚡ Aksi Cepat section | `MaterialSymbols.Bolt` | Section header |
| PDF thumb | `MaterialSymbols.PictureAsPdf` | Material type badge |
| DOC thumb | `MaterialSymbols.Description` | Material type badge |
| PPT thumb | `MaterialSymbols.Slideshow` | Material type badge |
| ⤓ Download | `MaterialSymbols.FileDownload` | Row action |
| 🔍 Search | `MaterialSymbols.Search` | Header action |
| 🔔 Notifications | `MaterialSymbols.NotificationsNone` | Header action |
| ‹ Back | `MaterialSymbols.ArrowBackIosNew` | Navigation |
| › Chevron | `MaterialSymbols.ChevronRight` | Row indicator |

### D.3 Projects icons

| Emoji / symbol | Intended icon | Note |
|---|---|---|
| 📦 Semua Proyek | `MaterialSymbols.Inventory2` | Section header |
| ⏱ Sprint / Minggu | `MaterialSymbols.Timer` | Sprint section |
| 💡 Ide | `MaterialSymbols.Lightbulb` | Idea board section |
| ☀️ Riko Hub project | `MaterialSymbols.WbSunny` | Project identity |
| 🚩 FlagForge project | `MaterialSymbols.Flag` | Project identity |
| 🎮 Game ITK project | `MaterialSymbols.SportsEsports` | Project identity |
| 📷 Photo portfolio | `MaterialSymbols.PhotoCamera` | Project identity |
| ✓ Milestone done | `MaterialSymbols.CheckCircle` (filled) | Milestone state |
| ○ Milestone pending | `MaterialSymbols.RadioButtonUnchecked` | Milestone state |
| + Sticky add | `MaterialSymbols.Add` | Add button |

### D.4 Personal icons

| Emoji / symbol | Intended icon | Note |
|---|---|---|
| ✨ Greeting | `MaterialSymbols.AutoAwesome` | Greeting strip |
| 🌱 Tasks section | `MaterialSymbols.Spa` | Section header |
| 🪶 Journal section | `MaterialSymbols.Edit` | Section header |
| 🔔 Reminders section | `MaterialSymbols.NotificationsActive` | Section header |
| 🌿 Routine section | `MaterialSymbols.SelfImprovement` | Section header |
| 📒 Notes section | `MaterialSymbols.BookmarkBorder` | Section header |
| ✓ Task done | `MaterialSymbols.CheckCircle` (filled, jade) | Task check |
| → Journal send | `MaterialSymbols.Send` | Send button |
| ✓ Reminder done | `MaterialSymbols.CheckCircle` (filled) | Reminder status |
| 🎵 Music note card | `MaterialSymbols.MusicNote` | Note type icon |
| 🌄 Goals note card | `MaterialSymbols.Landscape` | Note type icon |
| 📝 Free note card | `MaterialSymbols.StickyNote2` | Note type icon |
| 💊 Vitamin reminder | `MaterialSymbols.Medication` | Reminder icon |
| 📞 Call reminder | `MaterialSymbols.Call` | Reminder icon |
| 🧹 Clean reminder | `MaterialSymbols.CleaningServices` | Reminder icon |
| 💧 Water routine | `MaterialSymbols.WaterDrop` | Routine chip |
| 🏃 Exercise routine | `MaterialSymbols.DirectionsRun` | Routine chip |
| 📖 Read routine | `MaterialSymbols.MenuBook` | Routine chip |
| 🧘 Meditate routine | `MaterialSymbols.SelfImprovement` | Routine chip |
| ••• Menu | `MaterialSymbols.MoreVert` | Header action |

### D.5 Devices icons

| Emoji / symbol | Intended icon | Note |
|---|---|---|
| 📱 Phone | `MaterialSymbols.PhoneAndroid` | Device panel |
| 💻 Laptop | `MaterialSymbols.LaptopMac` (or `LaptopChromebook`) | Device panel |
| 🔀 Handoff | `MaterialSymbols.SwapHoriz` | Section header |
| 📄 File handoff | `MaterialSymbols.InsertDriveFile` | Handoff item |
| 🔗 Link handoff | `MaterialSymbols.Link` | Handoff item |
| ⚡ Kirim Cepat | `MaterialSymbols.Bolt` | Section header |
| 📤 Send | `MaterialSymbols.FileUpload` | Transfer card |
| 📄 File type | `MaterialSymbols.InsertDriveFile` | Transfer type |
| 🖼 Photo | `MaterialSymbols.Image` | Transfer type |
| 🔗 Link | `MaterialSymbols.Link` | Transfer type |
| 📝 Text | `MaterialSymbols.TextSnippet` | Transfer type |
| 📋 Clipboard | `MaterialSymbols.ContentPaste` | Conceptual card |
| 🔬 Conceptual | `MaterialSymbols.Science` | Conceptual badge |
| 🛡 Trusted | `MaterialSymbols.VerifiedUser` | Trusted section |
| 🕐 Activity | `MaterialSymbols.History` | Activity section |
| ✓ Done | `MaterialSymbols.CheckCircle` | Activity status |
| ℹ️ Info | `MaterialSymbols.Info` | Disclaimer card |

---

## E — Motion Contract

All durations reference the existing `Motion` object in
[`ui/theme/Motion.kt`](../../app/src/main/java/id/riko/hub/ui/theme/Motion.kt):
`fast=120ms`, `short=180ms`, `medium=380ms`, `long=600ms`, easing `standard=CubicBezierEasing(0.2,0,0,1)`.
`rememberReducedMotion()` already exists; every animation below must read it.

---

### MA — Academics

#### MA-1: Orbit satellite selection (tap to focus)

| Field | Value |
|---|---|
| Trigger | User taps a satellite node |
| Visual behavior | Tapped satellite translates to orbit core position; existing core slides to tapped satellite's position; Focus card content crossfades to the newly focused course |
| Duration | 200ms (position), 150ms (content crossfade) |
| Easing | `Motion.standard` |
| Start state | Satellite at peripheral position; Focus card shows old course |
| End state | Satellite at core; Focus card shows new course; orbit legend updates |
| Reduced motion | No position animation. Satellite is highlighted with a tangerine border; Focus card updates instantly via fade (`Motion.fast`). |
| Performance | Use `animateOffsetAsState`; avoid full canvas redraw on each frame. Cache orbit positions. |

#### MA-2: Focus card pulse dot

| Field | Value |
|---|---|
| Trigger | Focus card is visible and there is an active/current session |
| Visual behavior | 6dp circle alpha oscillates 1.0 → 0.5 → 1.0 |
| Duration | 2000ms cycle, infinite |
| Easing | `FastOutSlowIn` |
| Reduced motion | Dot is static (alpha=1.0), no cycle |
| Performance | Single `infiniteTransition`, alpha only — no layout change |

#### MA-3: Progress bar fill entrance

| Field | Value |
|---|---|
| Trigger | Focus card first enters composition |
| Visual behavior | Bar fills from 0% to actual progress value |
| Duration | `Motion.medium` (380ms) |
| Easing | `Motion.standard` |
| Reduced motion | Bar starts at full value, no animation |

---

### MP — Projects

#### MP-1: Hero unfold (page entry)

| Field | Value |
|---|---|
| Trigger | Projects page enters composition (navigation) |
| Visual behavior | Hero card slides up from below and fades in; milestone track items stagger in at 30ms intervals |
| Duration | `Motion.medium` for hero, 30ms stagger × 4 items |
| Easing | `Motion.standard` |
| Reduced motion | No slide; hero fades in at `Motion.fast` |

#### MP-2: Sticky note reveal

| Field | Value |
|---|---|
| Trigger | Idea board enters viewport on first scroll |
| Visual behavior | Each sticky note pops in with `scaleX/Y: 0.8 → 1.0` and `alpha: 0 → 1.0`, staggered 40ms |
| Duration | `Motion.short` per item |
| Easing | Spring — `dampingRatio=0.75f`, `stiffness=Medium` |
| Reduced motion | Stickies appear at full scale instantly |
| Performance | Trigger only once; use `remember(seen) { true }` gate. |

---

### MPe — Personal

#### MPe-1: Star field twinkle

| Field | Value |
|---|---|
| Trigger | Personal page is visible (foreground) |
| Visual behavior | 6 stars each oscillate alpha 0.3 → 1.0 on independent staggered cycles (3000–5000ms) |
| Duration | Per-star: 3000–5000ms, infinite |
| Easing | `CubicBezierEasing(0.4, 0, 0.6, 1)` (smooth sine-like) |
| Reduced motion | Stars are static at alpha=0.5; no animation |
| Performance | `infiniteTransition` shared for all stars; one transition per page lifecycle |

#### MPe-2: Task check animation

| Field | Value |
|---|---|
| Trigger | User taps an unchecked task circle |
| Visual behavior | Circle fills with jade (`scale 1.0 → 1.15 → 1.0`), checkmark draws in via path trim |
| Duration | `Motion.short` (180ms) |
| Easing | Spring for scale; linear for path trim |
| Haptic | `HapticFeedbackType.LongPress` (single tick) |
| Reduced motion | Circle turns jade and checkmark appears instantly |

#### MPe-3: Journal expand

| Field | Value |
|---|---|
| Trigger | User taps journal surface |
| Visual behavior | Journal card expands height via `animateContentSize`, input field becomes focused, keyboard slides up |
| Duration | `Motion.short` |
| Easing | `Motion.standard` |
| Reduced motion | Expand immediately, keyboard still slides (system behavior) |

---

### MD — Devices

#### MD-1: Bridge connection state transition

| Field | Value |
|---|---|
| Trigger | `SyncUiState.companion` connection state changes |
| Visual behavior | Bridge line color and opacity cross-fades; device panel borders cross-fade; status text cross-fades |
| Duration | `Motion.medium` (380ms) |
| Easing | `Motion.standard` |
| Start state | Previous connection state |
| End state | New connection state colors and text |
| Reduced motion | Instant state change; no fade |

#### MD-2: Transfer dot flow (Connected state)

| Field | Value |
|---|---|
| Trigger | Connection state == Connected |
| Visual behavior | 3 dots flow along bridge path via `PathMeasure.getPosTan()`, offset animated 0→1 in 1800ms cycles, staggered 600ms |
| Duration | 1800ms per dot, infinite |
| Easing | `LinearEasing` |
| Pause condition | Paused when page is not visible (`LocalLifecycleOwner`) |
| Reduced motion | No dots; static bridge line only |
| Performance | Single `Canvas.drawCircle()` per dot; no Composable overhead per dot |

#### MD-3: Transfer progress shimmer

| Field | Value |
|---|---|
| Trigger | A file transfer is in flight |
| Visual behavior | Translucent highlight sweeps right-to-left across the progress fill |
| Duration | 1200ms per sweep, infinite while transfer active |
| Easing | `LinearEasing` |
| Reduced motion | No shimmer; plain progress bar fill only |

---

## F — Interaction Contract

### F.1 Academics

| Interaction | Target | Behavior | Source |
|---|---|---|---|
| Tap orbit satellite | `OrbitCanvas` | Focus that course; update Focus card (MA-1) | **Proposed — requires implementation** |
| Tap orbit core | `OrbitCanvas` | Navigate to full course workspace (`CoursesScreen`) | **Integrates existing `Destination.Courses`** |
| Tap Focus card | `AcademicsFocusCard` | Navigate to `CoursesScreen` filtered to active course | **Integrates existing** |
| Tap next-action row | Focus card next-action | Open `TaskEditorSheet` for the linked assignment | **Integrates existing `AssignmentEditorDialog`** |
| Scroll deadline chips | `DeadlineChipRow` | Horizontal scroll, no snap | **Standard `LazyRow`** |
| Tap deadline chip | Chip | Open assignment detail (expand `AssignmentEditorDialog`) | **Integrates existing** |
| Tap material row | Material row | Open file (Vault if stored there, or system viewer) | **Integrates existing Vault flow** |
| Tap ⤓ download | Material action | Download material to device | **Integrates existing Drive download** |
| Tap "Catat Cepat" | Quick action | Open `TaskEditorSheet` with ACADEMIC category | **Integrates existing** |
| Tap "Scan Dokumen" | Quick action | Open system document scanner (Android) | **Proposed — new capability** |
| Tap "Jadwal Kuliah" | Quick action | Navigate to `TimelineScreen` filtered to CLASS events | **Integrates existing** |
| Tap "Simpan Link" | Quick action | Open shared task editor with link field | **Integrates existing** |
| "Lihat semua" deadlines | Section header | Navigate to full assignment list | **Integrates existing `CoursesScreen`** |
| "Lihat semua" materials | Section header | Navigate to Vault filtered to ACADEMIC | **Integrates existing Vault** |

### F.2 Projects

| Interaction | Target | Behavior | Source |
|---|---|---|---|
| Tap hero card | `ProjectsHeroSurface` | Navigate to project task list (filtered PROJECTS view) | **Integrates existing `TasksScreen`** |
| Tap hero chevron | Hero expand button | Navigate same as above | **Integrates existing** |
| Tap project row | Project card | Navigate to project task list | **Integrates existing** |
| Tap milestone item | Milestone row | Toggle milestone task status (done/pending) | **Integrates existing task mutation** |
| Tap filter chip | Filter row | Filter project list by status | **Proposed presentation filter** |
| Tap sprint day | Sprint strip | Jump to `TimelineScreen` for that date | **Integrates existing** |
| Tap sticky note | Idea board | Open note for editing | **Proposed — new Note entity or PROJECTS task** |
| Tap "+" sticky | Add chip | Create a new quick note for this project | **Proposed** |
| Tap "+" header | Header add button | Create a new project (open editor) | **Proposed — new project creation sheet** |
| Long-press project card | Project card | Show project options (archive, delete, etc.) | **Proposed** |
| "Lihat semua" sprint | Sprint header | Navigate to `TimelineScreen` filtered to current week | **Integrates existing** |

### F.3 Personal

| Interaction | Target | Behavior | Source |
|---|---|---|---|
| Tap task circle | `PersonalTaskRow` | Toggle task done/pending (MPe-2 animation) | **Integrates existing task mutation** |
| Tap task row | `PersonalTaskRow` | Open `TaskEditorSheet` for editing | **Integrates existing** |
| Tap journal surface | `PersonalJournalSurface` | Expand and focus text input (MPe-3) | **Proposed — new journal entry** |
| Submit journal | Send button | Save journal entry, collapse surface | **Proposed** |
| Tap reminder row | Reminder | Open `TaskEditorSheet` for that reminder | **Integrates existing `TaskReminders`** |
| Tap "Tambah ›" | Reminder add | Open `TaskEditorSheet` with reminder type | **Integrates existing** |
| Tap routine chip | Routine | Toggle done for today (no streak display) | **Integrates existing task completion** |
| Tap "Atur ›" | Routine section | Open a routine management sheet | **Proposed** |
| Tap note card | Note | Open full note for editing | **Proposed** |
| Tap "Lihat semua" notes | Notes section | Navigate to full notes list | **Proposed** |
| Tap greeting strip | Greeting | Dismiss or navigate to a motivational quote list | **Decorative — no navigation required** |

### F.4 Devices

| Interaction | Target | Behavior | Source |
|---|---|---|---|
| Tap "Hubungkan Perangkat" | Header button | Trigger companion pairing flow | **Integrates existing `SyncViewModel` pair flow** |
| Tap bridge center | Bridge | No action (visual only) | **Decorative** |
| Tap device panel | Phone / Laptop | Show device detail sheet (name, last seen, disconnect option) | **Integrates existing `CompanionRepository`** |
| Tap handoff item | `HandoffSection` row | Open the file on the other device via `HandoffRepository` | **Integrates existing handoff open** |
| "Lihat semua" handoff | Section header | Navigate to full handoff history | **Integrates existing** |
| Tap File/Foto/Link/Teks | Transfer grid | Open system file picker / share sheet for the respective type | **Integrates existing staged-file send** |
| Tap Clipboard card | Conceptual card | No action; tapping shows a toast: "Fitur ini belum tersedia" | **Disabled — conceptual only** |
| Tap trusted device | Trusted list | Show trust options (forget device) | **Integrates existing `CompanionRepository` forget** |
| Tap "Kelola" | Trusted section | Navigate to trusted device management | **Integrates existing** |
| Tap activity row | Activity item | Show transfer detail (file path, timestamp, size) | **Integrates existing history** |
| "Lihat semua" activity | Activity header | Navigate to full transfer history | **Integrates existing** |

---

## G — Design Fidelity Rules

These are the lines that must not be crossed during Compose implementation.
Each rule has an **ACCEPTABLE ADAPTATION** (native equivalent that preserves intent)
and a **DESIGN REGRESSION** (what to avoid).

### G.1 All four pages

| Element | ACCEPTABLE ADAPTATION | DESIGN REGRESSION |
|---|---|---|
| Graphite/Tangerine palette | Exact V2 token values in Kotlin constants | Reverting to the old ivory/pastel `LightScheme` colors as the primary dark background |
| Page-specific compositions | Each page has its own layout structure | Reducing all four pages to an identical `LazyColumn` with section headers |
| Organic shapes (blobs, orbit rings, bridge) | `Canvas` / `Path` in Compose | `Box` with rounded corners replacing every organic shape |
| Tangerine accent elements | `Brush.linearGradient` for fills; `1.5.dp` borders | `Color.fill` flat orange without gradient; border width under 1dp |
| Section header pattern | `SpacesSectionHeader` composable with icon + label + "Lihat semua" | Removing section labels and merging all rows into a flat undifferentiated list |
| Typography hierarchy | `displayMedium` page title, `titleSmall` section, `bodyMedium` rows | Using `bodyMedium` for the page title |
| Card border on hover/active | `animateColorAsState` from subtle → visible border | No press feedback; flat opaque card |
| Empty states | Informative text + subdued icon | Blank space or crash |

### G.2 Academics — specific rules

| Element | ACCEPTABLE | REGRESSION |
|---|---|---|
| Orbit canvas | `Canvas` draw of ellipses + positioned composables for satellites | A horizontal `LazyRow` of equal-size course chips pretending to be an orbit |
| Animated pulse dot on Focus card | `infiniteTransition` alpha on a 6dp circle | No pulse; static colored dot |
| Deadline urgency tiers | Three visually distinct chip styles (magenta/orange/jade) | All deadline chips are the same color regardless of urgency |
| Orbit satellite expansion beyond 4 courses | "+N" overflow chip in the 4th satellite slot | Satellites overflow the canvas boundary or overlap the core |

### G.3 Projects — specific rules

| Element | ACCEPTABLE | REGRESSION |
|---|---|---|
| Workshop texture | `drawWithCache` grid lines (0.5dp, alpha 0.015) | No texture; solid graphite card only |
| Milestone track inside hero | Vertical column of state indicators inside the hero card | Milestone items shown as a separate section below the hero |
| Sticky notes with rotation | `graphicsLayer { rotationZ }` ±1.5° per note | Flat rectangular note cards with no rotation |
| Left-edge project color accent | `3.dp` `drawBehind` vertical bar | No color differentiation between projects |

### G.4 Personal — specific rules

| Element | ACCEPTABLE | REGRESSION |
|---|---|---|
| Garden header (Canvas illustration) | Full `Canvas` implementation with blobs, stars, moon, plant icons | A solid graphite rectangle with the page title on it |
| Lined paper on journal | `drawWithCache` horizontal lines | Plain white/gray text field |
| Task check circle (not square) | `RoundedCornerShape(50%)` for the check control | Material `Checkbox` square component |
| No health/mood/financial panels | These sections are absent by design | Adding any sleep ring, mood score, finance widget as a "placeholder" |
| Star field reduced-motion fallback | Static dim dots at alpha=0.5 | No stars at all in reduced motion; the header becomes a plain blob |

### G.5 Devices — specific rules

| Element | ACCEPTABLE | REGRESSION |
|---|---|---|
| Bridge state machine (4 states) | `animateColorAsState` cross-fades for each state | A single always-green bridge regardless of connection state |
| Clipboard card "Konseptual" label | Always-visible, read-only badge, no action | Removing the label; showing it as a working feature; or hiding the card |
| Transfer progress from real data | `StateFlow` from `CompanionRepository` | A fake animated progress bar that fills to 100% on a timer |
| Disclaimer card (LAN-only) | Subdued informational text card, always visible | Hiding the disclaimer; implying remote connectivity |
| Disconnected bridge | Dark dashed line, no dot animation | Keeping the green animated bridge when the companion is offline |

---

## Acceptance Checklists

### ✅ Academics v2 Acceptance

- [ ] Orbit canvas renders at full screen width with correct ellipse proportions
- [ ] Active course occupies the orbit core; up to 4 other courses are satellites
- [ ] Tapping a satellite updates Focus card content and visual core
- [ ] Overflow: 5+ courses show "+N" instead of crashing or overflowing
- [ ] Focus card: tangerine gradient, pulsing dot, progress bar, next-action row
- [ ] Deadline chips: three urgency tier styles, horizontal scroll, no wrapping
- [ ] Materials list: PDF/DOC/PPT icons (vector), file name, metadata, download action
- [ ] Quick actions grid: 2×2 layout, correct icons, correct navigation targets
- [ ] Empty states for each section are implemented and readable
- [ ] Loading shimmer skeletons are present
- [ ] Reduced motion: pulse dot stops; orbit satellite change is instant fade
- [ ] All text is minimum 11sp; none is image-embedded
- [ ] All interactive elements have minimum 48dp touch target
- [ ] Section headers have correct TalkBack content descriptions
- [ ] All data is sourced from `CourseRepository` / `AssignmentRepository`; no hardcoded content

### ✅ Projects v2 Acceptance

- [ ] Workshop hero surface has visible grid texture (not a plain card)
- [ ] Hero ambient glow is visible in the top-right corner
- [ ] Milestone track (4 items) is inside the hero card, not below it
- [ ] Progress bar fill animates on first render
- [ ] Project list: each row has a colored left-edge accent
- [ ] Project filter chips: active filter is visually distinct (tangerine)
- [ ] Sprint week strip: today's day is highlighted tangerine; dots reflect project activity
- [ ] Sticky notes: 2–3 notes visible with slight rotation; add chip is the last item
- [ ] "+N" overflow chip appears in sticky board when more than 5 ideas exist
- [ ] Empty states: hero shows "no active project" prompt; list shows first-project CTA
- [ ] Reduced motion: sticky notes are flat (no rotation); hero entrance is an instant fade
- [ ] All project data is real (PROJECTS task category); no Git feeds, no fake sprint data
- [ ] Touch targets are all ≥ 48dp
- [ ] TalkBack announces project name, progress, and milestone status

### ✅ Personal v2 Acceptance

- [ ] Garden header is implemented in Canvas (no raster PNG)
- [ ] Stars are present and twinkle in normal motion mode; static in reduced motion
- [ ] Moon is a soft glow circle (not an emoji)
- [ ] Plant silhouettes are visible as dark icon shapes
- [ ] Status bar text is legible over the illustration
- [ ] Task circles are round (not square Material checkboxes)
- [ ] Tapping a task circle shows the jade fill animation
- [ ] Journal surface has visible horizontal lines (lined paper)
- [ ] Journal prompt is italic and readable
- [ ] Reminders source from existing `TaskReminders`; no mock data
- [ ] Routine chips are toggle-only; no streak count, no percentage ring, no score
- [ ] No sleep, mood, or financial sections exist anywhere on this page
- [ ] Notes grid: 2-column layout with one potential full-width card
- [ ] Reduced motion: stars are static; task check is instant; journal expand is instant
- [ ] Empty personal task list shows "Belum ada niat hari ini. Tambah +" prompt
- [ ] All text ≥ 11sp; TalkBack describes task names and reminder times

### ✅ Devices v2 Acceptance

- [ ] Bridge zone reflects actual `SyncUiState.companion` connection state
- [ ] Connected: green animated dots flow along bridge; green borders on both panels
- [ ] Disconnected: bridge is dark dashed; no dot animation; device panels subdued
- [ ] Connecting: tangerine dashed animated bridge; spinner-style visual
- [ ] Error: magenta bridge; explicit "Gagal terhubung" text + retry action
- [ ] Phone panel never shows a device name other than "Perangkat ini · Android"
- [ ] Laptop panel shows companion device name from `CompanionSnapshot`, not hardcoded
- [ ] Battery percentage shown only if `CompanionSnapshot` provides it; hidden otherwise
- [ ] Handoff queue shows real items from `HandoffSnapshot`; empty state is present
- [ ] "Buka di Laptop →" action calls the existing handoff open flow
- [ ] Transfer grid buttons open system pickers; they are not decorative
- [ ] Active transfer progress sourced from `CompanionRepository`; no fake timer
- [ ] Clipboard card: "Konseptual" badge always visible; no tap action; no "Tersinkron" label
- [ ] Disclaimer card (LAN-only) is always present in TrustedSection
- [ ] Trusted device list shows only actual trusted devices from Sync; no Apple devices unless Sync explicitly adds them
- [ ] Reduced motion: bridge transitions are instant; no dot animation; shimmer removed
- [ ] All states (offline, error, connecting) tested and readable without color alone
- [ ] Touch targets ≥ 48dp; TalkBack announces connection state and handoff item names

---

## Codex Implementation Order

> Phases below map to the existing `07_IMPLEMENTATION_PHASES.md` phasing.
> This order is safe — each step depends only on what came before.
> Do not skip phases; do not begin Phase 6 sub-pages before Phase 5 navigation is stable.

### Phase 0 — Prerequisite: Tangerine token (1–2 hours)

**File:** `ui/theme/SpacesTokens.kt` (new) or amend `RikoColors.kt`
**Task:** Add `tangerine = Color(0xFFE8742A)`, `tangerineDim = Color(0xFFC25F18)`,
`connectedGreen = Color(0xFF3ED87A)` as named, scoped constants.
Also add `SpacesTokens.conceptualBadgeAlpha = 0.07f` and
`SpacesTokens.workshopGridAlpha = 0.015f`.
**Do not modify** `DarkScheme` or `LightScheme`; these are additive space-scoped tokens.

**Gate:** Project Owner confirms `#E8742A` as the canonical Tangerine before Phase 6 begins.

---

### Phase 5a — Spaces navigation entry (parallel with Phase 5 in phasing doc)

**Task:** Add `Destination.Spaces` (and sub-destinations `Academics`, `Projects`,
`Personal`, `Devices`) to `Destinations.kt`. Wire them in `RikoNavHost.kt`.
Keep all existing routes (`Courses`, `Sync`, `Tasks`, etc.) reachable.

**Order within this phase:**
1. Add `Destination.Spaces` enum value; confirm spatial position with owner (D03 decision).
2. Add a `SpacesScreen.kt` (the four-portal entry page — out of scope for this handoff).
3. Add sub-destination routes: `Destination.AcademicsSpace`, `Destination.ProjectsSpace`, `Destination.PersonalSpace`, `Destination.DevicesSpace`.

---

### Phase 6a — Academics Space

**Files to create:**
- `ui/spaces/academics/AcademicsScreen.kt`
- `ui/spaces/academics/AcademicsViewModel.kt` (thin; delegates to `CoursesViewModel` + `AssignmentViewModel`)
- `ui/spaces/academics/OrbitCanvas.kt`
- `ui/spaces/academics/AcademicsFocusCard.kt`
- `ui/spaces/academics/AcademicsDeadlineChip.kt`

**Key implementation notes:**
1. `OrbitCanvas` uses `Canvas(Modifier.fillMaxWidth().height(200.dp))`. Pre-compute satellite positions in `remember { }`. Ellipse paths cached with `drawWithCache`.
2. Orbit selection state held in `AcademicsViewModel`. Satellite tap dispatches `selectCourse(courseId)`.
3. `AcademicsFocusCard` reads from `CourseRepository.getActiveCourse()` — define "active" as the next scheduled class for today.
4. Deadline data from `AssignmentRepository.getUpcomingAssignments(limit=5)`.
5. "Scan Dokumen" quick action: disabled with a coming-soon snackbar (same pattern as existing `comingSoon` in `RikoNavHost`).

**Acceptance gate:** Pass the Academics checklist above before moving to Phase 6b.

---

### Phase 6b — Devices Space

**Files to create:**
- `ui/spaces/devices/DevicesScreen.kt`
- `ui/spaces/devices/DevicesBridgeZone.kt`
- `ui/spaces/devices/DevicesConceptualCard.kt`

**Key implementation notes:**
1. `DevicesScreen` is a thin wrapper over `SyncViewModel` — consume `SyncUiState` directly.
2. `DevicesBridgeZone` receives `ConnectionState` (Connected/Disconnected/Connecting/Error) as a parameter derived from `SyncUiState.companion`. It owns only the bridge drawing.
3. Dot animation: use `LaunchedEffect(connectionState)` to start/stop the `infiniteTransition`.
4. `DevicesConceptualCard` is a `val` — no state, no ViewModel. Always shows the badge.
5. Handoff queue: `LazyColumn` limited to 3 items, bounded by `HandoffSnapshot.items.take(3)`.

**Acceptance gate:** Pass the Devices checklist before Phase 6c.

---

### Phase 6c — Projects Space

**Files to create:**
- `ui/spaces/projects/ProjectsScreen.kt`
- `ui/spaces/projects/ProjectsViewModel.kt` (wraps `TasksViewModel`, filters PROJECTS category)
- `ui/spaces/projects/ProjectsHeroSurface.kt`
- `ui/spaces/projects/ProjectsMilestoneTrack.kt`
- `ui/spaces/projects/ProjectsIdeaBoard.kt`

**Key implementation notes:**
1. "Project" is the PROJECTS task category. There is no separate Project entity in the current schema. `ProjectsViewModel` groups tasks by a `projectGroup` field (or by label/tag if available) to form project identity.
2. Active project = the PROJECTS-category task group with the most recent activity or highest priority. If the schema does not support grouping yet, show individual tasks in the hero rather than milestones — **do not invent schema changes**.
3. Sticky notes require a new lightweight `Memo` entity or a special `Task` sub-type — **propose this to the owner before Phase 6c begins**.
4. Workshop texture: `drawWithCache { ... }` — the draw block should run only when `size` changes.

**Acceptance gate:** Pass the Projects checklist. Confirm sticky note entity decision with owner first.

---

### Phase 6d — Personal Space

**Files to create:**
- `ui/spaces/personal/PersonalScreen.kt`
- `ui/spaces/personal/PersonalViewModel.kt` (wraps `TasksViewModel`, filters PERSONAL)
- `ui/spaces/personal/PersonalGardenHeader.kt`
- `ui/spaces/personal/PersonalJournalSurface.kt`
- `ui/spaces/personal/PersonalRoutineChip.kt`

**Key implementation notes:**
1. `PersonalGardenHeader` is a `Box` with a `Canvas` fill. Star positions: `remember(LocalDate.now()) { generateStarPositions(seed=date.toEpochDay()) }` — deterministic per-day positions so stars feel consistent but not identical each day.
2. Journal entry: requires a decision on the entity type before Phase 6d begins. **Propose to owner: a `Journal` table with `(date, body, prompt)` or a special PERSONAL task type**.
3. Routine chips: use PERSONAL tasks with `recurrence = DAILY`. `done` = task completed today. No streak, no score.
4. Notes: use existing tasks with a `body` field for note content, or a dedicated `Note` entity. **Do not invent a notes schema without owner approval.**
5. Health/mood/finance panels: if any code path attempts to add them, Codex must stop and flag to the owner.

**Acceptance gate:** Pass the Personal checklist. Journal and notes entity decisions resolved before start.

---

### Cross-cutting tasks (alongside Phases 6a–6d)

- `SpacesPageHeader`: extract the shared header composable after Phase 6a.
- `SpacesSectionHeader`, `SpacesDivider`, `SpacesBadge`: extract after first page that uses them.
- Motion: implement `rememberReducedMotion()` check in every animated composable (the helper already exists in `Motion.kt`).
- Accessibility: add `Modifier.semantics { contentDescription = ... }` for every Canvas-drawn element (orbit core, bridge zone, satellite nodes, garden header) — these are not automatically accessible.
- String resources: all visible strings in the four pages must go into `res/values/strings.xml`; no hardcoded Indonesian strings in source.

---

*End of Spaces V2 Implementation Handoff*

*Document prepared by Creative Director. Implementation by Codex. Final runtime verification by Riko.*
*Revision required for: Tangerine hex confirmation, navigation route order, sticky note / journal entity decisions.*
