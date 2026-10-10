# STATUS - Riko Hub

Maintained by the implementing agent (Astra). Update at the end of every phase. Newest entries at the top of each section.

## Current state

| Item | Value |
|------|-------|
| Version in development | 0.2.0 |
| Current phase | v0.2 development; v0.1 accepted by the owner for personal daily use (2026-10-08) |
| Last successful build | Daily Studio redesign: owner confirmed build/install (2026-10-10); installed APK updated 09:28:22 and SHA-256 matches local debug APK. Build duration not supplied. Release remains the historical Phase 6 artifact. |
| Last test run | 5 focused reminder JVM tests passed; new device checks are pending (ADB empty). Historical full-suite evidence: 46 JVM passed; 19 phone passed, 6 failed, 2 skipped across runs, including a passing Room v1→v2 migration. |
| Release status | Unsigned local build; not a public release |
| Owner acceptance | App launches and operates; primary features work and UX is satisfactory. Prior manual deferrals and noncritical formal verification are intentionally accepted as deferred. |

## Home startup placeholder — 2026-10-10

- Source trace: Home starts with `loading=true` until the first combined task/event/settings emission. The previous loading composition displayed the header and “Ruang fokus”, then three decorative skeleton bubbles after 150 ms. Preview fixtures are not on the runtime path; normal initial navigation is Home.
- Replaced the intermediate skeleton composition with a themed loading surface and delayed progress indicator; the loaded Daily Studio UI and repository/task behavior are preserved. No artificial minimum loading duration is added; read errors retain retry.
- Authorized ADB found the debug app installed at 15:28:16 and performed one fresh launch without clearing data. Recording could not run because this phone lacks `screenrecord`; the reported four silhouettes and exact flash timing were not visually verified. No recording was produced.
- Source review and `git diff --check` only. Compilation, updated installation and cold-launch visual verification remain owner-managed and pending; no Gradle, lint or test suites run.

## Daily Studio finalization and v0.2 readiness — 2026-10-10

- Owner acceptance of the redesign remains recorded. Final guide-removal verification is pending: authorized ADB inspection found the same 09:28:22 installation and APK SHA-256 `e638bd6964bbf761363e8b44be92ad8ff2f8e514d4610d2d8fadfa5edb43a80c`; the local APK also remains unchanged. All four guides are still visible in that installed build. A rebuild/update-install by Riko is required before calling the final source verified.
- No new runtime gesture/contrast success is claimed for guide removal. Earlier light/dark and compass-tap evidence remains under `build/verification/home-redesign/after/`. The final capture included an unrelated picture-in-picture overlay and was discarded; `build/verification/home-redesign/final/README.md` records provenance and the limitation. No personal tasks, settings or installations were modified in this pass.
- `git diff --check` passed. Working branch is `checkpoint/rikohub-phase6`; existing uncommitted Home changes, `home_redesign_strings.xml`, Miyu resources and Gradle JVM file are preserved. Only this status entry and local verification notes were added; no builds/tests, commit or push.
- Final Home checks after the correct install: removed guides and footer; light/dark continuity; compass tap and hold/drag; horizontal page swipes; bubble tap/peek and dismissal; Quick Add open/dismiss without saving. Bubble long-press completes a task, so it requires an explicitly authorized disposable fixture. Larger fonts, dense/empty states and mode-transition frame pacing remain unverified without suitable owner-controlled conditions.

### Proposed remaining v0.2 sequence — planning only

1. **Reminder device checks first:** existing implementation and focused fixtures are already available. Verify delivery, edit replacement, completion/deletion cancellation, duplicate/stale suppression, process restart and cold/warm notification entry. Use isolated fixtures with authorization, not personal assignments; no forced job execution alone counts as real timed delivery. Android 13+ permission handling needs a separate compatible device. Owner handles any APK/test builds.
2. **Local SAF export/import:** define format version, exported fields and stable record identity before coding. Use document picker with bounded input, full validation before writes, idempotent repeat imports, and explicit conflict reporting without overwriting existing records. Restore tasks/events in one Room transaction; reconcile reminders only after commit. Verify empty-store round trip, populated-store merge, duplicate import, malformed/oversized/unsupported-version input, rollback and persistence using disposable data. Define any settings restore separately rather than claiming a Room transaction also makes DataStore atomic. No cloud or broad storage permission.
3. **Native top-three widget:** reuse existing urgency ordering and exclude done/deleted tasks. Use lightweight Android 9-compatible native widget rendering, at most three real tasks, and app-opening actions. Update after committed task writes/import and at relevant time boundaries with bounded scheduling; avoid polling. Verify empty/one/three/overflow data, completion/deletion/edit/restore refresh, resize/readability, reboot and app-opening behavior. Depends on settled restore/reconciliation hooks, not Drive.

- These priorities follow `V03_IMPLEMENTATION_PLAN.md`, `04_ROADMAP.md` and current status; the current instruction is planning only and overrides older authorization to proceed into v0.3. No export/import, widget or Drive implementation started. Await owner approval after the handoff; v0.2 is not complete.

## Home “Daily Studio” redesign — 2026-10-10

- Implemented a substantial presentation pass: integrated plain date/greeting header, bubble field before supporting information, quiet linear progress and wrapping upcoming information, and one quick-add/compass action row. A single task now occupies a centered 68%-width blob; multi-task domain placement is unchanged. Empty fields no longer reserve an unused full-height bubble stage. Focus titles receive more room at normal text size; large-text layout and completion dots remain.
- Confirmed the owner's dark-theme issue in the installed APK: greeting/context text rendered black on dark mint. The custom Scaffold background had no matching foreground mapping. Explicit theme `onBackground` content color now covers the Scaffold and standalone Home content. Removed bubble radial glow; existing blob shapes, category fills, rings, drift and reduced-motion behavior remain.
- Source review found the new compass footer initially nested inside Home's swipe detector. Moved the footer outside the detector to preserve compass hold/drag and page-swipe separation. Task callbacks, peek, pin, completion/undo, overflow and navigation destinations remain wired to the existing engine; no data-layer or reminder edits.
- Files: `ui/home/HomeContent.kt`, `HomeScreen.kt`, `Bubble.kt`, `BubbleField.kt`, `navigation/RikoNavHost.kt`, `ui/theme/HomeTokens.kt`, `res/values/home_redesign_strings.xml`, and this document. Approved Miyu source/resource hashes remain unchanged.
- Baseline inspection: authorized OPPO CPH2015, 720×1600, density 320; installed `id.riko.hub.debug` 0.1.0 (100), updated 08:59:38. Installed/local APK SHA-256 matched. Light/dark calm Home screenshots are under `build/verification/home-redesign/before/`; README records provenance. Temporary app theme selection was restored to `Ikuti sistem` and visually confirmed. UI hierarchy dump failed with a null root; no personal tasks or global settings were changed.
- Checks: focused source/caller/resource review, `git diff --check`, single-bubble bounds calculation and icon hashes. Static calm-background contrast: light primary 13.30:1 / secondary 4.83:1; dark primary 12.68:1 / secondary 5.72:1. These are token calculations, not rendered verification. Worker follow-up reporting hit a usage limit; root completed missing resource integration and review corrections.
- Owner confirmed build/install. Post-install inspection verified APK SHA-256 `e638bd6964bbf761363e8b44be92ad8ff2f8e514d4610d2d8fadfa5edb43a80c` matches the local debug artifact; installed update time 09:28:22. Astra ran no Gradle/build/package/test/lint/emulator command. Updated light/dark calm Home screenshots were inspected under `build/verification/home-redesign/after/`: greeting/context contrast is corrected, the visible task title wraps within its bubble, bottom actions fit and background is continuous through system insets. Compass tap opens the dialog, close dismisses it, and Settings/back navigation works. Original `Ikuti sistem` theme was restored and screenshot-confirmed. No task data was modified. Hierarchy dump still returned a null root. No additional source change was warranted by this visual check.
- Owner accepts the Daily Studio Home redesign for Home only. This follow-up removes the Timeline text control and the bottom Sync/Tasks/Vault arrow guides; the updated build awaits owner build/install. This does not mark v0.2 complete.
- Pending owner QA: normal/dense/empty states, larger text, upcoming/progress content absent from the observed calm state, compass hold/drag, bubble actions and animation performance. Still screenshots establish layout, not frame pacing. Final v0.2 acceptance remains pending; no other milestone started.

## Home visual refinement — 2026-10-10

