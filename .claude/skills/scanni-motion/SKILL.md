---
name: scanni-motion
description: Motion, animation and haptic feedback rules for Scanni's Compose UI. Use when adding or changing any UI interaction, transition, list mutation or camera feedback so the app feels alive but never sluggish.
---

# Scanni Motion & Feedback

Motion explains causality; it is never decoration. Every animation answers
"what just happened?" in under a third of a second.

## Durations & easing

- Micro feedback (press scale, color change): **120–180ms**, `tween`.
- State/content changes (chip select, visibility, crossfade): **200–300ms**.
- Navigation transitions: **280ms** slide+fade (already wired in `ScanniNavHost`).
- Never exceed 450ms for anything the user waits on.

## The standard catalog (stable APIs only)

- Appear/disappear: `AnimatedVisibility` with `fadeIn/fadeOut` + `scaleIn/scaleOut`
  (FABs, badges) or `slideInVertically` (bars, sheets-like panels).
- Swap two faces of one element: `AnimatedContent` (header ↔ selection header,
  hint text changes) or `Crossfade` (tab bodies).
- Continuous values: `animateFloatAsState` / `animateColorAsState` with a `label`.
- List mutations: `Modifier.animateItem()` on LazyGrid/LazyList items so
  add/remove/reorder slide instead of teleporting.
- Live camera geometry (the detection quad) is **already temporally smoothed**
  by `QuadStabilizer` — do NOT add a second animation layer on top.

## Haptics — the feedback map

Use `LocalHapticFeedback`:
- Long-press entering selection or drag mode → `HapticFeedbackType.LongPress`.
- Capture fired / document locked → `LongPress` (strong, single).
- Never vibrate on plain taps, scrolls or every frame of a drag.

## Camera-specific

- Shutter press: scale to ~0.86 and back (140ms).
- Capture confirmation: white flash overlay snapped to ~0.65 alpha, animated to
  0 over ~420ms — plus haptic. Both already exist; keep them in sync.
- Steadiness ring: driven directly by `steadyProgress` (no extra easing — the
  data is the animation).

## Don'ts

- No infinite/looping animations outside progress indicators.
- No animation that blocks input (always `tween`, never delays on click paths).
- Don't animate text size; swap styles via `AnimatedContent` if needed.
