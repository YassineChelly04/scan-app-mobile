Date: 2026-03-28
Status: Approved in conversation, written for review

## Summary

This document defines the next review-editor slice for the Android scanner app: active-page delete and rotate actions plus drag-and-drop page reordering within the review strip. The scope builds directly on the merged manual-review crop flow already on `main`.

This slice completes the core pre-save page management workflow for review sessions:

- rotate the active page
- delete the active page
- reorder pages by dragging them in the review strip

The active page remains the only page with direct edit actions. The strip is responsible for page selection and reordering only.

## Problem

Review now supports:

- per-page crop state
- per-page enhancement mode
- page switching
- real perspective correction

But it still does not support the full page-editing workflow users need before save:

- bad pages cannot be deleted
- page orientation cannot be corrected in review
- multi-page order cannot be changed

This is still a product gap because a scanner that cannot fix page order, remove mistakes, or correct orientation is not yet a complete pre-save editor.

## Goals

This slice must deliver:

- rotate-left and rotate-right actions for the active review page
- delete action for the active review page
- drag-and-drop page reordering in the review strip
- save behavior that preserves final review order plus per-page crop, mode, and rotation

## Non-Goals

This slice does not include:

- page actions directly on thumbnails
- saved-document page editing after the review session
- OCR lifecycle work
- camera UX hardening
- document-detail page reordering after save

## Recommended Architecture

The review feature should continue to treat `ReviewViewModel` as the source of truth for the full review session:

- `pages: List<ReviewPageState>`
- `activePageIndex`

This slice extends that state model instead of introducing a second page-management surface.

`ReviewPageState` should gain page-local rotation state stored in 90-degree increments. Rotation belongs alongside crop and enhancement mode because it is part of the reviewed page output.

New review actions should be added to `ReviewViewModel`:

- `rotateActivePage(clockwise: Boolean)`
- `deleteActivePage()`
- `movePage(fromIndex: Int, toIndex: Int)`

This keeps page editing inside one focused review boundary:

- scanner owns captured drafts
- review owns page editing and ordering before save
- processing owns crop + rotation + enhancement output
- save consumes the final ordered review state

## Review Session State

`ReviewUiState` should keep the same top-level shape, but `ReviewPageState` should add:

- `rotationQuarterTurns: Int`

This value should be normalized to four states:

- `0`
- `1`
- `2`
- `3`

Meaning:

- `0` = no rotation
- `1` = 90 degrees clockwise
- `2` = 180 degrees
- `3` = 270 degrees clockwise

Behavior rules:

- crop, mode, processed output, error state, and rotation all move with the page during reorder
- deleting the active page must repair `activePageIndex` to the nearest valid page
- the user must not be left with an empty review session

If only one page remains, delete should be blocked or hidden rather than allowing an invalid empty-state save flow.

## UI Design

The review screen should keep one primary editing surface:

- reorderable page strip
- active page preview
- active-page crop editor
- active-page action row
- save and add-another-page controls

The page strip should become reorderable through drag-and-drop. The strip remains responsible only for:

- selecting the active page
- displaying page order
- drag-based reordering

The active page should expose a dedicated action row with:

- `Rotate Left`
- `Rotate Right`
- `Delete`

These actions should not appear on every thumbnail. Keeping actions on the active page prevents the strip from becoming visually overloaded and reduces accidental destructive actions during reordering.

## Interaction Rules

The review UI should behave like this:

1. Tapping a page in the strip makes it the active page.
2. Dragging a page in the strip reorders the session immediately.
3. The active-page highlight moves with the dragged page if that page is the one being moved.
4. Rotating the active page updates only that page's review state and reprocesses only that page.
5. Deleting the active page removes only that page and preserves the rest of the review session.
6. After delete, focus shifts to the nearest remaining page.

Delete confirmation is not required for this v1 slice. Immediate deletion is acceptable because the goal is fast review editing. If safety becomes an issue later, undo or confirmation can be added as a separate refinement.

## Processing Design

Processing must now respect three pieces of reviewed page state:

- crop corners
- rotation
- enhancement mode

The processing order should be:

1. decode source image
2. apply perspective crop from reviewed corners
3. apply reviewed rotation
4. apply enhancement mode
5. write processed output

Rotation should be page-local and non-destructive. Original source images remain unchanged.

The processor contract may continue to be page-local, but the reviewed page input consumed by save must include rotation.

## Save Behavior

Save must use:

- final review order
- final per-page crop corners
- final per-page rotation
- final per-page enhancement mode

This means the review session remains the source of truth for save.

Deleting a page removes it from the saved result entirely.

Reordering must change saved page order exactly as seen in review.

## Error Handling

Error handling should remain page-scoped:

- rotate processing failure affects only the active page
- deleting one page must not clear other pages’ processed outputs
- reordering must not force full-session reprocessing
- save remains blocked if any remaining page lacks a valid processed result

The review session should remain usable if one page fails after rotation or crop changes. The user should be able to switch pages, continue editing others, and fix the failing page without losing progress.

## Testing Strategy

Add ViewModel coverage for:

- deleting the active page
- preventing delete when only one page remains
- rotating the active page
- reordering pages while preserving per-page state
- active-page index repair after delete and reorder

Add save-flow coverage for:

- final saved order matching review order
- deleted pages not appearing in the saved document
- reviewed rotation being passed into page processing

Add processor coverage for:

- rotation changing output orientation or dimensions as expected
- crop + rotation combined behavior

Add UI coverage for:

- active-page rotate/delete buttons
- reorder interaction in the page strip
- active-page selection after reorder or delete

Add end-to-end flow coverage for:

- multi-page capture
- reorder in review
- save to document
- saved page count and order matching the final review session

## Implementation Notes

The clean implementation path is:

- extend `ReviewPageState` with `rotationQuarterTurns`
- add rotate/delete/reorder actions to `ReviewViewModel`
- update `ReviewScreen` to expose active-page actions and reorderable strip behavior
- extend processing input to include rotation
- update save orchestration and tests to preserve final review order and page-local rotation

This slice finishes the core pre-save review editor without expanding into post-save document editing.

## Success Criteria

This slice is complete when all of the following are true:

- the active review page can be rotated left and right
- the active review page can be deleted unless it is the only remaining page
- the review strip supports drag-and-drop reordering
- page-local crop, mode, processed output, and rotation move with the page during reorder
- save preserves the exact final review order
- deleted pages do not appear in the saved document
- rotation is reflected in the processed output used for save