- Source-level cause of the background mismatch: Home's calm mint wash was drawn only inside the navigation content; the parent Scaffold, compass area and insets retained the base theme background. Home now reports its existing animated color to the Scaffold while Home is active. Other destinations retain their own base background. This addresses the code-level split; the reported phone appearance has not been visually confirmed.
- Refined header spacing, made the date chip fit its text within the available width, grouped progress and next obligation, and replaced the obligation's compressed text columns with wrapping label/detail text. Existing spacing, typography and color tokens are reused. Bubble layout, interactions, motion, task logic and navigation behavior are unchanged.
- Files: `ui/home/HomeContent.kt`, `ui/home/HomeScreen.kt`, `navigation/RikoNavHost.kt`, and this status document. Existing uncommitted Miyu launcher artwork/resources are preserved.
- ADB check succeeded but returned no connected devices. No installed app inspection, screenshots, installation or task interactions were performed. Evidence note: `build/verification/home-polish/README.md`.
- Verification: focused source/caller review and `git diff --check` passed. Riko reported `:app:assembleDebug` succeeded in 4m40s (37 tasks, 5 executed / 32 up-to-date). No tests, lint, emulator or release command was run.
- Owner QA pending: install the debug build; inspect light/dark calm, normal, dense and empty Home; long obligation titles; increased text size; navigation transitions and background continuity through the compass/system insets on Android 9. No device was connected during this implementation pass, so Home screenshots remain pending.
- This finishes only the source refinement pass, not v0.2 acceptance. Reminder device verification, local export/import and restore validation remain separate unfinished work; comprehensive QA and final acceptance belong to Riko. No v0.3 work started.

## Local deadline reminders — 2026-10-09

