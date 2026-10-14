# Contributing

Riko Hub is a personal, offline-first Android planner with an optional Linux Companion. Good contributions make a daily workflow clearer, safer or easier to recover, while preserving local data ownership. The Project Owner reviews architectural and design decisions; a proposal or PR is an invitation to discuss, not automatic acceptance.

## Find a useful task

Read the [orientation](00_START_HERE.md), [feature status](01_FEATURES.md) and [current evidence](../STATUS.md). For future work, use the [Renaissance backlog](../renaissance/09_EXECUTION_BACKLOG.md) as a proposal list, then confirm scope with the Project Owner before taking a large architectural or design change. Small documentation fixes, reproducible bugs and focused accessibility improvements make good first contributions.

Keep one behavior or document goal per change. Put UI in `ui/`, pure planning rules in `domain/`, Room and repositories in `data/`, Android background work in `work/`, and Companion server/CLI behavior in `companion/riko_companion/`. Update nearby documentation when the behavior, protocol, setup or evidence status changes. Never turn a test file into a claim that its test passed.

## Branches in this checkout

These descriptions come from **current Git refs and commit subjects**, not a policy that every branch is active. Remote `origin/` refs mirror the local branch names shown here. `feature/claude-widget` is checked out in a separate worktree; do not alter it from this checkout.

| Branch | Quick explanation |
|---|---|
| `main` | Base branch at the early planner/Home implementation checkpoint. |
| `feat/rikohub-v0.1-foundation` | Another ref at the same foundation commit as `main`. |
| `checkpoint/rikohub-phase6` | Preservation checkpoint for v0.2 backup and native widget work. |
| `experiment/claude-widget-refinement` | Widget experiment ref at that same preservation checkpoint. |
| `feature/claude-widget` | Later owner-approved widget checkpoint in a separate worktree. |
| `feature/v03-materials-vault` | v0.3 Materials Vault and widget integration checkpoint. |
| `feature/v04-laptop-companion` | Current branch; HEAD contains the v0.5 LAN handoff MVP commit, while newer v0.6 work is uncommitted and unverified. |

Choose a descriptive topic branch from the agreed base. The repository shows both `feat/` and `feature/` prefixes; there is no single enforced naming rule in source. Before editing, inspect `git status` and preserve existing work. Use concise imperative commit subjects with a useful scope, such as `docs: clarify Companion pairing`; existing history also uses `feat:`, `test:` and `chore:`. Do not rewrite someone else’s branch history.

## Discuss and submit

For a design proposal, show the user problem, a small workflow or sketch, alternatives, and effects on large text, keyboard/TalkBack, reduced motion and small screens. Review references with the Project Owner before calling a visual direction approved. The [design direction](07_DESIGN_DIRECTION.md) documents current behavior and open questions.

For a pull request, include:

- The concrete problem and resulting behavior, with scope and related issue/backlog item if one exists.
- Files or data formats touched, migration/recovery implications, and screenshots only when they are real and reviewed.
- Checks actually run, their environment and result; clearly list checks left for Riko.
- Any new permission, network route, external API, stored secret or transfer consent path.
- Documentation updates and known limitations.

For a bug, give exact steps, expected and observed behavior, app revision, Android/Linux version, and **redacted** logs. For a feature suggestion, describe the workflow and why existing tasks, course views or handoff cannot already solve it. Do not share pairing payloads, tokens, signing keys, private file names or personal records in issues or screenshots. Changes to trust, files, backup, migrations or Drive access need an explicit data-safety review and owner acceptance.

[Getting started](05_GETTING_STARTED.md) · [Architecture](03_ARCHITECTURE.md) · [Roadmap](02_ROADMAP.md)
