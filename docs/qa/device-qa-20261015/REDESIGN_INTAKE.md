# Redesign intake — Maru / Argon

## Start here and authority

Read this intake, then [the full preserved audit](AUDIT.md), [surface inventory](surface-inventory.md), [source boundary review](source-review.md), [evidence index](EVIDENCE_INDEX.md), [measurements](measurements.json) and [provenance](PROVENANCE.json).

This is a QA baseline, not a design approval. The owner is collecting **new references** and the new design direction is not finalized. Historical `DESIGN.md`, older mockups, current visual tokens and installed skills are not automatic visual authority. The audit's proposed fixes, sampled colors and layout suggestions remain historical diagnostic proposals, not binding future design contracts. No palette, typography, layout style, icon system or navigation appearance is selected here.

Workflow: **Sol preserves QA → owner researches references → Maru / Gemini Argon creates design architecture and prototypes without production code changes → owner explicitly approves → Sol implements the approved contracts → Sol performs targeted regression QA.** No redesign is approved until the owner explicitly accepts it. Do not repeat the broad physical-device audit to prepare a design proposal.

## Existing scope and observed inventory

Riko Hub is an existing Kotlin/Jetpack Compose/Room Android organizer for tasks, courses and academic assignments, events/planning, projects/ideas, personal tasks/routines/notes/journals, optional Google Drive materials, and optional local laptop pairing/handoff. Backup, reminders, notification entry points and a launcher widget are also part of the source inventory.

The preserved runtime table has 13 routes, all reached in the original audit:

| Existing route | Observed runtime scope |
|---|---|
| Home / Today | Existing overdue focus, completed-today row, progress and task/settings shortcuts |
| Tasks | Existing list, completed bucket, search/no-results, new/existing editor |
| Timeline / Plan | Week/day modes, task stop, empty days, new event editor |
| Capture | Blank quick capture and Vault shortcut; lower recent list not separately certified |
| Vault | Disconnected account view only |
| Spaces | Four portals and back navigation; dark/light and font-size variations |
| Academics | Zero-course orbit/focus/deadline/exam/material placeholders and course navigation |
| Courses | Empty active/archive lists, new course editor and unsaved weekly row |
| Projects | Empty projects/ideas, project editor and empty idea dialog |
| Personal | Empty tasks/reminders/routines/notes/journal/history; note/routine editors; journal draft reproduction |
| Devices | Offline/unpaired bridge, empty trust/handoff/activity sections |
| Sync | Offline/unpaired state; no connection or transfer |
| Settings | Theme/name/backup views; import provider chooser cancelled |

The current dock exposes Home, Plan, Capture, Vault and Spaces. Tasks/Settings currently map to Home selection; Courses/Sync/child spaces map to Spaces. These are existing behaviors to inspect, not a requirement to keep the current dock layout or route arrangement. Preserve feature coverage and user data through any later approved restructuring.

Nested surfaces include task/project/note/routine sheets; event/course sheets; assignment dialog/course dropdown; idea dialog; journal history and draft/delete confirmations; native date/time pickers; system document providers; external authorization/viewers; pairing, trust and transfer confirmations. Source-mapped or conditional surfaces are not automatically runtime-verified; consult the inventory's status column.

## Audit facts and completeness

File inspection during preservation confirmed **15 numbered findings, 13 runtime route rows, 64 unique gallery images and 80 original PNGs**. All 64 selected image hashes match the original manifest. The owner reports approximately **1 hour 27 minutes** spent on the audit; the artifacts do not provide an independently verified full-session timing record. A saved 19-second warm-performance interval is not the total audit duration.

The original report records no saved/deleted user records, only disposable editor drafts; theme and font were restored. The original evidence directory is unchanged. All screenshots and raw logs remain private/local-only. The preserved reports disclose their limited privacy substitutions; see EVIDENCE_INDEX.md before opening or sharing original evidence.

### P1 — original IDs retained

