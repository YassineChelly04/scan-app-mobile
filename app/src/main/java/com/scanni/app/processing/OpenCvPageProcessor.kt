package com.scanni.app.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

class OpenCvPageProcessor : PageProcessor {
    override suspend fun process(
        originalPath: String,
        mode: EnhancementMode,
        corners: List<Float>
    ): String {
        val source = BitmapFactory.decodeFile(originalPath)
            ?: throw IllegalArgumentException("Unable to load image at $originalPath")
        val outputPath = originalPath.removeSuffix(".jpg") + "-${mode.name.lowercase()}.jpg"
        val outputFile = File(outputPath).apply {
            parentFile?.mkdirs()
        }
        val processed = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)

        try {
            for (x in 0 until source.width) {
                for (y in 0 until source.height) {
                    processed.setPixel(x, y, transformPixel(source.getPixel(x, y), mode))
                }
            }

            outputFile.outputStream().use { stream ->
                processed.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            return outputFile.absolutePath
        } finally {
            source.recycle()
            processed.recycle()
        }
    }

    private fun transformPixel(pixel: Int, mode: EnhancementMode): Int {
        val average = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
        return when (mode) {
            EnhancementMode.DOCUMENT -> {
                val value = if (average >= 140) 255 else 0
                Color.rgb(value, value, value)
            }

            EnhancementMode.BOOK -> {
                val value = (average + 35).coerceAtMost(255)
                Color.rgb(value, value, value)
            }

            EnhancementMode.WHITEBOARD -> {
                val boosted = ((average - 110) * 3).coerceIn(0, 255)
                Color.rgb(boosted, boosted, boosted)
            }
        }
    }
}
