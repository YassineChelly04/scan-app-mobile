package com.scanni.app.camera

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraXScannerController(
    private val outputDir: File
) : ScannerCameraController {

    override suspend fun capturePage(): CapturedPageDraft {
        outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val originalFile = File(outputDir, "scan_${timestamp}_original.jpg")
        val previewFile = File(outputDir, "scan_${timestamp}_preview.jpg")
        writeSampleImage(originalFile, Color.rgb(232, 219, 196), Color.rgb(58, 58, 58))
        writeSampleImage(previewFile, Color.rgb(244, 236, 219), Color.rgb(70, 70, 70))

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

    private fun writeSampleImage(file: File, backgroundColor: Int, accentColor: Int) {
        Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(backgroundColor)
            for (x in 140 until 760) {
                for (y in 180 until 1020) {
                    setPixel(x, y, accentColor)
                }
            }
            file.outputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            recycle()
        }
    }
}
