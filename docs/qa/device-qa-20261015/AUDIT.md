> Preservation note (15 October 2026): this is a privacy-reviewed copy of the historical audit artifact at `build/verification/device-qa-20261015/AUDIT.md`. Original bytes remain unchanged locally; see [provenance](PROVENANCE.json) and [evidence index](EVIDENCE_INDEX.md). Only the unique device serial and an existing task title are replaced where present; evidence links are rebased to their original local files. All findings, severity, reproductions, source pointers, caveats and unverified areas are retained. Historical fix suggestions, sampled colors and layout proposals are diagnostic context, not approved redesign requirements. Statements about actions or commits refer to the original audit/contributor, not this preservation commit. The full original audit used ADB; this preservation task does not.

What this repo does: Riko Hub is an Android organizer for tasks, courses, events, journals, notes and project work. It stores local data and optionally connects to Google Drive and a trusted laptop. This audit assumes one owner using the existing small dataset on an older physical phone; it does not assess server-scale load.

# Real-device QA and visual audit — 15 October 2026

**Verdict: Needs fixes before visual sign-off.** The installed app is navigable, but it loses an unsaved journal draft, breaks shared layouts at large text, and combines inconsistent light-mode surfaces. No redesign or implementation was performed.

## Evidence and identity

| Item | Observed value |
|---|---|
| Physical device | OPPO CPH2015, serial [DEVICE_SERIAL_REDACTED]; one authorized ADB device |
| Android | 9 / API 28 |
| Display | 720 × 1600 physical pixels; 320 dpi; approximately 360 × 800 dp |
| App IDs from Gradle | Release `id.riko.hub`; debug `id.riko.hub.debug` |
| Installed application | Only `id.riko.hub.debug` found; version 0.4.0, code 400, target SDK 37 |
| Installed update time | 2026-10-15 13:02:41, device time |
| Source context | HEAD `fac68fe`, with pre-existing uncommitted UI changes. The APK was not rebuilt, so source-to-binary identity is not established. Runtime findings are based on the installed app; source locations identify current likely repair points. |
| Original preferences | Dark theme; Android font scale 0.9 (Small) |
| Approved variations | Light theme, then Android Very large text (verified scale 1.6) |
| Restored preferences | Dark selected in [74](../../../build/verification/device-qa-20261015/74-settings-restored.png); Small/0.9 confirmed in [70](../../../build/verification/device-qa-20261015/70-font-restored-confirmed.png) and [device-info.txt](../../../build/verification/device-qa-20261015/device-info.txt) |
| Final device state | Riko Hub Home, original theme/font, no editor open: [80](../../../build/verification/device-qa-20261015/80-audit-end-home.png) |
| Existing data observed | Tasks: 8 total, one overdue and seven completed. No courses, project tasks, personal journal entries, or trusted laptop appeared in the inspected states. Drive disconnected; local Wi-Fi unavailable. |

The app launched and responded to safe taps, scrolling, Back, filtering and editor dismissal. The initial `am start -W` reported 7,057 ms, but the phone was locked, so this is **not** an isolated cold-start benchmark. A normal wake/swipe dismissed the non-secure lock. The initial lock-screen screenshot is excluded from the evidence gallery because it contains unrelated notifications.

UI Automator hierarchy export was attempted three times, including `--compressed`; each returned `null root node`. No XML hierarchy was obtained. This is an automation limitation, **not proof that accessibility semantics are missing**. Screenshots and inspected source informed navigation. No TalkBack certification is claimed.

Open [the local screenshot gallery](../../../build/verification/device-qa-20261015/evidence.html), [complete surface inventory](surface-inventory.md), or [source boundary review](source-review.md).

## Reachability and coverage

All 13 registered routes were reached on the phone. Reaching a route does not mean every conditional state passed.

