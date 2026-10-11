# STATUS - Riko Hub

Maintained by the implementing agent (Astra). Update at the end of every phase. Newest entries at the top of each section.

## Current state

| Item | Value |
|------|-------|
| Version in development | 0.4.0 (version code 400) |
| Current phase | Laptop Companion and interactive `rikohub` CLI source implementation on feature/v04-laptop-companion; owner verification and acceptance remain pending. v0.2 is closed; v0.3 live Drive QA is separately tracked. |
| v0.4 execution evidence | None: no builds, lint/static-analysis suites, automated tests, service startup or device verification executed. Historical v0.3 evidence below does not verify v0.4. |
| Last successful build | Owner-reported BUILD SUCCESSFUL in 7m 53s (2026-10-11), 44 tasks. Fresh v0.3 debug APK: 16,948,362 bytes, SHA-256 50ba882cdccae7b89073e86636b452e9c103c10e2c55d794de34cf5e7a8e7d5a. Not installed in this mission. Future builds are owner-controlled. |
| Last test run | Fresh reports: 11 focused Vault UI/state JVM tests passed, 0 failures/errors/skips. Ten Drive API tests compiled but have no execution report. Historical v0.2 evidence is retained below. |
| Release status | Unsigned local build; not a public release |
| Owner acceptance | v0.2 approved and closed. v0.3 implementation checkpointed at 44d6c67, with live Google QA separately pending. v0.4 requires owner verification and acceptance. |

## v0.4 CLI dashboard — 2026-10-12

- Added the user-level `rikohub` command source, interactive terminal dashboard, foreground start flow, status/devices/doctor views, and editable-install packaging entry point. Read-only views avoid creating or changing the Companion data directory; listener configuration accepts private IPv4 only and requires explicit LAN confirmation.
- Added CLI/storage test source and Arch Linux installation plus owner acceptance steps. Riko must run installation, tests, and runtime checks; none were executed for this phase. Existing Android pairing remains in the foreground Companion flow.
- Remote transport and background stop are not implemented. `rikohub start` keeps the server console in the foreground so approvals and revocation remain usable; an active process is detected through its data-directory lock.

## v0.4 — Laptop Companion implementation — 2026-10-11

- Created `feature/v04-laptop-companion` at protected v0.3 checkpoint `44d6c67`, preserving the untracked `gradle/gradle-daemon-jvm.properties`. No reset, clean, stash, commit, push or separate Claude-worktree change occurred. v0.2 stays accepted; outstanding v0.3 Google Drive QA is independently tracked and was not revisited.
- Added a real foreground Linux Python companion: explicit RFC1918 IPv4 bind, DNS-SD `_rikohub._tcp.local.`, HTTPS JSON endpoints, a persistent P-256 certificate/laptop UUID, console pairing approval, trusted-phone status and revocation, and clean stop/EOF/Ctrl-C/SIGTERM handling. No root or autostart service. Python dependencies are limited to cryptography and zeroconf; no Android dependency was added.
- Replaced the existing Sync placeholder with an Indonesian Laptop Companion UI on the unchanged navigation route. Includes nearby laptops, bounded refresh/loading/no-Wi-Fi/empty states, explicit pairing-key entry, approval feedback, saved laptop status, connect/disconnect, confirmed forget, and recovery controls. Existing Riko theme/ScreenFrame/shapes are reused.
- Pairing uses an owner-obtained out-of-band payload containing the laptop UUID, full DER SHA-256 certificate pin and random 256-bit one-time secret. The payload expires after 120 seconds; explicit laptop approval is required within 60 seconds and before expiry. TLS 1.2+ and the exact certificate pin protect initial pairing and reconnect. A fresh random bearer token authenticates the phone; the laptop persists only its SHA-256 hash. No network-provided pin, trust-on-first-use, plaintext fallback or custom encryption algorithm exists.
- Android stores one laptop's minimal trust record encrypted with Android Keystore AES-GCM in private preferences. Linux stores at most eight trusted phones and its identity in a process-locked owner-only directory, with atomic/fsynced writes and 0600 files. Pairing secrets and tokens are excluded from diagnostic logs. The explicit local `pair` command displays its temporary payload to the operator.
- Discovery and connection monitoring are foreground-only. Android uses the API-26-compatible NSD resolve flow, bounded scans/serialized resolution, Wi-Fi network binding, callback ownership checks and bounded HTTPS response reads. Heartbeats are every 15 seconds with capped network backoff; connected requires a validated protocol/peer/capability/status response. Laptop presence expires after 45 seconds without an authenticated check. Android 17 target-37 local-network permission is requested only on that OS; Android 9 has no new runtime prompt.
- Forget attempts authenticated remote revocation and performs explicit local deletion, with truthful offline/unconfirmed-revocation guidance. Certificate replacement requires re-pairing. Protocol 1 provides presence only; unsupported versions/identities fail closed. No file transfer, clipboard, remote shell/control, task synchronization, Drive synchronization, cloud relay, Room migration or app-data endpoint was introduced.
- Linux request handling is bounded to eight TLS workers, 8 KiB JSON/header limits, a five-second handshake timeout and a ten-second absolute request-receipt deadline independent of the approval wait. Shutdown coordinates socket/worker/timer cleanup and blocks post-stop state mutations.

Files:
- Added `companion/.gitignore`, `requirements.txt`, `README.md`, `riko_companion/{__init__.py,__main__.py,server.py,state.py,storage.py}`, and `tests/test_state.py`.
- Added `app/src/main/java/id/riko/hub/data/remote/device/{CompanionProtocol.kt,CompanionRepository.kt}`.
- Added `app/src/main/java/id/riko/hub/ui/sync/{SyncUiState.kt,SyncViewModel.kt}`; modified `SyncScreen.kt`.
- Added `app/src/main/res/values/companion_strings.xml` and `app/src/test/java/id/riko/hub/data/remote/device/CompanionProtocolTest.kt`.
- Modified `AppContainer.kt` (lazy companion service), `AndroidManifest.xml` (network/multicast/local-network permissions), `app/build.gradle.kts` (0.4.0/code 400), and this status document.
- Added [setup guide](V04_LAPTOP_COMPANION_SETUP.md) and [protocol/security contract](V04_COMPANION_PROTOCOL.md).

