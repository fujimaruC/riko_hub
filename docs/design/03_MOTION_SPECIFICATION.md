# Motion Specification (Design Proposal)

The references are static images; none proves that an element animates or defines duration, easing, or gesture behavior. This proposal translates the owner's approved intent into named motion behaviors while keeping timing and implementation values open for review.

## Motion principles

- Motion should explain navigation, reveal a changing task surface, or acknowledge an action.
- Keep transitions short enough to preserve a sense of direct manipulation. Avoid perpetual decorative movement that competes with reading.
- Use native lightweight animation and locally available artwork; core navigation and content must work offline.
- Never encode state only through motion. The settled screen must clearly communicate the same information.
- Respect system reduced-motion preference. Preserve the organic visual identity in the resting layout instead of replacing it with a plain static rectangle.

## Motion map

| ID | Trigger | Proposed behavior | State and accessibility notes |
|---|---|---|---|
| M1 — Custom navigation | User selects a primary destination | Selected item responds with a brief scale/shape emphasis while an organic contour or warm highlight travels to the destination; page content transitions with it. | Destination icon and label update immediately and remain clear at rest. Avoid blocking rapid navigation. Exact bar order remains an owner decision; see the manifest. |
| M2 — Lava header | Workspace enters or its content changes | Organic header contour animates as a lava-like form around the hero content. | The owner requires an animated lava-like header. Source image, amplitude, cycle length, path, and whether the motion settles or loops are unresolved. A settled/limited animation is only a proposed default, not an approved behavior; confirm the loop policy rather than silently deciding it. |
| M3 — Workspace expansion | User opens or expands the task surface | Surface grows into the available content area with coordinated list/card rearrangement and clear focus transfer. | Preserve scroll position and expose expanded/collapsed state to accessibility services. Avoid surprising content displacement. |
| M4 — Time Map | User selects a day, timeline item, or planning interval | Focus moves along the time path; the selected stop gains emphasis and adjacent items settle into view. | Selection must also be conveyed by text, shape, or outline. Avoid relying on color alone. The creative Timeline is provisionally approved as a concept, not an approved animation spec. |
| M5 — Tactile response | Tap, selection, or direct manipulation | Small pressed-state response and, where appropriate, a brief native haptic tick. | Haptics are optional and must respect system settings. Do not add haptics to passive movement or every scroll. |
| M6 — Task completion | User marks a task complete | Short check/shape transition followed by the task moving to its completed position or section. | Completion state updates independently of animation. Provide an undo path if the existing interaction supports one; do not invent persistence behavior here. |
| M7 — Candidate portal and illustration | User opens a Space or an illustration is present | Gentle entrance or parallax may add depth to the portal header. | Candidate-only, subject to A1–A4 approval. No status/content motion should imply real device, health, financial, study, or sync data. |

## Reduced motion and interruption

When system reduced motion is enabled, remove path travel, parallax, spring overshoot, and ongoing drift. Keep the same organic contour as a static shape; use a quick fade or immediate state change for feedback. Preserve the selected navigation destination, expanded/collapsed state, and completion result. Animation interruption or a second input should resolve to the latest requested state without leaving a partial visual state.

The app must remain usable when animations are disabled or finish immediately. Pause or cancel motion when a screen is no longer visible and when the app enters the background; resume only the current screen's appropriate motion when foregrounded. Avoid allocating animation work for off-screen content. If raster artwork is used, keep decoded images and derived caches bounded and release them when no longer needed. These lifecycle and resource constraints support native, lightweight behavior; they do not specify a new image pipeline.

Motion duration/easing, lava amplitude/cycle, and haptic policy are not specified numerically here. Choose and review them during implementation, including accessibility settings and slower devices. Do not interpret the proposed preference for restrained movement as approval to remove the required animated lava header.