- Preservation checkpoint: `7fbe3f7` contains the existing v0.2 work. Unexplained untracked `wl-copy` is excluded and untouched.
- Implemented local WorkManager reminders using existing deadline/offset fields, unique per-task work, post-commit reconciliation and app-resume recovery. Edits replace work; completion/deletion cancel it; reopening, restore and recurring successors are evaluated independently. Delivery rechecks the latest task inside a Room transaction before posting.
- New reminder times already elapsed are skipped; matching queued work may deliver late. No exact alarm, foreground service, networking, dependency or schema change. Date-only deadlines retain the existing local 23:59 instant. Disabled notifications are suppressed without retry polling.
- Added the Indonesian notification channel, explicit task-opening notification intent, and contextual Android 13+ `POST_NOTIFICATIONS` permission UI. Denial does not prevent saving tasks. Task opening waits for reads/writes and existing drafts; consumed notification intents are cleared.
- Compass drag translation now follows 50% of finger movement instead of 25% on both axes; hold appearance, activation thresholds and timing are unchanged.
- Verification: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest --tests id.riko.hub.domain.logic.TaskReminderDecisionTest :app:assembleDebugAndroidTest --offline --console plain --max-workers=1` passed in 10m26s; 5 reminder JVM tests passed, zero failures/errors. Earlier compiler failures in the scheduling callback and a debug preview caller were corrected before this incremental run. No lint, release pipeline, emulator initialization, data clearing or unrelated regression run.
- ADB was empty at final verification. Four `TaskReminderWorkerTest` methods (real WorkManager scheduling and notification posting, using isolated fixtures) and three focused `TaskCrudTest` methods are compiled but unexecuted. Android 13+ permission behavior, cold/warm notification entry, process-restart delivery and the lighter compass drag remain unverified on-device. The reminder milestone needs this focused device check before runtime acceptance.

### Six-failure triage

| Recorded failure | Classification and action |
|---|---|
| Return swipe/tap | D: cause unresolved; added a destination wait without changing gestures. Not reclassified as fixed without a rerun. |
| Theme selection | C: asynchronous DataStore/UI assertion; wait for persisted and rendered selection, and restore the original preference in the fixture. |
| Foundation title clipping | B: normal-size task-title ellipsis is intentional; allow confirmed single-line ellipsis below 1.3× while retaining geometric bounds checks. |
| Home peek/pin at 200% | D: failing text bounds are recorded, but the overflowing element is not localized. Layout and assertion remain unchanged. |
| Checklist selector | B: two legitimate item checkboxes; scope to the checklist group and explicitly select its first item. |
| Recurring-task CRUD | B: completion intentionally creates a successor; assert the source by ID and the generated task separately. |

- Next: finish the focused reminder device checks, then local manual export/import with SAF, a versioned format, validation and non-destructive duplicate handling. Export/import was not started during reminder implementation.

## v0.2 progress — 2026-10-08

- Checkpoint `0263983` preserves the Phase 5–6 work and the task extensions already present in the working tree.
- Existing implementation discovered at the checkpoint: task search, recurrence, checklists, estimates, and Timeline day mode. Reminder offsets are stored; notification delivery is not implemented.
- Added checklist completion/total progress to Tasks rows, hidden when a task has no checklist.
- Verification: debug APK assembled. The initial consolidated run stopped at Android test compilation; repaired a missing deadline-helper import and mapped task entities to domain models for checklist assertions. The affected Android test assembly and 15 targeted JVM tests then passed in 2m46s (0 failures/errors/skips). No repeated debug build, lint, release build, emulator or device test. One nonblocking MigrationTestHelper constructor deprecation warning remains.
- Subsequent owner-requested full verification on OPPO CPH2015: all 46 JVM tests passed. Continuous Home motion initially caused Compose idle timeouts; the owner disabled the three system animation scales for the filtered retry. A Room test-helper serialization ABI mismatch was fixed by constraining the existing serialization core runtime to 1.8.1 for app/test consistency. Both APKs rebuilt successfully; all four task-extension database tests, including migration row preservation and recurrence undo/redo, passed.
- Latest unique phone outcomes across the initial suite and affected-test reruns: 19 passed, 6 failed, 2 skipped. The filtered final run itself was 10 passed / 6 failed (16 tests). Remaining failures: opposite return swipe/tap does not display Home; theme selection remains unselected; Foundation rendering clips a long task title; Home peek/pin text exceeds bounds; checklist test selector matches two checkboxes; recurring-task CRUD test assumes a single row after completion. These assertions require follow-up; no claim that all six are product defects or that the UI suite passes.
- Two SeedPersistence tests were skipped because their explicit process-restart stage was not supplied; that staged fixture writes app settings. No app data was cleared or app uninstalled. Phone animation scales remained at 0 after testing; the owner was asked to restore all three to 1×. No lint or release build was run.
- Final filtered device log: `app/build/outputs/androidTest-results/connected/debug/CPH2015 - 9/testlog/test-results.log`. Report: `app/build/reports/androidTests/connected/debug/index.html`.
- v0.1 remains accepted for personal daily use by the owner. Its formal verification deferrals remain accepted; the unsigned build is not a public release.

## Phase tracker

| Phase | Name | Status | Date | Notes |
|-------|------|--------|------|-------|
| 0 | Project initialization | Done | 2026-10-05 | APK builds, installs and launches on a physical phone. |
| 1 | Theme and navigation shell | Done | 2026-10-06 | Navigation, visual/font-scale and reduced-motion checks pass; theme restart verified in Phase 2. Full TalkBack release walkthrough remains deferred by user. |
| 2 | Room database | Done | 2026-10-06 | Schema exported, JVM invariants and phone DAO checks pass; debug seed/settings survive process death. |
| 3 | Task CRUD | Done | 2026-10-06 | User approved Tasks usability; all available local gates pass; device-only checks deferred. |
| 4 | Home visualization | Done | 2026-10-06 | Implemented; direct testing was intentionally deferred and accepted by the owner for personal daily use. |
| 5 | Timeline | Done | 2026-10-07 | Implemented; recorded focused tests remain evidence for their scope; broader visual/accessibility checks are deferred by owner acceptance. |
| 6 | Polish and v0.1 release | Done | 2026-10-08 | Owner accepted v0.1 for personal daily use; formal checks remain deferred and the local release build is unsigned, not public. |

Status values: Not started, In progress, Done, Blocked.

## Phase 6 report: Polish and release preparation — 2026-10-08

### Backup policy follow-up — 2026-10-08
- Excluded app-private and device-protected data from cloud backup and device transfer. `allowBackup=false` alone is not sufficient on some Android 12+ OEM devices; legacy rules also cover Android 11 and earlier.
- Validation: `:app:processDebugResources :app:processReleaseMainManifest` and `:app:processReleaseResources --offline --console plain` passed; assertions confirmed debug/release packaged manifests retain `allowBackup=false` and both backup-rule references. XML policy assertion passed. No device-transfer runtime check was run; `adb devices -l` found no connected device, and `keystore.properties` is absent.

### What changed
- The user explicitly authorized proceeding into the next phase. This permits Phase 6 implementation without marking the outstanding Phase 4/5 acceptance checks complete.
- Settings now saves the display name, theme and navigation style through the existing DataStore. Blank names show an inline error; failed writes preserve the input and offer snackbar retry or dismissal. Failures are tracked per preference: a new write invalidates only that preference's stale retry/failure, unrelated failures stay pending, and dismissing one failure promotes the next oldest pending failure.
- Compass-only mode disables spatial Home and return-handle swipes and hides return swipe instructions. The compass and Home buttons remain available. Home edge arrows fade to 55% after the first three fresh activity launches; configuration recreation does not increment the counter.
- Added a palette-matched adaptive icon with a monochrome layer and native Android 12+ splash styling. Vault retains its honest disconnected state in an illustrated panel; Sync remains an honest static placeholder.
- Release builds enable R8/resource shrinking and optional local signing. An absent keystore produces an explicitly unsigned artifact. No release key is generated; unsigned APKs cannot be installed until signed.

### Files changed
- `MainActivity.kt`, `data/prefs/SettingsDataStore.kt`, `ui/RikoHub.kt`: launch counting, shared settings state and write/retry handling.
- `navigation/RikoNavHost.kt`, `ui/compass/SpatialNavLayer.kt`, `ui/components/ScreenFrame.kt`: navigation preference and truthful return controls.
- `ui/settings/SettingsScreen.kt`, `ui/home/HomeScreen.kt`, `ui/home/HomeContent.kt`, `res/values/settings_strings.xml`: editable settings and hint fading. Removed the unused `ui/theme/ThemePreference.kt` wrapper.
- `ui/vault/VaultScreen.kt`: existing cloud illustration and placeholder copy grouped in a panel.
- `AndroidManifest.xml`, launcher drawables/mipmap resources, color/style resources: icon and splash.
- `app/build.gradle.kts`, `app/proguard-rules.pro`, `README.md`: optimized release configuration and private signing instructions.
- `androidTest/.../ui/settings/SettingsTest.kt`: isolated preference storage, validation, hint thresholds, enabled/disabled swipes and compass fallback. `test/.../ui/SettingsWriteTrackerTest.kt`: pending failure ordering, per-preference invalidation, guarded stale retry, success clearing and ID-scoped dismissal.

### Architecture decisions
- Reuse the existing DataStore, Compose controls, snackbar host, navigation layer and Android platform splash. No new dependency, schema, networking or later-version feature.
- A navigation composition local controls every shared spatial gesture surface. Settings retains the simple state/callback interface rather than adding another ViewModel layer.
- Tests inject a DataStore backed by a disposable cache file and a cancellable scope. Merely using the instrumentation Context would not reliably isolate the singleton preferences delegate.
- Accessibility overrides the visual hint-opacity instruction: fading whole hint buttons yields 1.59:1 light / 3.42:1 dark text contrast at 55%. Hint labels now use full-opacity `onSurface`; only arrows fade. This follows the specification priority of accessibility over visual intent.
- The icon follows design-token section 11: ivory background, lavender organic shape, ink compass and small coral dot within the safe zone. Android splash backgrounds follow system light/dark resources.

### Tests / build result
- CONFIRMED: final local build on 2026-10-08 completed successfully in 2m 14s (`assembleDebug`, four selected JVM test classes, `assembleDebugAndroidTest`, `assembleRelease`); 122 tasks, 12 executed / 110 up-to-date. The release APK is unsigned (2,136,529 bytes, about 2.04 MiB).
- CONFIRMED: 14 selected JVM tests pass with 0 failures, errors or skips: TaskGrouping (3), TimelineBuilder (6), WriteRules (3), SettingsWriteTracker (2).
- CONFIRMED: lint completed in 3m 16s with 0 errors and 11 warnings. Warnings: four PluralsCandidate, three UnusedResources, one AndroidGradlePluginVersion, one NewerVersionAvailable, one DataExtractionRules and one ObsoleteSdkInt. Dependency upgrades and warning cleanup are deferred; no warning-driven code changes were made.
- CONFIRMED: debug and test APKs installed on OPPO CPH2015; `SettingsTest` passes its isolated preference storage/reopen, validation, hint-threshold and gesture checks. Full app relaunch and persistence of personal settings are not established by this isolated fixture. The first `TimelineLayoutTest` attempt stopped at `performScrollTo` because a lazy node was offscreen. After correcting test scrolling, the next run reached the empty-state check but failed on stale hardcoded copy (`Minggu ini masih lega.`; current string: `Ruang untuk cerita minggu ini.`). Normal and dense overflow inspections passed; empty-state overflow inspection was not reached. The resource-based assertion is corrected and the test APK rebuilt successfully in 1m 35s (52 tasks, 5 executed / 47 up-to-date). The phone initially disconnected before installation (`device ... not found`). After reconnection, the corrected APK installed successfully and the final layout-only run passed: `OK (1 test)` in 10.241 seconds. Normal dark, dense 200% light and empty 200% light overflow checks passed. Log: `app/build/reports/verification/phase6-layout-final.log`. No build or passing test was repeated in this final rerun.
- An earlier overlapping Gradle execution failed during binary result renaming. No code failure was inferred from that operational error; all results above come from the subsequent clean final build.
- CONFIRMED: debug and release manifests contain no `INTERNET` permission and set `allowBackup=false`. Android 12+ backup/device-transfer rules received static review and focused manifest/resource validation; runtime transfer behavior remains unverified.
- No screenshots were taken during this verification pass; no `/tmp` scratch was used. Task scratch directories `.work-phase6/` and `.work-timeline/` were removed; phone test logs remain in the normal `app/build/reports/verification/` output directory.
- These results document only the specific builds and checks performed; the owner has accepted remaining noncritical formal checks as deferred for personal daily use.
- Initial sandbox Gradle launch failed with `Could not determine a usable wildcard IP for this machine.` The build was rerun with the required sandbox escalation.
- First unrestricted build exposed a Gradle Kotlin DSL `Properties` import error; fixed with an explicit import before rerunning.
- Independent review found compass-only return instructions still advertised disabled swipes; corrected before verification.
- Launcher foreground was rendered from its Android vector paths and visually inspected. This is not a launcher/splash device test.

### Known limitations and acceptance checklist disposition
- **Functional and persistence:** previously recorded task/event tests remain evidence for their original scope. Isolated SettingsTest preference reopen and gesture checks pass; full app relaunch and personal-store persistence are not established. Actual task/event restart, process-death, Home quick-add timing and all-screen completion checks are not reclassified from source review alone.
- **Phase 5 layout:** historical logcat recorded two interaction passes and one layout overflow failure (`Kembali ke Hari Ini`). In the current focused test, the first attempt stopped because the target lazy node was offscreen; the next reached but failed a stale hardcoded empty-state string assertion. After correcting the scroll and resource-based assertion, the final focused phone test passed all three normal/dense/empty states. This proves the tested layout assertions on this phone, not all-width visual quality or TalkBack acceptance.
- **Offline and privacy:** no network integration is added. Airplane-mode operation and runtime network monitoring still need a device check. Signing secrets remain local and ignored; no release key is generated.
- **Visual and accessibility:** all-width screenshots (360/393/412/600dp), 130/200% font scaling, full TalkBack/focus/touch-target review and screen-by-screen contrast remain open. Hint arrows fade while labels retain full-contrast ink; rendered screen-wide contrast still needs verification.
- **UX state audit:** Home, Tasks and Timeline contain empty/loading/error/retry paths. Settings has validation and save retry. Vault/Sync have no remote operation, so loading/network-error states are not applicable; their disconnected copy is intentional. Fresh-user navigation discovery remains manual.
- **Performance:** cold start below 1.5 s, seven-bubble frame pacing, 500-task scrolling and repeated-navigation memory checks require a healthy runtime. No performance claim is inferred from compilation.
- **Code quality and release:** the selected JVM tests, debug/test/release builds, lint and release-manifest inspection are confirmed above. A clean-checkout build, signed release install/update preservation and R8 CRUD smoke test are separate gates. Android 12 backup/device-transfer runtime behavior remains unverified.
- **Owner acceptance (2026-10-08):** owner reports that the app launches and operates, primary features work, and UX is satisfactory; v0.1 is accepted for personal daily use. Prior manual deferrals and noncritical formal verification are accepted as deferred. This acceptance does not claim automated, accessibility, performance, signed-release or public-release verification, and does not individually accept undocumented task extensions in the dirty worktree.
- The local release artifact remains unsigned and is not a public release. Remaining checks below retain their recorded status and evidence.

### Deferred (TODO notes added)
- TODO(v0.1): remaining formal checks above are accepted as deferred for personal daily use; revisit before any signed or public release.
- TODO(v0.2): deliberate manual export/import. Settings continues to warn that data is stored only on this device.
- Vault/Drive v0.3 and Sync/transfer later-version TODOs remain unchanged.

### Suggested next step
- No further v0.1 work is required for the accepted personal-use scope. Revisit the remaining signed-release and visual/accessibility gates before any public release.

## Timeline layout refinement — 2026-10-07

- User authorized rearranging the messy Timeline and efficient screenshot-based verification. The initial phone screenshot showed an oversized return/header area, empty-day pills, a clipped today label and a path resembling a second card border.
- Reused the return handle with a Timeline-only compact option. Consolidated week controls, replaced day pills with quiet separators and a today badge, moved the path into a narrow gutter, removed heavy card outlines, grouped kind/category metadata and widened the Add action.
- Preserved Room data, chronology, editors, Undo and navigation. Dense/large-text paths remain straight; ordinary stops retain small alternating insets. Updated the UX specification to record the user-approved presentation refinement.
- Added an isolated layout check for normal dark, dense 200% light and empty 200% states. Screenshot writing is explicit opt-in to private app cache, never the gallery; the focused check passed on OPPO CPH2015 in Phase 6 verification without screenshot output. Task scratch files were removed.

## Phase 5 report: Timeline — 2026-10-07

### What changed
- User explicitly authorized proceeding to Phase 5 without direct Phase 4 testing. This overrides the earlier stop-after-Phase-4 instruction, not the outstanding acceptance gates. Phase 3 stays Done with its recorded automated results and user usability approval; Phase 4 stays In progress.
- Replaced Timeline’s placeholder with a Monday–Sunday narrative path combining events, deadlines and completed tasks. Added week navigation, positioning at today, day labels, muted past stops, today borders, empty/error/loading states and straight paths for dense days or large text.
- Added validated event create/edit/delete with native date/time pickers and accessible-timeout undo; task stops reuse the existing task editor. NOTE events retain their specific kind label.

### Files changed
- `domain/logic/TimelineBuilder.kt` and its JVM test: deterministic day grouping, ordering and half-open event clipping.
- `ui/timeline/TimelineScreen.kt`, `TimelineContent.kt`, `TimelineStopRow.kt`, `TimelineViewModel.kt`: presentation, Canvas path, lifecycle-owned Room state and write effects.
- `ui/components/EventEditorSheet.kt`, Timeline string resources and arrow vectors: event input, Indonesian copy and accessible navigation.
- `androidTest/.../ui/timeline/TimelineTest.kt`: event CRUD/undo, task stop, week navigation, stale retry and 200% validation scenarios.
- `README.md`, `docs/STATUS.md`, `docs/CHANGELOG.md`: usage, actual verification and visible unreleased changes.

### Architecture decisions
- Reuse concrete repositories, Room v1, injected Clock, existing task editor and design tokens. No new dependencies, schema, permissions or later-version integrations.
- Pure builder emits seven local dates, orders by time/kind/entity ID, and clips spanning events at each local midnight, including DST. A task can have separate deadline and completion stops.
- Visible lifecycle owns Room subscriptions and minute/midnight refresh; stopping cancels the loop. Canvas supplies decoration behind ordinary accessible, scrollable panels.
- Retry callbacks retain the failed entity/draft. A write-generation guard invalidates older retries when a newer write starts, preventing an old retry from overwriting a newer successful edit. Each write clears the previous snackbar; latest-effect collection prevents an old indefinite failure from delaying a later deletion’s Undo.

### Tests / build result
- CONFIRMED: `assembleDebug :app:testDebugUnitTest --tests id.riko.hub.domain.logic.TimelineBuilderTest assembleDebugAndroidTest --offline --console plain --no-daemon --max-workers=1`: `BUILD SUCCESSFUL in 6m 8s`; 75 tasks, 27 executed / 48 up-to-date. Bundled Android Studio JDK; Kotlin compiler in-process.
- CONFIRMED: final rebuild after snackbar/retry changes also succeeded on 2026-10-07: 75 tasks, 15 executed / 60 up-to-date. Gradle reported 9h 54m 6s elapsed across the interrupted overnight session; this is not a benchmark.
- CONFIRMED: TimelineBuilder JUnit XML reports 6 tests, 0 failures, 0 errors, 0 skipped. Covers stable ties, week/day boundaries, deleted rows, point/spanning events and a 23-hour DST day.
- CONFIRMED: event/Timeline previews compile, but previews were not rendered. Direct Phase 4 tests were intentionally not run.
- CONFIRMED: OPPO CPH2015 focused `id.riko.hub.ui.timeline.TimelineTest` run: `OK (2 tests)` in 31.642 seconds. Covers event create/edit/delete/undo, task-stop editor, week navigation, retry target protection and invalid-end save validation at local 200% font scale. In-memory Room fixture leaves personal task/event data intact. No screenshots were taken or saved.
- The first phone run failed both shared setup waits before Compose settled. Added `waitForIdle()` and a Timeline-root assertion before checking loaded state. Test APK rebuild passed in 4m 13s (52 tasks, 5 executed); the single focused rerun above passed. No broad test suite was rerun.
- NOT PASSED: `lintDebug --offline --console plain --no-daemon --max-workers=1` reached `:app:lintAnalyzeDebug` but exceeded the 300-second limit (exit 124). No current lint pass or clean report is claimed. A remaining verification daemon was stopped before the final rebuild.
- CONFIRMED: `git diff --check` passes. Scratch/log files used repository-local `.work-phase5/`, removed after checks. No `/tmp/` scratch was used in this pass. APKs and standard Gradle reports remain under `app/build/`.

### Known limitations
- Focused interactions and 200% event-editor validation pass on the phone. Full dense/multi-day visual review, all-content 200% readability, light/dark rendering and TalkBack remain unverified; the targeted tests do not prove those broader gates.
- Unsaved editor drafts survive configuration changes in the ViewModel, not process death.
- Historical status at the time of this Phase 5 report: Phase 5 remained In progress pending runtime acceptance; Phase 6 had not yet started. This was superseded by the subsequent user authorization and Phase 6 work recorded above.

### Deferred (TODO notes added)
- TODO(v0.1): complete lint on a responsive local tooling run; do not treat the timeout as a pass.
- TODO(v0.1): inspect empty/dense/multi-day weeks, light/dark themes and all-content 200% font scale, and complete TalkBack/touch checks. The two focused `TimelineTest` scenarios now pass on the phone.
- TODO(v0.1): optional long-press on an empty path to add an event; the specified Add event button is implemented.
- Home’s existing runtime, gesture, real-device frame-rate/jank and accessibility gates remain pending; no Phase 4 completion is claimed.
- The earlier cable limitation is historical. On 2026-10-07 the OPPO CPH2015 connected, both APK installations succeeded, and the focused phone checks passed after test synchronization was corrected. The actual app also launched successfully; its Timeline UI hierarchy showed the current week, today, a task deadline and Add event button.

### Suggested next step
- Resolve the lint timeout and complete the remaining focused visual/accessibility review. Keep Home checks deferred under the user’s skip instruction. Review before authorizing Phase 6; do not repeat the passing Timeline tests without relevant changes.

## Phase 4 report: Home visualization — 2026-10-06

### What changed
- Replaced the Home placeholder with greeting/date, today's progress, next obligation, ranked organic task bubbles, focus pin, calm/normal/dense modes, overflow and completed dots.
- Added peek actions, quick add through the existing editor, completion/reflow/entry motion, haptic completion and undo that restores the previous status.
- Added deterministic domain logic, lifecycle-controlled Room/time state, Indonesian descriptions/actions and large-text fallback. No Timeline implementation or later-version feature.

### Files changed
- `domain/logic/UrgencyCalculator.kt`, `FocusSelector.kt`, `BubbleLayoutPlanner.kt`, `HomeSnapshot.kt`: pure Home engine and time/progress projection.
- `ui/home/HomeViewModel.kt`, `HomeScreen.kt`, `HomeContent.kt`, `BubbleField.kt`, `Bubble.kt`, `BubblePeekSheet.kt`: state, Home presentation and interactions.
- `ui/theme/HomeTokens.kt`, `Shape.kt`: Home mode colors, blob curves and motion/size tokens.
- `data/prefs/SettingsDataStore.kt`: atomically clear only the expired pin that was observed, preserving a newer user pin.
- `ui/components/TaskEditorSheet.kt`: optional collapsed quick-add details; Tasks retains its expanded editor.
- `res/values/strings.xml`, debug preview gallery, Home engine/contrast/instrumentation tests: copy and verification support.

### Architecture decisions
- Reuse existing concrete Room repositories, DataStore, injected Clock, manual DI and editor. No dependency, schema or permission changes.
- Read projections use one clock snapshot. Rank/focus/layout/progress/next obligation are pure Kotlin. Progress is a set union so a task due and completed today counts once.
- A visible-screen subscription ticks at most 60 seconds apart and at the next local midnight, refreshing on every repository/preference change. Lifecycle stop immediately cancels reads/ticks despite StateFlow's 5-second subscriber grace period. Resume reads a fresh clock/zone.
- Drift reads animation state in a graphics layer; ID defines phase/period. Reduced motion and non-RESUMED state disable drift. Pairwise/edge clearance further caps the 2/3dp amplitude so animation cannot violate static overlap/bounds.
- Large text (130% and up) conservatively uses the specified three-task-plus-overflow fallback with vertically staggered large bubbles; the field scrolls. This also applies below 296dp field width to preserve touch targets. At 200%, all fallback bubbles exceed the 72dp minimum. Full titles/priority/deadline remain in TalkBack and peek; truncated bubble titles are intentional.
- Completed tasks briefly retain their prior placement while shrinking/fading/downward motion runs, then leave the field for the receded row (five dots plus count). Undo restores the original status, including IN_PROGRESS.

### Slot coordinate correction
- Original maximum-size template failed: slot 5 bottom was 0.043W outside H=0.95W; overlap 3/5 was about 0.04194W (allowed 0.024W); overlap 0/6 about 0.04175W (allowed 0.0216W).
- Changed only slot 5 `(0.55, 0.94)` → `(0.495, 0.894)` and slot 6 `(0.10, 0.52)` → `(0.09, 0.542)`. Slot maxima, tiers, field ratio and the other five coordinates remain unchanged. The adjustments move the bottom bubble inward and separate the two offending pairs without moving the focus.
- Tests cover all seven slots at maximum diameters, every mode, deterministic layout and simultaneous drift extremes.

### Tests / build result
- Core-only run: `testDebugUnitTest --offline --console plain`, `BUILD SUCCESSFUL in 2m 15s` before final UI additions.
- Final assembled build/test/lint results are pending; this is not yet final acceptance evidence.
- Initial UI compilation identified unsupported `Dp.Zero` / `animateDpOffsetAsState` calls and an ambiguous `LocalLifecycleOwner` import; replaced them with supported dp/Offset animation and an explicit lifecycle owner.
- One intermediate incremental compile overlapped a helper extraction and reported unresolved `largeTextPlan` / `driftAllowance`; verification was restarted against the stable source set.

### Known limitations
- Home has not been visually rendered or manually exercised in this session. Preview code and instrumentation compilation are not runtime verification.
- The temporary API 35 emulator remained offline after startup attempts; instrumentation and screenshot checks are unavailable in this environment.
- Phase 4 remains In progress until its required real-device performance gate and runtime/accessibility checks are completed. No 60fps or jank claim is made.

### Deferred (TODO notes added)
- TODO(v0.1): real-device 60fps/drift performance, gesture feel, TalkBack walkthrough, 100%/130%/200% font scaling and touch ergonomics.
- Physical-device-only verification deferred because no USB cable is currently available.
- TODO(v0.1): run Home and Tasks instrumentation on a healthy emulator/device; review actual light/dark renders at 360/393/412/600dp. Emulator failure is an environment limitation, not a confirmed product defect.

### Suggested next step
- When the USB cable is available, install the built debug/test APKs, run the focused Home/Tasks tests and perform the physical acceptance pass. Subsequently superseded by explicit user authorization to implement Phase 5 without direct Phase 4 testing; Phase 4 acceptance remains pending.

## Phase 3 final verification — 2026-10-06

- User manually tested Tasks, confirmed it is good and usable, and explicitly approved Phase 4.
- CONFIRMED: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --offline --console plain`: `BUILD SUCCESSFUL in 4m 3s`; 84 tasks, 1 executed / 83 up-to-date. Existing JVM results: 14 tests, zero failures/errors. Lint: 0 errors, 16 warnings. App and instrumentation APKs compile.
- Physical-device-only verification deferred because no USB cable is currently available.
- No connected device or default AVD. Attempted an isolated API 35 headless emulator with the installed image: initial userdata storage checks failed; moving its temporary files into ignored project build storage allowed startup, but repeated ADB checks returned `adb: device offline`. Stopped the unhealthy emulator. No instrumentation, screenshot or emulator font-scale result is claimed.
- Phase 3 is Done under the user's explicit hardware exception and manual usability approval. The existing CRUD/restart/200% instrumentation tests remain runnable but unexecuted in this pass. Physical Tasks font-scale and TalkBack follow-up remain deferred, not product failures.
- Phase 3 temporary build/emulator logs were removed during the user-requested cleanup. The historical report below describes earlier attempts; these are the final local results.
- Historical next step was Phase 4 only; subsequently superseded by the user’s explicit Phase 5 authorization.

