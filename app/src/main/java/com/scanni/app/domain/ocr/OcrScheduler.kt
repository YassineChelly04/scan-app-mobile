package com.scanni.app.domain.ocr

/** Schedules background text recognition for a saved document. */
interface OcrScheduler {
    fun scheduleDocument(documentId: String)

    /** Cancels any pending or running recognition for [documentId] (e.g. on delete). */
    fun cancelDocument(documentId: String)
}