| Route | Runtime evidence and scope |
|---|---|
| Home | [02 dark](../../../build/verification/device-qa-20261015/02-unlock-check.png), [34 light](../../../build/verification/device-qa-20261015/34-home-light.png), [35 scroll end](../../../build/verification/device-qa-20261015/35-home-lower.png), [61 large text](../../../build/verification/device-qa-20261015/61-home-font16.png). Existing focus and completed-today content; all-tasks link. Pin/complete were not tapped. |
| Tasks | [36](../../../build/verification/device-qa-20261015/36-tasks.png), [72 completed bucket](../../../build/verification/device-qa-20261015/72-tasks-done-expanded.png), [73 no search results](../../../build/verification/device-qa-20261015/73-search-empty.png). Temporary query cleared. New and existing task editors, deadline draft, recurrence/checklist/priority/status fields inspected; no save/delete/complete. |
| Timeline / Plan | [03 dark](../../../build/verification/device-qa-20261015/03-plan.png), [47 light/week](../../../build/verification/device-qa-20261015/47-plan-light.png), [48 day](../../../build/verification/device-qa-20261015/48-plan-day.png), [60 large text](../../../build/verification/device-qa-20261015/60-plan-font16.png). Existing completed-task stop and empty days. New event editor [49](../../../build/verification/device-qa-20261015/49-event-editor.png), lower fields/footer [51](../../../build/verification/device-qa-20261015/51-event-details.png); dismissed without save. |
| Capture | [04](../../../build/verification/device-qa-20261015/04-capture.png), [62 large text](../../../build/verification/device-qa-20261015/62-capture-font16.png). Blank capture form and Vault shortcut. No quick-capture or idea save. Lower recent list was source-mapped, not separately certified visually. |
| Vault | [05 disconnected](../../../build/verification/device-qa-20261015/05-vault.png), [63 large text](../../../build/verification/device-qa-20261015/63-vault-font16.png). Account authorization, connected folders/search/filters/file actions/loading/upload errors unavailable without account access. |
| Spaces | [06 dark](../../../build/verification/device-qa-20261015/06-spaces.png), [52 light](../../../build/verification/device-qa-20261015/52-spaces-light.png), [64–66 large text](../../../build/verification/device-qa-20261015/64-spaces-font16.png). Four portals navigated. |
| Academics | [07](../../../build/verification/device-qa-20261015/07-academics.png), [08 lower](../../../build/verification/device-qa-20261015/08-academics-lower.png), [65 large text](../../../build/verification/device-qa-20261015/65-academics-font16.png). Zero-course state; course navigation. Populated course orbit, assignment list, materials and exam states not available. |
| Courses | [09 active empty](../../../build/verification/device-qa-20261015/09-courses.png), [10 archive empty](../../../build/verification/device-qa-20261015/10-courses-archive.png). New course form [11](../../../build/verification/device-qa-20261015/11-course-editor.png), footer [78](../../../build/verification/device-qa-20261015/78-course-editor-lower.png), weekly row [79](../../../build/verification/device-qa-20261015/79-weekly-slot-draft.png). Weekly row added only to the unsaved editor and discarded. No course saved, archived or reordered. |
| Projects | [12](../../../build/verification/device-qa-20261015/12-projects.png), [15 lower/ideas](../../../build/verification/device-qa-20261015/15-projects-board.png). Empty project/idea states, contextual project editor [13–14](../../../build/verification/device-qa-20261015/13-project-editor.png), empty idea dialog [16](../../../build/verification/device-qa-20261015/16-idea-dialog.png). No project/idea save or deletion. Populated filters/cards not certified. |
| Personal | [17](../../../build/verification/device-qa-20261015/17-personal.png), [18 journal](../../../build/verification/device-qa-20261015/18-personal-journal.png), [19 empty history](../../../build/verification/device-qa-20261015/19-journal-history.png), [23 lower](../../../build/verification/device-qa-20261015/23-personal-lower.png). Routine [24](../../../build/verification/device-qa-20261015/24-routine-editor.png) and note [77](../../../build/verification/device-qa-20261015/77-note-editor.png) forms inspected and dismissed. Unsaved journal retention reproduced using a QA-only marker; no journal saved. |
| Devices | [26 bridge](../../../build/verification/device-qa-20261015/26-devices.png), [27](../../../build/verification/device-qa-20261015/27-devices-lower.png), [28 trust/activity](../../../build/verification/device-qa-20261015/28-devices-trust.png), [67 large text](../../../build/verification/device-qa-20261015/67-devices-font16.png). Offline/unpaired/empty states only. |
| Sync | [29](../../../build/verification/device-qa-20261015/29-sync.png). Offline/unpaired state. No pair, connect, disconnect, forget, trust reset, send, receive or storage clearing. |
| Settings | [30](../../../build/verification/device-qa-20261015/30-settings.png), [31 backup](../../../build/verification/device-qa-20261015/31-settings-backup.png), [33 light](../../../build/verification/device-qa-20261015/33-settings-light.png), [74 restored](../../../build/verification/device-qa-20261015/74-settings-restored.png). Name untouched. Import provider chooser [32](../../../build/verification/device-qa-20261015/32-import-picker.png) opened and cancelled before provider/file selection. No export/import. |

