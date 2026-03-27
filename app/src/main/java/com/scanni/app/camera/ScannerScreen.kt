package com.scanni.app.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    hasCameraPermission: Boolean,
    cameraPreview: @Composable () -> Unit,
    onGrantCameraAccessClick: () -> Unit,
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreview()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Camera access is required for live capture.")
                    Button(
                        modifier = Modifier.testTag("grant-camera-access"),
                        onClick = onGrantCameraAccessClick
                    ) {
                        Text(text = "Grant Camera Access")
                    }
                }
            }
        }

        if (hasCameraPermission) {
            Button(onClick = onCameraCaptureClick) {
                Text(text = "Capture Camera")
            }
        }
        Button(onClick = onSampleCaptureClick) {
            Text(text = "Capture Sample")
        }
        Button(onClick = onLibraryClick) {
            Text(text = "Library")
        }
    }
}
