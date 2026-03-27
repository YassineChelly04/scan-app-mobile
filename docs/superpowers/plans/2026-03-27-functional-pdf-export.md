# Functional PDF Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make PDF export actually usable from the app by letting a user open a saved document from the library, load its processed page files, generate a PDF from those processed pages, and launch Android share.

**Architecture:** Build the thinnest real export path on top of the current app. Reuse `PageEntity.imageUri` as the processed scan path, add the missing Room queries and document-detail state loader, then wire library taps into a real detail route that can export and share.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Room, Android `PdfDocument`, AndroidX `FileProvider`, JUnit, Robolectric, AndroidX Compose UI Test

---

## Planned File Structure

- `app/src/main/java/com/scanni/app/data/db/DocumentDao.kt`
  Responsibility: Add direct document lookup by ID.
- `app/src/main/java/com/scanni/app/data/db/PageDao.kt`
  Responsibility: Load ordered pages for a saved document.
- `app/src/main/java/com/scanni/app/data/db/AppDatabase.kt`
  Responsibility: Register `PageDao`.
- `app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt`
  Responsibility: Expose document-detail/export lookup contract.
- `app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt`
  Responsibility: Join document metadata with ordered page file paths.
- `app/src/main/java/com/scanni/app/document/ExportableDocument.kt`
  Responsibility: Represent one saved document plus the processed page paths used for export.
- `app/src/main/java/com/scanni/app/document/DocumentDetailViewModel.kt`
  Responsibility: Load detail state, validate processed page files, generate a PDF, and expose share-ready output.
- `app/src/main/java/com/scanni/app/document/DocumentDetailScreen.kt`
  Responsibility: Render the saved document metadata and share/export state.
- `app/src/main/java/com/scanni/app/navigation/AppRoute.kt`
  Responsibility: Add a helper for concrete `DocumentDetail` routes.
- `app/src/main/java/com/scanni/app/ScanniApp.kt`
  Responsibility: Wire library taps to document detail and connect export/share behavior.
- `app/src/main/java/com/scanni/app/export/PdfExporter.kt`
  Responsibility: Generate PDFs from the processed page image paths.
- `app/src/main/java/com/scanni/app/export/ShareDocumentUseCase.kt`
  Responsibility: Build the Android share intent for a generated PDF.
- `app/src/test/java/com/scanni/app/data/repo/LocalDocumentRepositoryTest.kt`
  Responsibility: Prove exportable document lookup returns ordered processed page paths.
- `app/src/test/java/com/scanni/app/document/DocumentDetailViewModelTest.kt`
  Responsibility: Prove export success and export rejection when processed page files are missing.
- `app/src/test/java/com/scanni/app/export/PdfExporterTest.kt`
  Responsibility: Prove a PDF is written from the planned sample page path.
- `app/src/test/java/com/scanni/app/export/ShareDocumentUseCaseTest.kt`
  Responsibility: Prove `FileProvider`-based sharing intent construction.
- `app/src/androidTest/java/com/scanni/app/document/DocumentDetailScreenTest.kt`
  Responsibility: Prove the detail screen shows the share action and error states.

## Task 1: Add Exportable Document Queries

**Files:**
- Create: `app/src/main/java/com/scanni/app/data/db/PageDao.kt`
- Modify: `app/src/main/java/com/scanni/app/data/db/DocumentDao.kt`
- Modify: `app/src/main/java/com/scanni/app/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt`
- Modify: `app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt`
- Create: `app/src/main/java/com/scanni/app/document/ExportableDocument.kt`
- Test: `app/src/test/java/com/scanni/app/data/repo/LocalDocumentRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository test for ordered processed page lookup**

```kotlin
@Test
fun getExportableDocument_returnsOrderedProcessedPagePaths() = runTest {
    val documentId = repository.createDocument(
        title = "Physics Chapter 3",
        folderId = null,
        pageCount = 2
    )
    db.pageDao().insertAll(
        listOf(
            PageEntity(documentId = documentId, pageNumber = 2, imageUri = "files/page-2-processed.jpg"),
            PageEntity(documentId = documentId, pageNumber = 1, imageUri = "files/page-1-processed.jpg")
        )
    )

    val exportable = repository.getExportableDocument(documentId)

    assertEquals("Physics Chapter 3", exportable?.title)
    assertEquals(
        listOf("files/page-1-processed.jpg", "files/page-2-processed.jpg"),
        exportable?.pageImageUris
    )
}
```

- [ ] **Step 2: Run the repository test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.data.repo.LocalDocumentRepositoryTest`

