---
name: scanni-stateful-screens
description: Every Scanni screen must design empty, loading, progress, error and success states explicitly. Use when building or reviewing any screen or async flow (OCR, rendering, export) so no state ever shows a blank or frozen UI.
---

# Scanni Stateful Screens

A screen is not done when the happy path renders. It is done when **every**
state a user can hit renders something intentional.

## The five states checklist

For each screen/async surface, answer all five:

1. **Empty** — first-run or zero results. Use `EmptyState(icon, title, body)`
   with guidance copy ("Point your camera at a document…"), never a blank pane.
2. **Loading (indeterminate)** — only when < ~1s and unavoidable:
   `CircularProgressIndicator` centered over the space the content will fill.
3. **Progress (determinate / long work)** — OCR, saving, exporting: show what is
   happening in words ("Reading text on your pages…") + `LinearProgressIndicator`.
   Disable the triggering button and change its label (Save → Saving…).
4. **Error** — message + a `Retry` affordance inline (not only a toast). Toasts
   are acceptable only for fire-and-forget failures (share/import).
5. **Success/Content** — and its degraded variants (page with no recognized
   text says so; missing thumbnail shows `surfaceVariant` placeholder).

## Patterns used in this codebase

- Image slots always render a `surfaceVariant` placeholder (`PageImage(null)`)
  — never white gaps while files load.
- Long renders keep the **previous** image visible with a spinner on top
  (Review previews) — avoid flashing to blank between filter changes.
- Async results arrive via `StateFlow` UI state + one-shot `SharedFlow` events;
  never block composition.
- Optimistic where safe: filter selection updates the chip instantly while the
  preview re-renders behind a spinner.

## Copy rules for states

- Say what the app is doing, not what the system is doing
  ("Reading text on your pages…", not "Running worker").
- Errors say what failed and what the user can do; no codes, no jargon.
- All state copy lives in `strings.xml` (EN) **and** `values-ar/strings.xml`.