## Phase 3 report: Task CRUD — 2026-10-06

### What changed
- Replaced the Tasks placeholder with a persistent grouped list, category filters, count summary, collapsed completed bucket, and a thumb-accessible Add action.
- Added a scrolling task editor with title/notes validation, category, native date and optional time pickers, deadline shortcuts, priority and status. Failed saves preserve the draft and offer retry.
- Added checkbox and right-swipe completion, left-swipe/editor soft deletion, and five-second snackbar undo. Accessibility-recommended timeouts extend the undo window.
- Preserved the existing palette, bundled fonts, varied rounded shapes, compass and return gestures. Added empty, delayed loading and retryable read-error states.

### Files changed
- `domain/logic/TaskGrouping.kt` (new): pure time buckets, category filtering and local deadline conversion.
- `ui/tasks/TasksViewModel.kt` (new): lifecycle-owned task state, Room writes, draft state and one-time snackbar effects.
- `ui/tasks/TasksScreen.kt` (modified): lazy grouped list, filters, states and sheet integration.
- `ui/tasks/TaskRow.kt`, `TaskLabels.kt` (new): task rows, accessible completion, swipes and Indonesian date/enum labels.
- `ui/components/TaskEditorSheet.kt`, `TaskDeadlineFields.kt` (new): validated editor and platform date/time controls.
- `ui/components/ScreenFrame.kt` (modified): extracted the existing return handle so Tasks can use a lazy list without nested vertical scrolling.
- `res/values/strings.xml` (modified): Indonesian task copy and accurate placeholder wording.
- `TaskGroupingTest.kt`, `ui/tasks/TaskCrudTest.kt` (new): bucket boundaries and focused UI/ViewModel persistence checks.
- `README.md`, `docs/STATUS.md` (modified): usage and progress notes.