Source review and verification limits:
- Confirmed branch `feature/v04-laptop-companion` and HEAD `44d6c67`. Compared 189 protected baseline files: no unexpected existing-file changes; the untracked Gradle JVM criteria file is byte-identical. Parsed the manifest and companion strings XML, resolved all 24 companion value-resource references, and inspected the existing Sync navigation route, unchanged min/target SDK and cleartext/backup restrictions.
- Independent source review corrected bounded Linux socket/shutdown handling and Android callback returns, stale trust reads, cancellation cleanup and reset/forget completion races. Final focused rechecks confirmed the reported corrections in source. Trust loading waits for pairing completion; epoch guards reject stale results, and local deletion plus state publication complete inside a non-cancellable cleanup. Endpoint caches are bounded to 32 entries/90 seconds; transient HTTP errors retain reconnect backoff. No compilation or runtime success follows from these reviews.
- Written but **not executed**: five Kotlin test methods covering payload/version/UUID/hex validation, RFC1918 addresses, capped backoff and asynchronous stale-read rejection during pairing/removal; twelve Python test methods covering one-use/expiry/approval/hash/revocation/persistence/shutdown/header/body bounds plus fake timer/socket/worker cleanup. The Kotlin epoch fixtures do not exercise the complete Android repository/Keystore/lifecycle or a real network.
- No Gradle task, build, APK compilation/packaging, lint, static-analysis suite, JVM/instrumentation/UI/integration test, dependency installation, Python service/test/compile execution, certificate generation, ADB or emulator command occurred. Existing v0.3 build/test artifacts are historical and do not verify v0.4. All build, test and runtime verification execution belongs exclusively to Riko.

Owner handoff:
1. Use [V04_LAPTOP_COMPANION_SETUP.md](V04_LAPTOP_COMPANION_SETUP.md) for exact Linux startup, Android build, Kotlin/Python test commands, security limits and QA checklist.
2. Build and test personally, then install with the existing signing identity while preserving app data. Verify Linux startup/stop, real DNS-SD, Android 9 discovery/lifecycle, initial pairing, invalid/expired/replayed key rejection and wrong-pin/protocol rejection.
3. Verify heartbeat status, reconnect after laptop restart and Wi-Fi loss/restoration, permission denial, online/offline forget, laptop-side revocation, interrupted approval and persistence failures. Never record secret payloads/tokens in evidence.
4. No v0.5 work started. v0.4 remains pending owner compilation, tests, network/security/device verification and acceptance.

Known limitations:
- One saved laptop on Android; eight phones on Linux; private IPv4/Wi-Fi only. No IPv6-only/VPN support, Windows implementation, background presence, QR scanner or automatic desktop network-interface migration. A changed laptop LAN address requires restarting with the correct bind address.
- Pairing-key entry is intentionally longer than a PIN; obtain it directly from the local laptop terminal through an owner-controlled trusted method. No companion transfer feature is used to deliver it.
- A pairing approved at the laptop but interrupted before Android saves the token can leave orphan trust; revoke the shown phone UUID on the laptop before retrying. Offline local forget cannot prove remote revocation.
- Foreground timers, Android NSD/OEM behavior, TLS interoperability, Keystore/storage failure recovery, cancellation races, Linux socket shutdown and dependency behavior remain unverified by execution. Source review is not a security certification.

## v0.3 — Google Drive Materials Vault implementation — 2026-10-11

- Owner explicitly accepted and closed v0.2, including the native widget. This mission did not repeat those audits or redesign accepted features. Safely created `feature/v03-materials-vault` from the current `checkpoint/rikohub-phase6` working state, carrying every uncommitted change. No commit, push, reset, cleanup, rebase, installation or personal-data operation occurred.
- Replaced the existing Vault placeholder on its existing navigation route with an Indonesian Materials Vault UI: disconnected/connected account state, typed folder/PDF/image/Google-document rows, current-folder breadcrumbs and back navigation, global debounced name search, manual refresh, paginated file listing, loading/empty/offline/permission/authentication feedback, destination confirmation and explicit upload controls. Shared Riko Hub theme, shapes and return-home navigation are reused. The file list is a bounded lazy list; accumulation caps at 500 items with visible guidance.
- Added real Google Identity Services `AuthorizationClient` integration, pinned to `play-services-auth:21.6.0`. Official documentation and the downloaded SDK JAR confirmed the actual request/result/Picker APIs; its AAR minimum SDK is 23 and the app remains minimum SDK 26. Browsing requests `drive.readonly`; a separate folder Picker request uses only `drive.file`, opt-out of previously granted scopes, explicit consent and folder-selection parameters. No deprecated GoogleSignIn flow, password, OAuth secret, backend or fake connection data is used. Account identity comes from Drive `about.user`, and upload grants are checked against the connected account and selected folder metadata.
- Tokens and connection state stay in memory. Authorization resolution, cancellation, partial grants, expired/revoked tokens, reconnect and disconnect have explicit states. Local disconnect clears state/private cache and requests Google grant revocation; failed remote revocation is reported truthfully. Immutable session/operation ownership prevents late authorization/download/upload callbacks from changing a newer session or opening stale account data.
- Real Drive REST operations run on IO coroutines using platform HTTPS connections and Android JSON: paged root/folder listing, escaped global name search, metadata lookup/account identity, resumable uploads and authenticated media downloads. SAF selection offers common academic document/image formats. Uploads stream bounded 256 KiB chunks with known/unknown-size progress, server acknowledgement checks, cancellation boundaries and genuine completion/failure handling. No large local upload copy or automatic app-data upload is introduced. A content-URI/filename gate rejects obvious databases and Riko Hub backup JSON names; it is not renamed-content detection.
- Google Workspace documents open through validated HTTPS Google viewing links; ordinary binary files use a local viewer with a read-only content URI and ClipData grant, with a Google-link fallback where available. The non-exported FileProvider exposes only `cacheDir/vault-open/`. Private download cache is capped at 32 MiB per file / 64 MiB total, with serialized operations, oldest-file pruning, partial-file cleanup and completed-file promotion. Tokens never enter external URLs.
- Added lazy services to AppContainer and INTERNET permission, restricted cleartext traffic, FileProvider registration/path XML, explicit ViewModel Compose dependency (already in the existing dependency family), and the necessary Google authorization dependency. JSON/coroutine-test additions are test-only. App version metadata is now 0.3.0 / code 300. Room schema, local task/event/checklist/recurrence logic, reminder schedules, backup format/hooks, widget sources/resources/assets and existing uncommitted Gradle criteria are unchanged from the mission baseline.

