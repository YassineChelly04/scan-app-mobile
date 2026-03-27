package com.scanni.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.scanni.app.review.ReviewViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanniApp() {
    val navController = rememberNavController()
    var pendingDraft by remember { mutableStateOf<CapturedPageDraft?>(null) }

    NavHost(navController = navController, startDestination = AppRoute.Scanner.route) {
        composable(AppRoute.Scanner.route) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            val controller = remember(context, lifecycleOwner) {
                CameraXScannerController(
                    appContext = context,
                    outputDir = File(context.cacheDir, "captures"),
                    lifecycleOwnerProvider = { lifecycleOwner }
                )
            }
            val scannerViewModel: ScannerViewModel = viewModel()
            val state by scannerViewModel.uiState.collectAsStateWithLifecycle()
            ScannerScreen(
                state = state,
                onCameraCaptureClick = {
                    coroutineScope.launch {
                        val draft = controller.capturePage()
                        scannerViewModel.onPageCaptured(draft)
                        pendingDraft = draft
                        navController.navigate(AppRoute.Review.route)
                    }
                },
                onSampleCaptureClick = {
                    coroutineScope.launch {
                        val outputDir = File(context.cacheDir, "captures")
                        outputDir.mkdirs()
                        val draft = CapturedPageDraft(
                            originalPath = File(outputDir, "sample-original.jpg").absolutePath,
                            previewPath = File(outputDir, "sample-preview.jpg").absolutePath,
                            detectedCorners = listOf(
                                0f, 0f,
                                1f, 0f,
                                1f, 1f,
                                0f, 1f
                            )
                        )
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
                        pendingDraft = draft
                        navController.navigate(AppRoute.Review.route)
                    }
                },
                onLibraryClick = { navController.navigate(AppRoute.Library.route) }
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
            val draft = pendingDraft
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
                onSaveClick = {
                    if (state.processedPath.isBlank()) return@ReviewScreen
                    coroutineScope.launch {
                        repository.saveProcessedDocument(
                            title = "Quick Scan",
                            folderId = null,
                            pageImageUris = listOf(state.processedPath)
                        )
                        withContext(Dispatchers.Main.immediate) {
                            pendingDraft = null
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