| ID | Finding | Type / redesign relevance |
|---|---|---|
| #1 | Leaving Personal silently loses an unsaved journal draft | Functional data-retention defect. Track an engineering fix/behavior contract; visual polish alone cannot resolve it. |
| #2 | Academics/Devices headers collapse into narrow fragments at large text | Visual/responsive accessibility defect. Directly informs header and action layout evaluation. |

### P2 — original IDs retained

| ID | Finding | Type / redesign relevance |
|---|---|---|
| #3 | Dock labels crowd or split at large text | Visual/navigation usability; inspect all selected positions and scaled captions. |
| #4 | Poor warm-navigation frame timing on the installed debug build | Performance behavior; investigate before assuming new visual complexity is affordable. Attribution limited. |
| #5 | Light task/event sheets use a lavender surface unlike adjacent screens | Visual/theme consistency; inspect component/surface roles, without inheriting the old palette. |
| #6 | Dark artwork rectangles remain on light portal cards | Visual/asset integration; evaluate artwork in the complete theme context. |
| #7 | Light input boundaries have low measured contrast | Accessibility/interactive affordance; evaluate new field boundaries. |
| #8 | Offline Devices asks for an inapplicable permission action on Android 9 | Functional/content accuracy; distinguish Wi-Fi availability from permission state. |
| #9 | Home progress wording hides its limited denominator | Product/content semantics. Confirm intended scope with owner before changing calculation. |
| #10 | A related navigation journey mixes languages and terminology | Content consistency; evaluate labels and date localization together. |
| #11 | Native date/time dialogs appear dark over app Light mode | Visual/platform integration; cause unresolved, dialogs remained usable. |
| #12 | Untouched new course/event forms immediately report errors | Mixed interaction/visual feedback defect; keep input validation while clarifying when errors appear. |

### P3 — original IDs retained

| ID | Finding | Type / redesign relevance |
|---|---|---|
| #13 | Personal guidance describes the older task-based entry flow | Content/functional guidance; align instructions with the actual note/routine actions. |
| #14 | Empty Academics repeats its focus placeholder | Visual/content hierarchy; evaluate redundant information. |
| #15 | Search-no-results uses first-use empty copy | Content/search recovery semantics; distinguish no matches from no records. |

Original audit priority groups also remain unchanged: **Must fix #1–3; Should fix #4–13; Nice to have #14–15.** They are not identical to severity groups: #3 is P2, and #13 is P3. Do not relabel them as all “critical.”

## Facts, hypotheses and decisions still open

