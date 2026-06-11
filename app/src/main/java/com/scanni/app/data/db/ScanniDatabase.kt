package com.scanni.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FolderEntity::class,
        DocumentEntity::class,
        PageEntity::class,
        PageFtsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ScanniDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun folderDao(): FolderDao
    abstract fun pageFtsDao(): PageFtsDao

    companion object {
        fun create(context: Context): ScanniDatabase =
            Room.databaseBuilder(context, ScanniDatabase::class.java, "scanni.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