Additional nested evidence: academic assignment dialog [44](../../../build/verification/device-qa-20261015/44-assignment.png), course dropdown [45](../../../build/verification/device-qa-20261015/45-course-dropdown.png), scrollable footer [46](../../../build/verification/device-qa-20261015/46-assignment-lower.png); native task date picker [38](../../../build/verification/device-qa-20261015/38-task-date-picker.png) and time picker [40](../../../build/verification/device-qa-20261015/40-time-picker.png), both cancelled. The assignment dialog footer remained reachable by scrolling; initial cropped content was not treated as a defect.

## Must fix

1. **Leaving Personal silently loses an unsaved journal draft — P1, confirmed.**
   - **What this is:** The inline journal accepts text before explicit Save. Its status correctly says the draft is unsaved.
   - **Problem:** Entering `QA_UNSAVED_DRAFT`, hiding the keyboard, tapping Spaces, then reopening Personal erased the text without a discard prompt. [Before](../../../build/verification/device-qa-20261015/20-journal-keyboard.png) → [departure](../../../build/verification/device-qa-20261015/21-journal-leave.png) → [empty return](../../../build/verification/device-qa-20261015/22-journal-return.png). Only a disposable draft was used.
   - **Fix:** Retain the draft above the popped Personal navigation entry, or route departure through a Save/Discard/Stay guard. Reuse the journal's existing dirty-draft protection rather than adding another editor abstraction. Repair points: `PersonalJournal.kt:74`, `:249`; `RikoNavHost.kt:82`.
   - **If we skip it:** A normal navigation tap can erase writing the owner has not saved. Stored journal deletion was not tested.

2. **Space headers collapse into narrow text columns at large font — P1, confirmed.**
   - **What this is:** Academics and Devices share a row containing Back, weighted heading/subtitle, and a trailing action.
   - **Problem:** At scale 1.6, “Manage courses” or “Buka Sinkronisasi” consumes most of the width. Titles and body text split into fragments down nearly the full screen: [Academics](../../../build/verification/device-qa-20261015/65-academics-font16.png), [Devices](../../../build/verification/device-qa-20261015/67-devices-font16.png).
   - **Fix:** At narrow width or large text, put the trailing action below the heading and subtitle. Change the shared `SpacesPageHeader` once (`ui/spaces/SpacesComponents.kt:69–85`); do not shrink the owner's chosen text size.
   - **If we skip it:** Large-text users must scroll through a broken header before reaching the screen's actual work.

3. **The dock does not accommodate large labels — P2, confirmed.**
   - **What this is:** Five equal-width navigation items share the bottom dock.
   - **Problem:** At scale 1.6, “Hari ini” crowds/touches “Plan” on Home and “Capture” breaks as “Captur / e” across routes. [Home](../../../build/verification/device-qa-20261015/61-home-font16.png), [Plan](../../../build/verification/device-qa-20261015/60-plan-font16.png), [Capture](../../../build/verification/device-qa-20261015/62-capture-font16.png). Navigation still responded; this is not a claimed total navigation failure.
   - **Fix:** Constrain captions to each item's measured width and use consistent two-line labels or shorter localized captions at large font. Recheck all five selected positions at 360 dp / scale 1.6 (`ui/navigation/RenaissanceDock.kt:100–137`).
   - **If we skip it:** The shared navigation remains difficult to scan for users who enlarge text.

