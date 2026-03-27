package com.scanni.app.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    controller: CameraXScannerController,
    modifier: Modifier = Modifier
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier.testTag("camera-preview"),
        factory = { context ->
            PreviewView(context).also { createdView ->
                previewView = createdView
            }
        },
        update = { updatedView ->
            previewView = updatedView
        }
    )

    LaunchedEffect(controller, previewView) {
        previewView?.let { readyPreview ->
            controller.bindPreview(readyPreview)
        }
    }
}
