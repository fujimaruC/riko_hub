# Current state audit

Status: **IMPLEMENTED source inventory**, with separately labeled acceptance and runtime evidence. Audit date: 2026-10-11. No build, test, lint or device command was executed.

## Repository identity and evidence limits

- Workspace: `/home/riko/Projects/riko_hub`.
- Actual branch: `feature/v04-laptop-companion`, matching the expected branch.
- Actual HEAD: `44d6c67`, `feat: integrate v0.3 Materials Vault and finalize widget`, matching the expected v0.3 baseline.
- Existing dirty tracked files: `app/build.gradle.kts`, manifest, `AppContainer.kt`, `SyncScreen.kt`, and `docs/STATUS.md`.
- Existing untracked work: Android companion sources/resources/tests, `companion/`, both v0.4 guides, and `gradle/gradle-daemon-jvm.properties`. These are part of the inspected product state and must not be discarded.

No branch or baseline discrepancy was found. v0.4 is not contained solely in the HEAD commit. An implementation handoff must preserve the working tree. This documentation task adds only the ten files under `docs/renaissance/`.

## Milestone evidence

| Area | Current evidence | What remains unconfirmed |
|---|---|---|
| v0.2 planner | IMPLEMENTED; OWNER-REPORTED accepted, including widget and local backup. | This session does not repeat acceptance tests or certify every device/configuration. |
| v0.3 Materials Vault | IMPLEMENTED. Riko reports OAuth configuration and APK installation performed. | Live authorization, browsing, upload, opening, cancellation and account switching end to end. |
| v0.4 LAN companion | IMPLEMENTED in the working tree. Riko reports a previous Linux startup at `192.168.1.49:8765`. | Android build/integration, discovery, pairing, TLS interoperability, revocation and end-to-end acceptance. |
| v0.4 remote access | PROPOSED; no remote path found in current source. | All overlay/manual-host implementation and acceptance. |

The reported LAN address is environment-specific, not a permanent default. No startup log was inspected or startup reproduced. `STATUS.md` still records no v0.4 execution in its implementation session and pending v0.3 setup; Riko's newer reports update those narrow facts. They do not prove Drive functionality or Android companion success.

`STATUS.md` records an owner-reported v0.3 successful build and 11 Vault test results, with ten Drive API tests lacking an execution report. Those are historical records, not freshly checked results and not evidence for v0.4.

## Implemented Android foundations

Source paths below are relative to the repository root. The [build configuration](../../app/build.gradle.kts) specifies Kotlin/Compose, one `:app` module, version `0.4.0`/400, minimum SDK 26 and compile/target SDK 37. Android 9 remains an acceptance target; it is not the minimum declared API level.

| Source and symbol | Implemented responsibility |
|---|---|
| `AppContainer.kt` — `AppContainer` | Manual lazy wiring of Room, repositories, DataStore, reminders, widgets, backup, Drive and companion services. |
| `navigation/RikoNavHost.kt` — `RikoNavHost`; `navigation/SpatialMap.kt` | Existing Home, Tasks, Timeline, Vault and Sync routes plus secondary navigation. Companion uses Sync. |
| `data/local/RikoDatabase.kt` — `RikoDatabase` | Room **version 2**, task and event tables; exported schema snapshots exist for versions 1 and 2. |
| `data/local/Migrations.kt` — `MIGRATION_1_2` | Explicit additive migration for task extensions, registered by AppContainer. |
| `data/local/entity/Entities.kt` — `TaskEntity`, `EventEntity` | Local numeric IDs, unique `syncId`, timestamps and soft-delete fields. Task extensions include estimates, recurrence, checklist JSON, reminder lead time and generated-successor relationships. |
| `domain/model/Models.kt` — `Task`, `Event`, `ChecklistItem`, `TaskStatus` | Current task states are NOT_STARTED, IN_PROGRESS and DONE. Course/assignment lifecycle entities do not yet exist. |
| `data/repository/Repositories.kt` — `TaskRepository`, `EventRepository` | Local mutations and observation. Task mutation hooks request reminders and widget refresh. |
| `data/prefs/SettingsDataStore.kt` — `SettingsDataStore` | Local preferences. This is separate from the companion's encrypted trust preferences. |

