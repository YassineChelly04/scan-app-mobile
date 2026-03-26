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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("scanner-screen")
            )
        }
    }
}