Files added:
- `app/src/main/java/id/riko/hub/data/remote/drive/DriveModels.kt`, `DriveApi.kt`, `DriveRepository.kt`, `GoogleDriveAuthorization.kt`.
- `app/src/main/java/id/riko/hub/ui/vault/VaultUiState.kt`, `VaultViewModel.kt`.
- `app/src/main/res/values/vault_strings.xml`; `app/src/main/res/xml/materials_file_paths.xml`.
- `app/src/main/res/drawable/ic_vault_document.xml`, `ic_vault_file.xml`, `ic_vault_folder.xml`, `ic_vault_image.xml`, `ic_vault_pdf.xml`.
- `app/src/test/java/id/riko/hub/data/remote/drive/DriveApiTest.kt`; `app/src/test/java/id/riko/hub/ui/vault/VaultPageTest.kt`, `VaultViewModelTest.kt`.
- `docs/V03_MATERIALS_VAULT_SETUP.md`.

Existing files modified in this mission:
- `app/src/main/java/id/riko/hub/ui/vault/VaultScreen.kt`.
- `app/src/main/java/id/riko/hub/AppContainer.kt`; `app/src/main/AndroidManifest.xml`.
- `app/build.gradle.kts`; `gradle/libs.versions.toml`; `docs/STATUS.md`.
The widget/other dirty files visible in Git status predate this mission and were preserved, not edited again.

Verification actually performed:
- Parsed 44 resource/manifest XML files, resolved 41 Vault source resource references, checked provider authorities/export/read grants/path scope, and compared protected SHA-256 baselines. Final whitespace checks passed. Independent source review closed account/session/cancellation/pagination/cache-budget/upload-cleanup issues and found no remaining material finding in its final focused recheck.
- The first focused build failed on two VaultScreen Kotlin errors; both were corrected. The owner then reported **BUILD SUCCESSFUL in 7m 53s**, 44 tasks (21 executed, 23 up-to-date). Fresh local artifacts confirm a debug APK for `id.riko.hub.debug`, version **0.3.0 / 300**, minimum SDK 26, **16,948,362 bytes**, SHA-256 `50ba882cdccae7b89073e86636b452e9c103c10e2c55d794de34cf5e7a8e7d5a`. Fresh JUnit reports show **11 tests passed, 0 failures/errors/skips**: VaultPageTest (3) and VaultViewModelTest (8). DriveApiTest compiled (10 test methods), but no execution report exists; those data tests are **not claimed as run**. The owner requested that future builds be left to them; no further build was started.
- Focused test source covers query escaping/global-vs-folder search, JSON metadata/page mapping, empty-page pagination, HTTP error classification, unknown/empty upload ranges and acknowledgements, cache budget, selected-material policy, paging cap and stale upload ownership, authorization/account/folder late completion after disconnect, account mismatch, selection cancellation, folder/search navigation and failed connect/reconnect. Tests use isolated fixtures/fakes; no account, Room or personal task data is involved. The ownership helper test does not exercise a full real overlapping transfer/reauthorization sequence.
- No lint, broad regression suite, release build, instrumentation, emulator, ADB/device screenshot, live Google authorization, real upload/download or private document/credential access was performed. Source and compilation evidence do not establish Android 9 runtime or live API success.

Owner setup and acceptance:
1. Follow [V03_MATERIALS_VAULT_SETUP.md](V03_MATERIALS_VAULT_SETUP.md): enable Drive/Picker APIs, configure audience/test users and scopes, register Android package plus existing signing SHA-1. No secret is embedded; Google Console configuration remains external.
2. Install the generated matching debug APK over the existing app, preserving data. Immediately test the disconnected screen, spatial navigation, text scaling and useful configuration/offline errors. After setup, verify consent cancellation/connect/reauthorization/disconnect and account switching.
3. Use disposable academic documents/folders to test paged traversal/search, explicit folder plus SAF selection, progress/cancellation/failure/success and affected-folder refresh, binary/Workspace viewing and missing viewers. Check no stale UI/file opening after disconnect and no local task sync.
4. Run the remaining 10 DriveApiTest cases with `JAVA_HOME=/opt/android-studio/jbr rtk gradlew :app:testDebugUnitTest --tests 'id.riko.hub.data.remote.drive.DriveApiTest' --console plain` (owner-controlled). Record live Google and Android 9 evidence before accepting v0.3. Recommended next work is to resolve any live integration issue found by these checks and close this version's acceptance; no v0.4+ work started.

Known limitations / external blockers:
- Cloud Console registration, restricted read-only scope consent/verification policy, Google Play services/account interaction and device acceptance are owner tasks. v0.3 is **not marked complete** while these remain unverified.
- Foreground ViewModel transfers do not survive process death or persist resumable sessions. A blocking SAF read or in-flight request observes cancellation after completion/timeout; cancellation cannot undo bytes already accepted by Drive. Retry after ambiguous failure can create a duplicate, so inspect Drive first.
- Tokens/account state are not persisted; reconnect uses the supported Google authorization flow and existing grants. Listing/search accumulation caps at 500 entries. Ordinary local opening caps at 32 MiB; larger files need the validated viewing-link fallback.
- No general offline library/marks, folder mappings, Workspace export, shared-drive-specific UI or shortcut expansion is implemented in this mission. These earlier roadmap extras were not part of the owner's current core browsing/search/upload/open scope. Internet errors are handled; genuine offline Drive browsing is not promised.


## Milestone 4 — targeted clock/Miyu/card polish — 2026-10-11

