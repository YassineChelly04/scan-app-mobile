package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.document.ExportableDocument
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
    suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long
    suspend fun saveProcessedDocument(title: String, folderId: Long?, pageImageUris: List<String>): Long
    suspend fun savePageText(documentId: Long, pageIndex: Int, text: String)
    suspend fun getExportableDocument(documentId: Long): ExportableDocument?
}
