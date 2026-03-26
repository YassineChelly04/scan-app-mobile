package com.scanni.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scanni.app.data.db.AppDatabase
import com.scanni.app.data.db.DocumentEntity
import com.scanni.app.data.db.FolderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertDocument_persistsFolderAndSearchableTitle() = runTest {
        val folderId = db.folderDao().insert(FolderEntity(name = "Semester 2"))
        db.documentDao().insert(
            DocumentEntity(
                title = "Linear Algebra Notes",
                folderId = folderId,
                pageCount = 3,
                ocrStatus = "pending"
            )
        )

        val documents = db.documentDao().observeLibrary("").first()
        assertEquals("Linear Algebra Notes", documents.single().title)
    }
}
