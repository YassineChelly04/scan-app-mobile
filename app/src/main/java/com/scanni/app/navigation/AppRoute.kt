package com.scanni.app.navigation

sealed class AppRoute(val route: String) {
    data object Scanner : AppRoute("scanner")
    data object Review : AppRoute("review")
    data object Library : AppRoute("library")
    data object DocumentDetail : AppRoute("document/{documentId}")
}
