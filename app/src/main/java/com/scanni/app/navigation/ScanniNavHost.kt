package com.scanni.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.scanni.app.di.AppGraph
import com.scanni.app.ui.document.DocumentScreen
import com.scanni.app.ui.editpage.EditPageScreen
import com.scanni.app.ui.library.LibraryScreen
import com.scanni.app.ui.review.ReviewScreen
import com.scanni.app.ui.scanner.ScannerScreen
import com.scanni.app.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
object ScannerRoute

@Serializable
object ReviewRoute

@Serializable
data class DocumentRoute(val documentId: String)

@Serializable
data class EditPageRoute(val documentId: String, val pageId: String)

@Serializable
object SettingsRoute

@Composable
fun ScanniNavHost(graph: AppGraph) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutHorizontally(tween(280)) { -it / 6 } + fadeOut(tween(280))
        },
        popEnterTransition = {
            slideInHorizontally(tween(280)) { -it / 6 } + fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(280))
        },
    ) {
        composable<LibraryRoute> {
            LibraryScreen(
                graph = graph,
                onScan = { navController.navigate(ScannerRoute) },
                onOpenDocument = { id -> navController.navigate(DocumentRoute(id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<ScannerRoute> {
            ScannerScreen(
                graph = graph,
                onClose = {
                    graph.scanSession.clear()
                    graph.fileStore.clearSession()
                    navController.popBackStack()
                },
                onReview = { navController.navigate(ReviewRoute) },
            )
        }

        composable<ReviewRoute> {
            ReviewScreen(
                graph = graph,
                onBackToCamera = { navController.popBackStack() },
                onDiscarded = { navController.popBackStack(LibraryRoute, inclusive = false) },
                onSaved = { documentId ->
                    navController.navigate(DocumentRoute(documentId)) {
                        popUpTo(LibraryRoute) { inclusive = false }
                    }
                },
            )
        }

        composable<DocumentRoute> { entry ->
            val route = entry.toRoute<DocumentRoute>()
            DocumentScreen(
                graph = graph,
                documentId = route.documentId,
                onBack = { navController.popBackStack() },
                onEditPage = { pageId ->
                    navController.navigate(EditPageRoute(route.documentId, pageId))
                },
            )
        }

        composable<EditPageRoute> { entry ->
            val route = entry.toRoute<EditPageRoute>()
            EditPageScreen(
                graph = graph,
                pageId = route.pageId,
                onDone = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                graph = graph,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
