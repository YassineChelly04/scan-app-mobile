package com.scanni.app.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    onCameraCaptureClick: () -> Unit,
    onSampleCaptureClick: () -> Unit,
    onLibraryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("scanner-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Pages: ${state.captureCount}")
        Button(onClick = onCameraCaptureClick) {
            Text(text = "Capture Camera")
        }
        Button(onClick = onSampleCaptureClick) {
            Text(text = "Capture Sample")
        }
        Button(onClick = onLibraryClick) {
            Text(text = "Library")
        }
    }
}
