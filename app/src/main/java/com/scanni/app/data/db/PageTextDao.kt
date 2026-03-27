package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PageTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pageText: PageTextEntity)

    @Query(
        """
        SELECT * FROM page_text
        WHERE documentId = :documentId AND pageIndex = :pageIndex
        LIMIT 1
        """
    )
    suspend fun getByDocumentAndPage(documentId: Long, pageIndex: Int): PageTextEntity?
}
