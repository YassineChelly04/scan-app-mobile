package com.scanni.app.domain.processing

import android.graphics.Bitmap
import com.scanni.app.core.geometry.Quad
import com.scanni.app.domain.model.ScanFilter

/** Renders a captured original into its final page image: perspective crop, rotation, filter. */
interface PageProcessor {

    /**
     * @param originalPath path to the captured JPEG (EXIF orientation respected)
     * @param quad perspective crop in normalized coordinates of the oriented original, or null for full frame
     * @param rotationDeg extra user rotation (clockwise, multiple of 90) applied after cropping
     * @param maxDimension cap for the longest output side
     */
    suspend fun render(
        originalPath: String,
        quad: Quad?,
        rotationDeg: Int,
        filter: ScanFilter,
        maxDimension: Int,
    ): Bitmap

    companion object {
        const val FULL_SIZE = 2600
        const val PREVIEW_SIZE = 1400
        const val THUMB_SIZE = 512
        const val FILTER_CHIP_SIZE = 220
    }
}