Expected: FAIL with unresolved `pageDao`, `getExportableDocument`, or `ExportableDocument`.

- [ ] **Step 3: Add the exportable document model and missing Room queries**

```kotlin
// app/src/main/java/com/scanni/app/document/ExportableDocument.kt
package com.scanni.app.document

data class ExportableDocument(
    val id: Long,
    val title: String,
    val pageCount: Int,
    val ocrStatus: String,
    val pageImageUris: List<String>
)
```

```kotlin
// app/src/main/java/com/scanni/app/data/db/PageDao.kt
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
```

```kotlin
// app/src/main/java/com/scanni/app/data/db/DocumentDao.kt
@Query(
    """
    SELECT * FROM documents
    WHERE id = :documentId
    LIMIT 1
    """
)
suspend fun getById(documentId: Long): DocumentEntity?
```

```kotlin
// app/src/main/java/com/scanni/app/data/db/AppDatabase.kt
abstract fun pageDao(): PageDao
```

```kotlin
// app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt
suspend fun getExportableDocument(documentId: Long): ExportableDocument?
```

```kotlin
// app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt
override suspend fun getExportableDocument(documentId: Long): ExportableDocument? {
    val document = documentDao.getById(documentId) ?: return null
    val pages = pageDao.getPagesForDocument(documentId)

    return ExportableDocument(
        id = document.id,
        title = document.title,
        pageCount = document.pageCount,
        ocrStatus = document.ocrStatus,
        pageImageUris = pages.map { page -> page.imageUri }
    )
}
```

- [ ] **Step 4: Run the repository test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.data.repo.LocalDocumentRepositoryTest`

Expected: PASS including `getExportableDocument_returnsOrderedProcessedPagePaths`.

- [ ] **Step 5: Commit the exportable document query layer**

```bash
git add app/src/main/java/com/scanni/app/data/db/DocumentDao.kt app/src/main/java/com/scanni/app/data/db/PageDao.kt app/src/main/java/com/scanni/app/data/db/AppDatabase.kt app/src/main/java/com/scanni/app/data/repo/DocumentRepository.kt app/src/main/java/com/scanni/app/data/repo/LocalDocumentRepository.kt app/src/main/java/com/scanni/app/document/ExportableDocument.kt app/src/test/java/com/scanni/app/data/repo/LocalDocumentRepositoryTest.kt
git commit -m "feat: add exportable document queries"
```

## Task 2: Add Document Detail Export State

**Files:**
- Create: `app/src/main/java/com/scanni/app/document/DocumentDetailViewModel.kt`
- Modify: `app/src/main/java/com/scanni/app/document/DocumentDetailScreen.kt`
- Test: `app/src/test/java/com/scanni/app/document/DocumentDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing document detail view model tests**

```kotlin
@Test
fun loadDocument_populatesDetailState() = runTest {
    val viewModel = DocumentDetailViewModel(
        documentId = 7L,
        repository = FakeRepository(
            exportableDocument = ExportableDocument(
                id = 7L,
                title = "Chemistry Notes",
                pageCount = 2,
                ocrStatus = "complete",
                pageImageUris = listOf("build/test-output/page-1.jpg", "build/test-output/page-2.jpg")
            )
        ),
        pdfExporter = FakePdfExporter(),
        outputDir = File("build/test-output")
    )

    viewModel.load()

    val state = viewModel.uiState.value
    assertEquals("Chemistry Notes", state.title)
    assertEquals(2, state.pageCount)
    assertTrue(state.canShare)
}
```

```kotlin
@Test
fun onShareClick_withMissingProcessedPage_setsError() = runTest {
    val missingPath = File("build/test-output/missing.jpg").absolutePath
    val viewModel = DocumentDetailViewModel(
        documentId = 9L,
        repository = FakeRepository(
            exportableDocument = ExportableDocument(
                id = 9L,
                title = "Biology Notes",
                pageCount = 1,
                ocrStatus = "complete",
                pageImageUris = listOf(missingPath)
            )
        ),
        pdfExporter = FakePdfExporter(),
        outputDir = File("build/test-output")
    )

    viewModel.load()
    viewModel.onShareClick()

    assertEquals("Processed page file missing.", viewModel.uiState.value.errorMessage)
    assertEquals(null, viewModel.uiState.value.generatedPdf)
}
```

