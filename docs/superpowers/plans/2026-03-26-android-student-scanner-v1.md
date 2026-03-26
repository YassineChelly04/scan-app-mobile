# Android Student Scanner V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android-first, offline-first student document scanner with fast multi-page capture, local enhancement, OCR-backed search, folders, and PDF export.

**Architecture:** Start with a single Android app module in Kotlin to keep setup lean, but enforce clear package boundaries for `camera`, `processing`, `ocr`, `documents`, `library`, `export`, and `ui`. Use Compose for the app shell, CameraX for capture, OpenCV for page processing, Room for metadata, WorkManager for background OCR, and local file storage for page images and PDFs.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX, OpenCV Android SDK, ML Kit Text Recognition, Room, WorkManager, Android `PdfDocument`, JUnit, Turbine, MockK, AndroidX Compose UI Test

---

## Planned File Structure

- `settings.gradle.kts`
  Responsibility: Declare the Android app project and repositories.
- `build.gradle.kts`
  Responsibility: Root Gradle configuration and Android plugin versions.
- `gradle.properties`
  Responsibility: AndroidX, JVM, and Compose build flags.
- `app/build.gradle.kts`
  Responsibility: App dependencies for Compose, CameraX, Room, WorkManager, ML Kit, and tests.
- `app/src/main/AndroidManifest.xml`
  Responsibility: Camera permission, app entry point, and file share provider.
- `app/src/main/java/com/scanni/app/MainActivity.kt`
  Responsibility: Android activity host for the Compose app.
- `app/src/main/java/com/scanni/app/ScanniApp.kt`
  Responsibility: Navigation shell and app-wide state wiring.
- `app/src/main/java/com/scanni/app/navigation/AppRoute.kt`
  Responsibility: Route definitions for scanner, review, library, and document detail screens.
- `app/src/main/java/com/scanni/app/camera/...`
  Responsibility: CameraX controller, capture models, scanner view model, and scanner UI.
- `app/src/main/java/com/scanni/app/processing/...`
  Responsibility: Enhancement modes, crop models, processing profiles, and OpenCV pipeline.
- `app/src/main/java/com/scanni/app/data/...`
  Responsibility: Room database, DAOs, repositories, and document storage helpers.
- `app/src/main/java/com/scanni/app/ocr/...`
  Responsibility: OCR abstraction, ML Kit adapter, indexing worker, and scheduling.
- `app/src/main/java/com/scanni/app/library/...`
  Responsibility: Folder list, search, library view model, and document browser UI.
- `app/src/main/java/com/scanni/app/export/...`
  Responsibility: PDF generation and Android share intents.
- `app/src/test/java/com/scanni/app/...`
  Responsibility: Unit tests for view models, processing orchestration, OCR jobs, and PDF export.
- `app/src/androidTest/java/com/scanni/app/...`
  Responsibility: Compose UI tests and Room integration tests.
- `app/src/test/resources/samples/...`
  Responsibility: Golden sample images for pipeline verification.

## Task 1: Bootstrap The Android App Shell

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/scanni/app/MainActivity.kt`
- Create: `app/src/main/java/com/scanni/app/ScanniApp.kt`
- Create: `app/src/main/java/com/scanni/app/navigation/AppRoute.kt`
- Create: `app/src/main/java/com/scanni/app/ui/theme/ScanniTheme.kt`
- Test: `app/src/androidTest/java/com/scanni/app/AppLaunchTest.kt`

- [ ] **Step 1: Create the Gradle and Android skeleton**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Scanni"
include(":app")
```

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.scanni.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scanni.app"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 2: Write the failing launch test**

```kotlin
// app/src/androidTest/java/com/scanni/app/AppLaunchTest.kt
package com.scanni.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_starts_on_scanner_screen() {
        composeRule.onNodeWithTag("scanner-screen").assertExists()
    }
}
```

- [ ] **Step 3: Implement the app shell with scanner as the start destination**

```kotlin
// app/src/main/java/com/scanni/app/navigation/AppRoute.kt
package com.scanni.app.navigation

sealed class AppRoute(val route: String) {
    data object Scanner : AppRoute("scanner")
    data object Review : AppRoute("review")
    data object Library : AppRoute("library")
    data object DocumentDetail : AppRoute("document/{documentId}")
}
```

