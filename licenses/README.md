# Bundled fonts

Nunito and Fredoka are distributed under the SIL Open Font License. Licenses are included here.

Sources, retrieved 2026-10-05:
- https://github.com/google/fonts/tree/main/ofl/nunito
- https://github.com/google/fonts/tree/main/ofl/fredoka

Static instances were generated with fontTools from the upstream variable TTFs: Nunito weights 500, 600, 700, 800; Fredoka weight 600, width 100. No outlines were changed. Bundled fonts work offline; no font service or runtime network request is used.

## Material Symbols

The three bundled rounded vector assets (cloud, smartphone, laptop_chromebook) come from Google's Material Symbols repository:
https://github.com/google/material-design-icons/tree/master/symbols/android

They are distributed under Apache License 2.0 (included). These are local vector resources, not a runtime icon library.

Vector XML theme tint attributes were removed; Compose applies the app color tokens. Vector paths are unchanged.
