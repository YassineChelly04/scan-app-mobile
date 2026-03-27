# Next Step To Reach Production

Date: 2026-03-27
Branch target: `feat/task-1-bootstrap-shell`

## What Is Still Missing Before This App Can Be Considered Production-Ready

This is ordered by actual release blockers, not by convenience.

## P0 — Core Product Blockers

### 1. Real Manual Review Control

This is the biggest remaining product gap.

Still missing:

- draggable crop corner handles in review
- visible page preview/crop overlay
- per-page manual crop confirmation
- review-side rotate action
- review-side delete page action

Files that will need work:

- `app/src/main/java/com/scanni/app/review/ReviewScreen.kt`
- `app/src/main/java/com/scanni/app/review/ReviewViewModel.kt`
- `app/src/main/java/com/scanni/app/review/PageReviewState.kt`
- `app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt`

Reason this is a production blocker:

- users still cannot fix a bad detected crop manually
- current review does not meet the approved v1 scanner spec

### 2. Processor Must Actually Use Crop Corners

Right now the processing pipeline writes output files, but it still does not use the passed `corners` for real perspective correction.

Still missing:

- map four detected/manual corners into a perspective transform
- crop/warp the page before enhancement
- keep non-destructive source image behavior

Files that will need work:

- `app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt`
- tests around golden image output and crop behavior

Reason this is a production blocker:

- manual crop UI would be fake until the processor respects corners
- scan quality is the core product claim

### 3. Full Multi-Page Review Editing

Multi-page save exists now, but review is still not a full multi-page editor.

Still missing:

- page thumbnail strip or page switcher
- per-page mode/crop state
- reorder pages before save
- delete single bad pages before save

Files that will need work:

- `app/src/main/java/com/scanni/app/camera/ScannerViewModel.kt`
- `app/src/main/java/com/scanni/app/review/*`
- `app/src/main/java/com/scanni/app/ScanniApp.kt`

Reason this is a production blocker:

- multi-page capture without real page management is incomplete for real users

## P1 — Functional Completeness Gaps

### 4. OCR Status Lifecycle

OCR jobs are now enqueued after save, but the user-facing document status is still incomplete.

Still missing:

- mark OCR status as `processing`
- mark OCR status as `complete` or `failed`
- expose the status consistently in library/detail screens
- add tests for OCR completion state transitions

Likely files:

- `app/src/main/java/com/scanni/app/data/db/DocumentEntity.kt`
- `app/src/main/java/com/scanni/app/data/db/DocumentDao.kt`
- `app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt`
- `app/src/main/java/com/scanni/app/ocr/OcrWorker.kt`
- `app/src/main/java/com/scanni/app/library/LibraryViewModel.kt`
- `app/src/main/java/com/scanni/app/document/DocumentDetailViewModel.kt`

### 5. Saved Document Page Management

Document metadata edit exists, but page-level saved-document management is still missing.

Still missing:

- reorder saved pages
- delete saved pages
- re-export after page changes

Reason:

- this was part of the approved v1 scope

### 6. Camera Capture UX Hardening

The live preview and permission path now exist, but the scanner UX is still basic.

Still missing:

- better capture-state feedback
- error UI if capture fails
- document overlay/guides in preview
- tested behavior on a physical device, not only emulator

## P2 — Quality And Release Hardening

### 7. Device QA Matrix

Before production, run manual QA on:

- at least one low/mid Android device
- at least one newer Android device
- different lighting conditions
- single-page and multi-page scans
- long OCR/background processing runs

### 8. Storage And Cleanup Policy

Still missing:

- clear policy for temporary capture files
- cleanup of abandoned review sessions
- validation of export/cache growth over time

### 9. Release Build Readiness

Still missing:

- signed release build process
- release app icon and branding assets
- minify/proguard review
- Play Store metadata and screenshots
- privacy wording for camera/file access

## Recommended Implementation Order

1. manual crop UI and editable review state
2. real perspective correction from corners
3. multi-page review controls: page switch, delete, reorder
4. OCR status lifecycle and completion handling
5. saved-document page management
6. release/device QA and shipping hardening

## Definition Of “Prod 100%” For This Branch

This app is ready for production only when all of these are true:

- user can capture single-page and multi-page documents reliably
- user can manually fix crop on any page
- processor uses manual/detected corners for real perspective correction
- user can review, reorder, rotate, and delete pages before save
- saved documents support rename/move and page management
- OCR runs in background and status updates correctly
- library search works from OCR text on real saved scans
- PDF export works on real multi-page documents
- app passes unit, connected, and physical-device QA for the main flows
