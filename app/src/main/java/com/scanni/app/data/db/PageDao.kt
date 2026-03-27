package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PageDao {
    @Insert
    suspend fun insertAll(pages: List<PageEntity>)

    @Query(
        """
        SELECT * FROM pages
        WHERE documentId = :documentId
        ORDER BY pageNumber ASC
        """
    )
    suspend fun getPagesForDocument(documentId: Long): List<PageEntity>
}
