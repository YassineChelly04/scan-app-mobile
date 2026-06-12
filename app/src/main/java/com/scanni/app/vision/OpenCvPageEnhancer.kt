package com.scanni.app.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.image.ImageIo
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.processing.PageProcessor
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc

/**
 * Renders final page images: perspective-corrects the detected quad, applies the
 * chosen enhancement filter and the user's rotation. Falls back to simple
 * Android ColorMatrix filters when the OpenCV native library is unavailable.
 */
class OpenCvPageEnhancer : PageProcessor {

    override suspend fun render(
        originalPath: String,
        quad: Quad?,
        rotationDeg: Int,
        filter: ScanFilter,
        maxDimension: Int,
    ): Bitmap = withContext(Dispatchers.Default) {
        val source = ImageIo.decodeOriented(originalPath, maxDimension)
        if (!VisionRuntime.isAvailable) {
            return@withContext renderFallback(source, rotationDeg, filter)
        }

        val src = Mat()
        Utils.bitmapToMat(source, src)
        source.recycle()

        var current = src
        if (quad != null && quad != Quad.FULL) {
            current = current.replacedBy { warp(it, quad) }
        }
        current = current.replacedBy { applyFilter(it, filter) }
        when (((rotationDeg % 360) + 360) % 360) {
            90 -> current = current.replacedBy { m -> Mat().also { Core.rotate(m, it, Core.ROTATE_90_CLOCKWISE) } }
            180 -> current = current.replacedBy { m -> Mat().also { Core.rotate(m, it, Core.ROTATE_180) } }
            270 -> current = current.replacedBy { m -> Mat().also { Core.rotate(m, it, Core.ROTATE_90_COUNTERCLOCKWISE) } }
        }

        val output = Bitmap.createBitmap(current.cols(), current.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(current, output)
        current.release()
        output
    }

    /** Applies [transform] and releases the receiver when a new Mat was produced. */
    private inline fun Mat.replacedBy(transform: (Mat) -> Mat): Mat {
        val result = transform(this)
        if (result !== this) release()
        return result
    }

    // --- Perspective correction ---

    private fun warp(src: Mat, quad: Quad): Mat {
        val w = src.cols().toFloat()
        val h = src.rows().toFloat()
        val tl = Point((quad.topLeft.x * w).toDouble(), (quad.topLeft.y * h).toDouble())
        val tr = Point((quad.topRight.x * w).toDouble(), (quad.topRight.y * h).toDouble())
        val br = Point((quad.bottomRight.x * w).toDouble(), (quad.bottomRight.y * h).toDouble())
        val bl = Point((quad.bottomLeft.x * w).toDouble(), (quad.bottomLeft.y * h).toDouble())

        val outW = max(distance(tl, tr), distance(bl, br)).roundToInt().coerceAtLeast(16)
        val outH = max(distance(tl, bl), distance(tr, br)).roundToInt().coerceAtLeast(16)

        val srcPoints = MatOfPoint2f(tl, tr, br, bl)
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(outW - 1.0, 0.0),
            Point(outW - 1.0, outH - 1.0),
            Point(0.0, outH - 1.0),
        )
        val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(
            src,
            warped,
            transform,
            Size(outW.toDouble(), outH.toDouble()),
            Imgproc.INTER_LINEAR,
        )
        srcPoints.release()
        dstPoints.release()
        transform.release()
        return warped
    }