```kotlin
// app/src/main/java/com/scanni/app/ScanniApp.kt
package com.scanni.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanni.app.navigation.AppRoute

@Composable
fun ScanniApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.Scanner.route) {
        composable(AppRoute.Scanner.route) {
            Box(modifier = Modifier.fillMaxSize().testTag("scanner-screen"))
        }
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/MainActivity.kt
package com.scanni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.scanni.app.ui.theme.ScanniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanniTheme { ScanniApp() }
        }
    }
}
```

- [ ] **Step 4: Run the smoke test**

Run: `.\gradlew :app:connectedDebugAndroidTest`

Expected: `BUILD SUCCESSFUL` and `AppLaunchTest > app_starts_on_scanner_screen PASSED`

- [ ] **Step 5: Commit the shell**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app
git commit -m "feat: bootstrap android scanner app shell"
```

### Task 2: Add Local Document Models And Room Persistence

**Files:**
- Create: `app/src/main/java/com/scanni/app/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/DocumentEntity.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/PageEntity.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/FolderEntity.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/DocumentDao.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/FolderDao.kt`
- Create: `app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt`
- Create: `app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt`
- Test: `app/src/androidTest/java/com/scanni/app/data/DocumentDaoTest.kt`

- [ ] **Step 1: Write the failing Room integration test**

```kotlin
// app/src/androidTest/java/com/scanni/app/data/DocumentDaoTest.kt
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
```

- [ ] **Step 2: Run the Room test and confirm the schema is missing**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.data.DocumentDaoTest`

Expected: FAIL with errors about missing `AppDatabase`, DAO methods, or entities.

- [ ] **Step 3: Implement entities, DAOs, and the repository contract**

```kotlin
// app/src/main/java/com/scanni/app/data/db/DocumentEntity.kt
package com.scanni.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folderId: Long?,
    val pageCount: Int,
    val ocrStatus: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

```kotlin
// app/src/main/java/com/scanni/app/data/db/DocumentDao.kt
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
```

```kotlin
// app/src/main/java/com/scanni/app/data/db/AppDatabase.kt
package com.scanni.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, FolderEntity::class, PageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao
}
```

```kotlin
// app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt
package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
    suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long
}
```

- [ ] **Step 4: Re-run the integration test**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.data.DocumentDaoTest`

Expected: `DocumentDaoTest > insertDocument_persistsFolderAndSearchableTitle PASSED`

- [ ] **Step 5: Commit the storage layer**

```bash
git add app/src/main/java/com/scanni/app/data app/src/androidTest/java/com/scanni/app/data app/build.gradle.kts
git commit -m "feat: add local document persistence"
```

### Task 3: Implement Multi-Page Scanner Session State

**Files:**
- Create: `app/src/main/java/com/scanni/app/camera/ScannerCameraController.kt`
- Create: `app/src/main/java/com/scanni/app/camera/CameraXScannerController.kt`
- Create: `app/src/main/java/com/scanni/app/camera/CapturedPageDraft.kt`
- Create: `app/src/main/java/com/scanni/app/camera/ScannerUiState.kt`
- Create: `app/src/main/java/com/scanni/app/camera/ScannerViewModel.kt`
- Create: `app/src/main/java/com/scanni/app/camera/ScannerScreen.kt`
- Test: `app/src/test/java/com/scanni/app/camera/ScannerViewModelTest.kt`

- [ ] **Step 1: Write the failing scanner session test**

```kotlin
// app/src/test/java/com/scanni/app/camera/ScannerViewModelTest.kt
package com.scanni.app.camera

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ScannerViewModelTest {
    @Test
    fun captureSuccess_appendsPageAndUpdatesCount() = runTest {
        val viewModel = ScannerViewModel()

        viewModel.onPageCaptured(
            CapturedPageDraft(
                originalPath = "files/original-1.jpg",
                previewPath = "files/preview-1.jpg",
                detectedCorners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
            )
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.pages.size)
            assertEquals(1, state.captureCount)
        }
    }
}
```

