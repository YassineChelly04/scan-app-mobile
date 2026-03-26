package com.scanni.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ScannerScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scanner-screen")
    )
}