## Should fix

4. **Warm navigation has poor frame timing on this installed build — P2, measured; attribution limited.**
   - **What this is:** Android's graphics counters measure delayed frames, commonly called jank.
   - **Problem:** After resetting graphics counters only, six warm transitions (Settings → Home → Plan → Capture → Vault → Spaces → Home) produced **30/30 janky frames**, median **81 ms**, 90th/95th **250 ms**, 99th **300 ms**, and 26 slow-UI-thread counts. The interval was 15:37:46–15:38:05 WITA, at restored dark/0.9 settings, with no screenshot during the measured transitions. See [raw counters](../../../build/verification/device-qa-20261015/gfxinfo-warm.txt).
   - **Fix:** Profile this exact route on the installed build and remove the measured main-thread/draw bottleneck before adding visual effects. Then compare a separately authorized release build. A specific code fix cannot be established from these counters alone.
   - **If we skip it:** Navigation on the owner's current installation remains choppy. These are a small debug-build sample on old hardware, not release benchmarks. Collected Choreographer warnings also flag OEM graphics/vsync timestamp issues; code-only blame is not justified.

5. **Light task/event sheets use an unintended lavender surface — P2, confirmed appearance; likely source cause.**
   - **What this is:** Core screens, sheets and academic dialogs should use the authored light palette.
   - **Problem:** Settings/page surfaces are cream (`#F3EBDD` / `#FBF6EC`), but task/event sheets are `#F7F2FA`; the assignment dialog is `#FFFDF8`. These are dominant colors sampled from the actual PNGs: [Settings](../../../build/verification/device-qa-20261015/33-settings-light.png), [task](../../../build/verification/device-qa-20261015/37-task-editor.png), [event](../../../build/verification/device-qa-20261015/49-event-editor.png), [assignment](../../../build/verification/device-qa-20261015/44-assignment.png).
   - **Fix:** Complete the light Material surface roles, particularly `surfaceContainerLow`, using existing palette colors, or explicitly set the shared sheet container. Current `LightScheme` omits several roles (`ui/theme/Color.kt:7–21`); sheets use default containers (`TaskEditorSheet.kt:52`, `EventEditorSheet.kt:49`). Default-role attribution is an inference, not an APK source match.
   - **If we skip it:** Opening a form visibly switches the app to another surface palette.

6. **Dark artwork rectangles remain visible in light Spaces — P2, confirmed.**
   - **What this is:** Each portal includes a crop of a shared illustration atlas.
   - **Problem:** Light-mode cards contain hard-edged black image rectangles that do not blend with their cream/tinted cards. [Normal](../../../build/verification/device-qa-20261015/52-spaces-light.png) and [large text](../../../build/verification/device-qa-20261015/64-spaces-font16.png) show the same boundaries; the dark background hides much of this mismatch.
   - **Fix:** Correct the image treatment at `SpacesScreen.kt:119–147`: use a transparent/theme-appropriate existing asset if available, or blend the image edge into the actual card fill. Do not introduce new page layouts to repair this asset integration issue.
   - **If we skip it:** Light mode keeps the appearance of separate dark image tiles pasted into otherwise light cards.

7. **Light input outlines are barely distinguishable from their background — P2, measured.**
   - **What this is:** Unfocused field outlines identify editable regions.
   - **Problem:** The authored outline `#E2D8C6` has only **1.31:1** contrast against page cream and **1.28:1** against the observed lavender sheet. The problem is visible in [Settings](../../../build/verification/device-qa-20261015/33-settings-light.png), [task fields](../../../build/verification/device-qa-20261015/43-task-details.png) and [assignment fields](../../../build/verification/device-qa-20261015/44-assignment.png).
   - **Fix:** Use the existing darker `#6F6877` ink for interactive field outlines, or an equally distinguishable palette value; leave decorative dividers independent. Repair point: `ui/theme/Color.kt:18`. Main body text `#6F6877` on cream measures 4.97:1; this is a field-boundary finding, not a claim that all text fails contrast.
   - **If we skip it:** Low-vision users have little visual help identifying where to enter text. A full accessibility conformance audit was not performed.