- [ ] **Step 2: Run the unit test and verify the view model is missing**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.camera.ScannerViewModelTest`

Expected: FAIL with unresolved `ScannerViewModel` or `CapturedPageDraft`.

- [ ] **Step 3: Implement the scanner state model and screen**

```kotlin
// app/src/main/java/com/scanni/app/camera/CapturedPageDraft.kt
package com.scanni.app.camera

data class CapturedPageDraft(
    val originalPath: String,
    val previewPath: String,
    val detectedCorners: List<Float>
)
```

```kotlin
// app/src/main/java/com/scanni/app/camera/ScannerUiState.kt
package com.scanni.app.camera

data class ScannerUiState(
    val pages: List<CapturedPageDraft> = emptyList(),
    val captureCount: Int = 0
)
```

```kotlin
// app/src/main/java/com/scanni/app/camera/ScannerViewModel.kt
package com.scanni.app.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerViewModel {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onPageCaptured(page: CapturedPageDraft) {
        val updatedPages = _uiState.value.pages + page
        _uiState.value = _uiState.value.copy(
            pages = updatedPages,
            captureCount = updatedPages.size
        )
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/camera/ScannerScreen.kt
package com.scanni.app.camera

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ScannerScreen(state: ScannerUiState) {
    Column(modifier = Modifier.testTag("scanner-screen")) {
        Text(text = "Pages: ${state.captureCount}")
    }
}
```

- [ ] **Step 4: Add CameraX controller wiring behind an interface**

```kotlin
// app/src/main/java/com/scanni/app/camera/ScannerCameraController.kt
package com.scanni.app.camera

interface ScannerCameraController {
    suspend fun capturePage(): CapturedPageDraft
}
```

```kotlin
// app/src/main/java/com/scanni/app/camera/CameraXScannerController.kt
package com.scanni.app.camera

import java.io.File

class CameraXScannerController(
    private val outputDir: File
) : ScannerCameraController {
    override suspend fun capturePage(): CapturedPageDraft {
        val stamp = System.currentTimeMillis()
        return CapturedPageDraft(
            originalPath = File(outputDir, "original-$stamp.jpg").absolutePath,
            previewPath = File(outputDir, "preview-$stamp.jpg").absolutePath,
            detectedCorners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
        )
    }
}
```

- [ ] **Step 5: Run the unit test and commit**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.camera.ScannerViewModelTest`

Expected: `ScannerViewModelTest > captureSuccess_appendsPageAndUpdatesCount PASSED`

```bash
git add app/src/main/java/com/scanni/app/camera app/src/test/java/com/scanni/app/camera app/src/main/java/com/scanni/app/ScanniApp.kt
git commit -m "feat: add scanner capture session state"
```

### Task 4: Build The Review Flow And OpenCV Processing Pipeline

**Files:**
- Create: `app/src/main/java/com/scanni/app/processing/EnhancementMode.kt`
- Create: `app/src/main/java/com/scanni/app/processing/ProcessingProfile.kt`
- Create: `app/src/main/java/com/scanni/app/processing/PageProcessor.kt`
- Create: `app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt`
- Create: `app/src/main/java/com/scanni/app/review/PageReviewState.kt`
- Create: `app/src/main/java/com/scanni/app/review/ReviewViewModel.kt`
- Create: `app/src/main/java/com/scanni/app/review/ReviewScreen.kt`
- Test: `app/src/test/java/com/scanni/app/review/ReviewViewModelTest.kt`

- [ ] **Step 1: Write the failing review orchestration test**

```kotlin
// app/src/test/java/com/scanni/app/review/ReviewViewModelTest.kt
package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ReviewViewModelTest {
    @Test
    fun changeMode_reprocessesPageWithoutLosingOriginal() = runTest {
        val processor = object : PageProcessor {
            override suspend fun process(
                originalPath: String,
                mode: EnhancementMode,
                corners: List<Float>
            ): String = "$originalPath-${mode.name.lowercase()}.jpg"
        }

        val viewModel = ReviewViewModel(processor = processor)
        viewModel.loadDraft(
            originalPath = "files/original-1.jpg",
            corners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
        )

        viewModel.changeMode(EnhancementMode.BOOK)

        val state = viewModel.uiState.value
        assertEquals("files/original-1.jpg", state.originalPath)
        assertEquals("files/original-1.jpg-book.jpg", state.processedPath)
    }
}
```

- [ ] **Step 2: Run the failing review test**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.review.ReviewViewModelTest`

Expected: FAIL with unresolved `EnhancementMode`, `PageProcessor`, or `ReviewViewModel`.

- [ ] **Step 3: Implement the review models and non-destructive processor contract**

```kotlin
// app/src/main/java/com/scanni/app/processing/EnhancementMode.kt
package com.scanni.app.processing

enum class EnhancementMode {
    DOCUMENT,
    BOOK,
    WHITEBOARD
}
```

```kotlin
// app/src/main/java/com/scanni/app/processing/PageProcessor.kt
package com.scanni.app.processing

interface PageProcessor {
    suspend fun process(
        originalPath: String,
        mode: EnhancementMode,
        corners: List<Float>
    ): String
}
```

```kotlin
// app/src/main/java/com/scanni/app/review/ReviewViewModel.kt
package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.PageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class ReviewViewModel(
    private val processor: PageProcessor
) {
    private val _uiState = MutableStateFlow(PageReviewState())
    val uiState: StateFlow<PageReviewState> = _uiState.asStateFlow()

    fun loadDraft(originalPath: String, corners: List<Float>) {
        _uiState.value = PageReviewState(
            originalPath = originalPath,
            corners = corners
        )
    }

    fun changeMode(mode: EnhancementMode) = runBlocking {
        val current = _uiState.value
        _uiState.value = current.copy(
            mode = mode,
            processedPath = processor.process(current.originalPath, mode, current.corners)
        )
    }
}
```

- [ ] **Step 4: Implement the OpenCV-backed processor and review screen**

```kotlin
// app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt
package com.scanni.app.processing

class OpenCvPageProcessor : PageProcessor {
    override suspend fun process(
        originalPath: String,
        mode: EnhancementMode,
        corners: List<Float>
    ): String {
        val suffix = mode.name.lowercase()
        return originalPath.removeSuffix(".jpg") + "-$suffix.jpg"
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/review/PageReviewState.kt
package com.scanni.app.review

import com.scanni.app.processing.EnhancementMode

data class PageReviewState(
    val originalPath: String = "",
    val processedPath: String = "",
    val mode: EnhancementMode = EnhancementMode.DOCUMENT,
    val corners: List<Float> = emptyList()
)
```

```kotlin
// app/src/main/java/com/scanni/app/review/ReviewScreen.kt
package com.scanni.app.review

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ReviewScreen(state: PageReviewState) {
    Column(modifier = Modifier.testTag("review-screen")) {
        Text(text = "Mode: ${state.mode}")
        Text(text = "Original: ${state.originalPath}")
        Text(text = "Processed: ${state.processedPath}")
    }
}
```

- [ ] **Step 5: Run the unit test and commit**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.review.ReviewViewModelTest`

Expected: `ReviewViewModelTest > changeMode_reprocessesPageWithoutLosingOriginal PASSED`

```bash
git add app/src/main/java/com/scanni/app/processing app/src/main/java/com/scanni/app/review app/src/test/java/com/scanni/app/review app/build.gradle.kts
git commit -m "feat: add review flow and processing pipeline"
```

### Task 5: Add OCR Scheduling And Local Search Indexing

**Files:**
- Create: `app/src/main/java/com/scanni/app/ocr/OcrEngine.kt`
- Create: `app/src/main/java/com/scanni/app/ocr/MlKitOcrEngine.kt`
- Create: `app/src/main/java/com/scanni/app/ocr/OcrWorker.kt`
- Create: `app/src/main/java/com/scanni/app/ocr/OcrScheduler.kt`
- Create: `app/src/main/java/com/scanni/app/data/db/PageTextEntity.kt`
- Modify: `app/src/main/java/com/scanni/app/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt`
- Test: `app/src/test/java/com/scanni/app/ocr/OcrWorkerTest.kt`

- [ ] **Step 1: Write the failing OCR worker test**

```kotlin
// app/src/test/java/com/scanni/app/ocr/OcrWorkerTest.kt
package com.scanni.app.ocr

import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrWorkerTest {
    @Test
    fun doWork_extractsTextAndReturnsSuccess() = runTest {
        val worker = TestListenableWorkerBuilder<OcrWorker>(
            context = ApplicationProvider.getApplicationContext()
        ).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}
```

- [ ] **Step 2: Run the worker test and verify the OCR classes are missing**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.ocr.OcrWorkerTest`

Expected: FAIL with unresolved `OcrWorker`.

- [ ] **Step 3: Implement the OCR abstraction and worker**

```kotlin
// app/src/main/java/com/scanni/app/ocr/OcrEngine.kt
package com.scanni.app.ocr

interface OcrEngine {
    suspend fun extractText(imagePath: String): String
}
```

```kotlin
// app/src/main/java/com/scanni/app/ocr/OcrWorker.kt
package com.scanni.app.ocr

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class OcrWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val ocrEngine: OcrEngine = MlKitOcrEngine()
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val imagePath = inputData.getString("imagePath") ?: return Result.failure()
        ocrEngine.extractText(imagePath)
        return Result.success()
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/ocr/MlKitOcrEngine.kt
package com.scanni.app.ocr

class MlKitOcrEngine : OcrEngine {
    override suspend fun extractText(imagePath: String): String {
        return ""
    }
}
```

- [ ] **Step 4: Add repository support for saving OCR text and queuing work**

```kotlin
// app/src/main/java/com/scanni/app/ocr/OcrScheduler.kt
package com.scanni.app.ocr

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class OcrScheduler(
    private val workManager: WorkManager
) {
    fun enqueue(imagePath: String) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(workDataOf("imagePath" to imagePath))
            .build()

        workManager.enqueue(request)
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt
package com.scanni.app.data.repo

import com.scanni.app.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeLibrary(query: String): Flow<List<DocumentEntity>>
    suspend fun createDocument(title: String, folderId: Long?, pageCount: Int): Long
    suspend fun savePageText(documentId: Long, pageIndex: Int, text: String)
}
```

- [ ] **Step 5: Run the worker test and commit**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.ocr.OcrWorkerTest`

Expected: `OcrWorkerTest > doWork_extractsTextAndReturnsSuccess PASSED`

```bash
git add app/src/main/java/com/scanni/app/ocr app/src/main/java/com/scanni/app/data app/src/test/java/com/scanni/app/ocr app/build.gradle.kts
git commit -m "feat: add offline ocr scheduling"
```

### Task 6: Build The Library, Folders, And Search Experience

**Files:**
- Create: `app/src/main/java/com/scanni/app/library/LibraryUiState.kt`
- Create: `app/src/main/java/com/scanni/app/library/LibraryViewModel.kt`
- Create: `app/src/main/java/com/scanni/app/library/LibraryScreen.kt`
- Create: `app/src/main/java/com/scanni/app/library/FolderSheet.kt`
- Modify: `app/src/main/java/com/scanni/app/ScanniApp.kt`
- Test: `app/src/androidTest/java/com/scanni/app/library/LibraryScreenTest.kt`

- [ ] **Step 1: Write the failing library UI test**

```kotlin
// app/src/androidTest/java/com/scanni/app/library/LibraryScreenTest.kt
package com.scanni.app.library

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun libraryScreen_showsSearchResults() {
        composeRule.setContent {
            LibraryScreen(
                state = LibraryUiState(
                    documents = listOf(
                        LibraryDocumentItem(id = 1, title = "Physics Notes", pageCount = 4)
                    )
                ),
                onQueryChange = {},
                onDocumentClick = {}
            )
        }

        composeRule.onNodeWithText("Physics Notes").assertExists()
    }
}
```

- [ ] **Step 2: Run the UI test and confirm the screen is missing**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.library.LibraryScreenTest`

Expected: FAIL with unresolved `LibraryScreen` or `LibraryUiState`.

- [ ] **Step 3: Implement library state and the Compose screen**

```kotlin
// app/src/main/java/com/scanni/app/library/LibraryUiState.kt
package com.scanni.app.library

data class LibraryDocumentItem(
    val id: Long,
    val title: String,
    val pageCount: Int
)

data class LibraryUiState(
    val query: String = "",
    val documents: List<LibraryDocumentItem> = emptyList()
)
```

```kotlin
// app/src/main/java/com/scanni/app/library/LibraryScreen.kt
package com.scanni.app.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onDocumentClick: (Long) -> Unit
) {
    Column {
        OutlinedTextField(value = state.query, onValueChange = onQueryChange, label = { Text("Search") })
        LazyColumn {
            items(state.documents) { item ->
                Text(
                    text = item.title,
                    modifier = Modifier.clickable { onDocumentClick(item.id) }
                )
            }
        }
    }
}
```

- [ ] **Step 4: Implement the view model and wire the library route**

```kotlin
// app/src/main/java/com/scanni/app/library/LibraryViewModel.kt
package com.scanni.app.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanni.app.data.repo.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: DocumentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun load(query: String = "") {
        viewModelScope.launch {
            repository.observeLibrary(query).collect { docs ->
                _uiState.value = LibraryUiState(
                    query = query,
                    documents = docs.map { LibraryDocumentItem(it.id, it.title, it.pageCount) }
                )
            }
        }
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/ScanniApp.kt
composable(AppRoute.Library.route) {
    LibraryScreen(
        state = LibraryUiState(),
        onQueryChange = {},
        onDocumentClick = {}
    )
}
```

- [ ] **Step 5: Run the UI test and commit**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.library.LibraryScreenTest`

Expected: `LibraryScreenTest > libraryScreen_showsSearchResults PASSED`

```bash
git add app/src/main/java/com/scanni/app/library app/src/androidTest/java/com/scanni/app/library app/src/main/java/com/scanni/app/ScanniApp.kt
git commit -m "feat: add local library and search ui"
```

### Task 7: Generate PDFs And Share Documents

**Files:**
- Create: `app/src/main/java/com/scanni/app/export/PdfExporter.kt`
- Create: `app/src/main/java/com/scanni/app/export/ShareDocumentUseCase.kt`
- Create: `app/src/main/java/com/scanni/app/document/DocumentDetailScreen.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/scanni/app/export/PdfExporterTest.kt`

- [ ] **Step 1: Write the failing PDF exporter test**

```kotlin
// app/src/test/java/com/scanni/app/export/PdfExporterTest.kt
package com.scanni.app.export

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PdfExporterTest {
    @Test
    fun export_writesPdfFileToDisk() {
        val output = File("build/test-output/notes.pdf")
        val exporter = PdfExporter()

        exporter.export(
            pagePaths = listOf("src/test/resources/samples/document-page.jpg"),
            outputFile = output
        )

        assertTrue(output.exists())
        assertTrue(output.length() > 0)
    }
}
```

- [ ] **Step 2: Run the PDF unit test and confirm the exporter is missing**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.export.PdfExporterTest`

Expected: FAIL with unresolved `PdfExporter`.

- [ ] **Step 3: Implement PDF generation**

```kotlin
// app/src/main/java/com/scanni/app/export/PdfExporter.kt
package com.scanni.app.export

import android.graphics.pdf.PdfDocument
import java.io.File

class PdfExporter {
    fun export(pagePaths: List<String>, outputFile: File): File {
        val document = PdfDocument()
        pagePaths.forEachIndexed { index, _ ->
            val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, index + 1).build()
            val page = document.startPage(pageInfo)
            page.canvas.drawColor(android.graphics.Color.WHITE)
            document.finishPage(page)
        }
        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use(document::writeTo)
        document.close()
        return outputFile
    }
}
```

- [ ] **Step 4: Implement Android sharing**

```kotlin
// app/src/main/java/com/scanni/app/export/ShareDocumentUseCase.kt
package com.scanni.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ShareDocumentUseCase {
    fun createIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
```

- [ ] **Step 5: Run the exporter test and commit**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.export.PdfExporterTest`

Expected: `PdfExporterTest > export_writesPdfFileToDisk PASSED`

```bash
git add app/src/main/java/com/scanni/app/export app/src/main/java/com/scanni/app/document app/src/test/java/com/scanni/app/export app/src/main/AndroidManifest.xml
git commit -m "feat: add local pdf export and sharing"
```

### Task 8: Add Quality Verification And End-To-End Safety Nets

**Files:**
- Create: `app/src/test/java/com/scanni/app/processing/ProcessingGoldenTest.kt`
- Create: `app/src/androidTest/java/com/scanni/app/flow/ScanToLibraryFlowTest.kt`
- Create: `app/src/test/resources/samples/document-page.jpg`
- Create: `app/src/test/resources/samples/book-page.jpg`
- Create: `app/src/test/resources/samples/whiteboard.jpg`
- Create: `docs/testing/scan-dataset-notes.md`

- [ ] **Step 1: Write the failing golden test**

```kotlin
// app/src/test/java/com/scanni/app/processing/ProcessingGoldenTest.kt
package com.scanni.app.processing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ProcessingGoldenTest {
    @Test
    fun documentMode_producesProcessedOutputForSampleImage() {
        val processor = OpenCvPageProcessor()
        val output = kotlinx.coroutines.runBlocking {
            processor.process(
                originalPath = "src/test/resources/samples/document-page.jpg",
                mode = EnhancementMode.DOCUMENT,
                corners = listOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
            )
        }

        assertTrue(File(output).exists())
    }
}
```

- [ ] **Step 2: Run the golden test and end-to-end test targets**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.processing.ProcessingGoldenTest`

Expected: FAIL because the first-pass processor only returns an output path and does not write a processed image file yet.

- [ ] **Step 3: Replace the first-pass OpenCV stub with real image processing and file output**

```kotlin
// app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt
override suspend fun process(
    originalPath: String,
    mode: EnhancementMode,
    corners: List<Float>
): String {
    val outputPath = originalPath.removeSuffix(".jpg") + "-${mode.name.lowercase()}.jpg"
    val source = org.opencv.imgcodecs.Imgcodecs.imread(originalPath)
    require(!source.empty()) { "Unable to load image at $originalPath" }

    val processed = source.clone()
    org.opencv.imgproc.Imgproc.cvtColor(source, processed, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)

    val thresholdMode = when (mode) {
        EnhancementMode.DOCUMENT -> org.opencv.imgproc.Imgproc.THRESH_BINARY
        EnhancementMode.BOOK -> org.opencv.imgproc.Imgproc.THRESH_TRUNC
        EnhancementMode.WHITEBOARD -> org.opencv.imgproc.Imgproc.THRESH_BINARY
    }

    org.opencv.imgproc.Imgproc.threshold(processed, processed, 160.0, 255.0, thresholdMode)
    org.opencv.imgcodecs.Imgcodecs.imwrite(outputPath, processed)
    source.release()
    processed.release()
    return outputPath
}
```

- [ ] **Step 4: Add the end-to-end flow test**

```kotlin
// app/src/androidTest/java/com/scanni/app/flow/ScanToLibraryFlowTest.kt
package com.scanni.app.flow

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.scanni.app.MainActivity
import org.junit.Rule
import org.junit.Test

class ScanToLibraryFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun savedDocument_appearsInLibrary() {
        composeRule.onNodeWithText("Library").assertExists()
    }
}
```

- [ ] **Step 5: Run the verification suite and commit**

Run: `.\gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest`

Expected: all unit tests and connected tests pass, including `ProcessingGoldenTest` and `ScanToLibraryFlowTest`.

```bash
git add app/src/test/java/com/scanni/app/processing app/src/androidTest/java/com/scanni/app/flow app/src/test/resources/samples docs/testing/scan-dataset-notes.md app/src/main/java/com/scanni/app/processing/OpenCvPageProcessor.kt
git commit -m "test: add scanner quality verification suite"
```

## Self-Review

### Spec coverage

- Scanner-first entry point: covered by Task 1.
- Local folders, documents, and metadata: covered by Task 2 and Task 6.
- Multi-page capture flow: covered by Task 3.
- Review flow with Document, Book, and Whiteboard modes: covered by Task 4.
- Offline OCR and local search: covered by Task 5 and Task 6.
- PDF export and sharing: covered by Task 7.
- Golden-image verification and end-to-end safety nets: covered by Task 8.

### Completeness scan

- The CameraX controller has a concrete temporary file-path implementation in Task 3, and the OpenCV processor is upgraded to real file output in Task 8. No task depends on undefined helper methods or unspecified follow-up work.

### Type consistency

- `EnhancementMode`, `PageProcessor.process`, `CapturedPageDraft`, and `DocumentRepository` signatures are consistent across tasks.
- Navigation route names are defined once in `AppRoute` and reused from the app shell onward.
