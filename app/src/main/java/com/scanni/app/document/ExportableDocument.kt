package com.scanni.app.document

data class ExportableDocument(
    val id: Long,
    val title: String,
    val pageCount: Int,
    val ocrStatus: String,
    val pageImageUris: List<String>
)
