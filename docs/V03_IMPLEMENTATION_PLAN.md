# v0.2–v0.3 implementation and release gates

User authorization (2026-10-08): continue implementation and testing through v0.3;
use lighter implementation agents with Astra coordinating; do not publish.
This supersedes the earlier restriction on building later versions and skipping
Home tests. It does not turn unperformed acceptance checks into passes.

## Working scope

Unless the user narrows the candidate list, include the union of the roadmap and
changelog candidates. Keep the existing visual identity, local-first task data,
spatial navigation, and Indonesian copy. Sync/laptop features remain v0.4+.

1. v0.2 task data: estimates, checklists, daily/weekly/monthly recurrence,
   optional deadline reminders, explicit non-destructive Room migration.
2. v0.2 interface: shared task editor additions, task search, Timeline day view.
3. v0.2 platform features: inexact local reminders, native top-three task widget,
   deliberate document-picker export/import with validation and atomic restore.
4. v0.3 Vault: optional Google authorization, least-privilege Drive access,
   four mapped folders, recent files/search, opening/downloading, offline marks,
   capped local cache/cleanup, explicit user-triggered upload.
5. Verify migration/data integrity, core logic, UI interactions, permissions,
   offline/error states, optimized release build, and integration boundaries.

## Behavior decisions

- Recurrence creates the next occurrence when the current task is completed;
  preserve completion history. Skip missed dates instead of creating a backlog.
  Calendar arithmetic uses the injected clock's zone. Undo must never delete an
  occurrence the user has edited. All completion entry points share one path.
- Checklists are bounded task-local steps, not a new hierarchy/project system.
- Reminders are optional and inexact (WorkManager/platform scheduling may delay
  them). Ask notification permission only when the user enables reminders.
- Export/import uses the Android document picker, no broad storage permission.
  Never replace the user's database silently or partially import invalid data.
- Google authorization is optional and belongs only to Vault. Task/event data
  remain local; OAuth credentials/tokens must not be logged or committed.
- Drive scope must remain `drive.file`/app-specific unless the user explicitly
  chooses broader access. Describe the actual accessible file set honestly.
- No automatic upload, destructive remote changes, analytics, or publication.

## Acceptance evidence

| Slice | Required proof |
|---|---|
| Task extensions | Boundaries rejected; old schema migrates intact; recurrence handles calendar/DST and missed dates; repeat completion is idempotent; undo preserves edited successors |
| Task/Timeline UI | Editor saves extensions from every entry point; search combines with category filter; day/week navigation remains consistent; large text and empty results have usable controls |
| Reminders/widget | Permission denial is recoverable; done/deleted/changed reminders do not fire stale notifications; duplicate notifications suppressed; widget shows at most three real active tasks and opens the app |
| Export/import | Round-trip in an empty database; invalid/oversize files rejected before writes; failure rolls back; existing task/event data never silently overwritten |
| Vault | Authorization cancellation/revocation handled; file selection remains scope-limited; paging/search/folder mapping; cache cap and offline open; partial downloads/uploads recover without corrupting cached files |
| Release | Full affected tests, debug/test/optimized release builds, lint, migration instrumentation, UI/runtime and signed update gates; live OAuth evidence separately recorded |

Track results in STATUS.md per completed slice. Run one coordinated Gradle job
at a time. Reuse unchanged passing results; rerun affected checks after changes.
Use isolated fixture data for instrumentation. Do not capture screenshots under
the existing user preference. Never claim TalkBack, performance, live OAuth,
signed update preservation, or a real campus week from compilation alone.

## External prerequisites

- Android device or healthy emulator for runtime acceptance.
- User-owned Google Cloud Drive/OAuth setup for live integration testing.
- Local release keystore/signing configuration for signed install/update gates;
  repository policy reserves key generation to the user.
- User's real campus-week trial and final release decision.

These prerequisites can block final release readiness while independent
implementation continues. Record the exact blocked checks rather than claiming
the application is ready with only the publish action left.
