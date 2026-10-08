# Changelog

All user-visible changes to Riko Hub. Format loosely follows Keep a Changelog. Versions follow `09_TESTING_AND_RELEASE.md` §6.

## [Unreleased]

### Changed
- Tidied Timeline with compact navigation, quieter day labels, a narrow path gutter and clearer event hierarchy.

### Added
- Checklist completion/total progress on task rows, shown only when a checklist exists.
- Existing task extensions preserved at the v0.2 checkpoint: search, recurrence, checklists, estimates, and Timeline day mode. Reminder offsets are stored, but notifications are not delivered.
- Persistent display-name and navigation-style settings, with validation and retry/dismiss actions on save failure; dismissing a failure reveals the next pending preference failure.
- Compass-only navigation and quieter Home hints after three launches.
- Adaptive app icon, Android 12+ splash, and optimized release packaging with optional private signing.
- Weekly Timeline with events, task deadlines and completed work, week navigation, today positioning and a readable dense/large-text path.
- Event creation/editing with native date/time pickers, validation, deletion and undo.
- Home urgency bubbles with daily focus pinning, progress, next obligation, calm/dense modes and task overflow.
- Home quick add, task peek actions, completion with undo, and reduced-motion/large-text behavior. Physical-device acceptance is pending.

### Planned for 0.1.0
- Home with priority bubbles, today's focus, progress and quick add
- Tasks with categories, deadlines, priority, status, undo delete
- Timeline as a narrative weekly path of events, deadlines and completed work
- Spatial navigation around Home plus a visible compass chip
- Vault and Sync placeholders (honest, not connected)
- Light and dark themes
- Fully offline, local-only data

## [0.1.0] - not yet released

(Fill in on release: date, highlights, known issues.)

## Future (not committed)

- 0.2.0: recurring tasks, subtasks, reminders, widget, export/import
- 0.3.0: Vault with Google Drive
- 0.4.0: Laptop companion and device presence
- 0.5.0: Send-to-laptop, clipboard, transfer history