- **Observed:** draft marker present before leaving Personal and absent on return, without a discard warning (#1). Existing saved-journal deletion was not tested.
- **Observed:** font scale 1.6 fragments Academics/Devices headers and crowds the dock (#2–3). Navigation still responded; the dock was not a total navigation failure.
- **Observed:** light surfaces and artwork differ across components (#5–6); field-outline ratios are recorded (#7).
- **Hypothesis:** omitted Material light surface roles explain the lavender sheet (#5). The dirty source tree was not rebuilt to establish exact APK/source identity.
- **Unresolved cause:** native picker appearance may involve OEM/theme interactions (#11). Source defaults are repair leads, not proven binary attribution.
- **Product decision:** #9 is a scope/wording mismatch, not an arithmetic error. Current code intentionally excludes overdue carryover from the denominator; changing it needs owner input.
- **Future design consideration:** Maru must propose realistic prototypes and measurable usability/anti-slop acceptance criteria during the design stage. This intake creates no new aesthetic rule, performance SLA or prototype approval. Trace future criteria to evidence or an explicit owner decision.

## Real-device constraints, scaling and dialogs

The device was an **OPPO CPH2015, Android 9/API 28**, 720 × 1600 pixels at 320 dpi (approximately 360 × 800 dp). The installed package was `id.riko.hub.debug`, version 0.4.0/code 400, targeting SDK 37. The source reported minimum SDK 26. Treat older hardware and platform behavior as part of the evidence; do not infer tablet, landscape or newer-Android results.

Font scale was Small **0.9**, changed through normal Android Settings to Very large **1.6**, and restored to 0.9. App theme was Dark → Light → Dark. Captures 60–67 are the actual large-font run; 53–55/58 are failed changes still at 0.9, and 68 is a failed restoration still at 1.6. Capture 70 plus the recorded getter confirms font restoration; 74 confirms theme restoration; 80 is final Home.

Examine shared heading/action measurement, caption widths, font wrapping and available content space without treating smaller text as a fix. The journal field remained above the keyboard and the dock hid while typing. Assignment/course footers were reachable by scrolling; a cropped initial screenshot is not proof of inaccessible content. Native pickers remained usable despite mismatched appearance. Do not claim TalkBack/accessibility conformance: UI Automator export failed three times with a null root, and TalkBack/hardware keyboard traversal was not exercised.

## Performance observations and limits

The separate warm interval was Settings → Home → Plan → Capture → Vault → Spaces → Home at restored Dark/0.9, with no screenshot during its transitions. Recorded results: **30/30 janky frames, median 81 ms, p90/p95 250 ms, p99 300 ms, 26 slow-UI-thread counts**. This is a small sample from the installed debug build on old hardware, not a release benchmark, performance budget or source attribution. OEM Choreographer warnings flag graphics/vsync timestamp issues.

The initial 7,057 ms launch occurred while the phone was locked and is not an isolated cold-start result. Initial 38/38 janky frames mixed startup/inspection. A single approximately 138 MiB memory PSS observation is not a leak test. Animator scale was 0.0, transition/window scales 1.0; the device was charging and sampled at 38°C. Background load, debug overhead, thermal state and OEM graphics timing limit conclusions.

## Features not tested

Drive authorization, connected browsing/search/filter/upload/recovery and external viewers; trusted/paired/connected devices and real handoff; backup preview/import/export; CRUD persistence, deletion/undo and destructive confirmations; populated course/project/personal states; notification delivery/deep links; widget behavior/lifecycle; injected storage/network errors and loading timing; rotation/tablets; TalkBack/hardware keyboard; release performance and long-run memory/battery. Do not replace “not exercised” with “passed.” The source review's task/event dirty-dismissal and sync inbound gaps remain unverified; its journal gap was later confirmed.

## What Maru must inspect before visual decisions

1. Read all linked preservation files and the original scope/caveats. Inspect the minimal private screenshot set in EVIDENCE_INDEX.md in context. No upload or publishing is authorized.
2. Inspect current navigation, state ownership and shared UI components: `navigation/RikoNavHost.kt`, `ui/navigation/RenaissanceDock.kt`, `ui/spaces/SpacesComponents.kt`, contextual task/event/course editors, `ui/spaces/personal/PersonalJournal.kt`, theme handling, native picker contexts and state-specific strings. Recheck historical line pointers against the then-current source.
3. Understand persistence boundaries and existing functionality sufficiently to preserve tasks, courses/assignments, notes/journals, reminders, backups, Drive and device trust/transfer behavior. No production Android code changes during the design phase.
4. Examine the owner's newly supplied references and discover relevant available design skills. Research appropriate UI/UX practices as part of Maru's stage; installed skills or older documents are inputs to evaluate, not approval.
5. Produce a new design language, implementation-ready contracts, realistic prototypes and measurable anti-slop/usability criteria, linking proposed changes to findings and owner choices. Record unresolved behavior questions and evidence gaps rather than inventing answers.
6. Submit the design for explicit owner approval. Only after approval does Sol implement the accepted contracts and run targeted regression against this baseline and the new criteria. Any future data-changing/device/build verification needs its own applicable authorization.

Owner attention: provide the new references, decide Home progress scope, review/approve the new design explicitly, and protect the private evidence directory from build cleanup. No old aesthetic is endorsed by this handoff.