### Architecture decisions
- Reused concrete repositories, native pickers, existing Compose/Lifecycle dependencies, and manual application wiring. No new dependency, schema or network permission.
- ViewModel state is collected with lifecycle awareness; Room reads and clock ticks are shared only while subscribed. The editor draft survives activity recreation in the ViewModel; an unsaved draft is not persisted through process death.
- “Minggu ini” means the next 2–7 local dates, as explicitly defined in the data model, not the remaining calendar week. Completed tasks always go to Selesai; deleted tasks are excluded.
- Date-only deadlines use local 23:59. An elapsed deadline today moves to Terlambat. Recompute every 30 seconds and immediately when the screen resumes after its subscription stops.
- ViewModel checks run against an isolated on-device Room fixture and an injected fixed clock, covering the real write path without adding mock/fake repository layers or test dependencies.
- The shared editor opens expanded in Tasks. Home quick-add integration belongs to Phase 4. Last-used category is retained within the Tasks ViewModel session.

### Tests / build result
- First compilation exposed an incorrect theme function name and recursive inferred return types; both were corrected.
- Initial build: `assembleDebug testDebugUnitTest assembleDebugAndroidTest --offline --console plain`: `BUILD SUCCESSFUL in 3m 33s`; 14 JVM tests, 0 failures/errors.
- The first phone attempt timed out because the display was asleep and lifecycle collection correctly remained stopped. Waking the phone resolved startup; the test host now keeps its own window awake without changing device settings.
- Test selector failures (`Expected exactly '1' node but found '2' nodes`) were corrected by distinguishing the category filter from the editor and capturing the active dialog root.
- The next run reached create/filter/edit and found the status label missing (`could not find any node ... 'Sedang dikerjakan'`). Lint identified the cause: `in_progress` collided with a private Compose resource carrying locale-specific translations. Renamed it to `task_in_progress`, removed obsolete placeholders, and used an Android plural resource for the count summary.
- Initial lint completed with 0 errors / 20 warnings. Final local checks after those fixes passed: assembleDebug, testDebugUnitTest, lintDebug and assembleDebugAndroidTest; 14 JVM tests, zero failures/errors; lint 0 errors / 16 warnings. Phone checks are deferred under the user-approved hardware exception.

