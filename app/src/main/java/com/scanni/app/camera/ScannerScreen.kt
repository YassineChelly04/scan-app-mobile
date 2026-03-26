package com.scanni.app.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ScannerScreen(state: ScannerUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scanner-screen")
    ) {
        Text(text = "Pages: ${state.captureCount}")
    }
}