8. **Offline Devices gives an inapplicable permission instruction — P2, confirmed.**
   - **What this is:** The bridge explains how to recover when local networking is unavailable.
   - **Problem:** On this Android 9 phone with no local Wi-Fi, the bridge says “Izinkan akses jaringan lokal untuk mencari laptop,” while the lower view and Sync say to connect to Wi-Fi. [Bridge](../../../build/verification/device-qa-20261015/26-devices.png), [Sync](../../../build/verification/device-qa-20261015/29-sync.png). The source only requests the dedicated local-network permission for API 37+, so there is no such permission prompt here (`SyncScreen.kt:72–75`).
   - **Fix:** Separate unavailable Wi-Fi from denied local-network permission; use the existing connect-to-Wi-Fi instruction for the former (`DevicesSpaceScreen.kt:155–157`, `values/devices_space_strings.xml:30`).
   - **If we skip it:** The owner is directed toward a permission action they cannot take on this phone.

9. **Home's completed-progress label hides its limited scope — P2, confirmed semantics problem.**
   - **What this is:** Home places a completion count directly under the current focus task.
   - **Problem:** An unresolved overdue task is prominent while the bar reads “1 dari 1 selesai.” The code intentionally counts tasks due today, completed today, and undated in-progress tasks; overdue carryover falls outside that set. [Evidence](../../../build/verification/device-qa-20261015/02-unlock-check.png), `domain/logic/HomeSnapshot.kt:29–31`, `HomeContent.kt:301–309`. This is not an arithmetic error.
   - **Fix:** Make the label state that limited scope and explain excluded overdue work, or deliberately include outstanding carryover if that is the intended daily workload. Do not silently change the denominator without a product decision; existing tests encode the current union policy.
   - **If we skip it:** A full progress bar appears to contradict the unfinished task immediately above it.

10. **The same navigation journey switches languages and terminology — P2, confirmed.**
   - **What this is:** Indonesian is used across the dock, Home and most forms.
   - **Problem:** “Akademik” opens an English “Academics / Your courses…” page, then course management switches to “Ruang kuliah” while retaining “Back to Academics.” Projects' date strip uses English day names within Indonesian copy. [Portal](../../../build/verification/device-qa-20261015/06-spaces.png), [Academics](../../../build/verification/device-qa-20261015/07-academics.png), [Courses](../../../build/verification/device-qa-20261015/09-courses.png), [Projects dates](../../../build/verification/device-qa-20261015/15-projects-board.png).
   - **Fix:** Localize the existing strings and date formatter consistently with the app's Indonesian presentation (`values/academics_space_strings.xml:2`, Courses return label, `ProjectsSpaceScreen.kt` date formatting). No layout replacement is needed.
   - **If we skip it:** Related screens continue to read like features assembled from different products.

11. **Native date/time pickers ignore the selected app appearance on this phone — P2, confirmed appearance; cause unresolved.**
   - **What this is:** Task deadlines open Android platform dialogs.
   - **Problem:** With app Light selected, both pickers render charcoal backgrounds and turquoise controls over the light/brown editor: [date](../../../build/verification/device-qa-20261015/38-task-date-picker.png), [time](../../../build/verification/device-qa-20261015/40-time-picker.png).
   - **Fix:** Ensure the picker context/theme follows the selected app mode, starting with the default `DatePickerDialog` / `TimePickerDialog` calls in `TaskDeadlineFields.kt:39–49`; apply the same check to event pickers. Verify on this OEM before choosing a specific fix. The current Activity style names a light platform theme, so the precise OEM/theme interaction remains unproven.
   - **If we skip it:** Deadline editing abruptly changes visual system. The native controls remained usable; their system-language labels alone were not treated as a defect.