### Known limitations
- Full TalkBack walkthrough remains deferred by the user to release review.
- Home quick add/bubbles and Timeline events remain Phases 4 and 5; no later-phase feature was introduced.
- User manual Tasks usability approval is accepted. Physical-device-only verification deferred because no USB cable is currently available. The focused CRUD/restart/font-scale instrumentation could not run on the unhealthy emulator.

### Deferred (TODO notes added)
- `TODO(v0.2)` in Tasks: recurring tasks and subtasks, consistent with the existing roadmap.

### Suggested next step
- Phase 3 is closed with local verification and user manual approval. Proceed to authorized Phase 4; retain deferred device checks for the next cable-connected session.

## Phase 2 report: Room database — 2026-10-06

### What changed
- Added Room version 1 for tasks/events, exact specified indexes, soft deletion, Flow queries and exported schema. All SQL parameters are bound; no destructive fallback.
- Added domain models, enum-safe mappers, concrete repositories and pure validation/timestamp rules. Transactions preserve record identity, completion invariants and monotonic update timestamps, including when the clock moves backward.
- Consolidated the existing theme preference into one `SettingsDataStore`, retaining its `settings` file and `theme_mode` key. Added storage APIs for the other specified settings without adding UI controls.
- Added lazy manual `AppContainer` wiring and an explicitly invoked debug seeder. Normal app launches do not seed; release source contains no seeder.
- Existing screen design and navigation are unchanged. The updated debug APK is installed on the phone.

### Files changed
- `core/time/Clock.kt` and `domain/model/Models.kt`: injectable time source and typed models.
- `domain/logic/WriteRules.kt`: shared title/notes/time/identity validation.
- `data/local/entity/Entities.kt`, `data/local/dao/Daos.kt`, `data/local/RikoDatabase.kt`: tables, indexes and queries.
- `data/mapper/Mappers.kt` and `data/repository/Repositories.kt`: typed reads and transactional writes.
- `data/prefs/SettingsDataStore.kt`, `AppContainer.kt`, `RikoHubApp.kt`, `ui/theme/ThemePreference.kt`: single settings store and application wiring.
- `app/build.gradle.kts` and generated `app/schemas/id.riko.hub.data.local.RikoDatabase/1.json`: schema export configuration and canonical Room output. Room generates the qualified database directory; no schema was edited by hand.
- `src/debug/.../DebugSeeder.kt`, `WriteRulesTest.kt`, `DatabaseTest.kt`, `SeedPersistenceTest.kt`: debug sample data and focused acceptance checks.
- `README.md` and this file: current scope, commands and progress.

### Architecture decisions
- Concrete repositories suffice until a second implementation or ViewModel test fake is needed. Skipped the recommended duplicate domain interfaces and `AppResult` wrapper; write failures are logged and rethrown for future screen handlers to surface.
- Domain settings reuse the original theme enum via a type alias. This preserves the serialized class name used by Phase 1 activity state; no preference migration is needed.
- Public read queries exclude deleted rows. Explicitly named internal `adminFindForWrite`/`adminCount` queries include them only for write transactions, restore and safe debug seeding.
- Query ranges use half-open time bounds; event overlap follows the architecture's `startAt < end` / `COALESCE(endAt, startAt) >= start` rule.
- The user explicitly authorized Phase 2 with the earlier full TalkBack walkthrough deferred. This phase did not repeat the broad navigation/screenshot suite or change phone system settings.

### Tests / build result
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest --offline --console plain`: `BUILD SUCCESSFUL in 1m 3s`; 75 tasks, 17 executed and 58 up-to-date.
- JVM: 11 tests, 0 failures/errors (3 new write-rule checks, 6 spatial checks, 2 contrast checks).
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew lintDebug --offline --console plain`: `BUILD SUCCESSFUL in 3m 7s`; lint 0 errors / 16 existing warnings.
- Generated schema reviewed: version 1, 13 task columns, 12 event columns, 3 task indexes and 2 event indexes, including both unique sync IDs.
- Debug and test APK installations on OPPO CPH2015 (Android 9): `Success`.
- Instrumentation restricted to `id.riko.hub.data.DatabaseTest`: `OK (2 tests)` in 1.304 seconds. Covers insert/edit, completion/reopening, all relevant deleted-row exclusions, restore, enum fallback, and spanning/point events at range boundaries.
- Instrumentation `SeedPersistenceTest#write_debug_seed -e persistenceStage write`: `OK (1 test)` in 1.478 seconds. Seeds once, confirms a repeat does not duplicate records, and writes an alternate theme.
- Ran `adb shell am force-stop id.riko.hub.debug`, then a separate instrumentation process with `SeedPersistenceTest#read_after_process_death -e persistenceStage read`: `OK (1 test)` in 1.265 seconds. Three tasks, one event and the saved theme survive; the original theme is restored in `finally`.
- Restart check uses `phase2-persistence-check.db` in private app storage, leaving the real `riko_hub.db` untouched. Tests with external restart stages skip during ordinary all-tests runs.
- Logs are under ignored `build/verification/phase2-*.log`. No new runtime or test dependency was added.

### Known limitations
- Tasks and events are not connected to screen UI yet; Phase 3 owns task management and Phase 5 owns event editing.
- Release signing/shrinking, full TalkBack review and existing lint warning disposition remain release work. No broad manual accessibility session was repeated.
- Existing project files were initially untracked with no commits. The local baseline records the existing foundation and Phase 2 together so the generated schema has a buildable source checkout.

### Deferred (TODO notes added)
- No new later-version features or TODOs. Existing recurring tasks, reminders, Drive, pairing and transfers remain deferred.
- Repository failures will be surfaced by Phase 3 ViewModels; no unused UI state/error wrapper was introduced in this data-only phase.

### Suggested next step
- Phase 3 task CRUD: wire persistent task lists, editor validation, category filters and undo deletion into the existing Tasks design.

## Phase 1 continuation — 2026-10-06

### What changed
- Preserved the existing screen layouts, colors, fonts, compass ball and navigation design.
- Assigned the bundled Nunito 14sp/20sp style to `bodySmall`, used by return hints. This removes the default Material font and undersized body text.
- Added a rendering check covering all six screens in both themes at 100%, 130% and 200% font scale, with screenshots saved to app-private external storage for review.
- After the user requested stopping further tests and continuing implementation, added local theme persistence with the existing DataStore dependency. Settings now states that the choice is saved locally; a failed write shows an Indonesian error message and preserves the selected theme for the session.

### Files changed
- `ui/theme/Type.kt`: explicit small-body typography.
- `ui/theme/ThemePreference.kt`: local `theme_mode` preference, safe default on read failure, and logged errors.
- `ui/RikoHub.kt`: load the saved theme and save selections without changing the screen design.
- `ui/settings/SettingsScreen.kt` and `res/values/strings.xml`: update the persistence description and remaining TODO.
- `androidTest/.../FoundationRenderingTest.kt`: text bounds, compass labels/click actions, and visual evidence.
- `README.md` and this file: current progress and verification limits.

### Architecture decisions
- Theme persistence is allowed by Phase 1 item 5 when trivial. Reused DataStore already declared by Phase 0; no new dependency, database, repository or ViewModel.
- The theme helper remains with the theme code for this phase. Phase 2 can consolidate it into the broader settings store when that store has actual settings to own. Do not create a second DataStore instance for the same `settings` file.
- Room, task CRUD, Home task bubbles, Timeline events and integrations remain unstarted.

