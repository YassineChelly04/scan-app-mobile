package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentDao
import com.scanni.app.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

class LocalDocumentRepository(
    private val documentDao: DocumentDao
) : DocumentRepository {
    override fun observeLibrary(query: String): Flow<List<DocumentEntity>> =
        documentDao.observeLibrary(query)

    override suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long =
        documentDao.insert(
            DocumentEntity(
                title = title,
                folderId = folderId,
                pageCount = pageCount,
                ocrStatus = DEFAULT_OCR_STATUS
            )
        )

    private companion object {
        const val DEFAULT_OCR_STATUS = "pending"
    }
}