Paths in this table begin under `app/src/main/java/id/riko/hub/` unless stated otherwise. `syncId` is a useful stable identifier, not proof of implemented database synchronization. `updatedAt` and `deletedAt` do not provide causal ordering, replication acknowledgements or a conflict policy.

## Implemented product workflows

- **Tasks and planning:** `ui/tasks/`, `domain/logic/TaskGrouping.kt`, `TaskRecurrence.kt` and repository code implement task management, search/filtering, checklists, estimates and recurrence. Checklist items are not a general subtask/dependency graph.
- **Daily Studio and Timeline:** `ui/home/HomeViewModel.kt`, `domain/logic/UrgencyCalculator.kt` and `TimelineBuilder.kt` feed existing daily and calendar views. `UrgencyCalculator.rank` excludes done/deleted records and orders by score, deadline, priority, creation time and ID. Future Deadline Radar must explain this policy rather than silently introduce a competing rank.
- **Reminders:** `work/TaskReminders.kt` contains scheduling/reconciliation and worker behavior. Background delivery is subject to Android execution and notification policy.
- **Widget:** `ui/widget/TaskWidgetProvider.kt` contains `TaskWidgetProvider` and `TaskWidgets`; `domain/logic/TaskWidgetSelection.kt` and presentation logic support native RemoteViews task display and native live clock behavior. Existing mutation/import callbacks are integration boundaries to preserve.
- **Backup:** `data/backup/LocalBackupService.kt` exposes `export`, `preview` and `importBackup`; `ui/settings/BackupSettings.kt` connects SAF interaction. The service uses a bounded JSON document, an additive import, conflict/stale-preview checks, transactional database changes and optional settings restoration. Limits include 8 MiB and 10,000 records. `AppContainer.backups` reconciles reminders and widgets. Settings and post-commit side effects have separate failure handling; do not describe the entire import as one all-or-nothing transaction.
- **Backup boundary:** the manifest has `allowBackup=false`. Explicit JSON export is implemented despite automatic platform backup being disabled. The JSON document contains task/event/settings data, not a device-trust or OAuth credential backup. It is not a full attachment library backup.

## Materials Vault

`data/remote/drive/GoogleDriveAuthorization.kt` defines `GoogleDriveAuthorization`/`DriveAuthorizer`. It uses Google Identity authorization with `drive.readonly` for browsing and a separate `drive.file` Picker grant for upload destinations. `DriveApi.kt`, `DriveRepository.kt` and `DriveModels.kt` provide REST/data boundaries. `ui/vault/VaultViewModel.kt` and `VaultScreen.kt` integrate listing, folder traversal, global name search, explicit SAF uploads and opening.

The source is not automatic task synchronization. Authorization state is transient; transfers are foreground operations. The recorded implementation limits include bounded listing/cache sizes and no durable process-death upload queue. A failed or interrupted upload may need duplicate reconciliation. Cached opening does not establish a general offline material library. Google Workspace documents, provider permissions and Google service availability remain distinct from locally owned files.

## Laptop Companion

| Source and symbol | Current behavior and boundary |
|---|---|
| `data/remote/device/CompanionProtocol.kt` — `parsePairingPayload`, `isPrivateIpv4`, `retryDelaySeconds` | Strict versioned pairing payload; RFC1918 IPv4 only; capped retry schedule. |
| `CompanionRepository.kt` — `setActive`, `startScan`, `requestBlocking` | Foreground NSD, Wi-Fi network callback, discovered endpoint cache and HTTPS requests bound to that Wi-Fi Network. No manual-host path. |
| `CompanionRepository.kt` — `pair`, `matchesPin`, `readTrust`, `writeTrust`, `forget` | Out-of-band certificate pin, one trusted laptop, Keystore AES-GCM record, bearer authentication and local/remote revocation handling. |
| `ui/sync/SyncScreen.kt`, `SyncViewModel.kt`, `SyncUiState.kt` | Indonesian discovery, pairing, recent connection state, reconnect/disconnect, forget and trust-reset controls. |
| `companion/riko_companion/__main__.py` — `private_address`, `run`, `console` | Required explicit RFC1918 bind; one TLS listener; mDNS tied to the selected interface; local approval/status/revoke commands. |
| `companion/riko_companion/server.py` — `BoundedTLSServer`, `Handler` | Bounded TLS/request processing and five protocol-1 routes: hello, pair, status, disconnect, trust deletion. No file or database endpoint. |
| `companion/riko_companion/state.py` — `CompanionState` | One-use secret, expiring explicit approval, token hashes, at most eight phones, recent presence and revocation. |
| `companion/riko_companion/storage.py` — `Storage` | Persistent laptop UUID/certificate/key, owner-only files, process lock and atomic trust storage. |

