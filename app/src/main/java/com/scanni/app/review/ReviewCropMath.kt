package com.scanni.app.review

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class CropMapping(
    val sourcePoints: FloatArray,
    val destinationPoints: FloatArray,
    val outputWidth: Int,
    val outputHeight: Int
)

object ReviewCropMath {
    fun buildCropMapping(
        imageWidth: Int,
        imageHeight: Int,
        corners: List<Float>
    ): CropMapping {
        val normalized = requireNormalizedCorners(corners)
        val sourcePoints = FloatArray(normalized.size)
        val maxX = (imageWidth - 1).coerceAtLeast(1).toFloat()
        val maxY = (imageHeight - 1).coerceAtLeast(1).toFloat()

        for (index in normalized.indices step 2) {
            sourcePoints[index] = normalized[index] * maxX
            sourcePoints[index + 1] = normalized[index + 1] * maxY
        }

        val topWidth = distance(sourcePoints[0], sourcePoints[1], sourcePoints[2], sourcePoints[3])
        val bottomWidth = distance(sourcePoints[6], sourcePoints[7], sourcePoints[4], sourcePoints[5])
        val leftHeight = distance(sourcePoints[0], sourcePoints[1], sourcePoints[6], sourcePoints[7])
        val rightHeight = distance(sourcePoints[2], sourcePoints[3], sourcePoints[4], sourcePoints[5])
        val outputWidth = max(1f, max(topWidth, bottomWidth)).roundToInt()
        val outputHeight = max(1f, max(leftHeight, rightHeight)).roundToInt()

        return CropMapping(
            sourcePoints = sourcePoints,
            destinationPoints = floatArrayOf(
                0f, 0f,
                (outputWidth - 1).coerceAtLeast(0).toFloat(), 0f,
                (outputWidth - 1).coerceAtLeast(0).toFloat(), (outputHeight - 1).coerceAtLeast(0).toFloat(),
                0f, (outputHeight - 1).coerceAtLeast(0).toFloat()
            ),
            outputWidth = outputWidth,
            outputHeight = outputHeight
        )
    }

    fun buildHomography(from: FloatArray, to: FloatArray): FloatArray {
        require(from.size == 8 && to.size == 8) { "Homography requires 4 point pairs" }
        val system = Array(8) { DoubleArray(9) }

        for (pointIndex in 0 until 4) {
            val fromX = from[pointIndex * 2].toDouble()
            val fromY = from[pointIndex * 2 + 1].toDouble()
            val toX = to[pointIndex * 2].toDouble()
            val toY = to[pointIndex * 2 + 1].toDouble()
            val row = pointIndex * 2

            system[row][0] = fromX
            system[row][1] = fromY
            system[row][2] = 1.0
            system[row][6] = -toX * fromX
            system[row][7] = -toX * fromY
            system[row][8] = toX

            system[row + 1][3] = fromX
            system[row + 1][4] = fromY
            system[row + 1][5] = 1.0
            system[row + 1][6] = -toY * fromX
            system[row + 1][7] = -toY * fromY
            system[row + 1][8] = toY
        }

        for (pivotIndex in 0 until 8) {
            var pivotRow = pivotIndex
            for (candidate in pivotIndex + 1 until 8) {
                if (abs(system[candidate][pivotIndex]) > abs(system[pivotRow][pivotIndex])) {
                    pivotRow = candidate
                }
            }
            require(abs(system[pivotRow][pivotIndex]) > 1e-8) { "Unable to map crop corners" }

            val swap = system[pivotIndex]
            system[pivotIndex] = system[pivotRow]
            system[pivotRow] = swap

            val pivot = system[pivotIndex][pivotIndex]
            for (column in pivotIndex until 9) {
                system[pivotIndex][column] /= pivot
            }

            for (row in 0 until 8) {
                if (row == pivotIndex) {
                    continue
                }
                val factor = system[row][pivotIndex]
                if (factor == 0.0) {
                    continue
                }
                for (column in pivotIndex until 9) {
                    system[row][column] -= factor * system[pivotIndex][column]
                }
            }
        }

        return floatArrayOf(
            system[0][8].toFloat(),
            system[1][8].toFloat(),
            system[2][8].toFloat(),
            system[3][8].toFloat(),
            system[4][8].toFloat(),
            system[5][8].toFloat(),
            system[6][8].toFloat(),
            system[7][8].toFloat(),
            1f
        )
    }

    private fun requireNormalizedCorners(corners: List<Float>): FloatArray {
        require(corners.size == 8) { "Expected 4 crop corners" }
        require(!hasDuplicatePoints(corners)) { "Crop corners must be distinct" }
        require(polygonArea(corners) > 0.0001f) { "Crop area is too small" }

        return corners.toFloatArray().also { normalized ->
            normalized.forEach { value ->
                require(value in 0f..1f) { "Crop corners must be normalized" }
            }
        }
    }

    private fun hasDuplicatePoints(corners: List<Float>): Boolean {
        for (index in corners.indices step 2) {
            for (otherIndex in index + 2 until corners.size step 2) {
                if (distance(corners[index], corners[index + 1], corners[otherIndex], corners[otherIndex + 1]) < 0.01f) {
                    return true
                }
            }
        }
        return false
    }

    private fun polygonArea(corners: List<Float>): Float {
        var area = 0f
        for (index in corners.indices step 2) {
            val nextIndex = (index + 2) % corners.size
            area += corners[index] * corners[nextIndex + 1] - corners[nextIndex] * corners[index + 1]
        }
        return abs(area) / 2f
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot(x2 - x1, y2 - y1)
}
