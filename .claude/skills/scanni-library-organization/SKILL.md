---
name: scanni-library-organization
description: Browse, search, folder and multi-select UX for Scanni's library/home screen. Use when changing the document grid, search field, folder bar, selection mode or any bulk action so the library stays scannable, fast to filter and safe to manage.
---

# Scanni Library & Organization

The library is the home screen — the first thing users see and the place they
return to. It must read as a wall of documents they can scan with their eyes,
filter in one tap, and manage without fear. All of this lives in
`ui/library/LibraryScreen.kt`.

## Grid first

- Documents render as a `LazyVerticalGrid(GridCells.Adaptive(minSize = 156.dp))`
  — adaptive columns so it works on any width. Cards are `ElevatedCard` with a
  3:4 `aspectRatio` thumbnail, a `titleSmall` title, and a `bodySmall`
  "date · N pages" line (pluralized via `pluralStringResource`).
- New/changed cards use `Modifier.animateItem()` so they slide rather than
  teleport. Bottom `contentPadding` is **96dp** so the last row clears the FAB.
- Title and metadata are always `maxLines = 1` + `TextOverflow.Ellipsis`.

## One scan action, always reachable

- A single `ExtendedFloatingActionButton` ("Scan") is the only primary action.
  It `scaleIn/fadeIn` on entry and **disappears in selection mode** — the FAB and
  the selection toolbar never coexist.

## Search is instant and in-place

- The `SearchField` pill (soft, filled-tonal, clearable) sits in the header and
  filters as you type. A non-empty query swaps the **whole body** to a
  `LazyColumn` of `ListItem`s — it does not filter the grid in place.
- Results show FTS snippets with matches bolded (`parseSnippet` turns `<b>…</b>`
  into bold spans). No snippet → show the date instead.
- Empty query = browse mode. Query with no hits = `EmptyState` with the query
  echoed ("No results for …"), never a blank list.

## Folders as chips, not a separate screen

- The `FolderBar` is a horizontal `FilterChip` row: "All documents", one chip per
  folder showing `name · count`, and a "New" chip. No nested navigation.
- Tapping the **already-selected** folder opens its management `DropdownMenu`
  (Rename / Delete) — there is no separate edit mode. Tapping an unselected
  folder just switches to it.
- Folder create/rename use `TextInputDialog`; delete uses `ConfirmDialog`.

## Selection mode — entered by touch, exited safely

- **Long-press** any card enters selection mode with a `LongPress` haptic; while
  in it, a plain tap toggles (via `combinedClickable`). Outside it, tap opens.
- The header `AnimatedContent`-swaps to a selection bar: Close (clears), the
  selected count (`titleLarge`), Move, Delete. Selected cards switch to
  `secondaryContainer` and show a `CheckCircle` badge.
- **Bulk delete is always behind `ConfirmDialog`** with a pluralized count
  (`R.plurals.delete_documents_message`). Move opens `MoveToFolderSheet`
  (a `ModalBottomSheet`), including a "No folder" option — never a dialog for a
  list pick.
- All transient screen state (`showMoveSheet`, `deleteFolderTarget`, …) is
  `rememberSaveable` so a rotation never drops a half-finished action.

## States & copy

- Empty library = friendly `EmptyState(DocumentScanner, title, body)` that points
  at scanning — see scanni-stateful-screens. Missing thumbnails fall back to the
  `surfaceVariant` placeholder, never a white gap.
- Every count and label is pluralized and lives in `strings.xml` **and**
  `values-ar/strings.xml`; chips and titles stay `maxLines = 1` so Arabic and
  long names don't clip.
