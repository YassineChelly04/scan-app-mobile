package com.scanni.app.domain.model

import com.scanni.app.core.geometry.Quad

/** Post-processing applied to a captured page after perspective correction. */
enum class ScanFilter {
    ORIGINAL,
    AUTO,
    GRAYSCALE,
    BLACK_WHITE,
    WHITEBOARD,
    PHOTO,
}

/** Capture modes, Lens-style. Each tunes detection and the default filter. */
enum class ScanMode(
    val defaultFilter: ScanFilter,
    val detectionEnabled: Boolean,
    /** Smallest detected area (fraction of the frame) accepted in this mode. */
    val minAreaFraction: Float,
) {
    DOCUMENT(ScanFilter.AUTO, detectionEnabled = true, minAreaFraction = 0.12f),
    WHITEBOARD(ScanFilter.WHITEBOARD, detectionEnabled = true, minAreaFraction = 0.18f),
    BUSINESS_CARD(ScanFilter.AUTO, detectionEnabled = true, minAreaFraction = 0.04f),
    PHOTO(ScanFilter.PHOTO, detectionEnabled = false, minAreaFraction = 0.12f),
}

enum class OcrStatus { PENDING, RUNNING, DONE, FAILED }

enum class OcrScript { LATIN, ARABIC }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val autoCapture: Boolean = true,
    val ocrScript: OcrScript = OcrScript.LATIN,
)

/** A page captured in the current scan session, before it is saved as a document. */
data class CapturedPage(
    val id: String,
    val originalPath: String,
    val widthPx: Int,
    val heightPx: Int,
    /** Quad found by the detector at capture time, if any. */
    val detectedQuad: Quad?,
    /** Crop actually applied; user edits move this one. */
    val quad: Quad,
    val rotationDeg: Int,
    val filter: ScanFilter,
)

data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val documentCount: Int,
)

data class Document(
    val id: String,
    val title: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val thumbPath: String?,
)

data class Page(
    val id: String,
    val documentId: String,
    val position: Int,
    val originalPath: String,
    val processedPath: String,
    val thumbPath: String,
    val widthPx: Int,
    val heightPx: Int,
    val quad: Quad?,
    val rotationDeg: Int,
    val filter: ScanFilter,
    val ocrStatus: OcrStatus,
    val ocrText: String?,
    val ocrWordsJson: String?,
)

data class SearchHit(
    val document: Document,
    /** Matching OCR fragment with <b> markers, when the hit came from page text. */
    val snippet: String?,
)

/** Everything the repository needs to persist one page of a new document. */
data class PageDraft(
    val id: String,
    val position: Int,
    val originalPath: String,
    val processedPath: String,
    val thumbPath: String,
    val widthPx: Int,
    val heightPx: Int,
    val quad: Quad?,
    val rotationDeg: Int,
    val filter: ScanFilter,
)
