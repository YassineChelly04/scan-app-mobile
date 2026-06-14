---
name: scanni-feedback-and-dialogs
description: Transient feedback and modal patterns for Scanni — dialogs, text input, confirmations, bottom sheets, overflow menus, toasts vs snackbars and inline progress. Use when prompting for input, confirming a destructive action, surfacing a result or showing a menu so interruptions stay rare, reversible and consistent.
---

# Scanni Feedback & Dialogs

Every interruption costs the user a beat. Scanni interrupts only when it must,
always reversibly, and always with the same handful of components so nothing
feels improvised. Shared primitives live in `ui/common/Components.kt`.

## Choose the lightest interruption that works

Inline state  >  snackbar/toast  >  bottom sheet  >  dialog. Never open a dialog
for something a chip, inline control, or sheet can do. A modal is a claim on the
user's full attention — earn it.

## Confirm only destructive, irreversible actions

- `ConfirmDialog` guards deletes (page, folder, documents) and discard. It
  defaults `destructive = true`, rendering the confirm label in
  `colorScheme.error`; Cancel is always present and is the safe dismiss.
- Reversible edits — filter, rotate, crop, reorder — **never** prompt; they just
  apply (the user can redo them). Don't dilute the confirm habit with safe
  actions.
- When the action affects a count, say the count and pluralize it
  (`pluralStringResource`, e.g. "Delete 3 documents?").

## Text input

- One field → `TextInputDialog`. It auto-focuses via `FocusRequester`, uses
  `ImeAction.Done`, treats a blank value as a no-op (confirm does nothing until
  there's text; it trims on confirm), and seeds `initialValue` for rename flows.
- More than one field belongs on a screen, not in a dialog.

## Menus and sheets

- Overflow actions (rename / move / re-run OCR / delete) live in a `DropdownMenu`
  anchored to a `MoreVert` button; each item has a decorative leading icon. Use a
  menu for *actions on this thing*.
- Picking from a list (move-to-folder) uses a `ModalBottomSheet`
  (`MoveToFolderSheet`), not a dialog — sheets scroll, hold more, and dismiss by
  swipe.

## Result feedback — inline, toast, or nothing

- **The control that starts work shows the work.** Disable it and change it:
  Save → "Saving…", and the Export button swaps its icon for an 18dp/2dp
  `CircularProgressIndicator` while exporting. No separate spinner floating
  elsewhere.
- **Toast** only for fire-and-forget confirmations with no follow-up action
  ("Text copied"). Toasts vanish and can't be acted on.
- **Errors the user can recover from** want an inline message + Retry (see
  scanni-stateful-screens), not a toast that disappears. Today's export/import
  failures use a toast — acceptable only because they're fire-and-forget; the
  moment a retry exists, move them inline.

## Wiring rules

- All modal visibility is `rememberSaveable { mutableStateOf(...) }` so rotation
  never drops an open dialog or a half-typed name.
- One-shot results (Saved, CopyText, ExportFailed) arrive as `SharedFlow` events
  collected in a `LaunchedEffect` — never derived from state, so they don't
  re-fire on recomposition.
- Cancel/confirm reuse shared `action_*` strings; every dialog, menu and sheet
  string exists in `strings.xml` **and** `values-ar/strings.xml`.
