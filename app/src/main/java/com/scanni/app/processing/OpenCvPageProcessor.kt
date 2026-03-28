package com.scanni.app.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.scanni.app.review.ReviewCropMath
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
        val warped = cropAndWarp(source, corners)
        val processed = Bitmap.createBitmap(warped.width, warped.height, Bitmap.Config.ARGB_8888)

        try {
            for (x in 0 until warped.width) {
                for (y in 0 until warped.height) {
                    processed.setPixel(x, y, transformPixel(warped.getPixel(x, y), mode))
                }
            }

            outputFile.outputStream().use { stream ->
                processed.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            return outputFile.absolutePath
        } finally {
            source.recycle()
            warped.recycle()
            processed.recycle()
        }
    }

    private fun cropAndWarp(source: Bitmap, corners: List<Float>): Bitmap {
        val mapping = ReviewCropMath.buildCropMapping(
            imageWidth = source.width,
            imageHeight = source.height,
            corners = corners
        )
        val transform = ReviewCropMath.buildHomography(
            mapping.destinationPoints,
            mapping.sourcePoints
        )

        return Bitmap.createBitmap(
            mapping.outputWidth,
            mapping.outputHeight,
            Bitmap.Config.ARGB_8888
        ).also { destination ->
            for (x in 0 until destination.width) {
                for (y in 0 until destination.height) {
                    val denominator = transform[6] * x + transform[7] * y + transform[8]
                    if (denominator == 0f) {
                        destination.setPixel(x, y, Color.WHITE)
                        continue
                    }

                    val sourceX = ((transform[0] * x) + (transform[1] * y) + transform[2]) / denominator
                    val sourceY = ((transform[3] * x) + (transform[4] * y) + transform[5]) / denominator
                    val clampedX = sourceX.toInt().coerceIn(0, source.width - 1)
                    val clampedY = sourceY.toInt().coerceIn(0, source.height - 1)
                    destination.setPixel(x, y, source.getPixel(clampedX, clampedY))
                }
            }
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
