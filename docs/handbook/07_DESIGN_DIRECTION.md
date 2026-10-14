# Design direction

Riko Hub's visual language is being explored. The current application has a recognizable playful Android UI, but it is not the final visual identity. A total redesign is under consideration and is **not implemented**.

## What exists today

The Android app uses Jetpack Compose with a shared theme, color, shape, spacing, typography, and motion definitions under `app/src/main/java/id/riko/hub/ui/theme/`. Current screens also reuse shared components such as `ScreenFrame`. The Home experience includes a bubble-based daily view and spatial navigation; the native launcher widget has its own fixed light palette and RemoteViews constraints. These describe current source, not an approved future design system.

The source includes Nunito and Fredoka font resources and a warm, playful visual treatment in parts of the interface. That is a description of what is present today, not a mandate that the redesign retain those choices. The implementation also includes Indonesian strings in several workflows, so language and localization consistency merit review.

## Known UX considerations

- Spatial or gesture-based navigation needs clear, discoverable alternatives and predictable focus behavior.
- The launcher widget follows platform RemoteViews constraints and has a fixed light palette; it does not automatically share every in-app theme behavior.
- Small screens, larger text, long task titles, TalkBack, contrast, and reduced motion need real review as layouts evolve.
- Friendly illustrations and playful details should support attention and warmth without obscuring deadlines or controls.
- Screens with network or provider dependencies need useful loading, empty, permission, offline, and failure states.

These are design considerations, not claims that every listed limitation is currently a defect or that every accessibility behavior has been verified.

## Redesign principles under exploration

The direction is to explore expressive layouts, distinctive navigation, polished transitions, an efficient information hierarchy, and lightweight interactions. The resulting interface should still feel fast and legible during ordinary use. No color palette, type scale, component library, navigation model, or art style is approved by this page.

| Principle | Practical test |
|---|---|
| Clear hierarchy | Can someone identify the next action and its deadline quickly? |
| Lightweight motion | Does motion explain a transition without delaying or distracting from work? |
| Playful usability | Do expressive details preserve readable labels, contrast, and clear control states? |
| Small-screen fit | Can key workflows work at narrow widths and larger text sizes? |
| Accessible alternatives | Can important actions be reached without precise gestures, with useful labels and focus order? |
| Consistency | Do related screens behave similarly without forcing every feature into the same layout? |

## Sketch and reference review

Contributors can bring sketches, interaction notes, or visual references to discuss. Treat them as proposals until the Project Owner reviews them. A useful proposal names the user problem, shows the important states (including empty and error states), and explains how it works at small sizes and with accessibility services. Do not add unapproved design assets or present a reference as the final Riko Hub aesthetic.

See [contributing](06_CONTRIBUTING.md) for the proposal workflow and [roadmap](02_ROADMAP.md) for product sequencing.
