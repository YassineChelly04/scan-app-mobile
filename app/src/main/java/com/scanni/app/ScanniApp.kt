package com.scanni.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.scanni.app.camera.CameraPreview
import com.scanni.app.camera.CameraXScannerController
import com.scanni.app.camera.CapturedPageDraft
import com.scanni.app.camera.ScannerScreen
import com.scanni.app.camera.ScannerViewModel
import com.scanni.app.data.db.AppDatabase
import com.scanni.app.data.repo.LocalDocumentRepository
import com.scanni.app.document.DocumentDetailScreen
import com.scanni.app.document.DocumentDetailViewModel
import com.scanni.app.export.PdfExporter
import com.scanni.app.export.ShareDocumentUseCase
import com.scanni.app.library.LibraryScreen
import com.scanni.app.library.LibraryViewModel
import com.scanni.app.navigation.AppRoute
import com.scanni.app.processing.EnhancementMode
import com.scanni.app.processing.OpenCvPageProcessor
import com.scanni.app.review.ReviewScreen
import com.scanni.app.review.SaveReviewedDocumentUseCase
import com.scanni.app.review.SaveReviewSessionUseCase
import com.scanni.app.review.ReviewViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanniApp() {
    val navController = rememberNavController()
    val scannerViewModel: ScannerViewModel = viewModel()
    val scannerState by scannerViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = AppRoute.Scanner.route) {
        composable(AppRoute.Scanner.route) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            var hasCameraPermission by remember(context) {
                mutableStateOf(context.hasCameraPermission())
            }
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasCameraPermission = granted
            }
            val controller = remember(context, lifecycleOwner) {
                CameraXScannerController(
                    appContext = context,
                    outputDir = File(context.cacheDir, "captures"),
                    lifecycleOwnerProvider = { lifecycleOwner }
                )
            }

            DisposableEffect(lifecycleOwner, context) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasCameraPermission = context.hasCameraPermission()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            ScannerScreen(
                state = scannerState,
                hasCameraPermission = hasCameraPermission,
                cameraPreview = {
                    CameraPreview(controller = controller)
                },
                onGrantCameraAccessClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCameraCaptureClick = {
                    coroutineScope.launch {
                        val draft = controller.capturePage()
                        scannerViewModel.onPageCaptured(draft)
                        navController.navigate(AppRoute.Review.route)
                    }
                },
                onSampleCaptureClick = {
                    coroutineScope.launch {
                        val draft = createSampleDraft(context)
                        CameraXScannerController.writeSampleImage(
                            File(draft.originalPath),
                            backgroundColor = android.graphics.Color.rgb(232, 219, 196),
                            accentColor = android.graphics.Color.rgb(58, 58, 58)
                        )
                        CameraXScannerController.writeSampleImage(
                            File(draft.previewPath),
                            backgroundColor = android.graphics.Color.rgb(244, 236, 219),
                            accentColor = android.graphics.Color.rgb(70, 70, 70)
                        )
                        scannerViewModel.onPageCaptured(draft)
                        navController.navigate(AppRoute.Review.route)
                    }
                },
                onLibraryClick = {
                    scannerViewModel.clearSession()
                    navController.navigate(AppRoute.Library.route)
                }
            )
        }
        composable(AppRoute.Review.route) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val database = AppDatabase.getInstance(context)
            val repository = remember(database) {
                LocalDocumentRepository(
                    documentDao = database.documentDao(),
                    pageTextDao = database.pageTextDao(),
                    pageDao = database.pageDao()
                )
            }
            val saveReviewedDocument = remember(repository) {
                SaveReviewedDocumentUseCase.create(
                    repository = repository,
                    workManager = WorkManager.getInstance(context)
                )
            }
            val saveReviewSession = remember(saveReviewedDocument) {
                SaveReviewSessionUseCase(
                    processor = OpenCvPageProcessor(),
                    saveReviewedDocument = saveReviewedDocument::invoke
                )
            }
            val draft = scannerState.pages.lastOrNull()
            val reviewViewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModel.factory(OpenCvPageProcessor())
            )
            val state by reviewViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(draft?.originalPath) {
                draft?.let {
                    reviewViewModel.loadDraft(
                        originalPath = it.originalPath,
                        corners = it.detectedCorners
                    )
                    reviewViewModel.changeMode(EnhancementMode.DOCUMENT)
                }
            }

            ReviewScreen(
                state = state,
                onModeChange = reviewViewModel::changeMode,
                onAddAnotherPageClick = {
                    navController.popBackStack()
                },
                onSaveClick = {
                    if (state.processedPath.isBlank() || scannerState.pages.isEmpty()) return@ReviewScreen
                    coroutineScope.launch {
                        saveReviewSession(
                            title = "Quick Scan",
                            folderId = null,
                            pages = scannerState.pages,
                            mode = state.mode
                        )
                        withContext(Dispatchers.Main.immediate) {
                            scannerViewModel.clearSession()
                            navController.navigate(AppRoute.Library.route) {
                                popUpTo(AppRoute.Scanner.route)
                            }
                        }
                    }
                }
            )
        }
        composable(AppRoute.Library.route) {
            val context = LocalContext.current
            val database = AppDatabase.getInstance(context)
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.factory(
                    repository = LocalDocumentRepository(
                        documentDao = database.documentDao(),
                        pageTextDao = database.pageTextDao(),
                        pageDao = database.pageDao()
                    ),
                    folderDao = database.folderDao()
                )
            )
            val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
            LibraryScreen(
                state = state,
                onQueryChange = libraryViewModel::onQueryChange,
                onFolderClick = libraryViewModel::onFolderClick,
                onDocumentClick = { documentId ->
                    navController.navigate(AppRoute.DocumentDetail.create(documentId))
                }
            )
        }
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
                    folderDao = database.folderDao(),
                    pdfExporter = PdfExporter(),
                    outputDir = File(context.cacheDir, "exports")
                )
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val shareDocument = ShareDocumentUseCase()

            LaunchedEffect(documentId) {
                viewModel.load()
            }

            DocumentDetailScreen(
                state = state,
                onTitleChange = viewModel::onTitleChange,
                onFolderSelected = viewModel::onFolderSelected,
                onSaveDetailsClick = viewModel::onSaveDetailsClick,
                onShareClick = viewModel::onShareClick
            )

            LaunchedEffect(state.generatedPdf) {
                state.generatedPdf?.let { pdf ->
                    context.startActivity(
                        Intent.createChooser(
                            shareDocument.createIntent(context, pdf),
                            "Share PDF"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    viewModel.onGeneratedPdfConsumed()
                }
            }
        }
    }
}

private fun android.content.Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun createSampleDraft(context: android.content.Context): CapturedPageDraft {
    val outputDir = File(context.cacheDir, "captures").apply { mkdirs() }
    val originalFile = File.createTempFile("sample_", "_original.jpg", outputDir)
    val previewFile = File.createTempFile("sample_", "_preview.jpg", outputDir)
    return CapturedPageDraft(
        originalPath = originalFile.absolutePath,
        previewPath = previewFile.absolutePath,
        detectedCorners = listOf(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )
    )
}