- Stayed on `checkpoint/rikohub-phase6` and preserved the existing dirty working tree. Implemented through GPT-6.1 Sol at medium reasoning as requested. This is a small resource-only refinement of the working widget.
- Compact clock maximum size increased from 52sp to 56sp and its region from 60dp to 64dp. Expanded clock increased from 56sp to 60sp and 68dp to 72dp. Native TextClock formats, minimum autosizing bounds, clock behavior and all IDs are unchanged; actual rendered digit size remains subject to platform autosizing.
- Miyu bounding boxes increased from 56×60dp to 60×64dp compact and from 68×68dp to 72×72dp expanded. She stays vertically centered on the right beside the weekday/date, with the taller clock/date regions placing her slightly lower. Transparency, aspect ratio, all eight assets and independent expression randomization remain intact. Root horizontal padding decreased from 12dp to 10dp, exactly preserving the date column width after mascot enlargement; vertical padding stays 12dp.
- Primary task card padding decreased from 10dp to 8dp horizontally and from 5dp to 3dp vertically. Titles retain 18sp primary / 16sp secondary; metadata, ellipsis, urgency selection and all available task rows remain unchanged. This gives the time/date region more visual weight without shrinking task text. No weather or decoration was added.
- Changed files: `app/src/main/res/layout/task_widget.xml`, `app/src/main/res/layout/task_widget_compact.xml`, `app/src/main/res/values/widget_styles.xml`, and this status document. Provider, presentation/selection logic, PendingIntents, navigation, mutation/import refresh hooks, recurrence, reminders, backup and PNGs are byte-for-byte unchanged from this pass's baseline.
- Verification performed: edited XML parsed, resource references resolved, view ID sets preserved and RemoteViews controls checked. Full current-state baseline hashes verified only the three intended resource files changed before documentation. Static normal-scale two-line primary height estimates are approximately 243.5dp compact and 259.5dp expanded; supported minimum height remains 272dp. Existing two/three-row height thresholds retain approximately 30.5dp/36.5dp estimated slack. These estimates are not rendered-device proof. Final whitespace and scope checks passed.
- No build, lint, test suite, ADB, installation or device screenshot was performed in this polish pass. The preceding successful APK/build recorded below does **not** contain these latest resource changes. No new resource names or runtime changes required another build for source verification.
- Owner QA: build/install the updated APK over the matching existing debug app while preserving data. Check the minimum and expanded widget sizes, full 12/24-hour digits, weekday/full date, Miyu's lower/right placement and size, and the smaller task card with a long two-line title plus metadata. Also check enlarged fonts, two/three-row sizes, normal task taps and mascot rotation after ordinary refreshes. Large fonts may still require a wider/taller widget; launcher rendering and final visual balance remain unverified. No device interaction was attempted; ask whether the phone is connected before any later ADB work.
- Owner acceptance remains required. No final v0.2 QA or v0.3 work began.

## Milestone 4 — final comprehensive widget design pass — 2026-10-11

This entry supersedes the visual dimensions and QA/build state in the earlier clock-refinement reports below; their history is retained.

- Completed one cohesive RemoteViews design pass: a single rounded ivory surface, small Riko Hub label, dominant ink Fredoka clock, prominent Indonesian weekday/full date, readable Nunito task rows, and reserved right-edge space for transparent Smol Miyu. Only the primary task has a quiet lavender fill; coral/lavender task markers remain useful urgency cues. The two decorative header dots remain removed. No weather, replacement artwork, library or network feature was added.
- Two deliberate Android 9-compatible layouts share 21 view IDs. Below 300dp reported width, the compact layout uses a clock up to 52sp, 19sp weekday, 16sp date and a 56×60dp mascot bounding box. At 300dp or wider, the expanded layout uses up to 56sp, 21sp weekday, 17sp date and a 68×68dp mascot. Native bounded clock autosizing can reduce digits to 30sp/32sp to avoid clipping. The date region grows with font size. Primary task titles are 18sp/bold and secondary titles 16sp, with controlled two-line ellipsis; real metadata is 12sp and remains visible with one task.
- Provider sizing remains 250dp minimum width and 272dp minimum height, resizable horizontally/vertically. Height/font-scale budgets show one primary task at the minimum, up to two at normal-scale heights of 344dp compact / 360dp expanded, and up to three at 420dp compact / 436dp expanded. A narrow but tall widget can show all three. Unused rows and their PendingIntents are cleared/hidden. At enlarged fonts, secondary rows hide first and Miyu hides when she would leave insufficient date width. The clock/date remain useful in empty, loading and unavailable states.
- Platform TextClock keeps live hours/minutes in the launcher: numeric `h:mm` for 12-hour mode or `HH:mm` for 24-hour mode, plus a separate small live period marker with `a` or an explicitly empty 24-hour format. It follows device time preference and timezone without minute-based background polling. Weekday/date use explicit Indonesian `id-ID` and current local timezone. Existing date/time/timezone/locale/reboot callbacks and the next-local-midnight fallback remain; WorkManager date refresh may be delayed by Android battery policy even while TextClock advances normally.
- All eight original owner PNGs and eight existing optimized drawable-nodpi copies are preserved byte-for-byte. Reserved image space, `fitCenter`, transparency and accessibility exclusion are retained. Each successful existing render chooses an expression independently at random; loading/error states use a stable first image. Selection is independent of task urgency/status, with no added polling, wake listener or background service.
- Existing urgency/top-three selection, completed/deleted filtering, task URI/ID navigation, immutable distinct task PendingIntents and home navigation are preserved. Header, clock region and date region open the app; task rows open the matching task. Database, recurrence, committed mutation/import hooks, reminder scheduling, backup format and app navigation contracts are unchanged. Existing widget enqueue/render exception isolation remains, so a rendering failure cannot roll back task persistence.
- Per the owner's later explicit request, switched from `experiment/claude-widget-refinement` to `checkpoint/rikohub-phase6` while carrying all working changes. HEAD and the checkpoint ref still equal `e25f6d7048e29610b329c1dffc48ab99edfacba7`; no commit, reset, cleanup, push, installation or data access occurred. Claude's valid dot removal, larger-mascot intent and earlier documentation are retained within the final composition. Existing uncommitted backup/reminder work and the untracked Gradle JVM criteria file are untouched.

Files changed in this final pass:

