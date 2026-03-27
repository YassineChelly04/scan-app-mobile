package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Query(
        """
        SELECT DISTINCT documents.* FROM documents
        LEFT JOIN page_text ON page_text.documentId = documents.id
        WHERE :query = ''
            OR documents.title LIKE '%' || :query || '%'
            OR page_text.text LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
}
