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
        SELECT * FROM documents
        WHERE :query = '' OR title LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
}
