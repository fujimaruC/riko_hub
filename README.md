# Riko Hub

A private, offline campus and life control center for Riko. Specifications live in [docs](docs/README.md).

## Build

Open this directory in Android Studio. Use its bundled JDK (25 on this laptop), SDK platform 37 and build tools 36.0.0. Set `sdk.dir` in ignored `local.properties` or set `ANDROID_HOME`.

```sh
export JAVA_HOME=/opt/android-studio/jbr
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest # requires an emulator or device
```

The default system Java 26 is not used for validation. No machine-wide Java or SDK upgrades are required.

Debug package: `id.riko.hub.debug`. Release package: `id.riko.hub`. Minimum Android: 8.0 (API 26).

Phases 0–4 are implemented; Phase 4 runtime and physical-device acceptance remain pending. Tasks supports local CRUD, category filters, deadline buckets, completion and undo deletion. Home adds ranked urgency bubbles, daily focus pinning, progress, next obligation, quick add and completion undo. Timeline event UI remains Phase 5. Release signing and shrinker verification belong to Phase 6; current release builds are unsigned and unminified.

## Foundation navigation

- Home is the center. Swipe its main panel up for Tasks, down for Timeline, left for Vault, and right for Sync.
- Horizontal drags beginning within 24dp of either edge are ignored by app navigation.
- Tap the icon-only compass ball for its labeled menu. Hold it to bloom soft bubbles, drag 28dp toward a destination, and release to move there. No labels appear during the hold. Home hints also work as buttons.
- Back returns to Home. Neighbor screens also have a full-width, 96dp return handle with a direction hint. Tap it or swipe in the indicated direction; scrolling the content does not navigate.
- Settings offers system/light/dark themes saved locally with DataStore. Display name and navigation-style preferences belong to Phase 6.
- System animator scale 0 switches navigation to fades. Fonts and icons are bundled; runtime needs no internet.

## Verification

The foundation has eight JVM checks for spatial mapping and text contrast, plus seven Compose instrumentation tests for compass menu and hold navigation, four-way swipes, edge exclusion, return-handle swipes/taps, theme restoration, and text layout at 100%, 130% and 200% font scale. Preview gallery code is in the debug source set.

Build APKs first, then run emulator checks separately on memory-constrained machines. The verification emulator is an isolated API 35 AOSP profile under `~/.cache/rikohub/avd`; it does not contain personal app data. See `docs/STATUS.md` for actual results and remaining manual checks.

Phase 2 verification on 2026-10-06: debug app/test APKs build, all 11 JVM tests pass, and lint has 0 errors / 16 existing warnings. Two focused database tests pass on the phone. A separate debug seed/write, force-stop, and read check proves task/event data and theme preferences survive process death; the original theme is restored afterward. See `docs/STATUS.md` for exact commands and results.

Earlier Phase 1 checks passed seven phone UI tests and six reduced-motion navigation tests, with 36 screenshots reviewed across themes/font scales. The full TalkBack walkthrough remains deferred to release review at the user's request.

## Persistence foundation

`AppContainer` lazily owns `riko_hub.db`, task/event repositories, the injected clock and `SettingsDataStore`. Public queries return domain models and exclude deleted rows. Writes validate titles/notes and use Room transactions. The generated v1 schema is in `app/schemas/id.riko.hub.data.local.RikoDatabase/1.json`; never edit it manually or enable destructive release migrations.

`DebugSeeder` exists only in debug source and runs only when explicitly requested. It does not seed normal launches. Restart checks use a separate private fixture database so they do not populate the real app database. Run the database checks with `-e class id.riko.hub.data.DatabaseTest`; the two restart stages require `SeedPersistenceTest#write_debug_seed -e persistenceStage write`, then force-stop, then `SeedPersistenceTest#read_after_process_death -e persistenceStage read` on the test runner. The staged checks skip during ordinary all-tests runs.

Validation added command-line tools and an API 35 AOSP system image to the user-owned SDK, plus an isolated AVD/cache. Existing Java defaults and Android platforms were not upgraded. The test emulator has been stopped.

## Task management

Open Tugas and tap Tambah. The editor supports category, a date with optional time, priority, status and notes. Save requires a trimmed title of 1–120 characters and notes no longer than 2000 characters. Date-only deadlines use local 23:59. Swipe right to complete, swipe left to delete, or use the checkbox and editor buttons. Deletion offers a five-second undo, extended when Android accessibility settings recommend it.

The done bucket starts collapsed. “Minggu ini” follows the data specification: the next 2–7 local dates, including across Sunday. Lists refresh their time buckets every 30 seconds while observed. The editor keeps its draft on write failure and activity recreation. Home quick add is integrated in Phase 4.

Focused Phase 3 phone checks use a separate `phase3-ui-check.db`, preserving the real task database. Run the test runner with `-e class id.riko.hub.ui.tasks.TaskCrudTest`; see `docs/STATUS.md` for results.

## Home visualization

Tap a bubble for details, start/edit/complete actions or a daily focus pin. Long press completes directly; Urungkan restores the previous status. Tap the focus bubble’s outer ring to pin/unpin it. Extra tasks open Tugas through the overflow bubble. Large text uses three large task bubbles plus overflow in a scrolling field.

Home uses Room flows and the injected clock, refreshing at least once per minute and at local midnight while the screen is started. Drift runs only while resumed and stops with system reduced motion. Home engine tests cover urgency, ranking, pins, modes, overflow, layout bounds/overlap, drift clearance, progress and rollover.

`id.riko.hub.ui.home.HomeTest` contains the focused Home UI/ViewModel checks. No physical-device result is claimed for Phase 4: the USB cable is unavailable. The local emulator startup attempt stayed offline; instrumentation and actual rendered review are pending a healthy runtime. See `docs/STATUS.md` for the current build results and exact deferred gates.