### Tests / build result
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --offline --console plain`: `BUILD SUCCESSFUL in 1m 32s`.
- Final pre-persistence build/lint command: `assembleDebug testDebugUnitTest lintDebug --offline --console plain`: `BUILD SUCCESSFUL in 2m 3s`.
- Fresh JVM rerun: `:app:testDebugUnitTest --rerun --offline --console plain`: `BUILD SUCCESSFUL in 40s`; SpatialMap 6 tests and Contrast 2 tests, 0 failures/errors.
- After adding theme persistence: `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug --offline --console plain`: `BUILD SUCCESSFUL in 48s`; 37 tasks, 15 executed and 22 up-to-date. No further test suite was run, following the user's request.
- The theme-persistence debug APK was installed over the existing phone app with `adb install -r`: `Success`. App data was preserved.
- Phone: OPPO CPH2015, Android 9, 720x1600, density 320 (360dp width). Latest compass build and test APK installed successfully. Launch returned `Status: ok`.
- `adb -s W4BI7L6SHQW879SG shell am instrument -w -r id.riko.hub.debug.test/androidx.test.runner.AndroidJUnitRunner`: `OK (7 tests)` in 77.052 seconds.
- Reduced motion: enabled Animation off through the phone's normal Developer options UI, confirmed `animator_duration_scale=0.0`, then ran `FoundationNavigationTest`: `OK (6 tests)` in 29.633 seconds. Original scale `1.0` was restored and confirmed.
- Reviewed all 36 screenshots covering six screens, both themes and three font scales. Large text wraps and content scrolls; compass stays visible. Evidence is under ignored `build/verification/phone-rendering/`.
- The first rendering assertion failed with `Text clipped: Kembali ke Hari Ini`. Detailed output showed intrinsic width rounded into an integer layout size, not a constrained or visibly clipped label. Replaced the coarse flag with actual line bounds and a one-pixel rounding allowance; the full phone suite then passed.
- An intermediate follow-up run was invalidated by opening Settings while instrumentation was still running (`No compose hierarchies found in the app`). The final reduced-motion run above completed without external UI interaction and passed.

### Known limitations
- The user explicitly stopped further testing before completing the full TalkBack walkthrough. Accessible compass names and click actions pass automated checks; a spoken-feedback walkthrough has not been completed.
- Theme persistence was implemented after those test runs. Its final build/install results are recorded above; do not treat the earlier UI results as a persistence restart test.
- Phone blocks direct system setting writes with `android.permission.WRITE_SETTINGS` and `android.permission.WRITE_SECURE_SETTINGS` errors. Font testing therefore uses Compose density overrides; the phone's original font scale remains 0.9. Reduced motion used the normal Settings UI instead.
- TalkBack remains off; the original Accessibility Menu service remains enabled. No permission-monitoring or security settings were changed.
- Lint's 16 warnings comprise 13 version notices, device-transfer metadata, the deferred app icon, and the unused old `compass` label. No library upgrade was needed.
- The isolated emulator remained unreliable; runtime evidence above comes from the phone. The emulator was stopped.

### Deferred (TODO notes added)
- No new later-version features. Display name and navigation preferences remain Phase 6; theme persistence is now implemented.
- Final TalkBack review and a theme restart smoke check remain open. Phase 1 stays In progress to keep those limitations visible.

### Suggested next step
- Complete the small remaining acceptance checks when testing is wanted again, then proceed to Phase 2 Room persistence. No additional Phase 1 screen redesign is needed.

## Bubbly helper ball — 2026-10-05

- Removed visible "Kompas" title from the floating control. It is now a 56dp icon button with the existing accessible name; tapping still opens the labeled menu.
- Holding blooms four pastel bubbles with spring motion. Dragging nudges the ball and changes its color; the hold overlay shows no destination names or instructions. Reduced motion snaps the bloom and color.
- Debug app and test APKs build; 8 JVM tests pass and lint reports 0 errors, 15 warnings. The latest APK installed on a phone, but USB disconnected before launch and UI retest. Previous navigation build passed all 6 phone UI tests.

## Compass hold shortcut — 2026-10-05

- Hold the compass to show all five destinations, drag at least 28dp toward one, then release to navigate. The highlighted label previews the choice. Holding without moving selects Home; a normal tap still opens the labeled menu.
- Reuses the existing spatial map and directional slide. Adds no dependency or floating service.
- Latest debug app and test APKs build; 8 JVM tests pass and lint passes. Latest APK installed and launched on a physical phone. All 6 navigation/theme UI tests pass there, including the held compass drag.

- The first all-screen swipe test started a horizontal gesture inside the 24dp system Back zone. Starting the test drag inside that protected edge makes the test reflect the documented behavior; all six UI tests then passed.

## Navigation follow-up — 2026-10-05

- A phone user reported difficult Home swipes and return navigation.
- The shared detector claims supported drags before child scrolling and button handling. Home owns horizontal swipes across the page; its central field also owns vertical swipes.
- Return controls are full width, at least 96dp high, and show their direction. Tap remains available. The 64dp swipe threshold and 24dp edge exclusion remain.
- Added phone-test coverage for four reverse swipes and return-button taps. Debug APK and test APK compiled, 8 JVM tests pass, and lint passes.
- The updated app and test APK installed on a physical phone. App launch returned `Status: ok`. ADB disconnected before instrumentation could start, so gesture behavior and visual acceptance remain unverified.

## Decisions log

Record deviations or assumptions. Format:

```
### YYYY-MM-DD - Short title
- Context:
- Decision:
- Alternatives considered:
- Impact on specs (does a doc need updating? ask Miyu):
```

### 2026-10-05 - Foundation scope and local toolchain
- The original ZIP contained 13 Markdown documents. All were read before code and extracted into `docs/`; the ZIP is unchanged.
- The project directory was empty and was not a Git repository. Initialized `feat/rikohub-v0.1-foundation`; no commit was created.
- Use the specified package `id.riko.hub`, minimum API 26, installed stable API 37, installed AGP 9.3.1 and Gradle 9.5.0. Use Android Studio's JDK 25 locally; emit Java/Kotlin 17 bytecode. No global toolchain changes.
- Prefer compatible stable versions already cached locally over upgrading the machine, following the session instruction. KSP 2.3.6 supports AGP built-in Kotlin; no separate Kotlin Android plugin is necessary.
- Only Phase 0 and Phase 1 are authorized. No speculative repository/domain packages or empty classes; add them with their first implementation.
- Phase 0 explicitly requires declaring Room/KSP, DataStore and WorkManager. They are included without implementing a database or worker.
- No connected device, AVD or system image existed at inspection. A temporary command-line tool installation and API 35 emulator image will be used for validation if available.

### Dependency justification
- AGP, Kotlin Compose compiler and KSP: required Android/Compose/Room build tooling; no app runtime size cost.
- Compose BOM, UI, Foundation, Material 3, Activity Compose: required UI and single-activity shell. Alternative XML views conflicts with the spec. These form most of the runtime UI size.
- Navigation Compose: required spatial destination/back-stack host; avoid a custom navigation framework. Adds navigation runtime.
- Lifecycle runtime-compose: lifecycle-aware state collection; avoids manually managing lifecycle observers for future flows. Small runtime dependency.
- Room runtime/ktx/compiler: explicitly declared in Phase 0; database implementation waits for Phase 2. Adds SQLite/Room runtime; compiler is build-only.
- DataStore Preferences: required settings persistence, deferred to its roadmap phase if not needed in the shell. Adds coroutine/file preferences runtime.
- WorkManager: explicitly required in Phase 0; no worker scheduled. Adds background-work runtime and its manifest components.
- JUnit, AndroidX Test runner and Compose UI tests: navigation and accessibility verification; test APK only.
- Compose tooling: previews, debug only. No image, icon, network or dependency-injection libraries.
- Exact transitive size varies by variant; the completed debug APK measures 13.09 MiB. No additional dependency outside the specified stack except Activity Compose, which is necessary to host Compose.

## Dependencies added beyond the approved list

- Activity Compose provides the required ComponentActivity/setContent host. The alternative is manually maintaining that integration; its small runtime cost is part of the measured 13.09 MiB debug APK. Other libraries are from the approved stack.

## Deferred (TODO notes)

| Tag | Where | Description |
|-----|-------|-------------|
| TODO(v0.2) | Settings | Manual export/import of data |
| TODO(v0.2) | Tasks | Recurring tasks, subtasks |
| TODO(v0.2) | Notifications | Deadline reminders |
| TODO(v0.3) | Vault | Google Drive integration |
| TODO(v0.4) | Sync | Laptop companion and device presence |
| TODO(v0.5) | Sync | Transfer, clipboard, sync queue |

## Known limitations

- Runtime acceptance is not complete. The isolated API 35 AOSP emulator displayed `Process system isn’t responding`, then `System UI isn’t responding`. A bounded recovery could not expose even the Phase 0 placeholder. These are Android system dialogs, not an observed app exception. The emulator was stopped.
- The Phase 0 APK installed successfully. `am start` initially timed out; a retry returned `Status: ok`, but UI hierarchy verification still found a system ANR dialog. This is not counted as a clean launch.
- Seven Phase 1 UI tests passed on the physical phone; six navigation cases also passed with reduced motion. Font-scale and light/dark screenshots were reviewed. Phase 2 verifies theme persistence across process death; full TalkBack remains deferred to release review.
- Latest pre-persistence lint has 0 errors and 16 warnings: 13 available-version notices, Android 12 device-transfer metadata, unused old compass label, and absent app icon. Versions deliberately reuse local tooling. Explicit device-transfer exclusions should be addressed before real data is introduced; `allowBackup=false` is already set.
- Theme choice is saved locally with DataStore and verified across process death. Display name and navigation-style UI remain Phase 6.
- Release signing, release shrinking and real-device performance profiling are not verified in this foundation session.
- The tool sandbox had a configuration error involving a read-only visualization `.git` path. Authorized commands were run through escalation; no sandbox settings or unrelated project files were changed.

## Open questions for Miyu

| # | Question | Default if unanswered |
|---|----------|-----------------------|
| 1 | Final package name | `id.riko.hub` |
| 2 | Display name default | "Riko" |
| 3 | Is Riko's mascot artwork to be added in v0.1? | No, deferred |
| 4 | Minimum Android version | 26 |

## Historical phase reports — 2026-10-05

## Phase 1 report: Theme and navigation shell

### What changed
- Added warm ivory and charcoal themes, exact domain colors, Nunito/Fredoka fonts, spacing/shape/motion tokens and a debug preview gallery.
- Added distinct Home, Tasks, Timeline, Vault and Sync placeholders, plus temporary theme settings. All UI text is Indonesian and resource-backed.
- Added a labeled spatial compass, Home swipes with 24dp edge exclusion, return handles, Back-to-Home behavior, directional transitions and a live system reduced-motion observer.
- No task data, database, bubbles, accounts, network integrations or Phase 2 work was implemented.

### Files changed
- `app/src/main/java/id/riko/hub/ui/theme/`: theme tokens and system-motion preference.
- `navigation/`: destinations, pure spatial map and Navigation Compose host.
- `ui/compass/`, `ui/components/`, and six screen packages: shell and honest placeholders.
- `res/values/strings.xml`, bundled fonts/vector resources, and `licenses/`: offline resources and attribution.
- JVM and instrumentation test source sets: seven logic/contrast checks and four UI checks.
- `app/src/debug/.../FoundationGallery.kt`: light/dark and large-text previews.
- Root README and this status file document setup, scope, decisions and verification.

### Architecture decisions
- Single `:app` module, package `id.riko.hub`, SDK 37/minSdk 26, built-in AGP Kotlin, no DI framework.
- Root composable owns temporary theme state. Navigation Compose owns the back stack. SpatialMap has no Android dependencies.
- Real data ownership, repositories, ViewModels and AppContainer wait for Phase 2; no empty layers were added.
- MainActivity is small and owns only activity/window setup. No workers are scheduled.

### Tests / build result
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --console plain`: `BUILD SUCCESSFUL in 10m 7s`; 84 tasks, 58 executed and 26 up-to-date.
- SpatialMap: 5 tests, 0 failures, 0 errors. Contrast: 2 tests, 0 failures, 0 errors.
- Lint: 0 errors, 15 documented warnings. Debug APK: 13.09 MiB.
- Instrumentation test APK and debug preview gallery compile. Four UI tests remain unexecuted because the emulator failed the preceding launch gate.
- Initial vector error: `error: resource attr/colorControlNormal ... not found.` Fixed by removing the AppCompat tint attribute and letting Compose tint the icons. The successful build above includes that fix.
- Actual runtime failures: `Process system isn’t responding`, `System UI isn’t responding`, and `AssertionError: Phase 0 screen not visible; emulator remains unhealthy`.
- Build/runtime logs and the system-ANR screenshot are preserved under ignored `build/verification/`. There is no successful app screenshot or completed visual review.

