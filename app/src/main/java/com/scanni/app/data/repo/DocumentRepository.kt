package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
    suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long
    suspend fun savePageText(documentId: Long, pageIndex: Int, text: String)
}
