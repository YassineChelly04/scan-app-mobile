package com.scanni.app.data.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface FolderDao {
    @Insert
    suspend fun insert(folder: FolderEntity): Long
}
