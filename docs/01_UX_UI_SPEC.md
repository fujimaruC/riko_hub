# 01 - UX / UI Specification

Exact values (hex, dp, ms) live in `07_DESIGN_TOKENS.md`. The urgency and bubble algorithm lives in `08_HOME_ENGINE_SPEC.md`. This file defines intent, structure and behavior.

## 1. Visual identity

**Keywords:** personal, creative, warm, modern, slightly playful, student-made, indie, clean, friendly, distinctive.

**It should feel like** a well-loved paper planner that came alive: warm paper background, colorful ink, soft hand-cut shapes.

**It must not feel like** private banking, a cyberpunk terminal, a corporate dashboard, or a children's game.

### Screen family rule (most important design decision)

| Screen | Character | Layout logic |
|--------|-----------|--------------|
| Home | expressive, organic, alive | Bubbles, asymmetry, overlap |
| Tasks | organized, scannable, calm | Grouped list with varied row shapes |
| Timeline | narrative, chronological | Vertical path with stops |
| Vault | file-oriented, practical | Folder shapes plus file list |
| Sync | device-oriented, technical but friendly | Triangular device map |

All five share: the color system, type scale, corner-radius vocabulary, motion timing and the compass chip. They differ in layout and density.

## 2. Color philosophy

- Background is **warm**, never pure white in light mode and never pure black in dark mode.
- Accents are used as **meaning**, not decoration. Each domain owns one accent. Coral is reserved for urgency and overdue.
- Text is near-black warm ink on light, warm off-white on dark.
- Saturated accents carry **dark ink text** (never white text on pastel).
- Same hues in dark mode, tuned for contrast.

Domain accents (names only, values in `07`):

| Domain | Accent |
|--------|--------|
| Akademik | Lavender |
| Organisasi | Sky blue |
| Personal | Mint |
| Project | Warm yellow |
| Urgent / overdue (any domain) | Coral |

## 3. Typography philosophy

- UI font: a **clean rounded sans-serif** (Nunito). Highly readable at 14-16sp.
- Display font: used **rarely** (Home greeting, empty-state headlines, app wordmark). Fredoka recommended.
- No serif luxury type. No monospace hacker type, except Sync device IDs MAY use a monospace.
- Body text MUST be at least 14sp. Metadata at least 12sp.
- Respect system font scaling up to 200%. Layouts must reflow, not clip.

## 4. Shape system

Avoid the square-card-grid look. Use a **vocabulary** of shapes and vary them deliberately.

| Shape | Use |
|-------|-----|
| Pill (full radius) | chips, filters, quick add, compass chip, task rows on Tasks |
| Blob (4 organic variants) | Home bubbles only |
| Tall rounded panel (radius 28dp) | Vault folders, Tasks category summary |
| Wide rounded panel (radius 24dp) | Timeline stop cards, Sync status |
| Asymmetric corner panel (e.g. 32/12/32/12) | hero areas, Home progress strip, empty states |
| Circle | device nodes, avatar, progress ring |

Rules:
- MUST NOT show more than 2 adjacent elements of identical shape and size.
- MAY overlap elements by up to 12dp for rhythm, only decorative elements, never interactive-over-interactive.
- Corner radii come only from the token set.

## 5. Spacing philosophy

- Base unit 4dp. Common steps: 4, 8, 12, 16, 24, 32, 48.
- Screen horizontal padding 20dp.
- Home has the most breathing room. Tasks is the densest but never cramped (minimum row height 56dp, touch targets at least 48dp).
- Alignment is strict even when shapes are irregular: text baselines align to a 20dp left gutter; bubbles are the exception.

## 6. Spatial navigation

```
                 Timeline
                    ↑
                    |
   Sync  ←-------  HOME  -------→  Vault
                    |
                    ↓
                  Tasks
```

### Gesture mapping (on Home)

| Gesture | Destination | Mental model |
|---------|-------------|--------------|
| Swipe up | Tasks (below) | Pull the lower world up |
| Swipe down | Timeline (above) | Pull the upper world down |
| Swipe left | Vault (right) | Pull the right world left |
| Swipe right | Sync (left) | Pull the left world right |

### Returning to Home (MUST support all)

1. System back from any non-Home screen goes to Home.
2. Compass chip tap.
3. Gesture in the opposite direction **when it will not conflict** (see below).

### Conflict rules