- [ ] **Step 2: Run the document detail tests to verify they fail**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.document.DocumentDetailViewModelTest`

Expected: FAIL with unresolved `DocumentDetailViewModel`, `canShare`, or `generatedPdf`.

- [ ] **Step 3: Implement the minimal document detail export controller**

```kotlin
// app/src/main/java/com/scanni/app/document/DocumentDetailScreen.kt
data class DocumentDetailUiState(
    val title: String = "",
    val pageCount: Int = 0,
    val ocrStatus: String = "",
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val canShare: Boolean = false,
    val errorMessage: String? = null,
    val generatedPdf: File? = null
)
```

```kotlin
// app/src/main/java/com/scanni/app/document/DocumentDetailViewModel.kt
class DocumentDetailViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository,
    private val pdfExporter: PdfExporter,
    private val outputDir: File
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    private var currentDocument: ExportableDocument? = null

    fun load() = viewModelScope.launch {
        val exportable = repository.getExportableDocument(documentId)
        currentDocument = exportable
        _uiState.value = if (exportable == null) {
            DocumentDetailUiState(isLoading = false, errorMessage = "Document not found.")
        } else {
            DocumentDetailUiState(
                title = exportable.title,
                pageCount = exportable.pageCount,
                ocrStatus = exportable.ocrStatus,
                isLoading = false,
                canShare = exportable.pageImageUris.isNotEmpty()
            )
        }
    }

    fun onShareClick() = viewModelScope.launch {
        val exportable = currentDocument ?: return@launch
        val paths = exportable.pageImageUris
        if (paths.any { path -> !File(path).exists() }) {
            _uiState.update { state -> state.copy(errorMessage = "Processed page file missing.") }
            return@launch
        }

        _uiState.update { state -> state.copy(isExporting = true, errorMessage = null) }
        val outputFile = File(outputDir, "document-${exportable.id}.pdf")
        val generated = pdfExporter.export(paths, outputFile)
        _uiState.update { state ->
            state.copy(isExporting = false, generatedPdf = generated)
        }
    }
}
```

- [ ] **Step 4: Run the document detail tests to verify they pass**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.document.DocumentDetailViewModelTest`

Expected: PASS with both load and missing-file export cases green.

- [ ] **Step 5: Commit the document detail export controller**

```bash
git add app/src/main/java/com/scanni/app/document/DocumentDetailViewModel.kt app/src/main/java/com/scanni/app/document/DocumentDetailScreen.kt app/src/test/java/com/scanni/app/document/DocumentDetailViewModelTest.kt
git commit -m "feat: add document detail export state"
```

## Task 3: Wire Library Navigation To Functional Document Detail

**Files:**
- Modify: `app/src/main/java/com/scanni/app/navigation/AppRoute.kt`
- Modify: `app/src/main/java/com/scanni/app/ScanniApp.kt`
- Test: `app/src/androidTest/java/com/scanni/app/document/DocumentDetailScreenTest.kt`

- [ ] **Step 1: Write the failing document detail screen test**

```kotlin
@Test
fun documentDetailScreen_showsShareAction() {
    composeRule.setContent {
        DocumentDetailScreen(
            state = DocumentDetailUiState(
                title = "Lecture Notes",
                pageCount = 3,
                ocrStatus = "complete",
                isLoading = false,
                canShare = true
            ),
            onShareClick = {}
        )
    }

    composeRule.onNodeWithText("Share PDF").assertExists()
}
```

