package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query(
        """
        SELECT * FROM folders
        ORDER BY createdAt DESC
        """
    )
    fun observeAll(): Flow<List<FolderEntity>>
}
