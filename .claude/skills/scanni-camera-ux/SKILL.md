---
name: scanni-camera-ux
description: Camera-first UX rules for Scanni's scanner screen — overlay legibility, guidance copy, auto-capture etiquette, immersive chrome. Use when touching anything in ui/scanner or the live detection overlay so capturing stays fast, calm and trustworthy.
---

# Scanni Camera UX

The scanner is the product's signature moment. It must feel like the phone
*sees* the document: calm, confident, zero clutter.

## Immersive chrome

- True black background; controls are white on `Color.Black.copy(alpha=0.4-0.55)`
  scrims so they read over any live image.
- Nothing covers the center of the frame except the detection overlay itself.
  Hints live at the top, controls at the bottom.

## The detection overlay

- Quad: translucent fill (≤ 0.28 alpha) + 2.5dp stroke + corner dots. White
  while tracking; switches to `tertiary` when locked. Color IS the status.
- The quad position comes from `QuadStabilizer` (EMA-smoothed) — render it
  directly; adding animation on top causes rubber-banding.
- Never draw the overlay when detection is off (Photo mode) or unavailable —
  absence of the quad must also be a designed state (hint explains it).

## Guidance copy

- One short hint at a time, in a pill: "Looking for a document…" →
  "Hold steady" → "Captured". Text changes crossfade; the pill never jumps.
- Never instruct with more than ~4 words; the user is holding a phone up.

## Auto-capture etiquette

- Fires only when: steady for the full duration AND auto-capture enabled AND
  cooldown elapsed (≥ 2s since last shot). Manual shutter always works.
- Every capture confirms twice within 150ms: white flash + haptic. The page
  count badge updates immediately (the stack thumbnail is the receipt).
- After capture, re-arm silently — never freeze the preview.

## Controls

- Shutter: 82dp ring; the steadiness progress draws AROUND it so the finger
  resting on the shutter watches the ring close. Locked = full ring in accent.
- Mode switcher: horizontal pills above the shutter (Lens convention), selected
  pill in `primary`, animated color, one tap to switch — never a dropdown.
- Flash, gallery, page-stack: 40-48dp scrim buttons in the corners.
- Tap-to-focus anywhere on the preview (handled by PreviewView touch listener).
