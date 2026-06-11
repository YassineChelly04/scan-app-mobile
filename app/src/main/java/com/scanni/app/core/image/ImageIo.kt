package com.scanni.app.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Bitmap decoding/encoding helpers. All decodes respect EXIF orientation. */
object ImageIo {

    /** Decodes [path] with the longest side capped near [maxDimension], EXIF rotation applied. */
    fun decodeOriented(path: String, maxDimension: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot decode $path" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), maxDimension)
        }
        val raw = BitmapFactory.decodeFile(path, options)
            ?: throw IllegalStateException("Cannot decode $path")
        val oriented = applyExifOrientation(raw, path)
        return scaleDownTo(oriented, maxDimension)
    }

    /** Width/height of the image at [path] after EXIF orientation. */
    fun orientedSize(path: String): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val rotation = exifRotationDegrees(path)
        return if (rotation == 90 || rotation == 270) {
            bounds.outHeight to bounds.outWidth
        } else {
            bounds.outWidth to bounds.outHeight
        }
    }

    fun saveJpeg(bitmap: Bitmap, file: File, quality: Int) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }

    /** Returns a copy scaled so the longest side is at most [maxDimension] (or the input itself). */
    fun scaleDownTo(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun sampleSize(longestSide: Int, maxDimension: Int): Int {
        var sample = 1
        var side = longestSide
        while (side / 2 >= maxDimension) {
            side /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifOrientation(bitmap: Bitmap, path: String): Bitmap {
        val rotation = exifRotationDegrees(path)
        return if (rotation == 0) bitmap else rotate(bitmap, rotation)
    }

    private fun exifRotationDegrees(path: String): Int = try {
        when (
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}
