# 06 - Acceptance Criteria for Riko Hub v0.1

v0.1 is "usable" only when every box is checked, or each unchecked box has a documented reason in `STATUS.md` accepted by Miyu.

Legend: `[ ]` open, `[x]` verified. Verification method in brackets: (T) automated test, (M) manual, (R) code review.

## FUNCTIONAL

- [ ] Create a task with only a title (T/M)
- [ ] Create a task with category, deadline, priority, notes (M)
- [ ] Edit every task field and see it persist (T/M)
- [ ] Mark a task done from Home, Tasks and the peek sheet (M)
- [ ] Mark a done task as not done; `completedAt` clears (T)
- [ ] Delete a task; undo restores it within 5 seconds (M)
- [ ] Task buckets (Terlambat, Hari ini, Besok, Minggu ini, Nanti, Selesai) place tasks correctly across midnight and week boundaries (T)
- [ ] Category filter works on Tasks (M)
- [ ] Quick add from Home creates a task in under 10 seconds with one thumb (M)
- [ ] Create, edit, delete and undo an event (M)
- [ ] Timeline merges events, task deadlines and completed tasks in correct order (T)
- [ ] Settings changes (theme, name, navigation style) apply immediately and persist (M)

## DATA PERSISTENCE

- [ ] Tasks and events survive app restart (M)
- [ ] Tasks and events survive process death (M: kill via adb) 
- [ ] Soft-deleted items do not appear anywhere (T)
- [ ] `status == DONE` iff `completedAt != null` always (T)
- [ ] Room schema exported and committed (R)
- [ ] `allowBackup=false` set (R)
- [ ] Release build starts with an empty database (M)

## OFFLINE

- [ ] App works fully in airplane mode (M)
- [ ] Manifest does NOT declare `INTERNET` permission in v0.1 (R)
- [ ] No fake online/sync indicators anywhere (M/R)
- [ ] Vault and Sync screens clearly show "not connected / coming soon" (M)

## VISUAL

- [ ] Light and dark themes both follow `07` tokens (R/M)
- [ ] Neither theme uses pure white or pure black backgrounds (R)
- [ ] No screen consists of a monotonous grid of identical cards (M)
- [ ] Home is visibly more expressive than Tasks; Tasks is visibly calmer than Home (M)
- [ ] Bubbles appear on Home only (R/M)
- [ ] The four domains are distinguishable at a glance by accent and shape cue (M)
- [ ] Urgent tasks are visually distinguishable on Home by size, position and highlight (M)
- [ ] Layout is intact at 360dp, 393dp, 412dp and 600dp widths (M)
- [ ] No text clipped at 130% and 200% font scale (M)

## UX

- [ ] Navigation is understandable without a tutorial: edge hints and compass chip make all five screens discoverable (M with a fresh user)
- [ ] Swipe up/down/left/right on Home reach Tasks/Timeline/Vault/Sync correctly (M)
- [ ] System back from any non-Home screen returns to Home (M)
- [ ] Swipes do not trigger Android system back gesture accidentally (edge exclusion works) (M)
- [ ] Scrolling Tasks or Timeline never accidentally navigates away (M)
- [ ] Home reflects completion immediately: bubble recedes, progress advances (M)
- [ ] All-clear state: Home becomes calm (M)
- [ ] 6+ urgent tasks: Home becomes denser but remains legible (M)
- [ ] Empty, error and loading states exist for each screen (M)
- [ ] All copy is Indonesian, short, warm, and non-guilt-inducing (R)

## PERFORMANCE

- [ ] Cold start to interactive Home under 1.5 s on a mid-range device (M)
- [ ] Home idle drift stays smooth (no visible jank) with 7 bubbles (M)
- [ ] Idle drift stops when app is backgrounded or reduced motion is on (M/R)
- [ ] Tasks list with 500 seeded tasks scrolls smoothly (M)
- [ ] No ANR or main-thread DB access (R)
- [ ] Release APK size under 25 MB (M)

## ACCESSIBILITY

- [ ] Every interactive element has at least a 48dp touch target (M/R)
- [ ] Text contrast at least 4.5:1 (body) and 3:1 (large) in both themes (M)
- [ ] TalkBack can reach and operate every screen, including bubbles and the compass chip (M)
- [ ] Bubble content descriptions include title, priority, and deadline (R/M)
- [ ] Every gesture has a non-gesture alternative (M)
- [ ] Reduced motion replaces slides with fades and disables drift (M)
- [ ] Meaning is never conveyed by color alone (M)

## CODE QUALITY

- [ ] Project builds from a clean checkout with `./gradlew assembleDebug` (M)
- [ ] Unit tests pass: `UrgencyCalculator`, `BubbleLayoutPlanner`, `TaskGrouping`, `TimelineBuilder`, repository invariants (T)
- [ ] DAO instrumented tests pass (T)
- [ ] No file over ~300 lines without a reason noted (R)
- [ ] No hardcoded colors or user-facing strings in composables (R)
- [ ] No unapproved dependencies (R)
- [ ] Previews exist for reusable components (R)
- [ ] `// TODO(v0.x)` notes exist for deferred features and are listed in `STATUS.md` (R)
- [ ] No unused code from later roadmap phases (R)
- [ ] Lint has no errors (M)

## PRIVACY

- [ ] No analytics, ads, crash SDKs or network libraries (R)
- [ ] No data leaves the device (M: check with network monitor)
- [ ] No secrets or keystores in the repo (R)

## FINAL GATE

- [ ] Riko uses it for one real campus week and reports no blocking problems (M, Miyu/Riko decides)
