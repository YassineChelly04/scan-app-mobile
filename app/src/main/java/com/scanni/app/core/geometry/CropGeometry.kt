package com.scanni.app.core.geometry

/**
 * Interaction rules for the manual crop editor. All operations preserve the
 * invariants of a usable crop: corners inside the image, convex shape, and no
 * edge collapsing below [MIN_EDGE].
 */
object CropGeometry {

    const val MIN_EDGE = 0.04f

    /** Handle identifiers: 0..3 corners (TL/TR/BR/BL), 4..7 edge midpoints (top/right/bottom/left). */
    const val HANDLE_NONE = -1

    fun isCornerHandle(handle: Int) = handle in 0..3
    fun isEdgeHandle(handle: Int) = handle in 4..7

    /**
     * Finds the handle nearest [touch], or [HANDLE_NONE] when nothing is within
     * [touchRadius]. Corners win over edges when both are in range.
     */
    fun hitTest(quad: Quad, touch: Vec2, touchRadius: Float): Int {
        var best = HANDLE_NONE
        var bestDistance = touchRadius
        for (i in 0 until 4) {
            val d = quad.corners[i].distanceTo(touch)
            if (d <= bestDistance) {
                best = i
                bestDistance = d
            }
        }
        if (best != HANDLE_NONE) return best
        for (i in 0 until 4) {
            val d = quad.edgeMidpoint(i).distanceTo(touch)
            if (d <= bestDistance) {
                best = i + 4
                bestDistance = d
            }
        }
        return best
    }

    /** Applies a drag of the given handle to [target] / by [delta], rejecting invalid shapes. */
    fun drag(quad: Quad, handle: Int, target: Vec2, delta: Vec2): Quad = when {
        isCornerHandle(handle) -> dragCorner(quad, handle, target)
        isEdgeHandle(handle) -> dragEdge(quad, handle - 4, delta)
        else -> quad
    }

    /** Moves one corner to [target] (clamped to the image), keeping the quad valid. */
    fun dragCorner(quad: Quad, corner: Int, target: Vec2): Quad {
        val candidate = quad.withCorner(corner, target.clamped())
        return if (candidate.isUsable()) candidate else quad
    }

    /**
     * Moves a whole edge by [delta]: both endpoint corners translate together.
     * The delta is reduced so neither corner leaves the image.
     */
    fun dragEdge(quad: Quad, edge: Int, delta: Vec2): Quad {
        val ia = edge
        val ib = (edge + 1) % 4
        val a = quad.corners[ia]
        val b = quad.corners[ib]

        val dx = clampComponent(delta.x, a.x, b.x)
        val dy = clampComponent(delta.y, a.y, b.y)
        val d = Vec2(dx, dy)

        val candidate = quad
            .withCorner(ia, a + d)
            .withCorner(ib, b + d)
        return if (candidate.isUsable()) candidate else quad
    }

    private fun clampComponent(delta: Float, v1: Float, v2: Float): Float {
        var d = delta
        if (d > 0) d = minOf(d, 1f - v1, 1f - v2)
        if (d < 0) d = maxOf(d, -v1, -v2)
        return d
    }

    private fun Quad.isUsable(): Boolean {
        if (!isConvex()) return false
        // Corner roles must keep their clockwise order — otherwise an edge dragged
        // through the opposite edge re-forms as a mirrored quad and the warp flips.
        if (signedArea() <= 0f) return false
        for (i in 0 until 4) {
            if (edgeLength(i) < MIN_EDGE) return false
        }
        return true
    }
}
