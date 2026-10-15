# Evidence index and privacy boundary

## Storage and verified counts

Original local directory: `build/verification/device-qa-20261015/`, relative to this repository.
Original gallery: [evidence.html](../../../build/verification/device-qa-20261015/evidence.html).

Artifact inspection confirmed **13 runtime route rows, findings #1–#15, 64 unique gallery image references and 80 PNG files total**. All 64 gallery images exist and match their original manifest SHA-256 hashes. The directory contains 96 files overall. No device inspection was repeated.

**No screenshots, original gallery HTML or raw diagnostic logs are committed.** The gallery contains relative image references, not an independent sanitized export. Representative evidence contains owner/task text, dates and notification context; capture 01 contains unrelated lock-screen notifications. A safe screenshot redaction/export was not established. The smallest useful checkpoint is the reports, measurements, filenames and hashes, with all images local-only. No redaction tool, generated replacement image, upload or external service was used.

The committed reports replace the unique device serial with `[DEVICE_SERIAL_REDACTED]` and one existing task title with `[EXISTING_ACADEMIC_TASK]`. Those substitutions do not change any observed behavior or reproduction. The originals retain the exact private values. The synthetic marker `QA_UNSAVED_DRAFT` remains because it is the disposable reproduction input, not a saved personal journal entry.

## How to use the references

- Screenshot links below and links rebased in AUDIT.md point to original, ignored local files. They intentionally cannot resolve in a fresh clone without the owner's private evidence directory.
- Do not publish a gallery, screenshot, log bundle or personal identifier merely because it is linked from a committed document. This index does not grant permission to share evidence.
- [PROVENANCE.json](PROVENANCE.json) records hashes and sizes of all 96 original files, gallery membership, source-document redactions and the preservation context. It contains no original device serial or task title.
- [measurements.json](measurements.json) preserves numerical observations with the serial redacted. Its palette samples document the old UI; they do not select the future palette.
- Originals remain unchanged. Since `build/` is disposable/ignored, the owner should privately archive this directory before cleaning build outputs. This checkpoint cannot reconstruct an image from its hash.
- Finding IDs below mean the image is explicitly linked from that finding's paragraph. Other images are route, form, restoration or investigation context; absence of an ID does not mean an image was missing.
- Gallery exclusion and filename meaning are recorded explicitly. In particular, 53–55 and 58 are not large-font evidence; 68 is not restoration proof; 70 is the confirmed font restoration.

## Screenshot inventory (all 80 local originals)

