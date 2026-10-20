# Riko Hub — Design and product rules

## Authority

`redesign-prototypes/reference-adaptation/full-app-v1.html` and its adjacent CSS/assets are the visual reference for the whole app. `today-v2.html` is a Today-only preview and does not override the full-app reference. The Renaissance direction is abandoned: do not bring back its black-cat art, old palette, or visual treatment.

The reference is a browser prototype. Its desktop frame, navigation sidebar, fake records, example names, venues, progress, toast behavior, and web-only interactions are not app requirements. Follow its mobile screen compositions, colors, fonts, typography hierarchy, spacing, illustrations, and component styling while keeping the real Android flows below.

## Shared visual language

- Deep charcoal canvas `#1A1B18` / `#222320`, raised surfaces `#2A2B27` / `#33342E`, ivory text `#F8F6EC`, muted text `#A3A499`, and border line `#3D3E37`.
- Accents (authentic M-CHEF palette):
  - Periwinkle Blue `#5463FF` / `#5C6AF6`: screen canvas for Categories (`#spaces`) and background container for Venue Capacity (`#home`).
  - Saturated Coral `#FF5A43` / `#FA6352`: Hero Revenue sprint card, Quickbooks card, high-volume badge, and wave chart.
  - Mustard / Golden Butter `#F5CB58`: Small Eatery category hero card, Xero card, and floating yellow `＋` FAB.
  - Pale Sage `#CBD7C9`: Operational Timing dial top stage, Reports screen top half, and Homebase card.
  - High-voltage Lime `#D9F35B`: Primary action buttons and badges.
  - Cyan / Mint `#9FE3DE`: Integration cards and secondary badges.
- Use the bundled Noto Sans regular and semibold for functional text and Noto Sans Condensed Black for selected display headings. Keep a clear scale: heavy condensed display for screen titles and focal moments (`CATEGORIES`, `REVENUE`, `INTEGRATIONS`, `OPERATIONAL TIMING`, `REPORTS`, `VENUE CAPACITY`, `AI OPERATIONS LEAD`); regular/medium body, helper, and metadata text; semibold for controls and section headings.
- Key reference screen compositions:
  - **Categories / Spaces**: Full vibrant periwinkle canvas `#5463FF` with stepped stacked peeking tabs (`04 / PERANGKAT`, `03 / PERSONAL`, `02 / PROYEK`) and a front hero card with action arrow button `↗`, badge pills, and vehicle art.
  - **Revenue / Projects**: Saturated coral card `#FF5A43` with diagonal hatched stripes chart, floating value pill, burndown bars, and tilted deliverable cards (-3.5deg, +2.5deg, -2deg) with black square monograms.
  - **Operational Timing / Plan**: Pale sage canvas `#CBD7C9` top half with 24-hour circular radial dial (12, 3, 6, 9 marks, coral focus wedge) and dark sliding schedule panel below.
  - **Reports / Vault**: Half sage canvas with whimsical art, dark sliding panel (`border-radius: 32px 32px 0 0`), waveform search, red square file tiles, and floating yellow `＋` FAB.
  - **Home**: Top avatar and greeting, location drop pill, dark priority card, and vibrant periwinkle Venue Capacity workload card with 7-day circular bubbles and coral wave area chart.
  - **Capture / AI Operations**: Avatar bubble with assistant prompt pills and bottom prompt bar with coral `＋` button and mic waveform.
  - **Integrations / Devices**: Tilted cards with connection status pills and high-contrast monograms (`▯`, `▱`, `▭`).
- Keep touch targets at least 48dp, preserve system font scaling, light/dark theme, Back/IME behavior, and reduce motion when the system requests it.
- Write concise, specific Indonesian UI copy. Say what is shown, what failed, or what an action does. Avoid slogans, filler, generic encouragement, fake claims, and AI-sounding phrasing.

## Reference-to-route map

| Full App screen | Existing Riko Hub route | Product behavior |
|---|---|---|
| Hari ini | `Home` | Real task/event focus, search, completion, and navigation. |
| Tugas | `Tasks` | Existing task filters, editing, status, reminders, and checklist. |
| Rencana | `Timeline` | Existing event/task agenda and editor. |
| Tangkap cepat | `Capture` | Existing quick task/note capture and file handoff. |
| Gudang materi | `Vault` | Existing Drive connection, folders, and file operations. |
| Ruang kerja | `Spaces` | Existing links to academic, project, personal, and device areas. |
| Akademik | `AcademicsSpace` | Real course, assignment, exam, and vault state. |
| Mata kuliah | `Courses` | Real courses, weekly slots, archive, and edit flows. |
| Proyek | `ProjectsSpace` | Project-category tasks and separate saved idea notes. |
| Personal | `PersonalSpace` | Existing routine and journal data. |
| Perangkat | `DevicesSpace` | Real local pairing and handoff state. |
| Sinkronisasi | `Sync` | Existing sync and backup behavior. |
| Pengaturan | `Settings` | Existing theme, display-name, and app settings. |

Every reference screen has an existing route. Retain the app’s route graph, dock labels, and navigation behavior; the reference’s browser route menu is not the Android dock specification.

## Forms

Adapt the reference editor and capture form to native Android. Preserve the current form-specific surfaces and behavior: task/project/note/routine editor; event editor; course editor; academic assignment editor; quick capture; personal journal; project idea editor; and Vault/backup dialogs where present. Keep all real fields, links, validation, deletion, reminders, recurrence, checklists, save/error recovery, and keyboard/focus/inset behavior. Do not replace a specialized workflow with a generic form or drop fields that the preview omits.

## Projects behavior and data limit

The app does not have a dedicated Project database entity. Its project cards are `Task` records with `Category.PROJECTS`; idea-board entries are separate `SpaceNote` records. The Projects screen must describe that honestly, use only stored data, preserve the existing filters and creation/edit workflows, and never seed preview projects or fake progress.

Project-card swiping only browses the currently filtered set. Advancing moves the front card to the back cyclically without changing a task. Provide accessible previous/next controls and a position indicator; handle no projects and one project honestly, and respect reduced-motion settings.

## Data and copy integrity

Do not change the database schema or migrate data for visual work. Do not fabricate preview tasks, courses, events, rooms, authors, project progress, device activity, or sync state. Keep feature names consistent and error/empty states actionable. Stored user content must not be rewritten as part of copy cleanup.
