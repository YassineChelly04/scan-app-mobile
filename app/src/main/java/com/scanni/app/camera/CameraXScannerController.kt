package com.scanni.app.camera

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraXScannerController(
    private val outputDir: File
) : ScannerCameraController {

    override suspend fun capturePage(): CapturedPageDraft {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val originalFile = File(outputDir, "scan_${timestamp}_original.jpg")
        val previewFile = File(outputDir, "scan_${timestamp}_preview.jpg")

        return CapturedPageDraft(
            originalPath = originalFile.absolutePath,
            previewPath = previewFile.absolutePath,
            detectedCorners = listOf(
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            )
        )
    }
}
