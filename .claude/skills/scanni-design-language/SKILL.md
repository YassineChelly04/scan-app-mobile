---
name: scanni-design-language
description: Scanni's visual design system — Material 3 token discipline, spacing grid, shape and typography scale. Use whenever creating or modifying any Compose UI in this repo so screens stay coherent, branded and modern.
---

# Scanni Design Language

Scanni looks like one product, not a collection of screens. Every visual decision
flows from Material 3 tokens — never hardcoded colors or ad-hoc sizes.

## Color

- **Always** use `MaterialTheme.colorScheme.*` roles. Never `Color(0xFF...)` in a
  screen — brand colors live only in `ui/theme/Theme.kt`.
- Role usage: `primary` = brand actions (FAB, save, selected states);
  `tertiary` = the scan accent (steadiness ring, locked quad, success);
  `surfaceVariant` = card/thumbnail backgrounds; `onSurfaceVariant` = captions.
- The **camera and crop screens are the exception**: they sit on true black
  (`Color.Black`) with white chrome at 40–55% alpha scrims, because camera UIs
  must be legible over arbitrary live content.
- Dynamic color (Android 12+) is a user setting — code must look right with ANY
  seed color. Never assume primary is blue.

## Spacing — the 4dp grid

- All padding/sizes are multiples of 4dp. Screen gutters: **16dp** (dense lists)
  or **20–24dp** (content pages). Related elements: 8dp. Sections: 16–24dp.
- Lists/grids: 12dp between cards, `contentPadding` bottom ≥ 96dp when a FAB
  floats above the content.

## Shape

- Use `MaterialTheme.shapes`: `small` (10dp) thumbnails & chips, `medium` (16dp)
  cards & page previews, `extraLarge`/`CircleShape` for pills, search fields,
  camera controls.
- Full-bleed images get clipped (`Modifier.clip(...)`) — never square-cornered
  raw images inside padded layouts.

## Typography

- One style per role: screen titles `headlineMedium`/`titleLarge`, card titles
  `titleSmall`, metadata `bodySmall` + `onSurfaceVariant`, chip/labels
  `labelLarge`/`labelSmall`. Never set raw `fontSize` in screens.
- Single-line text that can overflow always gets `maxLines = 1` +
  `overflow = TextOverflow.Ellipsis`.

## Hierarchy rules

- One primary action per screen (FAB or filled Button). Everything else is
  tonal, text or icon level.
- Content first: chrome (bars, sheets) uses `surface`; only selection/branding
  moments use containers (`primaryContainer`, `secondaryContainer`).
