# 05 - Rules for GPT-6 Astra

These are direct instructions to the implementing agent. They apply for the whole project lifetime.

## A. Mandatory rules

1. **Read every `.md` file in this pack before writing any code.** Start with `README.md`, then follow its reading order.
2. **Treat these files as the project source of truth.** If code and docs disagree, the docs win. If the docs are wrong, say so in your report and ask Miyu; do not silently diverge.
3. **Do not redesign Riko Hub into a generic productivity app.** No bottom tab bar as the primary navigation, no default Material dashboard, no identical card grids. Re-read `01` §1 and §4 whenever unsure.
4. **Do not implement later roadmap features early.** No Google Drive, no sync, no networking, no notifications, no recurring tasks in v0.1, even if "it would be easy".
5. **Work incrementally.** One roadmap phase at a time, in order. Finish and report before starting the next.
6. **Explain before a large refactor.** If a change touches more than ~5 files or alters architecture, state why in the report first and wait for approval when the change is not required by the spec.
7. **Prefer readable Kotlin over clever Kotlin.** No obscure operator tricks, no deeply nested lambdas, no reflection.
8. **Keep Compose components small.** One responsibility each. A composable over ~80 lines should be split.
9. **Avoid gigantic single files.** Soft limit 300 lines per file.
10. **Do not add libraries without a real reason.** Check `02` §13. Any new dependency needs a written justification in `STATUS.md` (what, why, alternatives, size cost).
11. **Do not remove working behavior** because another architecture is preferred.
12. **Keep the project buildable after every phase and after every meaningful commit.**
13. **Test your changes.** Run unit tests and the build. Run instrumented/UI tests where the phase specifies them. Report actual results, never assumed ones.
14. **When something is ambiguous, choose the smallest reasonable implementation consistent with the spec**, and note the assumption in `STATUS.md`.
15. **Do not modify unrelated areas.** Keep diffs focused.
16. **Do not silently change the visual or product direction.** Colors, shapes, navigation model and tone are product decisions.
17. **Maintain TODO notes for deferred features.** Format: `// TODO(v0.3): <what>` and list them in `STATUS.md` under "Deferred".
18. **Use fake or preview data where needed during early UI work.** Never ship fake data in release builds.
19. **Compose previews are encouraged.** Every reusable component SHOULD have a `@Preview` (light and dark where it matters).
20. **Never pretend an unfinished integration works.** No fake "Online" badges, fake sync times, fake files, fake storage numbers.

## B. Additional rules

21. **Never hardcode colors, radii or durations in screens.** Use tokens from `07`.
22. **UI strings in `strings.xml`** (Indonesian default). No hardcoded user-facing text in composables.
23. **No analytics, no crash-reporting SDKs, no ads, no network permission in v0.1.** The manifest MUST NOT declare `INTERNET` in v0.1.
24. **Use the injected `Clock`** for time. Never call system time directly in logic.
25. **Do not edit generated or schema files by hand** (Room schemas are generated).
26. **Never commit secrets or keystores.** Signing passwords go in local, git-ignored properties.
27. **If you find a bug in an earlier phase, fix it with a small focused change and report it.**
28. **If a spec requirement is impossible or harmful, stop and report.** Do not invent a workaround that violates a MUST.
29. **Respect accessibility requirements as acceptance gates**, not polish.
30. **Update `STATUS.md` at the end of every phase** and `CHANGELOG.md` when a user-visible release is cut.

## C. Decision priority when rules conflict

1. Product constitution (`00`)
2. Safety, privacy and data integrity
3. Explicit MUST/MUST NOT in any spec
4. Accessibility and usability
5. UX/UI spec visual intent
6. Architecture recommendations
7. Your own preferences

Usability beats novelty. If a spatial gesture makes the app harder to use, keep the compass alternative and report.

## D. Report format (after every phase)

Reply with exactly these sections:

```
## Phase <N> report: <name>

### What changed
- Short bullet list in plain language (what Riko would notice, then technical)

### Files changed
- path/to/File.kt (new | modified | deleted): one-line reason

### Architecture decisions
- Decision, alternatives considered, why

### Tests / build result
- Command run, result (pass/fail, counts). Paste failures verbatim.
- Manual checks performed (device/emulator, font scale, dark mode, TalkBack)

### Known limitations
- Honest list. Include anything not meeting acceptance criteria.

### Deferred (TODO notes added)
- TODO(v0.x): ...

### Suggested next step
- One recommended next action and why
```

## E. Git practice (RECOMMENDED)

- Small commits with clear messages: `phase-3: add task editor sheet`.
- One branch per phase when practical.
- Never rewrite published history.

## F. Behavior when blocked

- If a tool/build/environment issue blocks you, report the exact error and what you tried. Do not paper over it.
- If you are unsure whether something is in scope, assume it is NOT in scope and ask.

## G. Definition of "done" for any task

Builds, tests pass, matches the spec, previews exist, strings are externalized, STATUS updated, report delivered.
