# Functional PDF Export Design

Date: 2026-03-27
Status: Approved in conversation, written for review

## Summary

This document defines the design change needed to make PDF export truly functional in the current Android scanner app instead of merely implemented in isolation.

The required behavior is:

- a user can open a saved document from the library
- the app can load that document's saved pages
- export uses the processed scan output, not the raw capture
- tapping share generates a real PDF from those processed page files
- the generated PDF is shared through Android's normal file-sharing flow

This expands beyond the original Task 7 boundary because the original plan created export primitives but did not require wiring them through the live application flow. The design here keeps the expansion narrow and focused on real export behavior.

## Product Requirement

The export path must behave like a real document scanner workflow:

- the image is first cropped
- a document filter is applied
- the exported PDF uses that processed result

The system must not silently export raw uncropped captures when processed output is missing. If processed output is unavailable, export should fail clearly instead of producing the wrong result.

## Recommended Approach

Use the smallest real end-to-end path on top of the current architecture:

- keep `LibraryScreen` as the entry to saved documents
- wire library item selection into a real document-detail route
- load document metadata and ordered page records from Room
- collect processed page file paths from saved page records
- build the PDF from those processed image files
- share the generated file with `FileProvider`

This is preferred over a broader architecture refactor because it delivers working export without redesigning the full scanner, review, and library stack.

## User Flow

### Library to document detail

- The user opens the library.
- The user taps a saved document.
- The app navigates to `DocumentDetail`.

### Document detail to export

- The document detail screen displays:
  - title
  - page count
  - OCR status
  - share action
- When the user taps `Share PDF`, the app:
  - loads the processed image path for every page in the document
  - validates that every required file exists
  - generates a PDF in app-local storage
  - creates a share intent through `FileProvider`
  - launches Android share

## Data Requirements

### Document lookup

The app needs a direct document lookup by ID so the detail screen can load one saved document.

Required database behavior:

- load document metadata by `documentId`
- load pages for that document in stable page order

### Page records

Saved pages must expose the processed output path used for export.

The export path should use processed page output only. The processed page file is the source of truth for PDF generation because it reflects the cropped and filtered result the user expects.

If the current page model does not store the processed path yet, the schema must be extended and the Room layer updated accordingly.

## Components

### Navigation

`ScanniApp` should add a real `DocumentDetail` destination using the existing `AppRoute.DocumentDetail` route shape.

`LibraryScreen` document taps should navigate to that destination instead of using a no-op callback.

### Document detail state

Add a focused document-detail state loader responsible for:

- loading document metadata
- loading ordered page records
- exposing export state:
  - idle
  - exporting
  - error

This logic should stay isolated to the document-detail area instead of leaking into library or export utilities.

### PDF generation

`PdfExporter` should generate the PDF from actual processed image files.

Expected behavior:

- one PDF page per processed image path
- stable page order matching stored page order
- readable white background output appropriate for document export

If a page image cannot be loaded, export should fail and report an error.

### Sharing

`ShareDocumentUseCase` remains the Android boundary for sharing.

The app should use:

- `FileProvider`
- `${context.packageName}.fileprovider`
- app-local output storage for the generated PDF

The manifest and provider path configuration should be the minimum required for that flow.

## Error Handling

### Invalid document

If the document ID is invalid or missing, the detail screen should show a simple "document not found" state.

### Missing processed pages

If any page lacks a processed image path, export must not fall back to the raw image. The UI should present export as unavailable or fail with a clear error message.

### Missing files on disk

If a processed image path exists in metadata but the file is missing on disk, export must fail clearly and not launch share.

### Export failure

If PDF generation throws or file writing fails:

- keep the user on the detail screen
- surface an export error state
- do not attempt to launch Android share

## Scope

### In scope

- library-to-detail navigation
- detail screen wiring for one saved document
- document/page lookup needed for export
- export from processed saved page files
- Android share of the generated PDF
- tests for the new functional path

### Out of scope

- redesigning the scanner capture pipeline
- revising the review pipeline beyond storing the processed file path needed by export
- adding cloud export or sync
- adding advanced PDF layout controls
- adding batch export for multiple documents

## Testing Strategy

### Unit tests

Add or update tests for:

- PDF generation from page image paths
- document detail export state behavior
- missing processed page prevents export
- missing document shows a non-shareable state

### Android tests

Add coverage for:

- tapping a library document opens the detail route
- a detail screen can surface the share action for a valid saved document

### Verification

At minimum, verification for this slice should include:

- targeted unit tests for export and document detail
- full unit test suite
- Android instrumentation suite if route wiring changes

## Acceptance Criteria

This design is complete when all of the following are true:

- a user can reach a saved document from the library
- the detail screen loads that document's saved pages
- export uses the processed page output
- the app generates a PDF from those processed files
- Android share launches only after successful PDF creation
- export fails clearly when processed output is missing
