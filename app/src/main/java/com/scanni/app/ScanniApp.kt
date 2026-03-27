package com.scanni.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scanni.app.camera.ScannerScreen
import com.scanni.app.camera.ScannerViewModel
import com.scanni.app.data.db.AppDatabase
import com.scanni.app.data.repo.LocalDocumentRepository
import com.scanni.app.library.LibraryScreen
import com.scanni.app.library.LibraryViewModel
import com.scanni.app.navigation.AppRoute

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
                onDocumentClick = {}
            )
        }
    }
}