    private fun distance(a: Point, b: Point): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy).toFloat()
    }

    // --- Filters (input/output RGBA) ---

    private fun applyFilter(src: Mat, filter: ScanFilter): Mat = when (filter) {
        ScanFilter.ORIGINAL -> src
        ScanFilter.AUTO -> magicColor(src)
        ScanFilter.GRAYSCALE -> grayscale(src)
        ScanFilter.BLACK_WHITE -> blackAndWhite(src)
        ScanFilter.WHITEBOARD -> whiteboard(src)
        ScanFilter.PHOTO -> photo(src)
    }

    /** Lens-style "magic": flatten illumination so paper goes white, then gentle contrast + sharpen. */
    private fun magicColor(src: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        val flattened = flattenIllumination(rgb, strength = 235.0)
        rgb.release()
        flattened.convertTo(flattened, -1, 1.05, -8.0)
        val sharpened = unsharp(flattened, amount = 0.35)
        flattened.release()
        val out = Mat()
        Imgproc.cvtColor(sharpened, out, Imgproc.COLOR_RGB2RGBA)
        sharpened.release()
        return out
    }

    private fun grayscale(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        val clahe: CLAHE = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        val equalized = Mat()
        clahe.apply(gray, equalized)
        gray.release()
        val out = Mat()
        Imgproc.cvtColor(equalized, out, Imgproc.COLOR_GRAY2RGBA)
        equalized.release()
        return out
    }

    private fun blackAndWhite(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 0.0)
        val binary = Mat()
        val block = oddAtLeast(max(gray.cols(), gray.rows()) / 60, 25)
        Imgproc.adaptiveThreshold(
            gray,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            block,
            12.0,
        )
        gray.release()
        val out = Mat()
        Imgproc.cvtColor(binary, out, Imgproc.COLOR_GRAY2RGBA)
        binary.release()
        return out
    }

    private fun whiteboard(src: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        val flattened = flattenIllumination(rgb, strength = 248.0)
        rgb.release()

        // Boost marker colors that flattening washed out.
        val hsv = Mat()
        Imgproc.cvtColor(flattened, hsv, Imgproc.COLOR_RGB2HSV)
        flattened.release()
        val channels = ArrayList<Mat>(3)
        Core.split(hsv, channels)
        channels[1].convertTo(channels[1], -1, 1.45, 0.0)
        Core.merge(channels, hsv)
        channels.forEach { it.release() }
        val boosted = Mat()
        Imgproc.cvtColor(hsv, boosted, Imgproc.COLOR_HSV2RGB)
        hsv.release()

        val sharpened = unsharp(boosted, amount = 0.3)
        boosted.release()
        val out = Mat()
        Imgproc.cvtColor(sharpened, out, Imgproc.COLOR_RGB2RGBA)
        sharpened.release()
        return out
    }

    private fun photo(src: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        rgb.convertTo(rgb, -1, 1.07, -6.0)
        val sharpened = unsharp(rgb, amount = 0.25)
        rgb.release()
        val out = Mat()
        Imgproc.cvtColor(sharpened, out, Imgproc.COLOR_RGB2RGBA)
        sharpened.release()
        return out
    }

    /**
     * Divides each channel by a heavily blurred copy of itself (estimated on a
     * downscaled image for speed), pushing the paper background toward white
     * while keeping ink. [strength] is the target background level (0..255).
     */
    private fun flattenIllumination(rgb: Mat, strength: Double): Mat {
        val channels = ArrayList<Mat>(3)
        Core.split(rgb, channels)
        val result = ArrayList<Mat>(3)
        for (channel in channels) {
            val background = estimateBackground(channel)
            val divided = Mat()
            Core.divide(channel, background, divided, strength)
            background.release()
            channel.release()
            result.add(divided)
        }
        val merged = Mat()
        Core.merge(result, merged)
        result.forEach { it.release() }
        return merged
    }

    private fun estimateBackground(channel: Mat): Mat {
        val small = Mat()
        val scale = 0.25
        Imgproc.resize(channel, small, Size(), scale, scale, Imgproc.INTER_AREA)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0))
        Imgproc.morphologyEx(small, small, Imgproc.MORPH_CLOSE, kernel)
        kernel.release()
        Imgproc.GaussianBlur(small, small, Size(11.0, 11.0), 0.0)
        val background = Mat()
        Imgproc.resize(small, background, Size(channel.cols().toDouble(), channel.rows().toDouble()))
        small.release()
        // Avoid division by ~0 in deep shadows.
        val floor = Mat(background.size(), background.type(), org.opencv.core.Scalar(16.0))
        Core.max(background, floor, background)
        floor.release()
        return background
    }

    private fun unsharp(src: Mat, amount: Double): Mat {
        val blurred = Mat()
        Imgproc.GaussianBlur(src, blurred, Size(0.0, 0.0), 2.5)
        val sharpened = Mat()
        Core.addWeighted(src, 1.0 + amount, blurred, -amount, 0.0, sharpened)
        blurred.release()
        return sharpened
    }

    private fun oddAtLeast(value: Int, minimum: Int): Int {
        val v = max(value, minimum)
        return if (v % 2 == 1) v else v + 1
    }

    // --- Fallback without OpenCV ---

    private fun renderFallback(source: Bitmap, rotationDeg: Int, filter: ScanFilter): Bitmap {
        val rotated = ImageIo.rotate(source, rotationDeg)
        val matrix = when (filter) {
            ScanFilter.GRAYSCALE, ScanFilter.BLACK_WHITE -> ColorMatrix().apply {
                setSaturation(0f)
                if (filter == ScanFilter.BLACK_WHITE) postConcat(contrastMatrix(1.6f))
            }
            ScanFilter.AUTO, ScanFilter.WHITEBOARD, ScanFilter.PHOTO -> contrastColorMatrix(1.12f)
            ScanFilter.ORIGINAL -> return rotated
        }
        val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        rotated.recycle()
        return output
    }

    private fun contrastColorMatrix(contrast: Float): ColorMatrix = contrastMatrix(contrast)

    private fun contrastMatrix(contrast: Float): ColorMatrix {
        val translate = (1f - contrast) * 128f
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
