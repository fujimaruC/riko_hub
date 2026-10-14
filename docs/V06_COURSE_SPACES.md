# v0.6 Phase 1 — Course Spaces

This phase implements the local course foundation only. The owner authorized source implementation on `feature/v04-laptop-companion`, starting from clean HEAD `947b057f230c8083512e7553ce9a5aab7de89507`. Builds, tests, lint, device checks, dependency installation, and Git commits remain owner-controlled.

## Scope

Open Course Spaces from Tasks. Courses support creation, editing, archiving, restoration, and persistent ordering within the active or archived list. Each course has a required title and optional code, instructor, academic period, named color, and short icon text. Course details display a weekly timetable and explain the future task and material associations.

Weekly slots store a weekday (ISO Monday=1 through Sunday=7), start/end minutes, and optional location. They are local timetable references, not generated Planner events or reminders. Slots end after they start on the same day; `24:00` is allowed only as an end time. A course supports at most 28 slots. Calendar synchronization and overnight sessions are outside this phase.

Task-to-course association and the academic lifecycle belong to Phase 2. Material references and SAF/Drive attachment handling belong to Phase 3. Lab templates, Deadline Radar, the academic dashboard, and a Home summary are not implemented here.

## Database and identity

The actual starting Room database version is **2**, containing `tasks` and `events`. This phase advances it to **3** with an explicit additive `MIGRATION_2_3`. Existing installations upgrade through `1 → 2 → 3` or `2 → 3`; both migrations are registered in `AppContainer`. No destructive fallback is used. Existing task and event tables, records, recurrence fields, and completion semantics are unchanged.

New tables:

- `courses`: local numeric primary key, unique stable `syncId`, metadata, persistent `sortOrder`, nullable `archivedAt`, and creation/update timestamps. An index covers archive state and ordering.
- `course_weekly_slots`: local numeric primary key, `courseId`, weekday, start/end minutes, and optional location. Its indexed foreign key references `courses.id` with `ON DELETE CASCADE` and `ON UPDATE NO ACTION`.

Course and slot writes are transactional. Editing preserves the course identity, original creation time, archive state, and order. Slot replacement affects only that course. Archiving preserves every slot and permits restoration. No course deletion is exposed. If a future feature physically deletes a course, the foreign key removes only its owned slots; it does not affect tasks, events, or external files.

There are no task/course or material/course foreign keys in this phase. Old tasks remain valid without a course. A missing course produces an empty detail/list state rather than a forced task mutation. Imported identities are resolved to local numeric IDs before inserting owned slots.

Room schema exports for versions 1 and 2 remain intact. The authentic `app/schemas/id.riko.hub.data.local.RikoDatabase/3.json` must be generated and reviewed by the owner's next build. Agents did not fabricate a Room identity hash or run schema generation. The migration validation test requires that generated schema asset.

## Backup and restore

New JSON exports use **format version 2** and include courses with nested weekly slots, ordering, timestamps, and archive state. Portable references use `syncId`; database row IDs are not exported. Existing format version 1 backups remain accepted as containing no course records. Importing an old backup leaves existing courses untouched.

The existing insert-only merge policy remains: exact duplicates are skipped; an existing stable ID with different content is reported as a conflict and the entire local record wins. Course slots are compared and retained with their owning course. Preview counts include course and slot additions. A changed local course, order, archive state, or schedule invalidates the preview before any import mutation.

The existing 8 MiB byte limit, parser limits, and 10,000-record limit remain. Course and slot records count toward the record limit. Malformed metadata or schedules are rejected before mutation. Imports commit task, event, course, and slot insertions in one transaction. Settings restoration and existing reminder/widget reconciliation retain their established behavior.

Backups remain explicit plaintext JSON exports, not document archives or encrypted vaults. They include no Google or Companion credentials. Older app versions cannot import format version 2; retain old backups and upgrade the receiving app before importing new archives. This restriction is shown in the backup UI.

## Owner verification

Source tests cover course creation/editing/archive/order, validation rollback, migration preservation, backup compatibility and course merges, and Compose course workflows. Test source is not execution evidence.

Before acceptance, Riko must build and inspect the generated schema, execute the focused tests, and verify an upgrade with disposable copies of v1/v2 task/event databases. Verify offline course editing, timetable validation, ordering after reopening, archive restoration, large text and small screens, system Back, failed-write retry, old backup import, new backup round trip, local conflict preservation, and stale preview rejection. Existing Home, Tasks, Timeline, Daily Studio, widgets/reminders, Vault, Sync, and Settings also need the owner's chosen regression checks.

No builds, Gradle tasks, tests, lint, ADB, application launches, services, dependency installation, commits, pushes, merges, or branch changes were executed by agents. Runtime behavior and migration correctness remain unverified. Stop at Phase 1 and wait for the owner's approval before Phase 2.

## File inventory

Paths below are relative to the repository root.

Created:

- `app/src/main/java/id/riko/hub/domain/model/Course.kt`
- `app/src/main/java/id/riko/hub/data/local/entity/CourseEntities.kt`
- `app/src/main/java/id/riko/hub/data/local/dao/CourseDao.kt`
- `app/src/main/java/id/riko/hub/data/repository/CourseRepository.kt`
- `app/src/main/java/id/riko/hub/ui/courses/CoursesViewModel.kt`
- `app/src/main/java/id/riko/hub/ui/courses/CoursesScreen.kt`
- `app/src/main/java/id/riko/hub/ui/courses/CourseEditorSheet.kt`
- `app/src/main/res/values/course_strings.xml`
- `app/src/androidTest/java/id/riko/hub/data/CourseRepositoryTest.kt`
- `app/src/androidTest/java/id/riko/hub/data/backup/CourseBackupTest.kt`
- `app/src/androidTest/java/id/riko/hub/ui/courses/CoursesTest.kt`
- `app/src/test/java/id/riko/hub/ui/courses/CourseTimeTest.kt`
- `docs/V06_COURSE_SPACES.md`

Modified:

- `app/src/main/java/id/riko/hub/AppContainer.kt`
- `app/src/main/java/id/riko/hub/data/local/RikoDatabase.kt`
- `app/src/main/java/id/riko/hub/data/local/Migrations.kt`
- `app/src/main/java/id/riko/hub/data/backup/LocalBackupService.kt`
- `app/src/main/java/id/riko/hub/navigation/Destinations.kt`
- `app/src/main/java/id/riko/hub/navigation/RikoNavHost.kt`
- `app/src/main/java/id/riko/hub/navigation/SpatialMap.kt`
- `app/src/main/java/id/riko/hub/ui/compass/CompassChip.kt`
- `app/src/main/java/id/riko/hub/ui/components/DestinationLabel.kt`
- `app/src/main/java/id/riko/hub/ui/tasks/TasksScreen.kt`
- `app/src/main/java/id/riko/hub/ui/settings/BackupSettings.kt`
- `app/src/main/res/values/backup_strings.xml`
- `app/src/androidTest/java/id/riko/hub/data/backup/LocalBackupServiceTest.kt`
- `app/src/androidTest/java/id/riko/hub/data/backup/LocalBackupMergeTest.kt`
- `docs/STATUS.md`
