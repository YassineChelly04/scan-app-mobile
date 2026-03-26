package com.scanni.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scanni.app.camera.ScannerScreen
import com.scanni.app.camera.ScannerViewModel
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
    }
}
