package com.scanni.app.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.scanni.app.core.geometry.Quad
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Streams live document detections to the scanner UI. Works on the Y (luma)
 * plane only — no color conversion — and rotates the frame into display
 * orientation before detecting, so emitted quads map 1:1 onto the preview.
 */
class CameraFrameAnalyzer(
    private val detector: OpenCvDocumentDetector,
    private val isEnabled: () -> Boolean,
    private val minAreaFraction: () -> Float,
    /** Detection result plus the display-oriented frame size (for overlay mapping). */
    private val onResult: (Quad?, Int, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val rotated = image.imageInfo.rotationDegrees == 90 || image.imageInfo.rotationDegrees == 270
            val orientedWidth = if (rotated) image.height else image.width
            val orientedHeight = if (rotated) image.width else image.height
            if (!isEnabled() || !VisionRuntime.isAvailable) {
                onResult(null, orientedWidth, orientedHeight)
                return
            }
            val quad = detectIn(image)
            onResult(quad, orientedWidth, orientedHeight)
        } catch (_: Throwable) {
            onResult(null, 0, 0)
        } finally {
            image.close()
        }
    }

    private fun detectIn(image: ImageProxy): Quad? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val wrapped = Mat(
            image.height,
            image.width,
            CvType.CV_8UC1,
            buffer,
            plane.rowStride.toLong(),
        )
        val oriented = Mat()
        try {
            when (image.imageInfo.rotationDegrees) {
                90 -> Core.rotate(wrapped, oriented, Core.ROTATE_90_CLOCKWISE)
                180 -> Core.rotate(wrapped, oriented, Core.ROTATE_180)
                270 -> Core.rotate(wrapped, oriented, Core.ROTATE_90_COUNTERCLOCKWISE)
                else -> wrapped.copyTo(oriented)
            }
            return detector.detect(oriented, minAreaFraction())
        } finally {
            oriented.release()
            wrapped.release()
        }
    }
}
