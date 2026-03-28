Date: 2026-03-28
Status: Approved in conversation, written for review

## Summary

This document defines the next production-blocking review slice for the Android scanner app: real manual crop editing during review, backed by actual perspective correction in processing. The scope includes page-by-page editable review state for every captured page, a simple page switcher inside review, and save behavior that uses the edited per-page review data rather than the original detected crop data from capture.

This slice intentionally does not include page delete, page reorder, or page rotate. Those remain follow-on work once the review model is upgraded to support true per-page editing.

## Problem

The current review flow is not yet a real review editor:

- review only loads the last captured page
- crop corners are stored but not editable
- the processor ignores corners entirely
- save reprocesses from the original detected corners, so any future manual crop UI would be fake

This is a release blocker because scan quality is the product's primary claim, and users must be able to correct bad edge detection before saving.

## Goals

This slice must deliver:

- real manual crop editing for the active review page
- real perspective correction from the chosen crop corners
- per-page review state across a captured multi-page session
- a simple page switcher so users can move between captured pages during review
- save behavior that processes each page from its edited review state

## Non-Goals

This slice does not include:

- page deletion
- page reorder
- page rotation
- saved-document page editing after the review session
- auto-save or persisted draft review sessions

## Recommended Architecture

Review should become a dedicated session model owned by `ReviewViewModel`, not an extension of `ScannerViewModel`.

`ScannerViewModel` remains responsible only for capture-session accumulation. When the user enters review, the captured drafts are transformed into review-page items that are owned and edited by `ReviewViewModel`.

This keeps the boundary clean:

- capture owns camera and accumulated raw drafts
- review owns editable crop and enhancement state
- processing owns perspective correction and enhancement
- save consumes reviewed page state

This design is preferred over mutating scanner state in place because the review feature set will continue to grow, and future actions like delete, reorder, and rotate should live in one focused review boundary.

## Review Session State

The review route should load the full captured session instead of only the last page.

`ReviewViewModel` should expose one session state with:

- `pages: List<ReviewPageState>`
- `activePageIndex`
- session-level save/loading state

Each `ReviewPageState` should contain:

- immutable source input:
  - original image path
- editable review data:
  - crop corners
  - enhancement mode
- derived output:
  - processed preview path
- page-local status:
  - `isProcessing`
  - `errorMessage`

Per-page state must be preserved when the user switches between pages. Editing one page must not overwrite another page's crop or mode.

## User Flow

The review flow should work like this:

1. The scanner captures one or more pages as it does now.
2. Entering review converts all captured drafts into `ReviewPageState` items.
3. The first page becomes active and is processed immediately with its initial detected corners.
4. The user switches between pages with a simple page switcher.
5. The user edits the active page's crop corners and mode.
6. Leaving a page and returning to it restores that page's latest review state.
7. Saving the session processes each page from its current edited corners and mode.

The "Add Another Page" action remains available and returns the user to scanner capture without discarding the accumulated session.

## Review UI

The review screen should move from text-only status output to a functional review layout with:

- a large page preview for the active page
- a visible crop quadrilateral overlay
- four draggable crop corner handles
- enhancement mode controls for the active page
- a simple page switcher or thumbnail strip for page-to-page navigation
- session-level save action
- existing "Add Another Page" action

The page switcher can be simple in this slice. It does not need drag-to-reorder behavior yet. The important requirement is that users can clearly move among pages and understand which page is active.

## Crop Interaction Rules

Crop editing should follow these rules:

- dragging a corner updates only the active page's in-memory crop state
- reprocessing should occur after drag completion, not on every pointer movement
- each page keeps its own corners and enhancement mode
- the crop overlay must always reflect the current active page state

Corners should continue to be represented as normalized quadrilateral points so the review UI and processor can share one coordinate contract regardless of image size.

## Processing Design

`OpenCvPageProcessor` must start using the passed `corners` for real perspective correction.

The processor should:

1. decode the original image
2. validate and normalize the crop corners
3. map normalized corners into source-image coordinates
4. build a quadrilateral-to-rectangle transform
5. warp the selected region into a rectangular bitmap
6. apply the chosen enhancement mode to the warped bitmap
7. write the processed output to a separate file

Original capture files must remain unchanged. This preserves non-destructive editing and allows future reprocessing from source images.

The processor contract should remain page-local. It should not need to understand multi-page review sessions.

## Save Behavior

The current save path processes pages using the original `CapturedPageDraft.detectedCorners`. That is no longer sufficient once review becomes editable.

Save must instead use the current `ReviewPageState` values for each page:

- current original path
- current crop corners
- current enhancement mode

This means the review session model becomes the source of truth for save. Capture data is only the starting draft.

## Error Handling

Error handling should be page-scoped wherever possible:

- if a page fails to process, that page keeps its source image and editable state
- one page's processing failure must not destroy the whole review session
- switching to another page must still work normally
- the failing page should display a clear page-local error state

Save should be blocked if any page lacks a valid processed result for its current edits. The user should be able to correct the issue without losing the rest of the session.

Crop inputs should be guarded against invalid shapes, including:

- missing points
- duplicate points
- nearly zero-area selections
- obviously out-of-bounds normalized coordinates

Invalid crop input should fail clearly rather than silently generating a misleading output.

## Testing Strategy

Testing should focus on the upgraded review boundary and the real crop-processing contract.

Add unit tests for:

- review session loading from multiple captured pages
- active-page switching
- per-page mode persistence
- per-page crop persistence
- reprocessing only the active page after a crop or mode change
- save orchestration using edited review state instead of original detected corners

Add UI or instrumented tests for:

- page switcher behavior
- active-page preview updates
- crop-handle interaction state changes

Add processor tests for:

- normalized corner mapping into image coordinates
- perspective correction producing a different output when corners change
- invalid corner input failing clearly

## Implementation Notes

The smallest clean implementation path is:

- introduce `ReviewPageState` and a session-level `ReviewUiState`
- change `ReviewViewModel` to load a full list of captured pages
- add active-page selection and crop-update actions
- update `ReviewScreen` to render the active page and switcher
- teach `OpenCvPageProcessor` to warp from quadrilateral corners before enhancement
- update save orchestration to use reviewed page state

This prepares the codebase for the next review-editor slice without requiring a second boundary change later.

## Success Criteria

This slice is complete when all of the following are true:

- review opens with all captured pages, not only the last page
- users can switch between pages during review
- users can manually drag crop corners on the active page
- crop edits persist per page while navigating within the review session
- the processor uses the chosen corners for real perspective correction
- save uses the reviewed per-page crop and mode values
- failures remain page-scoped and do not discard the session