12. **Untouched new forms immediately display errors — P2, confirmed.**
   - **What this is:** A new course or event initially has a blank required title.
   - **Problem:** Before typing or submitting, the course footer shows a red general error about names, lengths and HH:mm scheduling despite having no schedule row. The event opens with “Judul wajib diisi.” [Course](../../../build/verification/device-qa-20261015/78-course-editor-lower.png), [event](../../../build/verification/device-qa-20261015/49-event-editor.png). Source shows unconditional invalid-state feedback in `CourseEditorSheet.kt:87` and `EventEditorSheet.kt:175–187`.
   - **Fix:** Keep required guidance neutral until the relevant field is touched or submission is attempted; show only the applicable field error. Keep validation and disabled Save intact.
   - **If we skip it:** Creating a record starts with the app appearing broken and reporting errors the owner has not caused.

13. **Personal guidance describes the older task-based entry flow — P3, confirmed.**
   - **What this is:** Empty routines and notes explain how to start.
   - **Problem:** Personal says to create a personal task and set its deadline/repetition, and says notes appear when added to tasks. Immediately beside those messages are dedicated “Tambah kebiasaan” and “Tambah catatan” actions with contextual editors. [Guidance](../../../build/verification/device-qa-20261015/23-personal-lower.png), [routine form](../../../build/verification/device-qa-20261015/24-routine-editor.png), [note form](../../../build/verification/device-qa-20261015/77-note-editor.png).
   - **Fix:** Update the two existing help strings to describe the visible contextual actions (`renaissance_spaces_refresh_strings.xml:4`, `personal_space_strings.xml:56`).
   - **If we skip it:** The owner follows a longer, different flow than the one the screen now provides.

## Nice to have

14. **Empty Academics repeats the focus placeholder — P3, confirmed.**
   - **What this is:** With no courses, the orbit center and a separate card both show “Current focus / Choose a course.”
   - **Problem:** The duplicate content consumes space without adding information, around the already-visible Open Courses action. [Top](../../../build/verification/device-qa-20261015/07-academics.png), [lower](../../../build/verification/device-qa-20261015/08-academics-lower.png).
   - **Fix:** Delete the redundant `EmptyFocusCourseCard()` call and its private helper (`AcademicsSpaceScreen.kt:180`, `:238–248`). A whole-tree source search found no other reference. This removes 12 code lines, excluding optional blank-line cleanup.
   - **If we skip it:** First-use Academics stays repetitive and pushes useful actions further down.

15. **Search-no-results uses first-use empty copy — P3, confirmed.**
   - **What this is:** Tasks supports a search field and an empty-result panel.
   - **Problem:** Searching for a nonmatching QA marker shows “Belum ada tugas di sini” while the header still reports eight tasks. The recovery button works and the query was cleared. [Evidence](../../../build/verification/device-qa-20261015/73-search-empty.png); `TasksScreen.kt:184–198` selects the same heading for filtered and unfiltered empty states.
   - **Fix:** Use “Tidak ada tugas yang cocok” for filtered/search emptiness; keep first-use wording for a genuinely empty dataset.
   - **If we skip it:** A failed search looks like an empty collection rather than zero matching results.

## Checks that did not reveal a confirmed defect

- All 13 routes rendered and safe Back/dock navigation responded. No crash was observed; PID 31290 remained alive throughout the inspected session. No `FATAL EXCEPTION` or `AndroidRuntime` failure was found in the collected app-scoped logs. Logs are snapshots of a finite ring buffer, not complete crash-history certification.
- The journal field stayed visible above the keyboard; the dock hid while typing. No saved journal was needed for the reproduction.
- Task, project, routine, note, event and course forms opened; inspected Save actions were disabled for invalid blank data. The observed course footer and assignment dialog footer were reachable by scrolling.
- Day mode and completed-task bucket expansion rendered; a no-results search recovered through “Tampilkan semua.” Existing task count remained eight in the later list capture.
- Both light and dark modes rendered and could be switched back; Plan adapted its layout for large text. These checks do not cancel the shared-header and dock failures.
- Sampled memory PSS (memory attributed to the process) was about 138 MiB. This single observation is not a leak or long-session memory test.