See [existing protocol](../V04_COMPANION_PROTOCOL.md) for exact messages. Presence means a recent authenticated response, not a permanent socket. Pairing is pinned HTTPS plus a per-phone token, not mutual-certificate TLS. A canceled response after approval can leave orphan laptop trust requiring local revocation.

Tailscale cannot be enabled by documentation alone: its usual IPv4 range is outside both existing validators; physical Wi-Fi binding bypasses the desired VPN path; manual endpoints and overlay listener configuration are absent. Document 04 specifies these changes without claiming implementation.

## Test source inventory, not test results

| Test source | Behavior represented | Important gap |
|---|---|---|
| `app/src/test/.../domain/logic/{HomeEngineTest,TaskRecurrenceTest,TimelineBuilderTest,TaskReminderDecisionTest,TaskWidgetSelectionTest,TaskWidgetPresentationTest}.kt` | Pure ordering, recurrence, timeline, reminder decisions and widget selection/presentation. | Android/OEM rendering and delivery require device evidence. |
| `app/src/androidTest/.../data/TaskExtensionDatabaseTest.kt` | Task extensions and migration source cases. | No execution in this audit. |
| `app/src/androidTest/.../data/backup/{LocalBackupServiceTest,LocalBackupMergeTest}.kt` | Backup parsing/import/conflict cases. | Real SAF/provider failures and recovery remain owner checks. |
| `app/src/androidTest/.../{data/TaskWidgetMutationHookTest,work/LocalBackupReminderReconcileTest,work/TaskReminderWorkerTest}.kt` | Integration hooks and worker cases. | Source presence does not establish their results. |
| `app/src/test/.../data/remote/drive/DriveApiTest.kt`; `ui/vault/{VaultPageTest,VaultViewModelTest}.kt` | API mapping/state/paging/authentication cases. | No live Google end-to-end proof. |
| `app/src/test/.../data/remote/device/CompanionProtocolTest.kt` | Five methods for payload, address, retry and trust epoch interleavings. | Does not exercise the entire repository, Keystore, NSD or TLS stack. |
| `companion/tests/test_state.py` | Twelve source methods for state, persistence and bounded server behavior using fixtures. | No cross-device protocol acceptance or real network verification here. |

Here `...` means `java/id/riko/hub`, not an additional directory. No test result is inferred from a filename or method count.

## Debt, questions and historical differences

1. **P0 evidence gap:** validate existing v0.4 before enlarging its transport surface. Record failures before changing protocol behavior.
2. **P0 remote compatibility:** separate endpoint policy/routing from trust. Do not fix the RFC1918 restriction by accepting arbitrary public hosts or disabling pinning.
3. **P0 recovery coverage:** test real lifecycle cancellation, storage failure, revocation and process restart. Helper epoch tests do not prove repository orchestration.
4. **P1 implementation concentration:** `CompanionRepository` combines NSD, connectivity, TLS and trust storage. Extract only the seams needed for remote transport and deterministic checks; no architecture replacement is justified.
5. **P1 identity gaps:** local numeric IDs and Android content URIs cannot become portable Linux identifiers. New links must use stable domain IDs and explicit file references.
6. **P1 historical specifications:** `docs/03_DATA_MODEL.md` describes version 1; current Room is version 2. Old v0.1 backup statements are historical. `docs/04_ROADMAP.md` proposes timestamp last-write-wins and LAN-only v0.4; the new direction replaces those future assumptions.
7. **UNKNOWN:** actual installed v0.4 APK/version, currently usable overlay client on Android 9, owner preference for hosted versus self-managed networking, metered-data policy, realistic file sizes and retention budgets.

No source defect beyond the documented design boundaries is asserted without a reproducible case. Existing accepted functionality remains the baseline. See [ordered backlog](09_EXECUTION_BACKLOG.md) for evidence collection and decision tasks.
