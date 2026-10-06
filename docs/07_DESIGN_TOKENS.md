# 07 - Design Tokens

Single source for visual values. Code MUST expose these as Kotlin constants/`CompositionLocal`s in `ui/theme`. Screens never hardcode values.

Contrast values listed are targets; Astra MUST verify the final pairs with a contrast checker and adjust within 5% lightness if any pair fails.

## 1. Color - Light

### Neutrals

| Token | Hex | Use |
|-------|-----|-----|
| `background` | `#FBF6EC` | warm ivory app background |
| `surface` | `#FFFDF8` | sheets, panels |
| `surfaceVariant` | `#F3EBDD` | quiet fills, rows |
| `outline` | `#E2D8C6` | hairlines |
| `onBackground` / `onSurface` | `#2B2630` | primary text (warm ink) |
| `onSurfaceMuted` | `#6F6877` | secondary text |
| `scrim` | `#2B2630` at 40% | sheet scrim |

### Accents and containers

| Name | Accent | Container (soft fill) | Ink on accent |
|------|--------|-----------------------|---------------|
| Lavender (Akademik) | `#A793EA` | `#E9E3FB` | `#2B2630` |
| Sky (Organisasi) | `#6DB8EE` | `#DCEFFC` | `#2B2630` |
| Mint (Personal) | `#6FD0AD` | `#D8F3E8` | `#2B2630` |
| Warm yellow (Project) | `#F4C04F` | `#FFF0C9` | `#2B2630` |
| Coral (Urgent / overdue) | `#FF7F6E` | `#FFE0DA` | `#2B2630` |
| Peach (decorative only) | `#FFB48A` | `#FFE7D8` | `#2B2630` |

## 2. Color - Dark

### Neutrals

| Token | Hex | Use |
|-------|-----|-----|
| `background` | `#17181F` | deep charcoal-navy |
| `surface` | `#1F2029` | sheets, panels |
| `surfaceVariant` | `#2A2B37` | rows, quiet fills |
| `outline` | `#3A3B49` | hairlines |
| `onBackground` / `onSurface` | `#F1ECE2` | primary text |
| `onSurfaceMuted` | `#A39EAD` | secondary text |
| `scrim` | `#000000` at 55% | sheet scrim |

### Accents and containers

| Name | Accent | Container (dark tint) | Ink on accent |
|------|--------|-----------------------|---------------|
| Lavender | `#B9A8F2` | `#3A3360` | `#1B1822` |
| Sky | `#82C6F4` | `#1F3A52` | `#14202B` |
| Mint | `#82DDBC` | `#1D4136` | `#12251F` |
| Warm yellow | `#F7CD68` | `#4A3E1B` | `#2A2208` |
| Coral | `#FF907F` | `#5A2C28` | `#2B1210` |
| Peach | `#FFC19B` | `#503528` | `#2B1A10` |

## 3. Semantic mapping

| Semantic | Rule |
|----------|------|
| Domain color | Academic=Lavender, Organization=Sky, Personal=Mint, Projects=Yellow |
| Urgent / overdue | Coral. Overrides domain tint on flags, ring and "Terlambat" bucket header only; domain identity stays on the dot/shape cue |
| Focus bubble | Solid domain accent fill, ink text, 3dp ring in `onSurface` at 20% and a soft glow (light: accent 25% blur 24dp; dark: accent 30% blur 20dp) |
| Normal bubble (light) | Container fill, 1.5dp accent outline, ink text |
| Normal bubble (dark) | Dark container fill, 1.5dp accent outline, `onSurface` text |
| Receded bubble | 35% opacity, scale 0.6, no glow |
| Disabled | `onSurface` at 38% |
| Placeholder screens | Neutral `surfaceVariant` with `onSurfaceMuted`; never an accent that suggests "online" (no green status) |

## 4. Typography

| Role | Font | Weight | Size / line height | Use |
|------|------|--------|--------------------|-----|
| `displayLarge` | Fredoka | 600 | 36 / 40 sp | Home greeting only |
| `displayMedium` | Fredoka | 600 | 28 / 32 sp | Screen titles (Tugas, Timeline, Vault, Sync), empty-state headline |
| `titleLarge` | Nunito | 800 | 22 / 28 sp | Sheet titles |
| `titleMedium` | Nunito | 700 | 17 / 24 sp | Task title, stop title, bubble title (large) |
| `titleSmall` | Nunito | 700 | 15 / 20 sp | Bubble title (small), bucket header |
| `bodyLarge` | Nunito | 500 | 16 / 24 sp | Notes, descriptions |
| `bodyMedium` | Nunito | 500 | 14 / 20 sp | Secondary lines |
| `labelLarge` | Nunito | 700 | 14 / 20 sp | Buttons, pills |
| `labelMedium` | Nunito | 600 | 12 / 16 sp | Metadata |
| `labelSmall` | Nunito | 600 | 11 / 14 sp | Edge hints |
| `mono` | JetBrains Mono (optional) | 500 | 12 / 16 sp | Device IDs in Sync only |