- `app/src/main/res/layout/task_widget.xml` and new `task_widget_compact.xml` — complete composition and bounded native clocks.
- `app/src/main/res/values/widget_styles.xml`, `widget_colors.xml`, `widget_strings.xml` and `app/src/main/res/drawable/widget_background.xml` — typography, contrast, empty 24-hour period format and rounded surface.
- `app/src/main/java/id/riko/hub/ui/widget/TaskWidgetProvider.kt` — layout choice, mascot visibility, metadata and shared home tap wiring.
- `app/src/main/java/id/riko/hub/domain/logic/TaskWidgetPresentation.kt` — presentation capacity and mascot width budgets; selection algorithm unchanged.
- `app/src/test/java/id/riko/hub/domain/logic/TaskWidgetPresentationTest.kt` — focused capacity/font-scale and mascot source cases; local-midnight case retained.
- `docs/STATUS.md` — this handoff. PNGs, manifest and provider metadata were reviewed but not modified in this final pass.

Verification actually performed:

- Parsed 37 manifest/resource XML files; resolved 113 widget Kotlin/XML resource references, including bundled fonts and both layouts. Confirmed identical 21 IDs, supported RemoteViews controls, bounded numeric TextClocks, 12/24-hour formats, growing date regions, valid provider registration/initial layout, non-exported receiver and disabled periodic widget updates.
- Reviewed PendingIntent identity/flags, cleared hidden-row intents, task routing and post-commit refresh isolation. Baseline SHA-256 comparisons confirmed every protected source/document/asset outside the allowed final-pass files remained unchanged. Both original and optimized Miyu sets are intact. `git diff --check` and new-layout whitespace checks passed. Independent source review found no outstanding material issue after sizing and tap corrections.
- Static measurements using the actual bundled font files checked all 1,440 hour/minute strings and long Indonesian date text. Maximum normal-scale numeric widths were approximately 132dp compact and 142dp expanded, within estimated available widths; “30 September 2026” measured approximately 146dp/155dp against 170dp/208dp date columns. Calculated contrast: ink on ivory 13.70:1, ink on lavender 11.84:1, muted text on ivory 5.19:1. These are static estimates, not Android-rendered or accessibility/device evidence.
- The owner's later explicit build request was completed through RTK: `JAVA_HOME=/opt/android-studio/jbr rtk gradlew assembleDebug --offline --console plain` — **BUILD SUCCESSFUL in 6m 46s**, 37 tasks (13 executed, 24 up-to-date). The initial sandbox attempt stopped at a read-only Gradle cache lock before Gradle ran; the authorized elevated retry completed. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, **16,148,104 bytes**, SHA-256 `41410d25272452ff2f7edc7e93c42995c905e74e354ae187968b674b366ce98a`. Its provider class timestamp is newer than the final header-intent correction. This proves compilation/resource linking, not launcher rendering.
- Updated focused JVM test source was reviewed but not executed or compiled by assembleDebug. No lint, broad regression suite, instrumentation, emulator, installation, device screenshot or rendered widget QA was performed. The referenced approved mockup/latest phone screenshot was not available as a local reference during this pass; composition follows the written requirements and existing project assets.

Owner QA after this single build:

1. Install the generated debug APK over the existing matching debug app using the current signing identity, preserving data. Do not uninstall or clear data. Inspect the new widget on OPPO CPH2015/Android 9 for dominant unclipped clock digits, readable weekday/full date, balanced task rows and a recognizable secondary Miyu.
2. Check 12-hour AM/PM and 24-hour mode, minute changes without opening the app, empty-task clock usefulness, midnight date rollover and time/timezone/locale/reboot recovery. Allow for inexact WorkManager date refresh and record any delay.
3. Resize at the minimum 250×272dp and across the 300dp width breakpoint; try narrow/tall and wider/taller sizes. Verify one/two/three rows, long titles/real metadata, zero/one/two/three/overflow task states, enlarged font settings and TalkBack. Confirm Miyu never overlaps text and hides before compromising the date at constrained font sizes.
4. Tap the header/clock/date and each task with the app cold and warm. Verify correct destinations and safe handling of a stale deleted task. Taps must not complete/delete anything.
5. With disposable QA tasks/backups, check create/edit/complete/delete/undo/recurrence and successful local import refresh all installed widgets. Confirm reminders and accepted backup behavior remain intact.
6. Observe several normal render/resize/task refreshes for randomized Miyu expression, without expecting a timer-driven change. Accept the final appearance before authorizing final v0.2 QA.

Remaining acceptance gaps: launcher rendering and touch/accessibility behavior are unobserved in this pass; launcher-reported sizing can differ from these static budgets. Very large system text may require a taller/wider widget even after mascot/secondary-row suppression. Full dates can ellipsize at constrained sizes; the static Indonesian date can lag midnight under background restrictions. The widget intentionally retains its light palette. Owner visual/functional acceptance remains required; final v0.2 QA and v0.3 have not begun.

## Milestone 4 — final clock widget visual refinement — 2026-10-11