| Conflict | Resolution |
|----------|------------|
| Android system back gesture on left/right edges | Spatial horizontal swipes MUST ignore drags that start within 24dp of either screen edge |
| Vertical list scrolling on Tasks vs "swipe down to Home" | Return gesture on Tasks triggers only as **overscroll at the top of the list** or via the grab handle at the top of the screen; never mid-scroll |
| Timeline scroll vs "swipe up to Home" | Same, but at the **bottom** overscroll or grab handle |
| Horizontal carousels inside a screen | Carousel wins inside its bounds |

### Compass chip (MUST)

- A small pill in the bottom-right corner, 48dp touch target, always visible on all five screens.
- Shows a tiny 5-dot compass with the current location highlighted.
- Tap expands to a radial/fan of the five destinations with labels (Hari Ini, Tugas, Timeline, Vault, Sync).
- This is the accessibility and discoverability fallback. It MUST be reachable by TalkBack.

### Settings option (MUST)

`Gaya navigasi`: `Spasial + kompas` (default) or `Kompas saja` (disables swipes).

### Transitions

The screen slides in the direction that matches its spatial position. Duration 380ms standard easing. Reduced motion: cross-fade 120ms.

### Position hints

On Home, small edge hints (a label plus a chevron, 11sp) show each neighbor, faded to 55% opacity after the first 3 launches. Hints MUST remain available on tap of the compass.

## 7. Home screen (Hari Ini)

**PURPOSE**
Answer "What should I care about right now?" in under 3 seconds.

**CONTENT** (top to bottom)

| Block | Details |
|-------|---------|
| Greeting header | Display font. "Halo, {name}." plus a short contextual line (e.g. "Sore ini ada 2 hal penting."). Date chip (Rabu, 2 Okt). |
| Today progress strip | Asymmetric panel. Done-of-total as a ring or curved arc. Text "3 dari 7 selesai". |
| Next obligation pill | Nearest upcoming event or timed task in the next 24h. |
| Bubble field | The main area. One bubble per active task, per `08`. |
| Focus indicator | The top bubble has a highlight ring and the label "Fokus sekarang". |
| Quick add | Floating pill at bottom center-left: "Tambah cepat". Never hidden by the compass chip. |
| Compass chip | Bottom right. |

**LAYOUT**
The bubble field occupies roughly 55% of the screen height. It scrolls only if the user increases the font scale such that the field cannot fit; otherwise it is static.

**INTERACTION**

| Action | Result |
|--------|--------|
| Tap bubble | Opens a bottom sheet "peek" with title, deadline, category, notes, and buttons Selesai / Mulai / Edit |
| Long press bubble | Marks done directly (haptic tick) |
| Drag-up on bubble | MAY mark done. Optional, only if it does not conflict with spatial swipe. |
| Tap focus ring | Pin or unpin as manual focus |
| Quick add tap | Bottom sheet with title field focused, category chips, deadline shortcuts (Hari ini, Besok, Pilih), save. See §7a. |

**Completion behavior:** the bubble shrinks, fades to 35%, and drifts to the bottom edge; the remaining bubbles reflow smoothly (spring, 400ms). Progress ring advances. If no urgent items remain, the field enters **calm mode** (`08` §6).

**VISUAL PRIORITY**
1. Focus bubble. 2. Greeting. 3. Other large bubbles. 4. Progress strip. 5. Everything else.

**DO**
- Let size and position communicate urgency.
- Let the field feel slightly alive (gentle drift, within limits).
- Keep a clear empty-ish calm state.

**DON'T**
- Don't turn bubbles into a grid.
- Don't show more than 7 bubbles; the rest collapse into "+N lainnya".
- Don't add gamification (streaks, points, confetti storms).
- Don't put full metadata on a bubble: title (max 2 lines), and a single short deadline label at most.

### 7a. Quick add flow

Goal: new task in under 10 seconds.

1. Tap "Tambah cepat". Bottom sheet opens with keyboard up.
2. Type a title.
3. Optional: tap a category chip (default: last used), tap a deadline shortcut.
4. Press Simpan (or keyboard Done). Sheet closes, a new bubble pops in.

Priority defaults to Medium. The user MAY expand "Detail" for notes and priority. Smart parsing of text (like "besok") MUST NOT be built in v0.1.

## 8. Tasks screen

**PURPOSE**
Easy to scan, calm, complete management of all tasks.

**CONTENT**
- Header "Tugas" with count summary ("5 hari ini, 12 total").
- Category filter row: Semua, Akademik, Organisasi, Personal, Project (scrollable pills).
- Grouped list by time bucket: **Hari ini, Besok, Minggu ini, Nanti, Selesai** (Selesai collapsed by default).
- "Terlambat" bucket on top when there are overdue tasks, tinted coral.

