---
name: scanni-touch-ergonomics
description: Touch targets, gestures, one-handed reach, RTL and edge-to-edge inset rules for Scanni. Use when laying out any interactive Compose UI, gesture handler, or screen chrome so the app is comfortable on real phones in either hand and in Arabic.
---

# Scanni Touch Ergonomics

Scanni is used one-handed, over a desk, often in a hurry. Layout follows the
thumb, not the mockup.

## Touch targets

- Minimum interactive size **48x48dp** (`IconButton` provides this; custom
  tappables need explicit `size`/`padding` to reach it).
- Drag handles get a **larger logical hit area than their visual** — crop
  corners draw at ~9dp but hit-test at 28dp (`CropGeometry.hitTest` radius).
- Adjacent targets keep ≥ 8dp gaps so fat thumbs don't misfire.

## Reach zones

- Primary actions live in the **bottom third**: shutter, mode selector, filter
  row, toolbar, FAB. Top bar is for exit/overflow only.
- Destructive actions are never directly under the thumb's resting position
  next to a primary action; they sit a row away or behind a confirm dialog.

## Gestures

- Every gesture has a visible affordance: drag = visible handles (corner dots,
  thumbnails that lift+scale on long-press), zoom = it's a photo (universal),
  swipe pager = peeking `contentPadding`.
- Long-press always confirms with `HapticFeedbackType.LongPress` at the moment
  the mode engages.
- Custom gestures must `change.consume()` so parents (pager, scroll) don't
  fight the drag.
- Precision tasks get a **magnifier loupe** (crop corners) — the finger must
  never hide what it's adjusting.

## RTL & localization (Arabic is first-class)

- Directional icons use `Icons.AutoMirrored.*` (back arrows, file-move).
- Use `start/end` padding semantics (`padding(start=...)`), never `left/right`.
- Test every string in `values-ar`: labels must fit pills/chips without clipping
  (keep `maxLines=1` + ellipsis on constrained text).

## Edge-to-edge

- The activity is edge-to-edge. Every full-screen layout applies
  `statusBarsPadding()` to top chrome and `navigationBarsPadding()` to bottom
  controls; `Scaffold` handles it for standard screens.
- Camera/crop draw UNDER the bars (immersive) but keep controls inside insets.
