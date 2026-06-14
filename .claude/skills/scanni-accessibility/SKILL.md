---
name: scanni-accessibility
description: Accessibility and assistive-tech UX for Scanni — content descriptions, TalkBack semantics, selection and live-region state, dynamic type and contrast. Use when adding or changing any Compose UI, icon, image, custom control or status text so the app stays fully usable with TalkBack, large fonts and high contrast.
---

# Scanni Accessibility

A scanner is a tool people reach for in a hurry, sometimes one-handed, sometimes
with TalkBack on. Every control must be reachable, named, and announced — not
just visible. Accessibility is part of "done", not a later pass.

## Content descriptions — name what acts, hide what decorates

- Standalone actionable icons get a real label from `strings.xml` — the
  `cd_*` convention: `cd_back`, `cd_close`, `cd_more_options`, plus screen
  scoped ones (`scanner_cd_capture`, `scanner_cd_flash_on/off`,
  `scanner_cd_gallery`). `IconButton(...) { Icon(vector, stringResource(...)) }`.
- Decorative icons that sit next to a text label pass `contentDescription = null`
  — `FilterChip` leading icons, `DropdownMenuItem` leading icons, the FAB icon
  (the "Scan" text names it), the selection `CheckCircle`. Never let TalkBack
  read "Folder, Folder, Receipts".
- **Custom-drawn / `Box`-based tappables have no intrinsic label** — give them
  `Modifier.semantics { contentDescription = ... }`. The scanner shutter and
  mode pills are the model (`ScannerScreen.kt`). The same rule applies to any
  `Column { Icon; Text }.clickable` action row (review/edit toolbars).

## Images & merging

- `PageImage` takes a `contentDescription` (defaults null). A **standalone**
  image (document pager) must pass a real label ("Page 2 of 5"). A thumbnail
  inside a card stays decorative **only if** the card is merged and its title
  names it.
- Make each grid/list card one focus stop: `Modifier.semantics(mergeDescendants = true)`
  so thumbnail + title + "date · N pages" announce as a single node, not three.

## State must be spoken, not just shown

- Selection is currently visual-only (`secondaryContainer` + `CheckCircle`).
  TalkBack will not say "selected" from color. Use `Modifier.semantics { selected = true }`
  (or `selectable`/`toggleable` with a `Role`) on selectable cards so the state
  is announced and the role is correct.
- Disabled controls keep their label; never convey disabled by alpha alone.

## Live regions — announce changes the eye would catch

- Status text that changes silently needs a live region, or a TalkBack user
  never hears it: the camera hint pill ("Looking for a document…" → "Hold
  steady" → "Captured") and progress copy ("Reading text on your pages…",
  "Saving…"). Wrap with `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`.
- Use `Assertive` only for the capture confirmation (it interrupts); everything
  else is `Polite`.

## Dynamic type & contrast

- Never set raw `fontSize` — only `MaterialTheme.typography` sp styles, which
  scale with the user's font size. Audit fixed-height chrome (the 56/64/68dp top
  bars) at 200% font + display zoom; prefer `heightIn(min = …)` so rows grow
  instead of clipping.
- Trust M3 `on*` role pairs for contrast; don't drop essential text to
  `onSurfaceVariant` at small sizes. Camera/crop chrome keeps white on
  `Color.Black.copy(alpha ≥ 0.4)` scrims so it stays legible over any frame.
- Minimum touch target 48×48dp (see scanni-touch-ergonomics) is an
  accessibility requirement, not only an ergonomic one.

## Verify

- Turn on TalkBack and swipe through every screen: each stop is named, selection
  and progress are announced, no decorative node steals focus.
- All labels live in `strings.xml` **and** `values-ar/strings.xml`; reading order
  follows layout direction (use `start/end`, `Icons.AutoMirrored.*`).
