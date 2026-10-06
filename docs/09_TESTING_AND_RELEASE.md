# 09 - Testing and Release

## 1. Test pyramid

| Layer | Tool | What | Required in v0.1 |
|-------|------|------|------------------|
| Pure logic unit tests | JUnit4/5 | `UrgencyCalculator`, `BubbleLayoutPlanner`, `TaskGrouping`, `TimelineBuilder`, validators | REQUIRED |
| ViewModel tests | JUnit + coroutines-test + Turbine (optional) | State transitions with fake repositories and a fake `Clock` | REQUIRED for Home, Tasks |
| DAO / DB tests | AndroidX Test + in-memory Room | Insert, queries, soft delete, restore, ordering, invariants | REQUIRED |
| Migration tests | `MigrationTestHelper` | From v2 onward | REQUIRED when v2 exists |
| Compose UI tests | `createComposeRule` | Create a task via quick add; complete a bubble; navigate via compass; filter tasks | REQUIRED (small set, see §3) |
| Screenshot tests | Paparazzi / Roborazzi | Visual regression | MAY, not in v0.1 |
| Manual device checks | Checklist in `06` | Accessibility, offline, performance | REQUIRED |

## 2. Test rules

- Tests MUST use the injected `Clock`. No test may depend on the real date.
- Tests MUST be deterministic; no sleeps.
- Each bug fix gets a regression test where practical.
- Test names describe behavior: `overdue_high_priority_task_scores_85_and_is_tier_xl`.
- Fakes over mocks. Do not add Mockito/MockK unless a clear need appears (record in `STATUS.md`).

## 3. Minimum Compose UI test set

1. Quick add: open sheet, type title, save, bubble with that title appears on Home.
2. Complete: long press a bubble, progress text changes, bubble recedes.
3. Navigation: tap compass, go to Tasks, system back returns to Home.
4. Tasks: filter to Akademik shows only Akademik tasks.
5. Editor validation: blank title disables Save.

## 4. Commands (reference)

```
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

Astra MUST run at least `testDebugUnitTest` and `assembleDebug` at the end of each phase and report results verbatim. Instrumented tests are run when a device/emulator is available; if not, say so explicitly.

## 5. Build variants

| Variant | applicationId suffix | Purpose | Notes |
|---------|---------------------|---------|-------|
| `debug` | `.debug` | Daily development | Debug seeder enabled; may show a small "DEBUG" marker in Settings |
| `release` | none | Real use | Seeder excluded; `minifyEnabled true` once stable with R8 rules verified (Phase 6); `shrinkResources true` |

Both variants can be installed side by side because of the suffix.

## 6. Versioning

- `versionName`: semantic-ish, `0.1.0`, `0.1.1`, `0.2.0`.
- `versionCode`: integer, increment on every distributed build. Formula: `major*10000 + minor*100 + patch` (0.1.0 = 100).
- Tag releases in git: `v0.1.0`.

## 7. Signing

- Create a local release keystore (Riko/Miyu does this once; Astra documents the exact `keytool` command in the app README but never generates or commits the keystore).
- Signing config reads from `keystore.properties` (git-ignored): `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- If no properties file exists, the release build falls back to unsigned output and prints a clear message.
- Back up the keystore privately. Losing it prevents updating the installed app.

## 8. Distribution (personal)

This is a single-user private app. Distribution is by direct APK install.

1. Build `assembleRelease` (signed).
2. Copy `app-release.apk` to the phone (USB, Google Drive, or local share).
3. Enable "Install unknown apps" for the file manager, install, then disable it again.
4. Updates: install the new APK over the old one (same signing key, higher `versionCode`).

Optional (later): Play Console internal testing track; out of scope for v0.1.

## 9. Pre-release checklist

- [ ] All items in `06_ACCEPTANCE_CRITERIA.md` resolved
- [ ] `versionName` and `versionCode` bumped
- [ ] `CHANGELOG.md` updated
- [ ] Release APK installed on the real phone, fresh install, airplane mode test
- [ ] Update-install test (older build, then newer build) preserves data
- [ ] Database schema file for the version committed
- [ ] Manifest permissions reviewed (none dangerous; no `INTERNET` in v0.1)
- [ ] ProGuard/R8 build smoke-tested (open every screen, create/edit/delete a task and event)

## 10. Performance checks (manual)

| Check | Target |
|-------|--------|
| Cold start | < 1.5 s to interactive Home on a mid-range phone |
| Home with 7 bubbles drifting | no visible jank; no layout recomposition of the whole screen per frame (drift animates via graphicsLayer/offset lambdas, not state in composition) |
| Tasks with 500 rows | smooth scroll (use stable keys in LazyColumn) |
| APK size | < 25 MB |
| Memory | no leak across 20 navigations |

Compose performance guidance for Astra:
- Use `key(task.id)` in lists.
- Drive drift with `graphicsLayer { translationX = ... }` reading animation state inside the lambda.
- Keep UiState immutable and stable (`@Immutable` or stable collections).

## 11. Backup and data safety (v0.1)

- No automatic cloud backup (`allowBackup=false`).
- A manual export/import is NOT in v0.1; record as `TODO(v0.2)` and warn in Settings: "Data tersimpan hanya di perangkat ini."