- [ ] **Step 2: Run the screen test to verify it fails if the API is still outdated**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.document.DocumentDetailScreenTest`

Expected: FAIL if `DocumentDetailScreen` has not yet been updated for the new state contract.

- [ ] **Step 3: Wire the library click path into the detail route**

```kotlin
// app/src/main/java/com/scanni/app/navigation/AppRoute.kt
sealed class AppRoute(val route: String) {
    data object Scanner : AppRoute("scanner")
    data object Review : AppRoute("review")
    data object Library : AppRoute("library")
    data object DocumentDetail : AppRoute("document/{documentId}") {
        fun create(documentId: Long): String = "document/$documentId"
    }
}
```

```kotlin
// app/src/main/java/com/scanni/app/ScanniApp.kt
LibraryScreen(
    state = state,
    onQueryChange = libraryViewModel::onQueryChange,
    onFolderClick = libraryViewModel::onFolderClick,
    onDocumentClick = { documentId ->
        navController.navigate(AppRoute.DocumentDetail.create(documentId))
    }
)
```

```kotlin
// app/src/main/java/com/scanni/app/ScanniApp.kt
composable(AppRoute.DocumentDetail.route) { backStackEntry ->
    val context = LocalContext.current
    val database = AppDatabase.getInstance(context)
    val documentId = backStackEntry.arguments?.getString("documentId")?.toLongOrNull() ?: -1L
    val viewModel: DocumentDetailViewModel = viewModel(
        factory = DocumentDetailViewModel.factory(
            documentId = documentId,
            repository = LocalDocumentRepository(
                documentDao = database.documentDao(),
                pageTextDao = database.pageTextDao(),
                pageDao = database.pageDao()
            ),
            pdfExporter = PdfExporter(),
            outputDir = File(context.cacheDir, "exports")
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(documentId) { viewModel.load() }
    val shareDocument = ShareDocumentUseCase()
    DocumentDetailScreen(
        state = state,
        onShareClick = { viewModel.onShareClick() }
    )
    LaunchedEffect(state.generatedPdf) {
        state.generatedPdf?.let { pdf ->
            context.startActivity(
                Intent.createChooser(
                    shareDocument.createIntent(context, pdf),
                    "Share PDF"
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
```

- [ ] **Step 4: Run the screen test to verify it passes**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests com.scanni.app.document.DocumentDetailScreenTest`

Expected: PASS with the share action visible.

- [ ] **Step 5: Commit the document detail route wiring**

```bash
git add app/src/main/java/com/scanni/app/navigation/AppRoute.kt app/src/main/java/com/scanni/app/ScanniApp.kt app/src/androidTest/java/com/scanni/app/document/DocumentDetailScreenTest.kt
git commit -m "feat: wire library to document detail export"
```

## Task 4: Finalize Real PDF Export And Share Verification

**Files:**
- Modify: `app/src/main/java/com/scanni/app/export/PdfExporter.kt`
- Modify: `app/src/main/java/com/scanni/app/export/ShareDocumentUseCase.kt`
- Modify: `app/src/test/java/com/scanni/app/export/PdfExporterTest.kt`
- Modify: `app/src/test/java/com/scanni/app/export/ShareDocumentUseCaseTest.kt`

- [ ] **Step 1: Tighten the failing export tests around the functional path**

```kotlin
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
```

```kotlin
@Test
fun createIntent_usesFileProviderAuthority() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val file = File(context.cacheDir, "share.pdf").apply { writeText("pdf") }
    val intent = ShareDocumentUseCase().createIntent(context, file)

    assertEquals(Intent.ACTION_SEND, intent.action)
    assertEquals("application/pdf", intent.type)
    assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
}
```

- [ ] **Step 2: Run the targeted export tests**

Run: `.\gradlew :app:testDebugUnitTest --tests com.scanni.app.export.PdfExporterTest --tests com.scanni.app.export.ShareDocumentUseCaseTest`

Expected: PASS with the planned sample image path and `FileProvider` sharing contract.

- [ ] **Step 3: Verify the functional export slice end to end**

Run: `.\gradlew :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`

Run: `.\gradlew :app:connectedDebugAndroidTest`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit the final functional export verification updates**

```bash
git add app/src/main/java/com/scanni/app/export/PdfExporter.kt app/src/main/java/com/scanni/app/export/ShareDocumentUseCase.kt app/src/test/java/com/scanni/app/export/PdfExporterTest.kt app/src/test/java/com/scanni/app/export/ShareDocumentUseCaseTest.kt
git commit -m "test: verify functional pdf export flow"
```

## Self-Review

### Spec coverage

- Library-to-detail navigation: covered by Task 3.
- Document/page lookup for export: covered by Task 1.
- Export state, missing-file rejection, and generated PDF handoff: covered by Task 2.
- Real PDF generation from processed page paths plus Android share contract: covered by Task 4.

### Placeholder scan

- No `TODO`, `TBD`, or deferred “add tests later” placeholders remain.
- Every task includes concrete files, code, commands, and commit points.

### Type consistency

- `ExportableDocument.pageImageUris` is the single page-path contract used across repository, view model, and exporter tasks.
- `AppRoute.DocumentDetail.create(documentId)` is the single route builder used by navigation wiring.
- `DocumentDetailUiState.generatedPdf` is the single share handoff from view model to UI.
