package com.scanni.app.core.geometry

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** A 2D point/vector. Coordinates are normalized to [0, 1] unless stated otherwise. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    fun distanceTo(other: Vec2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun lerp(other: Vec2, t: Float) = Vec2(x + (other.x - x) * t, y + (other.y - y) * t)

    fun clamped(min: Float = 0f, max: Float = 1f) =
        Vec2(x.coerceIn(min, max), y.coerceIn(min, max))
}

/**
 * A convex quadrilateral in normalized image coordinates (y axis pointing down),
 * with corners in fixed visual order: top-left, top-right, bottom-right, bottom-left.
 */
data class Quad(
    val topLeft: Vec2,
    val topRight: Vec2,
    val bottomRight: Vec2,
    val bottomLeft: Vec2,
) {
    val corners: List<Vec2> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun withCorner(index: Int, value: Vec2): Quad = when (index) {
        0 -> copy(topLeft = value)
        1 -> copy(topRight = value)
        2 -> copy(bottomRight = value)
        3 -> copy(bottomLeft = value)
        else -> throw IllegalArgumentException("Corner index $index out of range")
    }

    /** Edge i connects corner i to corner (i + 1) % 4: 0=top, 1=right, 2=bottom, 3=left. */
    fun edgeMidpoint(index: Int): Vec2 {
        val a = corners[index]
        val b = corners[(index + 1) % 4]
        return Vec2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }

    fun edgeLength(index: Int): Float = corners[index].distanceTo(corners[(index + 1) % 4])

    /** Shoelace area. Normalized units (so 1.0 == the whole frame). */
    fun area(): Float = abs(signedArea())

    /**
     * Shoelace sum / 2 with sign: positive when corners run clockwise in screen
     * coordinates (y down) — the canonical TL/TR/BR/BL order. A negative value
     * means corner roles are mirrored/flipped.
     */
    fun signedArea(): Float {
        var sum = 0f
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            sum += a.x * b.y - b.x * a.y
        }
        return sum / 2f
    }

    /** True when all cross products along the boundary share a sign (no self-intersection). */
    fun isConvex(): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val a = corners[i]
            val b = corners[(i + 1) % 4]
            val c = corners[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(cross) < 1e-7f) continue
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (sign != s) return false
        }
        return sign != 0
    }

    fun lerp(other: Quad, t: Float) = Quad(
        topLeft.lerp(other.topLeft, t),
        topRight.lerp(other.topRight, t),
        bottomRight.lerp(other.bottomRight, t),
        bottomLeft.lerp(other.bottomLeft, t),
    )

    fun maxCornerDistance(other: Quad): Float =
        corners.zip(other.corners).maxOf { (a, b) -> a.distanceTo(b) }

    fun clamped(): Quad = Quad(
        topLeft.clamped(), topRight.clamped(), bottomRight.clamped(), bottomLeft.clamped(),
    )

    /**
     * The quad as seen after rotating the underlying image by [degrees] clockwise
     * (multiples of 90). Corner roles are remapped so ordering stays TL/TR/BR/BL.
     */
    fun rotatedClockwise(degrees: Int): Quad {
        var quad = this
        repeat(((degrees / 90) % 4 + 4) % 4) {
            fun map(p: Vec2) = Vec2(1f - p.y, p.x)
            quad = Quad(
                topLeft = map(quad.bottomLeft),
                topRight = map(quad.topLeft),
                bottomRight = map(quad.topRight),
                bottomLeft = map(quad.bottomRight),
            )
        }
        return quad
    }

    /** Serializes to a compact, locale-independent string for storage. */
    fun encode(): String = corners.joinToString(";") { "${it.x},${it.y}" }

    companion object {
        val FULL = Quad(Vec2(0f, 0f), Vec2(1f, 0f), Vec2(1f, 1f), Vec2(0f, 1f))

        fun decode(value: String?): Quad? {
            if (value.isNullOrBlank()) return null
            val points = value.split(";").mapNotNull { pair ->
                val parts = pair.split(",")
                if (parts.size != 2) return@mapNotNull null
                val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
                Vec2(x, y)
            }
            if (points.size != 4) return null
            return Quad(points[0], points[1], points[2], points[3])
        }

        /**
         * Orders four arbitrary corner points into TL/TR/BR/BL. Points are sorted by
         * angle around their centroid (clockwise in screen coordinates), then the
         * cycle is rotated so the corner nearest the top-left starts the list.
         */
        fun fromUnordered(points: List<Vec2>): Quad {
            require(points.size == 4) { "Expected 4 points, got ${points.size}" }
            val cx = points.sumOf { it.x.toDouble() }.toFloat() / 4f
            val cy = points.sumOf { it.y.toDouble() }.toFloat() / 4f
            val sorted = points.sortedBy { atan2((it.y - cy).toDouble(), (it.x - cx).toDouble()) }
            val startIndex = (0 until 4).minBy { sorted[it].x + sorted[it].y }
            val ordered = List(4) { sorted[(startIndex + it) % 4] }
            return Quad(ordered[0], ordered[1], ordered[2], ordered[3])
        }
    }
}