Rules: sentence case everywhere. No ALL CAPS labels. No more than 2 font families on a screen (Fredoka only for the displays above).

## 5. Shape tokens

| Token | Value |
|-------|-------|
| `radiusXS` | 8dp |
| `radiusS` | 12dp |
| `radiusM` | 16dp |
| `radiusL` | 24dp |
| `radiusXL` | 28dp |
| `radiusPill` | 50% (fully rounded) |
| `asymA` | topStart 32dp, topEnd 12dp, bottomEnd 32dp, bottomStart 12dp |
| `asymB` | topStart 12dp, topEnd 32dp, bottomEnd 12dp, bottomStart 32dp |
| `sheet` | top corners 32dp |

### Blob variants (Home bubbles)

Four organic shapes defined as `GenericShape` / `Path` built from cubic curves inside a unit square. Choose variant by `taskId % 4`. Define each by 8 control radii (fractions of the half-size) to keep them near-circular:

| Variant | Corner radii factors (TL, TR, BR, BL) per axis, horizontal / vertical |
|---------|-----------------------------------------------------------------------|
| A | 58/42, 52/48, 42/58, 48/52 (%) |
| B | 48/52, 60/40, 52/48, 40/60 |
| C | 55/45, 45/55, 60/40, 50/50 |
| D | 42/58, 55/45, 50/50, 58/42 |

(Pairs read: top-left horizontal/vertical radius, etc. Equivalent to CSS `border-radius: a b c d / e f g h`. Keep every bubble between 92% and 108% aspect ratio.)

## 6. Spacing tokens

| Token | dp |
|-------|----|
| `space1` | 4 |
| `space2` | 8 |
| `space3` | 12 |
| `space4` | 16 |
| `space5` | 20 (screen horizontal padding) |
| `space6` | 24 |
| `space8` | 32 |
| `space12` | 48 |
| Minimum touch target | 48 |
| Task row min height | 56 |
| Compass chip | 48 touch, 40 visual |

## 7. Elevation

Prefer tonal elevation (surface tint) over shadow.

| Level | Use | Light | Dark |
|-------|-----|-------|------|
| 0 | flat | none | none |
| 1 | rows, panels | 1dp soft shadow `#2B2630` at 6% | `surface` +4% white tint |
| 2 | sheets, compass chip | 3dp soft shadow at 10% | `surface` +8% white tint |

## 8. Motion tokens

| Token | Value |
|-------|-------|
| `durationFast` | 120 ms |
| `durationShort` | 180 ms |
| `durationMedium` | 380 ms |
| `durationLong` | 600 ms |
| `easingStandard` | cubic-bezier(0.2, 0.0, 0.0, 1.0) (emphasized decelerate) |
| `springBubble` | dampingRatio 0.75, stiffness Medium |
| `driftAmplitudeDp` | 3 |
| `driftPeriodRange` | 6000-10000 ms |
| `reducedMotion` | durations capped at 120 ms, only fades, drift off |

Helper: `rememberReducedMotion()` reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.

## 9. Iconography

- Use Material Symbols (rounded variant) via vector assets included in the project. Do not add an icon library dependency solely for icons; add only the needed vector drawables.
- Stroke weight consistent (400). Filled style for selected state in the compass chip.
- NO emoji as UI icons.

## 10. Illustration

- Empty states use simple Compose-drawn shapes (blobs, dots, small path "plant sprout", "cloud") in domain accents.
- No stock images. No external illustration files in v0.1.
- Riko mascot artwork is OPTIONAL and out of scope for v0.1 (the user may supply later).

## 11. App icon

Adaptive icon: warm ivory background, a lavender blob with a small coral dot and a compass-like four-point mark. Foreground within the 66dp safe zone. Provide monochrome layer for themed icons (Android 13+).

## 12. Forbidden values

- `#000000` and `#FFFFFF` as backgrounds
- Gold or black-gold palettes as identity
- Neon / cyberpunk gradients
- Drop shadows heavier than level 2