**LAYOUT**
- Task rows are pills or soft rounded rectangles with a left domain-colored dot or shape cue. Row corner radius alternates by position in a small pattern (e.g. 28/16 and 16/28) to avoid monotony, but alignment stays strict.
- Each row shows: checkbox circle, title (1 line, ellipsis), one secondary line (deadline in plain language, e.g. "Hari ini, 23.59"). Category is shown by the color cue plus a tiny label only when filter is "Semua".
- Priority High shows a small coral flag. Medium/Low show nothing.
- Status In Progress shows a small half-filled indicator.

**INTERACTION**

| Action | Result |
|--------|--------|
| Tap row | Opens task detail sheet (edit) |
| Tap checkbox | Toggle Done (with haptic and subtle animation) |
| Swipe row right | Done |
| Swipe row left | Delete (soft) with Snackbar "Dibatalkan?" undo, 5s |
| FAB-style "Tambah" pill | Same sheet as quick add, expanded |
| Pull to top overscroll | Return to Home (see conflict rules) |

**VISUAL PRIORITY**
1. Overdue / Today buckets. 2. Task titles. 3. Deadline text. 4. Category cues.

**DO**
- Show only the minimum metadata per row.
- Use bucket headers with counts.
- Keep the Done bucket collapsed.

**DON'T**
- Don't display a grid of square cards.
- Don't show priority, status, category and notes on every row.
- Don't use bubbles here.

### Task editor (sheet)

Fields: Judul (required), Kategori, Deadline (date + optional time), Prioritas (Rendah/Sedang/Tinggi), Status (Belum mulai/Sedang dikerjakan/Selesai), Catatan. Save button disabled while title is blank. Delete button inside the editor with confirm via undo-snackbar (no modal).

## 9. Timeline screen

**PURPOSE**
"What happened and what will happen in my life this week?" A narrative view, not a calendar grid.

**CONTENT**
- Header "Timeline" and week range ("30 Sep - 6 Okt").
- Week switcher: small prev/next arrows and "Minggu ini".
- A vertical winding path running down the screen with **stops**. Each stop is one of:
  - Event (class, exam, meeting, activity)
  - Task deadline
  - Completed task (shown with soft check)
  - Note ("catatan penting"), as event of kind NOTE
- Day separators with a rounded day label ("Rabu, 2 Okt") and a "Hari ini" marker.
- Past stops are muted; future are full color; today stops have a pulse-free highlight.

**LAYOUT**
- Path: a gentle S-curve line (3dp stroke, domain tint). Stops alternate left/right of the path with wide rounded panels (radius 24dp) that vary width with content.
- Readability beats art: in a given day, if there are more than 6 stops, the path simplifies to a straight vertical line.
- Keep path as a drawn Canvas element behind the content, not a layout.
- User-authorized refinement (2026-10-07): Timeline uses a compact return handle, a single week-control row, quiet day separators and a narrow path gutter. Cards alternate with small insets instead of large side-to-side jumps; the path must not loop around card borders. Reserve tonal emphasis for today and keep empty days visually light.

**INTERACTION**

| Action | Result |
|--------|--------|
| Tap stop | Opens detail sheet (event or task) |
| Long press on empty path | Creates event at that day (MAY, can be deferred) |
| "Tambah acara" pill | Creates an event (title, kind, start, optional end, location, notes) |
| Scroll | Vertical; scroll to Today on open |

**VISUAL PRIORITY**
1. Today and next 48h. 2. Event titles and times. 3. Day labels. 4. Past items (muted).

**DO**
- Open scrolled to today.
- Show time ranges as "08.00-10.00".
- Mix past and future so it reads as a life log.

**DON'T**
- Don't render a month grid.
- Don't use the path if it hurts legibility.
- Don't create events from tasks automatically except through deadlines.

## 10. Vault screen (concept, placeholder in v0.1)

**PURPOSE**
The future home for files, backed by Google Drive. In v0.1 it MUST be an honest placeholder.

**CONTENT (v0.1)**
- Header "Vault".
- Four tall rounded folder shapes labeled Kuliah, Projects, Personal, Inbox, each with a status text "Belum terhubung".
- A friendly empty-state panel: "Vault belum terhubung ke Google Drive. Ini akan hadir di versi 0.3." with an illustration area.
- NO fake file names, NO fake storage numbers.

**CONTENT (future)**
Storage ring, folders, recent files, search, offline marks, upload, send to laptop.

