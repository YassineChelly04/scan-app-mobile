package com.scanni.app.vision

import android.graphics.Bitmap
import com.scanni.app.core.geometry.Quad
import com.scanni.app.core.geometry.Vec2
import com.scanni.app.core.image.ImageIo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Finds the dominant document quadrilateral in a frame.
 *
 * Two binarization strategies run over a downscaled grayscale frame — Canny edges
 * (strong outlines) and adaptive threshold (low-contrast paper on similar
 * backgrounds). Contours from both are reduced to convex quads with an epsilon
 * ladder, then scored by area x squareness; the best quad wins.
 */
class OpenCvDocumentDetector {

    /**
     * @param gray single-channel frame, already display-oriented
     * @return quad in normalized [0,1] coordinates of the frame, or null
     */
    fun detect(gray: Mat, minAreaFraction: Float): Quad? {
        if (!VisionRuntime.isAvailable || gray.empty()) return null

        val scale = WORK_SIZE.toFloat() / max(gray.cols(), gray.rows())
        val work = Mat()
        if (scale < 1f) {
            Imgproc.resize(
                gray,
                work,
                Size(gray.cols() * scale.toDouble(), gray.rows() * scale.toDouble()),
                0.0,
                0.0,
                Imgproc.INTER_AREA,
            )
        } else {
            gray.copyTo(work)
        }

        val blurred = Mat()
        Imgproc.GaussianBlur(work, blurred, Size(5.0, 5.0), 0.0)

        val frameArea = (work.cols() * work.rows()).toFloat()
        var best: ScoredQuad? = null
        val binary = Mat()
        try {
            // Strategy 1: Canny edges, dilated so broken outlines connect.
            Imgproc.Canny(blurred, binary, 50.0, 150.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(binary, binary, kernel, Point(-1.0, -1.0), 2)
            kernel.release()
            best = bestQuad(binary, frameArea, minAreaFraction, best)

            // Strategy 2: adaptive threshold for soft/low-contrast boundaries.
            Imgproc.adaptiveThreshold(
                blurred,
                binary,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                ADAPTIVE_BLOCK,
                ADAPTIVE_C,
            )
            best = bestQuad(binary, frameArea, minAreaFraction, best)
        } finally {
            binary.release()
            blurred.release()
        }

        val result = best?.let { scored ->
            val w = work.cols().toFloat()
            val h = work.rows().toFloat()
            Quad.fromUnordered(scored.points.map { Vec2(it.x.toFloat() / w, it.y.toFloat() / h) })
                .clamped()
        }
        work.release()
        return result?.takeIf { it.isConvex() }
    }

    /**
     * Decodes an EXIF-oriented bitmap from [path] and detects the document quad —
     * used by the crop editor to (re)find the paper on demand. Returns null if the
     * image can't be read or no credible document is found.
     */
    suspend fun detectFile(path: String, minAreaFraction: Float = CROP_ASSIST_MIN_AREA): Quad? =
        withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = ImageIo.decodeOriented(path, FILE_DETECT_SIZE)
                try {
                    detect(bitmap, minAreaFraction)
                } finally {
                    bitmap.recycle()
                }
            }.getOrNull()
        }

    /** Detection on a captured/imported image (e.g. for initial crop suggestions). */
    fun detect(bitmap: Bitmap, minAreaFraction: Float): Quad? {
        if (!VisionRuntime.isAvailable) return null
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            detect(gray, minAreaFraction)
        } finally {
            rgba.release()
            gray.release()
        }
    }

    private class ScoredQuad(val points: List<Point>, val score: Float)

    private fun bestQuad(
        binary: Mat,
        frameArea: Float,
        minAreaFraction: Float,
        currentBest: ScoredQuad?,
    ): ScoredQuad? {
        var best = currentBest
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            binary,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE,
        )
        hierarchy.release()

        val candidates = contours
            .sortedByDescending { Imgproc.contourArea(it) }
            .take(MAX_CONTOURS)

        for (contour in candidates) {
            if (Imgproc.contourArea(contour) < frameArea * minAreaFraction) break
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            for (epsilon in EPSILON_LADDER) {
                val approx2f = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx2f, epsilon * peri, true)
                val points = approx2f.toArray().toList()
                approx2f.release()
                if (points.size != 4) continue

                val quadContour = MatOfPoint(*points.toTypedArray())
                val convex = Imgproc.isContourConvex(quadContour)
                val area = Imgproc.contourArea(quadContour).toFloat()
                quadContour.release()
                if (!convex || area < frameArea * minAreaFraction) continue

                val squareness = squareness(points)
                if (squareness <= 0f) continue
                val score = (area / frameArea) * squareness
                if (best == null || score > best.score) {
                    best = ScoredQuad(points, score)
                }
                break
            }
            contour2f.release()
        }
        contours.forEach { it.release() }
        return best
    }

    /**
     * 1.0 for perfect right angles, falling toward 0 as corners skew;
     * <= 0 when any corner is too far from 90° to be a credible page.
     */
    private fun squareness(points: List<Point>): Float {
        var worst = 0.0
        for (i in points.indices) {
            val a = points[(i + 3) % 4]
            val b = points[i]
            val c = points[(i + 1) % 4]
            val v1x = a.x - b.x
            val v1y = a.y - b.y
            val v2x = c.x - b.x
            val v2y = c.y - b.y
            val len1 = sqrt(v1x * v1x + v1y * v1y)
            val len2 = sqrt(v2x * v2x + v2y * v2y)
            if (len1 < 1.0 || len2 < 1.0) return 0f
            val cos = abs((v1x * v2x + v1y * v2y) / (len1 * len2))
            worst = max(worst, cos)
        }
        if (worst > MAX_CORNER_COS) return 0f
        return (1.0 - worst).toFloat()
    }

    private companion object {
        const val WORK_SIZE = 480
        const val FILE_DETECT_SIZE = 1280
        /** Permissive area floor for on-demand crop-editor detection. */
        const val CROP_ASSIST_MIN_AREA = 0.06f
        const val MAX_CONTOURS = 12
        const val ADAPTIVE_BLOCK = 31
        const val ADAPTIVE_C = 8.0
        /** cos(90° ± ~55°) — generous to allow steep perspective. */
        const val MAX_CORNER_COS = 0.57
        val EPSILON_LADDER = floatArrayOf(0.02f, 0.035f, 0.05f)
    }
}
