package com.scanni.app.domain.ocr

/** Schedules background text recognition for a saved document. */
interface OcrScheduler {
    fun scheduleDocument(documentId: String)
}
