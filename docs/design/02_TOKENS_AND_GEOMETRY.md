# Tokens and Geometry (Design Proposal)

This document translates the preferred Graphite & Tangerine direction into a semantic design vocabulary. These are proposals for the redesign, not owner-approved numeric values. Existing exact values in [`docs/07_DESIGN_TOKENS.md`](../07_DESIGN_TOKENS.md) belong to the earlier design system and must not silently be presented as approval of this direction. The reference images do not establish exact hex colors, dp geometry, typography, or responsive rules.

## Principles

- Keep the interface warm and playful while retaining a mature, calm tone and readable information density.
- Give each page its own layout character while sharing a compact visual vocabulary.
- Use organic contours as structure and interaction feedback, not as noise behind every control.
- Keep content legible over dark surfaces. Orange is a signature accent, not the only signal for status or selection.
- Preserve native behavior and offline use. Decoration must not require a network asset or obscure the function of a control.

## Semantic color roles

| Role | Proposed appearance | Use |
|---|---|---|
| `canvas` | Deep graphite | Main app background; avoid pure black. |
| `surface` | Raised charcoal | Cards, sheets, and grouped content. |
| `surfaceQuiet` | Slightly differentiated graphite | Secondary rows and inset fields. |
| `inkPrimary` | Warm near-white | Main titles and values. |
| `inkSecondary` | Muted cool gray | Supporting text; maintain readable contrast. |
| `accentPrimary` | Tangerine / warm orange | Main selected state, primary action, and signature glow. |
| `accentSecondary` | Restrained jade, blue, or magenta | Domain identity where useful; avoid giving every card a saturated gradient. |
| `statusPositive` | Accessible green/jade | Confirmed completion or connection only. |
| `statusAttention` | Warm coral or amber | Due/attention states with a textual cue. |
| `outlineSubtle` | Low-contrast warm graphite edge | Surface boundaries and focus separators. |

Color values, gradient stops, contrast ratios, and light-theme behavior require later design and implementation review. Do not sample a screenshot pixel and claim it as a canonical token. Status must remain understandable without color alone.

## Type roles

Use a compact, highly legible rounded sans-serif system with clear separation between display and functional text. The board suggests a bold friendly display face, handwritten lettering as occasional illustration, and a neutral readable UI face; it does not identify actual fonts.

| Role | Intended use |
|---|---|
| `display` | Short page greeting or hero title; sparse use. |
| `title` | Page titles and prominent section headings. |
| `body` | Task names, event titles, and instructional text. |
| `support` | Descriptions and secondary labels. |
| `metadata` | Dates, times, counts, and compact supplementary details. |
| `action` | Buttons, selected destinations, and filter labels. |

Exact family, weight, size, line height, and scaling limits remain to be chosen. Dense pages must retain readable text and reflow with system font scaling; avoid shrinking metadata until it is decorative.

## Geometry and spacing

Use a small spacing scale based on a consistent base unit, then tune against real device sizes and font scaling. Keep content aligned even when the illustration or header contour is asymmetric. Prefer consistent horizontal gutters, deliberate section spacing, and predictable touch areas over pixel imitation of the mockups.

| Semantic token | Proposal |
|---|---|
| `spaceBase` | Smallest layout increment; choose a 4dp-based scale if compatible with the implementation. |
| `screenGutter` | Consistent side inset, adjusted for device and safe area. |
| `sectionGap` | Clear pause between content groups. |
| `rowGap` | Tight but distinguishable spacing between related rows. |
| `touchMinimum` | At least 48dp for interactive targets. |
| `shapeControl` | Rounded compact controls for chips and filters. |
| `shapeCard` | Large-radius panels; vary size and grouping to avoid a uniform tile grid. |
| `shapeOrganic` | Custom curved path reserved for hero surfaces, the Task Workspace, and selected navigation transitions. |

Organic header and lava contours should be drawn as scalable paths or native shapes so they adapt to width and font size. A curved edge may visually connect a hero and content region, but must not alter the hit area or make text overlap. Do not infer fixed radii from the device frames in the references.

## Source geometry observations

These relations describe visible composition, not approved measurements. The asset status column follows the manifest; every numeric dimension, radius, path, and spacing remains unspecified by the owner.

| ID and source | Status | Observed geometry relationship | Design implication |
|---|---|---|---|
| A1 — [`ChatGPT Image Oct 13, 2026, 08_17_43 AM-1.png`](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_43%20AM-1.png) | CANDIDATE | Portrait phone screen; broad irregular wave spans the header behind the focus area. Narrow course cards sit in a horizontal strip; date/deadline cards form a separate row; dense lower panels use paired columns. | Keep the hero contour behind content without reducing text contrast. Let narrow course cards scroll or reflow; protect dates and actions from clipping. |
| A2 — [`ChatGPT Image Oct 13, 2026, 08_17_45 AM-2.png`](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_45%20AM-2.png) | CANDIDATE | Tall hero shares width between project summary and checklist. Project rows are wide horizontal bands with progress and tags; a seven-day sprint band precedes two lower panels. | Preserve the split hierarchy only where available width and text scale allow; allow rows and lower panels to stack. |
| A3 — [`ChatGPT Image Oct 13, 2026, 08_17_46 AM-3.png`](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_46%20AM-3.png) | CANDIDATE | Moonlit window, sleeping cat and plants occupy a deep illustrated header. Rest/mood and habit/reminder sections use paired panels, followed by a wide journal surface. | Keep the illustration in a bounded hero area. Sensitive health, mood, or finance surfaces need separate approval. |
| A4 — [`ChatGPT Image Oct 13, 2026, 08_17_48 AM-4.png`](../../redesign-idea/ChatGPT%20Image%20Oct%2013,%202026,%2008_17_48%20AM-4.png) | CANDIDATE | Header device tableau leads to a connected pair of device cards, then a full-width queue; transfer and clipboard sit side by side; trust and history occupy full-width bands. | Connection lines and states need non-color labels. Paired cards must adapt to narrow widths and offline/unpaired states. |
| A5 — [`Riko Hub_ Cozy Academic Homebase.png`](../../redesign-idea/Riko%20Hub_%20Cozy%20Academic%20Homebase.png) | UNKNOWN | Landscape board arranges five independent portrait screens side by side. Repeated nav bars and orange organic contours form the shared shell. | Do not treat board layout as a tablet layout or canonical navigation order. Owner authorized proposed Today/Plan mapping; acceptance and board approval remain unknown. |

The owner-approved Task Workspace has no source image. Its animated lava-like organic header and expanding task surface are requirements; their path and relationship to the content area must be proposed and reviewed without claiming reference-derived geometry.

## Surface and illustration use

- Build foreground/background separation with tonal surface changes and restrained edge highlights. Glows are occasional accents around a primary focal point.
- Keep decorative stars, dots, plants, and mascot illustrations outside critical reading and touch areas.
- Illustrations may be a future branded system; the supplied cat artwork is reference material and does not establish a required mascot asset or licensing/source pipeline.
- Reserve vivid gradients for the primary hero or active state. Use semantic flat colors for status, category, and action controls.
- Keep the selected destination apparent through icon and label treatment; a contour alone is insufficient.

## Unresolved choices

The owner has not supplied exact color values, font choices, spacing/radius values, theme behavior, or contrast verification for the new direction. The legacy token document contains exact values for its own palette, so any reuse or replacement needs a deliberate design decision. The final token set should be documented before UI implementation hard-codes visual values.