## Performance and diagnostic files

| File | Meaning / limit |
|---|---|
| [gfxinfo-initial.txt](../../../build/verification/device-qa-20261015/gfxinfo-initial.txt) | Mixed initial session: 38/38 janky frames, median 150 ms; includes startup and is not a clean comparison. |
| [gfxinfo-before-reset.txt](../../../build/verification/device-qa-20261015/gfxinfo-before-reset.txt) | Counter snapshot before the isolated warm interval. Only graphics counters were reset. |
| [gfxinfo-warm.txt](../../../build/verification/device-qa-20261015/gfxinfo-warm.txt) | Separate six-transition interval at original preferences: 30/30 janky frames, median 81 ms. |
| [warm-sample-start.txt](../../../build/verification/device-qa-20261015/warm-sample-start.txt), [end](../../../build/verification/device-qa-20261015/warm-sample-end.txt) | Device times for that interval. |
| [app-warnings.txt](../../../build/verification/device-qa-20261015/app-warnings.txt) | Initial app-only warning/error buffer. OEM `ANR_LOG` entries report ~3.5 s and ~2.3 s long messages during launch; they are not proof of a system-declared ANR. |
| [app-warnings-final.txt](../../../build/verification/device-qa-20261015/app-warnings-final.txt) | Later app-only buffer, including Choreographer warnings about future vsync timestamps. |
| [meminfo.txt](../../../build/verification/device-qa-20261015/meminfo.txt), [framestats.txt](../../../build/verification/device-qa-20261015/framestats.txt) | One memory snapshot and earlier frame details; no stress testing. |
| [measurements.json](measurements.json) | Screenshot colors, contrast calculations, identity and restoration record. |

Original animation settings were read only: animator duration 0.0, transition 1.0, window 1.0. The phone was charging; a sampled battery temperature was 38.0°C. Debug overhead, device age, background applications, thermal conditions and OEM graphics timing can affect results. No frame-time-to-source attribution is claimed.

## Permission boundaries and remaining gaps

No Kotlin/Compose/config source was changed. No build, Gradle test, lint, APK installation, package/tool install, uninstall, storage/cache clear, branch operation, commit or push was run. Artifacts are local under this ignored `build/verification/` directory.

No task, course, project, idea, note, event or journal record was saved, edited persistently or deleted. Temporary editor values and one unsaved weekly row were discarded. Theme and font were the only deliberate persistent preferences changed, with explicit owner approval, and both were restored. Launching the app naturally runs its existing launch-count/reconciliation behavior; those internals were not suppressed or reset.

Drive authorization/connected browsing/upload/error recovery, paired/trusted/connected devices, real handoff and its confirmations, backup preview/import/export, CRUD persistence/undo, populated course/project/personal states, notification delivery/deep-link behavior, widget layout, induced storage/network failures, loading-state timing, rotation/tablets, TalkBack/hardware keyboard traversal, release performance and long-run battery/memory behavior remain **not exercised**. Source mapping is provided for these states, not a pass result. More coverage needs approved disposable records, a test account/peer, or separately authorized builds/tests as appropriate.

Capture naming caveats: `14-projects-lower.png` is the lower project editor; `25-personal-return-state.png` is Spaces after dismissal. `53–55` and `58` were captured after unsuccessful font-change attempts and still use 0.9. `68-font-restored.png` was a failed restore attempt and still shows Devices at 1.6; **70** is the confirmed restoration. `50-event-editor-lower.png` still shows the keyboard, where a swipe entered a transient `by` draft; that event was discarded. Those misleadingly named intermediate attempts are excluded from the main gallery. No permission workaround or grant was used; successful font changes used the normal Settings slider.

Verdict: Fix journal retention and the two large-text layout defects first, then complete theme consistency and profile the current installation.

Lean: -12 lines, -0 dependencies possible by removing the redundant empty-focus card.

Not checked: Data-changing, account/peer-dependent, injected-failure and system integration paths listed above; no release or automated test result is implied.