### Known limitations
- Runtime, visual, font-scale, reduced-motion and TalkBack acceptance remain open. Do not mark the phases Done until those checks run on a healthy device.
- See the Known limitations section above for warning disposition and temporary settings behavior.

### Deferred (TODO notes added)
- Phase 2: Room database and real data ownership.
- Phase 3: task CRUD. Phase 4: Home engine/bubbles. Phase 5: Timeline/events.
- Phase 6: persisted settings, navigation style, hint fading, app icon, release signing and final accessibility checks.
- v0.2+: background work/export/recurrence; v0.3+: Drive; v0.4+: laptop; v0.5+: transfers/clipboard/sync.

### Suggested next step
- First run the existing four UI tests and visual/accessibility checks on a healthy emulator or physical device. Review the foundation before authorizing Phase 2 Room persistence.


## Phase 0 report: Project initialization

### What changed
- Created the single-module Android application and the initial Riko Hub placeholder.

### Files changed
- Gradle Kotlin DSL, version catalog, verified Gradle wrapper, app manifest, Application, Activity, resources, README and .gitignore created.
- All 13 specification Markdown files extracted into docs.

### Architecture decisions
- Built-in AGP Kotlin; Java 17 bytecode, SDK 37, package id.riko.hub, minSdk 26.
- Room/KSP, DataStore and WorkManager declared as explicitly required, without later-phase implementations.
- Two Gradle workers limit memory pressure on this laptop.

### Tests / build result
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest --console plain`: BUILD SUCCESSFUL in 15m 39s, 40 tasks executed.
- Unit test task: NO-SOURCE; the Phase 0 screen has no custom logic.
- Warning: `Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so.`
- Merged manifest has allowBackup=false and no INTERNET permission. WorkManager contributes WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED and FOREGROUND_SERVICE.

### Known limitations
- No device or pre-existing emulator image. Runtime validation is pending an emulator download. The original Phase 0 APK is preserved separately; shell implementation proceeds while that environmental setup completes.

### Deferred (TODO notes added)
- Theme and navigation are Phase 1. Database and data-backed screens remain Phase 2 onward.

### Suggested next step
- Validate the preserved APK on the emulator and finish the authorized Phase 1 shell.

## Phase 1 implementation decisions

- Use `rememberSaveable` for the temporary theme choice; do not bring Phase 2 preferences implementation into the shell. Theme persistence, display name, navigation-style preference and launch-count hint fading remain Phase 6.
- Navigation Compose owns the back stack. SpatialMap is pure Kotlin. All destinations use one navigation callback with single-top/pop-to-Home behavior.
- Home's field owns vertical swipes so the surrounding page can scroll with large text. Horizontal swipes can also start elsewhere on Home. Secondary screen gestures live only on a return handle.
- Edge exclusion uses the original pointer-down position, not the position after touch slop. Short and ambiguous diagonal drags do not navigate.
- Reduced motion observes the system animator scale live; settings transitions always fade because Settings is outside the spatial map.
- Nunito and Fredoka are bundled as static font instances, with OFL licenses. Three local rounded Material Symbols vector assets are included with Apache license; their AppCompat tint attribute was removed because Compose provides tint.
- Accent containers use primary text, not muted text. The supplied muted token fails 4.5:1 on some accent containers; it is used only on neutral surfaces where contrast passes.
- ViewModels, AppContainer, database models and repositories wait until there is actual data ownership in Phase 2. No empty architectural layers were created.
- Final automated result: 7 tests passed, lint 0 errors / 15 warnings, app and test APKs built. Runtime verification is blocked as documented above.
- Initial vector linking failure was `error: resource attr/colorControlNormal ... not found.` Fixed by removing the three XML tint attributes. A first emulator boot check timed out while Gradle was running; build and emulator checks are now serialized.

### Additional deferred notes

| Tag | Where | Description |
|-----|-------|-------------|
| TODO(v0.1) | ui/home, ui/theme/Shape | Real Home content and blob shapes in Phase 4 |
| TODO(v0.1) | ui/tasks | Task CRUD and buckets in Phase 3 |
| TODO(v0.1) | ui/timeline | Events and weekly timeline in Phase 5 |
| TODO(v0.1) | ui/settings | Persistent preferences and navigation style in Phase 6 |
| TODO(v0.2) | work/DeferredWork | Background cleanup/reminders only when authorized |
