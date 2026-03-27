package com.scanni.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import java.io.File

@Composable
fun ScanniApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoute.Scanner.route) {
        composable(AppRoute.Scanner.route) {
            val scannerViewModel: ScannerViewModel = viewModel()
            val state by scannerViewModel.uiState.collectAsStateWithLifecycle()
            ScannerScreen(state = state)
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
