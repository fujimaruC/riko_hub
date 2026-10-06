# Riko Hub - Project Specification Pack

Riko Hub is a private, single-user, local-first Android app that works as a personal campus control center for Riko.

This folder is the **source of truth** for building it. If code and these documents disagree, the documents win unless the product owner (Miyu) explicitly changes them.

## Reading order (MANDATORY for the implementing agent)

| # | File | What it settles |
|---|------|-----------------|
| 1 | `00_PRODUCT_VISION.md` | Why the app exists, what it is not, MVP definition |
| 2 | `01_UX_UI_SPEC.md` | Look, feel, every screen, navigation, states |
| 3 | `07_DESIGN_TOKENS.md` | Exact colors, shapes, type scale, spacing, motion values |
| 4 | `08_HOME_ENGINE_SPEC.md` | Urgency algorithm and bubble layout rules for Home |
| 5 | `02_ANDROID_ARCHITECTURE.md` | Stack, packages, layers, state, DI |
| 6 | `03_DATA_MODEL.md` | Room entities, indexes, time, migrations |
| 7 | `04_ROADMAP.md` | Phases with acceptance criteria |
| 8 | `05_ASTRA_RULES.md` | Behavioral rules and reporting format |
| 9 | `06_ACCEPTANCE_CRITERIA.md` | What "v0.1 is usable" means |
| 10 | `09_TESTING_AND_RELEASE.md` | Tests, build variants, APK distribution |

`STATUS.md` is the running log the implementing agent updates after every phase.
`CHANGELOG.md` records user-visible changes per release.

## Normative keywords

| Word | Meaning |
|------|---------|
| MUST / MUST NOT | Non-negotiable. Violating it is a defect. |
| SHOULD / SHOULD NOT | Default behavior. Deviate only with a written reason in `STATUS.md`. |
| MAY | Optional. Do it only if cheap and it adds no scope. |

## Glossary

| Term | Meaning |
|------|---------|
| Hari Ini / Home | The centered, expressive screen |
| Bubble | An organic, priority-sized visual object for one task. Home only. |
| Priority | User-set value on a task (Low / Medium / High) |
| Urgency | Computed score (see `08`), distinct from Priority |
| Spatial navigation | Swipe directions mapped to where screens "live" relative to Home |
| Compass chip | Always-visible, tappable alternative to gestures |
| Receded | Visual state of a completed bubble (small, faded) |
| Astra | The implementing coding agent (GPT-6 Astra) |
| Miyu | The product owner giving direction |

## One-paragraph summary

Open the app and instantly see what matters right now. Home is playful and spatial. Tasks, Timeline, Vault and Sync are calmer and more structured. Everything works offline. Nothing leaves the phone in v0.1.

## Language rule

The app UI is in **Bahasa Indonesia**. All code identifiers, comments and these documents are in English. Strings live in `res/values/strings.xml` (Indonesian default). An English `values-en` MAY be added later.