**LAYOUT**
Folders: tall panels at differing heights in a 2-column staggered layout. File rows (future) are compact wide rows with a file-type glyph.

**INTERACTION (v0.1)**
Tapping a folder shows a snackbar: "Segera hadir". Nothing else.

**DO** Be practical-looking and quiet.
**DON'T** Don't pretend files exist. Don't use bubbles.

## 11. Sync screen (concept, placeholder in v0.1)

**PURPOSE**
The future bridge between Phone, Google Drive and Laptop. In v0.1, show the structure only.

**CONTENT (v0.1)**
- Header "Sync".
- Triangular device map: Google Drive (top), HP (bottom-left), Laptop (bottom-right) as circles with dashed connectors.
- Every node shows status "Belum disiapkan" in a neutral gray. The phone node MAY say "Perangkat ini".
- Status panel (asymmetric): "Sinkronisasi belum aktif. Data kamu tersimpan di perangkat ini."
- A short roadmap list: Kirim file ke laptop, Clipboard, Riwayat transfer, each with "Segera".

**INTERACTION (v0.1)**
No active controls except the compass and back.

**DO** Keep the device map. Keep honest status wording.
**DON'T** No "Online" badges. No timestamps for fake syncs. No progress bars.

## 12. Empty states

| Screen | Message (Indonesian) | Action |
|--------|----------------------|--------|
| Home, no tasks | "Hari ini masih kosong. Tambah satu hal kecil dulu?" | Quick add |
| Home, all done | "Semua beres. Tarik napas dulu." | none (calm mode) |
| Tasks filtered empty | "Belum ada tugas di sini." | Tambah |
| Timeline, empty week | "Minggu ini masih lega." | Tambah acara |
| Vault/Sync | See sections above | none |

Each empty state has a small organic illustration drawn with Compose shapes (no external images required). Tone: warm, short, never guilt-inducing.

## 13. Loading states

Local data loads fast. Show nothing blocking.

- Initial load of Room: a skeleton of 3 soft bubbles (Home) or 4 pill rows (Tasks) shown only if loading exceeds 150ms.
- No spinners on local operations.
- No full-screen loading indicators.

## 14. Error states

| Case | Behavior |
|------|----------|
| Save failed | Snackbar "Gagal menyimpan. Coba lagi." with action Coba lagi. Input preserved. |
| Read failed | Inline panel "Data tidak bisa dimuat" with a retry button. |
| Validation | Inline helper text under the field. Never a modal. |
| Unknown | Log with `Log.e`; show the generic snackbar. Never crash on DB errors. |

## 15. Animations

| Moment | Spec |
|--------|------|
| Screen change | Directional slide 380ms |
| Bubble complete | Shrink to 0.6, fade to 0.35, drift down 24dp, 360ms |
| Bubble reflow | Spring (damping 0.75, stiffness medium) |
| Bubble add | Scale from 0.6 to 1.0 with overshoot 5% |
| Bubble idle drift | ±3dp sine, 6-10s per bubble, phase from task id |
| Calm mode transition | Cross-fade background tint 600ms |
| Dense mode | Bubble scale to 0.9 over 400ms |
| Checkbox | Draw-on check 180ms |

Rules:
- Animations MUST NOT block input.
- Idle drift MUST stop when reduced motion is on or the app is in the background.
- No parallax, no particle storms.

## 16. Accessibility

| Requirement | Detail |
|-------------|--------|
| Touch targets | At least 48dp |
| Contrast | Text 4.5:1 minimum, large text 3:1 |
| TalkBack | Every bubble has a content description like "Persiapan UTS, penting, jatuh tempo hari ini 23.59, ketuk untuk detail" |
| Gestures | Every swipe has a non-gesture alternative (compass chip, buttons) |
| Reduced motion | Respect system animator scale 0 and `Settings.Global.ANIMATOR_DURATION_SCALE`; disable drift, replace slides with fades |
| Font scaling | Test at 100%, 130%, 200% |
| Color not alone | Urgency also shown by size, flag icon and text, never by color only |
| Focus order | Home order: greeting, focus bubble, other bubbles by rank, quick add, compass |

## 17. Dark mode

- Dark mode is a **designed theme**, not an inversion.
- Backgrounds deep charcoal/navy (see `07`), never `#000000`.
- Accent hues are preserved; bubble fills use tinted dark containers with a bright accent outline for non-focus bubbles, and a solid accent fill with dark ink text for the focus bubble.
- Respect system setting by default; user can force Light or Dark in Settings.
- Elevation expressed by lighter surface tints, not heavy shadows.