- Owner reports the preceding native widget works on OPPO CPH2015 (Android 9), displays real tasks and opens the correct task when tapped. This is owner-reported verification, not a device test performed in this refinement. The new appearance remains pending owner build, installation and visual acceptance; final v0.2 QA and v0.3 have not begun.
- Refined the existing widget into one ivory rounded surface: small Riko Hub header, dominant 48sp ink clock, 19sp Indonesian weekday, 16sp full date, 17sp bold task titles and subtle lavender/coral dots. A 42dp Smol Miyu sits in reserved header space at the corner, without covering text. The existing 250dp width and 272dp minimum height remain. There are no nested task cards or weather elements, APIs, permissions or dependencies.
- Platform `TextClock` runs inside the launcher RemoteViews and displays `h:mm a` or `HH:mm` according to device time preference. No fixed timezone is assigned. Time remains visible in loading, empty and unavailable task states. No minute WorkManager polling, screen-wake listener, background service or exact alarm was introduced. [TextClock](https://developer.android.com/reference/android/widget/TextClock) is supported by [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews).
- Weekday and full date are formatted explicitly with Indonesian `id-ID` locale and the current local timezone. Existing bounded urgency scheduling remains; when no eligible task produces a boundary, the widget schedules the next local midnight for its date. Time/timezone/date callbacks are preserved and `LOCALE_CHANGED` now requests rendering. This avoids a stale date indefinitely when no tasks exist, but WorkManager execution can still be delayed by Android background policy. The clock itself does not depend on that work.
- The existing top-three urgency selection, active/deleted filtering, task PendingIntent identity and task routing are unchanged. Presentation chooses one, two or three rows from launcher height and font scale: conservative normal-scale thresholds are 322dp for two rows and 386dp for three. Smaller sizes retain the primary task and hide metadata; larger sizes display concise deadline/priority metadata. Unused rows and their click actions are hidden/cleared. Header, clock and date taps open the app. Repository mutation hooks, local import hooks, recurrence, reminders and database schema are unchanged.
- All eight owner assets in `design-assets/miyu-widget/` are present. Optimized copies in `drawable-nodpi/widget_miyu_01.png` through `widget_miyu_08.png` map to source filenames in sorted filename order. Transparent empty margins were trimmed and copies resized proportionally to at most 192px, retaining RGBA alpha. Combined copies total **459,400 bytes**, versus **11,366,525 bytes** for originals. Originals remain unchanged. No replacement artwork was generated.
- Each successful existing widget render independently chooses one of eight expressions at random. Selection does not read task urgency or status and requests no extra refresh. Loading/error renders use the first expression as a stable fallback. The decorative image preserves aspect ratio with `fitCenter` and is excluded from accessibility focus.
- Verification actually executed: **36 XML files** parsed; **79 widget source/resource references** resolved; source assertions checked RemoteViews-supported classes, TextClock 12/24-hour formats and default timezone, absence of weather references, and all eight PNG dimensions/alpha. Baseline hashes verified the task selection, navigation, mutation/import wiring, backup/reminder sources and original artwork are preserved. Final whitespace checks passed. Independent source review found no material issue. These checks do not prove compilation or rendered appearance.
- Added two focused JVM test methods for row capacity thresholds/font scale and next local midnight across DST when tasks are empty. Test source was reviewed; **tests were not executed**. No Gradle build, lint, installation or device QA was performed.

Files changed in this refinement:

- `app/src/main/AndroidManifest.xml` — locale refresh action.
- `app/src/main/java/id/riko/hub/ui/widget/TaskWidgetProvider.kt` — clock/date presentation, row capacity, mascot selection and empty-task date scheduling.
- `app/src/main/java/id/riko/hub/domain/logic/TaskWidgetPresentation.kt` — pure row capacity and local midnight helpers.
- `app/src/test/java/id/riko/hub/domain/logic/TaskWidgetPresentationTest.kt` — focused source tests.
- `app/src/main/res/layout/task_widget.xml`
- `app/src/main/res/values/widget_styles.xml`
- `app/src/main/res/values/widget_strings.xml`
- `app/src/main/res/drawable-nodpi/widget_miyu_01.png` through `widget_miyu_08.png`
- `docs/STATUS.md`

### Owner visual acceptance

Build/install once using the existing signing identity and preserving app data. Optionally run the focused presentation test class with `./gradlew :app:testDebugUnitTest --tests id.riko.hub.domain.logic.TaskWidgetPresentationTest`.

1. Inspect the new widget on OPPO CPH2015 in 12-hour and 24-hour modes. Verify complete clock digits and AM/PM, Indonesian weekday/full date, and the small corner mascot without text overlap.
2. Resize from the supported minimum to taller/wider sizes. Verify one/two/three task presentation, readable long titles, metadata, empty rows and task-specific taps. Check enlarged fonts and TalkBack; no rendered fit or accessibility success is claimed here.
3. Leave the widget visible across minute changes without opening the app, including with zero tasks. Verify the clock advances. Cross midnight and check the date, allowing for delayed background work; test time/timezone/locale changes on a QA device.
4. Check empty/loading/unavailable task presentation still retains clock/date. Observe several normal task/resize refreshes to verify mascot variation without urgency-dependent expressions.

Remaining limitations: fixed light palette; static Indonesian date can lag midnight while WorkManager is delayed; minimum-width 12-hour clock and large-font layouts may truncate and require a wider/taller widget. Compilation, updated launcher rendering, background clock lifecycle and final visual acceptance remain owner QA. Stop here until owner acceptance.

### Refinement 1 - mascot enlargement and header dot removal - 2026-10-11

- Source-only change to res/layout/task_widget.xml. The two static header dot ImageViews (widget_lavender_dot, widget_coral_dot) were removed. Mascot widget_miyu changed from 42dp to 52dp square, about 24 percent larger, with fitCenter kept. Nothing else was edited.
- Root cause: the dots were two static ImageViews with no ids in the header row, 7dp and 4dp. They were separate from the task-row markers that TaskWidgetProvider.kt sets at runtime (line 147), so the drawable files were kept for those markers.
- Header minHeight stays 48dp, so the header now measures about 52dp (+4dp). The capacity helper budgets 48dp for the header plus 16dp of host slack, so this uses 4dp of that slack. Row thresholds (322dp and 386dp) depend only on reported widget height and are unchanged.
- Clipping risk: one row needs about 246dp against the 272dp minimum, so clipping is not expected on paper. The tightest points are the 322dp and 386dp heights and enlarged fonts, with about 12dp of slack left. This is not rendered evidence.
- Not changed: task selection and ordering, clock and date mechanisms, PendingIntents and header tap targets, backup and restore, reminders, widget lifecycle and refresh, mascot randomization.
- Verified by source reads only: layout re-read after the edit shows both dot views gone and widget_miyu at 52dp. The provider references only widget_miyu and widget_header from the header area. No Gradle, lint, install, emulator or device test was run, so compilation and rendered appearance are untested. Owner QA is needed at minimum size, at 322dp and 386dp heights, and with enlarged fonts.

## v0.2 Milestone 4 — native top-three task widget — 2026-10-10

- Source implementation is complete for owner review. Milestone 3 has been manually accepted by the owner. Milestone 4 acceptance and final v0.2 QA remain pending; no v0.3 work is authorized or started.
- Verified branch `checkpoint/rikohub-phase6` and the dirty working tree before implementation. Existing local backup implementation, tests, preferences, reminder work and the untracked Gradle JVM file are preserved. The only extension to the existing backup wiring is a widget request in `AppContainer` after its post-commit reminder callback, using `finally` so reminder enqueue failure cannot suppress widget refresh.
- Native `AppWidgetProvider` and `RemoteViews` display up to three active tasks, ordered by `UrgencyCalculator.rank(...).take(3)`. Completed and soft-deleted records are excluded. Home's manual pin is separate from this priority list; the widget introduces no competing urgency algorithm. The app header, compact Indonesian focus label, task titles, local deadline/priority metadata, and loading/empty/unavailable states use standard widget-compatible layouts and the existing ivory, lavender, ink and coral palette. No library, network interface, animation, collection service or database migration was added.
- Header taps open the app. Row taps use the existing task-ID entry contract and task editor destination. Explicit activity PendingIntents use distinct widget-specific data URIs, `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`, and `CLEAR_TOP | SINGLE_TOP`. Existing navigation consumes absent/deleted IDs without opening a nonexistent task. Widget taps perform no task mutation.
- The existing repository callback runs after committed create/edit/status/delete/restore transactions, including recurrence successor creation/restoration and completion undo. It now also requests widget updates. Successful local backup import requests updates after Room commit and the separate preference step, including reminder reconciliation failure. Widget enqueue/render failures are isolated from persistence and do not change backup's existing reminder-failure reporting.
- Addition, system widget updates, resize, reboot, time/timezone changes, package replacement and app resume request refresh. `DATE_CHANGED` is also registered; Android's background broadcast restrictions mean the widget does not rely on its delivery. Serialized immediate render work reads current active tasks. A separate unique one-time job targets the earliest next local midnight, three-hour urgency boundary, or strict overdue boundary (`deadline + 1 ms`). A frozen clock snapshot keeps each render's ordering/labels consistent. No active tasks cancel boundary work; removing the last widget cancels it. Provider periodic updates are disabled. Rendering retries at most three attempts, and widget work is separate from reminder work.
- Focused evidence actually executed: Python ElementTree parsed **36** resource/manifest XML files; **66** widget Kotlin/XML resource references resolved; assertions checked RemoteViews-supported layout classes, provider metadata/registration, non-exported receiver, disabled periodic polling, resize minimum and existing minSdk 26. Baseline hashes confirmed existing changed/new files outside authorized integration/documentation remain intact. `git diff --check` and new-file whitespace checks passed. Independent source review found no material issue after correcting unsupported plain View markers, minimum touch/resize sizes, cancellation propagation and failure handling. These checks are not compilation or runtime evidence.
- Added **5 JVM test methods** for zero/one/two/three/overflow selection, mixed urgency order and ties, done/deleted filtering, WITA/DST midnight, three-hour/strict-overdue transitions and overflow-safe deadline handling. Added **2 isolated Room instrumentation test methods** for successful mutation callbacks, recurrence/undo/delete/restore, failed-write suppression and callback failure preserving committed data. Existing backup tests already cover rollback skipping post-commit effects and callback/preference failure after commit. **None of these test methods was executed in this milestone.**

Changed files for Milestone 4 (relative to project root):

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/id/riko/hub/AppContainer.kt`
- `app/src/main/java/id/riko/hub/MainActivity.kt`
- `app/src/main/java/id/riko/hub/domain/logic/TaskWidgetSelection.kt`
- `app/src/main/java/id/riko/hub/ui/widget/TaskWidgetProvider.kt`
- `app/src/main/res/layout/task_widget.xml`
- `app/src/main/res/xml/task_widget_info.xml`
- `app/src/main/res/values/widget_strings.xml`
- `app/src/main/res/values/widget_styles.xml`
- `app/src/main/res/values/widget_colors.xml`
- `app/src/main/res/drawable/widget_background.xml`
- `app/src/main/res/drawable/widget_lavender_pill.xml`
- `app/src/main/res/drawable/widget_lavender_dot.xml`
- `app/src/main/res/drawable/widget_coral_dot.xml`
- `app/src/test/java/id/riko/hub/domain/logic/TaskWidgetSelectionTest.kt`
- `app/src/androidTest/java/id/riko/hub/data/TaskWidgetMutationHookTest.kt`
- `docs/STATUS.md`

### Limitations and remaining acceptance gaps

- WorkManager execution is inexact and can be delayed by Doze, battery policy or launcher behavior. Time labels/ranking can lag their boundary until work runs; app resume and system callbacks provide recovery. No exact alarm or foreground service is used.
- The widget deliberately uses a fixed ivory/light palette, regardless of app theme. It supports horizontal/vertical enlargement with a 250dp minimum width and 272dp minimum height. Long titles/metadata truncate; large fonts may require a taller widget. Actual host sizing, accessibility, font scaling and rendering have not been observed.
- No Gradle build, lint, compiled tests, installation, device interactions or personal task changes were performed. API compatibility was checked against source and official documentation, not an Android 9 device. APK compilation/resource linking, cold/warm task navigation, launcher lifecycle, scheduling and owner acceptance remain open.
- Platform references: [RemoteViews supported controls](https://developer.android.com/reference/android/widget/RemoteViews), [receiver export behavior](https://developer.android.com/guide/topics/manifest/receiver-element), [PendingIntent identity/mutability](https://developer.android.com/reference/android/app/PendingIntent), [widget layout and resize metadata](https://developer.android.com/develop/ui/views/appwidgets/layouts), and [inexact delayed WorkManager jobs](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### Owner build and widget QA

Build the APK once and run the focused selection test class:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --tests id.riko.hub.domain.logic.TaskWidgetSelectionTest
```

On an owner-managed disposable QA installation/device, run only the mutation-hook instrumentation class:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=id.riko.hub.data.TaskWidgetMutationHookTest
```

The new instrumentation tests use in-memory Room and do not access the personal task database or enqueue reminder/widget jobs. Owner handles APK installation using the existing signing identity and preserving data; no uninstall or data clearing is needed.

1. On Android 9, add the widget with zero, one, two, three and more than three disposable active tasks. Verify urgency/tie ordering and exclusion of completed/deleted tasks. Home pinning must not reorder this priority list.
2. Tap the header, then each row with the app closed and already open. Verify the matching editor opens. Delete a disposable task while its old widget row remains visible, then tap it before refresh; the app must safely show Tasks. Verify taps do not complete/delete tasks.
3. Create/edit titles, priorities and deadlines; complete/delete/undo/restore tasks; complete and undo recurring tasks. Check all installed widget instances refresh after each commit and show recurrence successors correctly.
4. Import a disposable local backup, with and without preference restore. Verify imported active tasks appear and repeated imports remain non-destructive. Preserve the accepted backup/reminder behavior.
5. Resize to supported minimum and larger sizes; inspect long titles, 200% text, TalkBack, empty/error presentation and multiple widget instances. Remove the last widget, then add it again.
6. Cross a deadline's three-hour and overdue boundaries and local midnight; test reboot and time/timezone changes on the QA device. Allow for inexact scheduling and record actual observed delays. Confirm reopening the app refreshes the widget after delayed/background work.

Record build, focused tests and phone observations before accepting Milestone 4. Stop here until owner acceptance authorizes final v0.2 QA.

## v0.2 Milestone 3 — manual local backup — 2026-10-10

- Implementation based on checkpoint `c2881ee` on `checkpoint/rikohub-phase6`. Daily Studio, Miyu assets, Room version 2 and existing task mutation behavior are preserved. The untracked `gradle/gradle-daemon-jvm.properties` is untouched. No device data access, builds, installations, lint, commits or pushes in this milestone.
- Settings offers SAF `CreateDocument` export and `OpenDocument` import. UTF-8 JSON format version 1 contains metadata (`app`, epoch-millisecond `createdAt`), all tasks/events including completed records and tombstones, and theme/name/navigation/hints/pin preferences. No WorkManager jobs or local database IDs are exported. Relationships use `generatedFromSyncId` and `pinnedFocusSyncId`; pin dates use ISO local dates. No dependency, storage permission or cloud integration added.
- Validation rejects unsupported versions, missing/unknown/duplicate keys, wrong types, malformed UTF-8/JSON, invalid enums/ranges/relationships, duplicate identities and cycles before mutation. Files are limited to 8 MiB and 10,000 total task/event records, with nesting/allocation limits. Android's JSON reader needs an additional lexical check for malformed escapes/literals; see [AOSP JsonReader source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/util/JsonReader.java).
- Import preview shows totals, inserts, duplicates and conflicts; confirmation is required. Existing records, including deleted ones, win by `syncId`. Conflicting recurrence successors and dependent new descendants are skipped rather than overwriting the existing chain. Preview is invalidated if Room data changes before confirmation. Accepted task/event inserts and local relationship remapping occur in one Room transaction; insertion failure rolls back that transaction.
- Preference restoration defaults off. Opt-in restoration uses a separate DataStore edit after Room commit and remaps the pin through stable identity. Preference failure leaves imported records intact and is reported separately. Reminder reconciliation is requested after Room commit using the existing unique-work scheduler, including after preference failure; enqueue failure is reported with a reopen-app retry instruction. This is not cross-storage atomicity or proof of notification delivery.
- Source review corrected a cancellation gap around transaction return: a confirmed, bounded import completes the transaction and post-commit reconciliation even if its screen closes. Cancellation before entry writes nothing. Removed backup-only display-name/location length restrictions that rejected data accepted by existing writers. Rollback tests compare the original, nondefault preference store.
- Final source checks: tracked and new-file whitespace checks pass; values XML parses and all backup UI string references resolve. Tests cover malformed structure/UTF-8, limits/future versions, round trips/tombstones, merge conflicts, ancestry/pin remapping, rollback, partial preference failures, cancellation and repeated reminder reconciliation. These are source-reviewed assertions, not executed test results.
- Focused isolated instrumentation tests are included for codec/round-trip and merge/rollback behavior. They have **not been executed**. Compilation, SAF provider behavior, configuration/process recreation, accessibility and visual checks remain owner QA. Milestone 3 was subsequently manually accepted by the owner; its historical unexecuted-test gaps remain recorded. See the Milestone 4 entry above for current widget status. v0.2 remains incomplete; v0.3 is not started.

Changed files (relative to the project root):

- `app/src/main/java/id/riko/hub/data/backup/LocalBackupService.kt`
- `app/src/main/java/id/riko/hub/data/local/dao/Daos.kt`
- `app/src/main/java/id/riko/hub/data/prefs/SettingsDataStore.kt`
- `app/src/main/java/id/riko/hub/ui/settings/BackupSettings.kt`
- `app/src/main/java/id/riko/hub/ui/settings/SettingsScreen.kt`
- `app/src/main/java/id/riko/hub/ui/RikoHub.kt`
- `app/src/main/java/id/riko/hub/navigation/RikoNavHost.kt`
- `app/src/main/java/id/riko/hub/AppContainer.kt`
- `app/src/main/java/id/riko/hub/work/TaskReminders.kt`
- `app/src/main/res/values/backup_strings.xml`
- `app/src/androidTest/java/id/riko/hub/data/backup/LocalBackupServiceTest.kt`
- `app/src/androidTest/java/id/riko/hub/data/backup/LocalBackupMergeTest.kt`
- `app/src/androidTest/java/id/riko/hub/work/LocalBackupReminderReconcileTest.kt`
- `docs/STATUS.md`

### Owner verification for Milestone 3

Build once, then run only the three backup test classes on a disposable QA installation/device:

```sh
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=id.riko.hub.data.backup.LocalBackupServiceTest,id.riko.hub.data.backup.LocalBackupMergeTest,id.riko.hub.work.LocalBackupReminderReconcileTest
```

The tests use in-memory databases and temporary preferences, not the user's task database. The reminder test schedules only isolated jobs with large fixture IDs and cancels those jobs afterward. Instrumentation installation/lifecycle is still owner-managed. To update the personal app without clearing data, use the same debug signing identity: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Manual QA: cancel both pickers; export/reimport with preference restore unchecked; verify duplicates/conflicts and no overwrite; opt into settings restoration using disposable fixtures; inspect Indonesian preview/errors and large text; rotate/background during selection; try invalid/future/oversized files; verify reminder enqueue/delivery and completed/deleted exclusion using disposable tasks. A process interruption may leave an incomplete export file; retry export. Record actual results before acceptance.

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
