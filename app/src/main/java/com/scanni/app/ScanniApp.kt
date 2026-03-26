package com.scanni.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanni.app.navigation.AppRoute

@Composable
fun ScanniApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoute.Scanner.route) {
        composable(AppRoute.Scanner.route) {
            ScannerScreen()
        }
    }
}