| Original filename / local pointer | Finding IDs | In original 64-image gallery | Commit status | Privacy assessment | Interpretation / caveat |
|---|---|---|---|---|---|
| [01-home.png](../../../build/verification/device-qa-20261015/01-home.png) | Context only | No | Local-only; not committed | Unrelated notification content present | Lock screen; unrelated notifications; excluded from original gallery. |
| [02-unlock-check.png](../../../build/verification/device-qa-20261015/02-unlock-check.png) | #9 | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [03-plan.png](../../../build/verification/device-qa-20261015/03-plan.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [04-capture.png](../../../build/verification/device-qa-20261015/04-capture.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [05-vault.png](../../../build/verification/device-qa-20261015/05-vault.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [06-spaces.png](../../../build/verification/device-qa-20261015/06-spaces.png) | #10 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [07-academics.png](../../../build/verification/device-qa-20261015/07-academics.png) | #10, #14 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [08-academics-lower.png](../../../build/verification/device-qa-20261015/08-academics-lower.png) | #14 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [09-courses.png](../../../build/verification/device-qa-20261015/09-courses.png) | #10 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [10-courses-archive.png](../../../build/verification/device-qa-20261015/10-courses-archive.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [11-course-editor.png](../../../build/verification/device-qa-20261015/11-course-editor.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [12-projects.png](../../../build/verification/device-qa-20261015/12-projects.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [13-project-editor.png](../../../build/verification/device-qa-20261015/13-project-editor.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [14-projects-lower.png](../../../build/verification/device-qa-20261015/14-projects-lower.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Lower project editor, despite filename; excluded from gallery. |
| [15-projects-board.png](../../../build/verification/device-qa-20261015/15-projects-board.png) | #10 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [16-idea-dialog.png](../../../build/verification/device-qa-20261015/16-idea-dialog.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [17-personal.png](../../../build/verification/device-qa-20261015/17-personal.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [18-personal-journal.png](../../../build/verification/device-qa-20261015/18-personal-journal.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [19-journal-history.png](../../../build/verification/device-qa-20261015/19-journal-history.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [20-journal-keyboard.png](../../../build/verification/device-qa-20261015/20-journal-keyboard.png) | #1 | Yes | Local-only; not committed | Synthetic QA draft marker; journal/device context | Original UI capture; inspect with its report context. |
| [21-journal-leave.png](../../../build/verification/device-qa-20261015/21-journal-leave.png) | #1 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [22-journal-return.png](../../../build/verification/device-qa-20261015/22-journal-return.png) | #1 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [23-personal-lower.png](../../../build/verification/device-qa-20261015/23-personal-lower.png) | #13 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [24-routine-editor.png](../../../build/verification/device-qa-20261015/24-routine-editor.png) | #13 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [25-personal-return-state.png](../../../build/verification/device-qa-20261015/25-personal-return-state.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Spaces after editor dismissal, despite filename; excluded from gallery. |
| [26-devices.png](../../../build/verification/device-qa-20261015/26-devices.png) | #8 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [27-devices-lower.png](../../../build/verification/device-qa-20261015/27-devices-lower.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [28-devices-trust.png](../../../build/verification/device-qa-20261015/28-devices-trust.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [29-sync.png](../../../build/verification/device-qa-20261015/29-sync.png) | #8 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [30-settings.png](../../../build/verification/device-qa-20261015/30-settings.png) | Context only | Yes | Local-only; not committed | Owner display name and device context may be present | Original UI capture; inspect with its report context. |
| [31-settings-backup.png](../../../build/verification/device-qa-20261015/31-settings-backup.png) | Context only | Yes | Local-only; not committed | Owner display name and device context may be present | Original UI capture; inspect with its report context. |
| [32-import-picker.png](../../../build/verification/device-qa-20261015/32-import-picker.png) | Context only | Yes | Local-only; not committed | Owner display name and device context may be present | Original UI capture; inspect with its report context. |
| [33-settings-light.png](../../../build/verification/device-qa-20261015/33-settings-light.png) | #5, #7 | Yes | Local-only; not committed | Owner display name and device context may be present | Original UI capture; inspect with its report context. |
| [34-home-light.png](../../../build/verification/device-qa-20261015/34-home-light.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [35-home-lower.png](../../../build/verification/device-qa-20261015/35-home-lower.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [36-tasks.png](../../../build/verification/device-qa-20261015/36-tasks.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [37-task-editor.png](../../../build/verification/device-qa-20261015/37-task-editor.png) | #5 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [38-task-date-picker.png](../../../build/verification/device-qa-20261015/38-task-date-picker.png) | #11 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [39-task-date-draft.png](../../../build/verification/device-qa-20261015/39-task-date-draft.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [40-time-picker.png](../../../build/verification/device-qa-20261015/40-time-picker.png) | #11 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [41-task-dismiss-check.png](../../../build/verification/device-qa-20261015/41-task-dismiss-check.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [42-existing-task.png](../../../build/verification/device-qa-20261015/42-existing-task.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [43-task-details.png](../../../build/verification/device-qa-20261015/43-task-details.png) | #7 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [44-assignment.png](../../../build/verification/device-qa-20261015/44-assignment.png) | #5, #7 | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [45-course-dropdown.png](../../../build/verification/device-qa-20261015/45-course-dropdown.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [46-assignment-lower.png](../../../build/verification/device-qa-20261015/46-assignment-lower.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [47-plan-light.png](../../../build/verification/device-qa-20261015/47-plan-light.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [48-plan-day.png](../../../build/verification/device-qa-20261015/48-plan-day.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [49-event-editor.png](../../../build/verification/device-qa-20261015/49-event-editor.png) | #5, #12 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [50-event-editor-lower.png](../../../build/verification/device-qa-20261015/50-event-editor-lower.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Keyboard still visible; transient draft, then discarded; excluded. |
| [51-event-details.png](../../../build/verification/device-qa-20261015/51-event-details.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [52-spaces-light.png](../../../build/verification/device-qa-20261015/52-spaces-light.png) | #6 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [53-spaces-large.png](../../../build/verification/device-qa-20261015/53-spaces-large.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Failed font-change attempt; still 0.9, not large-text evidence. |
| [54-home-large.png](../../../build/verification/device-qa-20261015/54-home-large.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Failed font-change attempt; still 0.9, not large-text evidence. |
| [55-plan-large.png](../../../build/verification/device-qa-20261015/55-plan-large.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Failed font-change attempt; still 0.9, not large-text evidence. |
| [56-system-display.png](../../../build/verification/device-qa-20261015/56-system-display.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Android Display Settings; not an application UI finding. |
| [57-font-original.png](../../../build/verification/device-qa-20261015/57-font-original.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Original Small font-setting screen (0.9). |
| [58-current-large.png](../../../build/verification/device-qa-20261015/58-current-large.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Failed font-change attempt; still 0.9. |
| [59-font-changed.png](../../../build/verification/device-qa-20261015/59-font-changed.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Android font slider changed; measured 1.6; not application UI. |
| [60-plan-font16.png](../../../build/verification/device-qa-20261015/60-plan-font16.png) | #3 | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [61-home-font16.png](../../../build/verification/device-qa-20261015/61-home-font16.png) | #3 | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [62-capture-font16.png](../../../build/verification/device-qa-20261015/62-capture-font16.png) | #3 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [63-vault-font16.png](../../../build/verification/device-qa-20261015/63-vault-font16.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [64-spaces-font16.png](../../../build/verification/device-qa-20261015/64-spaces-font16.png) | #6 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [65-academics-font16.png](../../../build/verification/device-qa-20261015/65-academics-font16.png) | #2 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [66-spaces-lower-font16.png](../../../build/verification/device-qa-20261015/66-spaces-lower-font16.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [67-devices-font16.png](../../../build/verification/device-qa-20261015/67-devices-font16.png) | #2 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [68-font-restored.png](../../../build/verification/device-qa-20261015/68-font-restored.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Failed restore attempt; still Devices at 1.6, not restoration proof. |
| [69-font-restore-ui.png](../../../build/verification/device-qa-20261015/69-font-restore-ui.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Android font slider before successful restoration. |
| [70-font-restored-confirmed.png](../../../build/verification/device-qa-20261015/70-font-restored-confirmed.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Confirmed font restoration (0.9), with recorded shell getter. |
| [71-home-after-font-restore.png](../../../build/verification/device-qa-20261015/71-home-after-font-restore.png) | Context only | No | Local-only; not committed | Task/display text and schedule dates may be present | Home after font restoration; light theme still active. |
| [72-tasks-done-expanded.png](../../../build/verification/device-qa-20261015/72-tasks-done-expanded.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [73-search-empty.png](../../../build/verification/device-qa-20261015/73-search-empty.png) | #15 | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [74-settings-restored.png](../../../build/verification/device-qa-20261015/74-settings-restored.png) | Context only | Yes | Local-only; not committed | Owner display name and device context may be present | Original Dark theme restored; font 0.9. |
| [75-final-home.png](../../../build/verification/device-qa-20261015/75-final-home.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Original UI capture; inspect with its report context. |
| [76-personal-final-lower.png](../../../build/verification/device-qa-20261015/76-personal-final-lower.png) | Context only | No | Local-only; not committed | Personal/system context may be present; unredacted | Later Personal empty-state context. |
| [77-note-editor.png](../../../build/verification/device-qa-20261015/77-note-editor.png) | #13 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [78-course-editor-lower.png](../../../build/verification/device-qa-20261015/78-course-editor-lower.png) | #12 | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Original UI capture; inspect with its report context. |
| [79-weekly-slot-draft.png](../../../build/verification/device-qa-20261015/79-weekly-slot-draft.png) | Context only | Yes | Local-only; not committed | Personal/system context may be present; unredacted | Weekly slot only in unsaved course draft; discarded. |
| [80-audit-end-home.png](../../../build/verification/device-qa-20261015/80-audit-end-home.png) | Context only | Yes | Local-only; not committed | Task/display text and schedule dates may be present | Final Home with original Dark theme/font; no editor open. |

## Other original artifacts

All original file hashes, including these files, are in PROVENANCE.json. Local-only diagnostics may contain device/app context and are not a public log archive.

| Original filename | Durable treatment |
|---|---|
| `AUDIT.md` | Privacy-reviewed documentation copy committed |
| `app-warnings-final.txt` | Local-only; not committed |
| `app-warnings.txt` | Local-only; not committed |
| `device-info.txt` | Local-only; not committed |
| `evidence-manifest.json` | Local-only; not committed |
| `evidence.html` | Local-only; not committed |
| `framestats.txt` | Local-only; not committed |
| `gfxinfo-before-reset.txt` | Local-only; not committed |
| `gfxinfo-initial.txt` | Local-only; not committed |
| `gfxinfo-warm.txt` | Local-only; not committed |
| `measurements.json` | Privacy-reviewed documentation copy committed |
| `meminfo.txt` | Local-only; not committed |
| `source-review.md` | Privacy-reviewed documentation copy committed |
| `surface-inventory.md` | Privacy-reviewed documentation copy committed |
| `warm-sample-end.txt` | Local-only; not committed |
| `warm-sample-start.txt` | Local-only; not committed |

## Minimum local reading set for Maru

Inspect only as needed, privately: journal before/departure/return 20/21/22 (#1); header failures 65/67 (#2); dock 60/61/62 (#3); light surfaces 33/37/44/49 (#5); portal artwork 52/64 (#6); fields 33/43/44 (#7); offline views 26/29 (#8); Home 02 (#9); language context 06/07/09/15 (#10); pickers 38/40 (#11); new forms 49/78 (#12); Personal guidance/forms 23/24/77 (#13); repeated empty state 07/08 (#14); search 73 (#15). For #4, use preserved numerical measurements and, if available, private `gfxinfo-warm.txt`; screenshots cannot prove frame timing.

Do not use capture 01 as app evidence. The original report's capture caveats take precedence over filenames.
