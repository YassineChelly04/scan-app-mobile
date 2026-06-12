# Scanni Architecture

Scanni is a single-module Compose app with strict internal layering. The rule
that organizes everything: **logic that can be pure, is pure** — the geometry,
text and layout math live in `core/` with zero Android imports and full unit
coverage, while Android-coupled code (camera, OpenCV, Room, WorkManager) stays
behind small interfaces.

```
┌─────────────────────────── ui/ ───────────────────────────┐
│  Compose screens + ViewModels (StateFlow in, events out)  │
└──────────────┬────────────────────────────┬───────────────┘
               │ domain/usecase             │ observes
┌──────────────▼──────────────┐  ┌──────────▼──────────────┐
│ domain/  models, contracts, │  │ data/  Room + FTS4,     │
│ ScanSession, use cases      │  │ DataStore, PageFileStore│
└──────┬───────────┬──────────┘  └─────────────────────────┘
       │           │
┌──────▼─────┐ ┌───▼──────────────┐   ┌────────────────────┐
│ vision/    │ │ ocr/  ML Kit +   │   │ export/  PdfBox    │
│ OpenCV     │ │ Tesseract + Work │   │ searchable PDFs    │
└────────────┘ └──────────────────┘   └────────────────────┘
        all pure math delegated to core/ (unit-tested)
```

## The scan pipeline

1. **Live detection** — `CameraFrameAnalyzer` wraps each CameraX analysis frame's
   Y-plane directly into an OpenCV `Mat` (no color conversion), rotates it to
   display orientation and hands it to `OpenCvDocumentDetector`:
   downscale to 480px → Gaussian blur → two binarization strategies (Canny +
   dilate for strong outlines; adaptive threshold for low-contrast paper) →
   contours → `approxPolyDP` over an epsilon ladder → convex 4-gons scored by
   `area × squareness`. Result: a normalized `Quad`.
2. **Stabilization** — `QuadStabilizer` (pure) EMA-smooths the quad, tolerates
   short dropouts, measures steadiness, and reports
   `Searching / Tracking(progress) / Locked`. The scanner ViewModel fires
   auto-capture on `Locked` (respecting a 2s cooldown), drawing the progress as
   the ring around the shutter.
3. **Capture** — `ImageCapture` writes a JPEG into the session cache; detection
   re-runs on the still (sharper than the preview) for the final suggested crop.
   Pages accumulate in `ScanSession` (in-memory, shared between scanner and
   review).
4. **Review** — previews render via `OpenCvPageEnhancer`: perspective warp
   (`getPerspectiveTransform` sized by opposite-edge maxima) → filter → user
   rotation. Renders are cached on disk keyed by `(quad, rotation, filter)`.
   The crop editor manipulates the quad with `CropGeometry` (pure): corner and
   edge drags, bounds clamping, convexity + **signed-area orientation** guards
   so a crop can never invert and mirror the warp.
5. **Save** — `SaveScanUseCase` renders finals (2600px), moves originals out of
   the cache, writes thumbnails, inserts Room rows, and enqueues OCR.

## Filters (OpenCvPageEnhancer)

The signature "Magic" look flattens illumination instead of just boosting
contrast: each RGB channel is divided by a blurred morphological-close estimate
of its own background (computed at ¼ scale for speed), pushing paper to white
while preserving ink color; then a gentle S-curve and unsharp mask. Whiteboard
adds HSV saturation recovery; B&W uses size-adaptive Gaussian thresholding.
If the OpenCV native library ever fails to load, the enhancer degrades to
ColorMatrix-based filters and full-frame (no warp) — the app never breaks.

## OCR

`OcrWorker` (WorkManager, unique per document, APPEND_OR_REPLACE) runs
`RunOcrUseCase`: pick engine by settings — **ML Kit Text Recognition v2**
(Latin) or **Tesseract 5** via Tesseract4Android (Arabic; `ara.traineddata`
ships in assets and is copied to app storage on first use). Both produce
`OcrResult(text, words[])` where each word carries a normalized bounding box.
Text is stored on the page row *and* mirrored into an FTS4 table
(`page_fts`, doc/page ids `notIndexed`) for instant full-text search with
`snippet()` highlights. WorkManager is manually initialized so the worker gets
its dependencies from `AppGraph` (no reflection construction).

## Searchable PDFs

`SearchablePdfWriter` (PdfBox-Android) embeds each processed JPEG without
recompression (`JPEGFactory`), sizes pages full-bleed to the image aspect within
A4 bounds, then draws every OCR word with `RenderingMode.NEITHER` (invisible)
at its box — font size from box height, horizontal text-matrix scale matching
box width (`core/export/PdfLayout`, unit-tested). Words Helvetica can't encode
fall back to an embedded Noto Sans Arabic subset, so Arabic PDFs are searchable
too.

## Data

- **Room**: `folders ← documents ← pages` with cascading deletes + `page_fts`
  (FTS4). Page rows store the quad (compact string encoding), rotation, filter,
  OCR status/text/words-JSON and a `revision` counter for cache busting.
- **Files**: `PageFileStore` owns `files/documents/<doc>/<page>_{original,
  processed,thumb}.jpg`, the scan-session cache, render-preview cache, and
  24h-pruned export cache. Database rows and files are deleted together.
- **Settings**: DataStore preferences (theme, dynamic color, auto-capture,
  OCR script).

## Why no DI framework / multi-module split

`AppGraph` is ~80 lines of lazy singletons; ViewModels get constructor-injected
via a tiny `graphViewModel` helper. At this size, Hilt would add build
complexity and annotation processing for zero practical gain — the graph is
readable in one screen and trivially fake-able in tests. Same logic for staying
single-module: package boundaries are enforced by review, and the pure `core/`
layer is the part that benefits from isolation, which it gets by having no
Android imports (verified by compiling it standalone in CI-less environments).

## Testing

`core/` + `domain/ScanSession` are tested on the JVM with no Android
dependencies (54 tests): quad ordering/convexity/orientation, stabilizer
state machine, overlay coordinate mapping, crop drag rules, FTS query
sanitizing, PDF layout math, default naming, list reordering, session ops.
The orientation guard in `CropGeometry` exists because a test caught an edge
dragged through its opposite edge re-forming as a mirrored quad.

## UI/UX skills

Front-end work follows five repo skills in `.claude/skills/`:
`scanni-design-language` (M3 tokens, spacing, type), `scanni-motion`
(animation catalog + haptics map), `scanni-stateful-screens` (empty/loading/
progress/error/success for every surface), `scanni-touch-ergonomics`
(48dp targets, reach zones, RTL, insets), `scanni-camera-ux` (overlay
legibility, guidance copy, auto-capture etiquette).
