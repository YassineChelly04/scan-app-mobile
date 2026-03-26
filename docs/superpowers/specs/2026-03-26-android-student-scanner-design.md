# Android Student Scanner Design

Date: 2026-03-26
Status: Approved in conversation, written for review

## Summary

This document defines the v1 design for an Android-first document scanning application intended to do the same core job as CamScanner, but better for a focused audience. The product target is students first, with scan quality as the primary differentiator. The app is offline-first, processes documents on-device, and emphasizes faster capture plus materially better results for handouts, notes, book pages, and whiteboards.

The v1 product goal is not to match every CamScanner feature. The goal is to outperform general-purpose scanner apps on academic scanning quality and capture flow while keeping the library, search, and export experience practical enough for daily use.

## Product Positioning

### Primary audience

Students are the first launch audience. The product is optimized for:

- Lecture handouts
- Printed notes
- Notebook pages
- Book pages
- Whiteboards

Professionals remain a future expansion path, but the initial product decisions should prioritize the scanning conditions and workflows students face most often.

### Core differentiator

The main reason this product should exist is better scan quality in real use. In v1, "better than CamScanner" means:

- More reliable edge detection
- Better perspective correction
- Better cleanup of shadows and uneven lighting
- Better results on curved book pages
- Better whiteboard enhancement
- OCR that is good enough for practical local search

### Scope level

The chosen scope is a prosumer v1, not a bare scanner and not a full platform. It includes:

- Fast scanning
- Multi-page capture
- Review and enhancement
- Offline OCR
- Local folders
- Full-text search
- Editing and reordering pages
- Local PDF export and sharing

It explicitly excludes:

- Accounts
- Cloud sync
- Collaboration
- Payments
- AI summarization
- Team workflows

## Platform And Stack

### Platform choice

The app is Android-first for v1. This choice reduces complexity in the most critical area, which is the capture and processing pipeline.

### Implementation approach

The recommended implementation is a native Android application in Kotlin.

This is preferred over Flutter or React Native because the success of the product depends on low-latency camera control, robust on-device image processing, and predictable performance across a range of Android hardware. Cross-platform UI convenience is less important than scan quality and responsiveness in the first release.

## High-Level Architecture

The application should be organized around four main layers.

### 1. Capture layer

This layer owns the camera experience and nothing else. It should:

- Open quickly into a live camera preview
- Use CameraX
- Support multi-page capture
- Show page guides and capture assistance
- Handle focus and exposure behavior suitable for documents

The capture layer should be optimized for poor classroom lighting and fast repeated scans.

### 2. Document processing layer

This layer is the product core. It transforms captured images into readable scanned pages through:

- Edge detection
- Quadrilateral detection
- Perspective correction
- Contrast normalization
- Thresholding
- Denoising
- Shadow and glare mitigation
- Page cleanup

The user-facing enhancement modes should be implemented as parameter presets on top of one shared pipeline:

- Document
- Book
- Whiteboard

### 3. OCR and indexing layer

This layer runs OCR locally on processed pages and stores extracted text so documents become searchable. OCR should be asynchronous and must not block the user from saving scans.

### 4. Library and export layer

This layer handles long-term document use:

- Folder organization
- Rename and move
- Page reordering
- Page deletion
- Search
- PDF export
- Image sharing

## Module Boundaries

Even if the codebase starts in one Android app module, the internal boundaries should be clear from the beginning. The recommended structure is:

- `camera`
- `processing`
- `ocr`
- `documents`
- `ui`

At minimum, these should be separated into focused packages with clear interfaces. The document processing pipeline must stay isolated from UI code so it remains testable and replaceable.

## User Flow

### Default entry

The app should open directly into the scanner camera, not a dashboard. This supports the target audience, who often need to scan immediately during class or while studying.

### Capture flow

The user can capture one page or multiple pages in sequence without being forced back to the library after each shot. Multi-page scanning is a first-class flow.

### Review flow

After capture, the user enters a lightweight review screen where they can:

- Fix crop
- Rotate pages
- Delete poor captures
- Reorder pages
- Switch enhancement modes

The default enhancement should be strong enough that most pages need no manual adjustment.

