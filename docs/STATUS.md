# STATUS - Riko Hub

Maintained by the implementing agent (Astra). Update at the end of every phase. Newest entries at the top of each section.

## Current state

| Item | Value |
|------|-------|
| Version in development | 0.1.0 |
| Current phase | Phase 2 complete; Phase 3 task CRUD next |
| Last successful build | Phase 2: assembleDebug + assembleDebugAndroidTest, 2026-10-06; BUILD SUCCESSFUL in 1m 3s |
| Last test run | 11 JVM tests passed; 2 database phone tests passed; seed write/read after force-stop passed; lint 0 errors / 16 warnings |
| Blockers | None for Phase 2; full TalkBack walkthrough remains deferred to release review |

## Phase tracker

| Phase | Name | Status | Date | Notes |
|-------|------|--------|------|-------|
| 0 | Project initialization | Done | 2026-10-05 | APK builds, installs and launches on a physical phone. |
| 1 | Theme and navigation shell | Done | 2026-10-06 | Navigation, visual/font-scale and reduced-motion checks pass; theme restart verified in Phase 2. Full TalkBack release walkthrough remains deferred by user. |
| 2 | Room database | Done | 2026-10-06 | Schema exported, JVM invariants and phone DAO checks pass; debug seed/settings survive process death. |
| 3 | Task CRUD | Not started | | |
| 4 | Home visualization | Not started | | |
| 5 | Timeline | Not started | | |
| 6 | Polish and v0.1 release | Not started | | |

Status values: Not started, In progress, Done, Blocked.

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