### Save flow

When the user saves a document:

- The document should appear in the local library immediately
- OCR should continue in the background
- Search should become available when OCR completes

Saving must not wait on OCR unless the user explicitly chooses a workflow that requires text extraction first.

### Library flow

The user can:

- Browse local folders
- Rename documents
- Move documents between folders
- Search by OCR text
- Open and re-edit saved documents

### Export flow

The primary export target is PDF. Exported documents should emphasize readability and consistent output over aggressive compression.

## Technical Design

### Camera

Use CameraX with a custom scanner screen rather than a generic wrapper. This is necessary for:

- Fast startup
- Live overlays
- Document-focused focus and exposure handling
- Controlled capture flow
- Future capture heuristics

### Image processing

Use OpenCV on Android as the core image processing foundation for v1. It offers the primitives needed to implement the quality-focused pipeline locally and efficiently.

Enhancement modes should map to preset tuning rather than separate systems:

- `Document`: aggressive cleanup for flat printed pages
- `Book`: better tolerance for curvature and uneven page lighting
- `Whiteboard`: stronger contrast cleanup and background suppression

### OCR

Use an offline Android-compatible OCR engine with an abstraction layer around it. The first candidate is on-device ML Kit text recognition if the quality is sufficient for student documents. The OCR component should be replaceable if later benchmarking shows it is not good enough for books or classroom material.

OCR should run on processed page images, not raw captures, because the processing layer is expected to improve text readability and search quality.

### Data storage

Use:

- Room for metadata, folders, page ordering, and searchable text references
- Local file storage for original captures, processed images, and generated PDFs

This avoids storing large binary content in the database while keeping library queries fast.

### Non-destructive editing

The system must preserve original captured images alongside processed versions. Any saved page should be reprocessable from the original source without forcing the user to rescan.

## Failure Handling

The application must degrade gracefully.

### Capture and crop failures

If automatic edge detection is uncertain, the user should be moved to manual crop adjustment instead of silently accepting a poor crop.

### OCR failures

If OCR fails or produces weak results, the document should still save normally. The user should retain access to the scan even if search support is incomplete.

### Performance limits

On slower devices, heavier processing and OCR tasks should happen off the main thread. Capture responsiveness is more important than finishing all background work immediately.

### Reliability rule

The core rule is:

- Never lose a scan
- Never block the user unnecessarily
- Never pretend quality is better than it is

## Testing Strategy

Testing should emphasize the quality pipeline and end-to-end flow rather than only UI interaction.

### Unit tests

Add unit tests for:

- Processing mode selection
- Metadata and document storage rules
- OCR job orchestration
- Export assembly logic

### Instrumented Android tests

Add instrumented tests for:

- Capture to review flow
- Review to save flow
- Save to searchable library flow

### Golden image verification

Build a representative image dataset and verify processing behavior against it. The dataset should include:

- Flat pages
- Skewed pages
- Shadow-heavy scans
- Book pages with curvature
- Whiteboards
- Low-light classroom captures

### OCR benchmarking

Evaluate OCR quality on real student-oriented sample material and use measured results to decide whether the default OCR engine is sufficient.

### Export verification

Verify that generated PDFs preserve:

- Page order
- Readability
- Expected page dimensions

## V1 Deliverables

The first release should include:

- Android app in Kotlin
- Fast scanner camera screen
- Multi-page capture
- Manual crop correction
- Document, Book, and Whiteboard enhancement modes
- Offline OCR
- Local folders
- Full-text search
- Page reordering
- Rename and move document actions
- Local PDF export and sharing

## Success Criteria

The product should be considered successful for v1 if:

- Students can scan multi-page academic material quickly
- Processed pages are clearly more readable than raw camera captures
- Search works well enough to retrieve documents through OCR text
- The app remains fully useful without login or network access
- The capture and save flow feels faster and cleaner than typical generic scanner apps

## Follow-On Work After V1

If v1 succeeds, the most natural next expansions are:

- Professional document tuning for invoices and forms
- Improved OCR backends if benchmarking shows clear limits
- Optional sync or backup
- Cross-platform expansion after the Android quality baseline is proven
