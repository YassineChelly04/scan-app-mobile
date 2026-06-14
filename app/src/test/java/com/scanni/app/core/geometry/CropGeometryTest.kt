package com.scanni.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {

    private val square = Quad(
        Vec2(0.2f, 0.2f),
        Vec2(0.8f, 0.2f),
        Vec2(0.8f, 0.8f),
        Vec2(0.2f, 0.8f),
    )

    @Test
    fun `hitTest finds the nearest corner`() {
        val handle = CropGeometry.hitTest(square, Vec2(0.21f, 0.19f), touchRadius = 0.05f)
        assertEquals(0, handle)
        assertTrue(CropGeometry.isCornerHandle(handle))
    }

    @Test
    fun `hitTest finds an edge midpoint when no corner is near`() {
        val handle = CropGeometry.hitTest(square, Vec2(0.5f, 0.21f), touchRadius = 0.05f)
        assertEquals(4, handle)
        assertTrue(CropGeometry.isEdgeHandle(handle))
    }

    @Test
    fun `hitTest misses when nothing is in range`() {
        val handle = CropGeometry.hitTest(square, Vec2(0.5f, 0.5f), touchRadius = 0.05f)
        assertEquals(CropGeometry.HANDLE_NONE, handle)
    }

    @Test
    fun `corner drag moves the corner`() {
        val result = CropGeometry.dragCorner(square, 0, Vec2(0.1f, 0.1f))
        assertEquals(Vec2(0.1f, 0.1f), result.topLeft)
    }

    @Test
    fun `corner drag clamps to image bounds`() {
        val result = CropGeometry.dragCorner(square, 0, Vec2(-0.4f, -0.4f))
        assertEquals(Vec2(0f, 0f), result.topLeft)
    }

    @Test
    fun `corner drag rejects shapes that stop being convex`() {
        // Dragging the top-left corner past the bottom-right corner is rejected.
        val result = CropGeometry.dragCorner(square, 0, Vec2(0.95f, 0.95f))
        assertEquals(square, result)
    }

    @Test
    fun `edge drag translates both corners`() {
        val result = CropGeometry.dragEdge(square, 0, Vec2(0f, -0.1f))
        assertEquals(0.1f, result.topLeft.y, 1e-5f)
        assertEquals(0.1f, result.topRight.y, 1e-5f)
        assertEquals(0.2f, result.topLeft.x, 1e-5f)
    }

    @Test
    fun `edge drag stops at image bounds`() {
        val result = CropGeometry.dragEdge(square, 0, Vec2(0f, -0.6f))
        assertEquals(0f, result.topLeft.y, 1e-5f)
        assertEquals(0f, result.topRight.y, 1e-5f)
    }

    @Test
    fun `edge drag cannot collapse the quad`() {
        // Push the top edge down through the bottom edge.
        val result = CropGeometry.dragEdge(square, 0, Vec2(0f, 0.7f))
        assertEquals(square, result)
    }

    @Test
    fun `snapToCorner snaps when within radius`() {
        val snapped = CropGeometry.snapToCorner(
            target = Vec2(0.205f, 0.205f),
            reference = Vec2(0.2f, 0.2f),
            radius = 0.03f,
        )
        assertEquals(Vec2(0.2f, 0.2f), snapped)
    }

    @Test
    fun `snapToCorner leaves the target alone when out of range`() {
        val target = Vec2(0.5f, 0.5f)
        val snapped = CropGeometry.snapToCorner(target, Vec2(0.2f, 0.2f), radius = 0.03f)
        assertEquals(target, snapped)
    }

    @Test
    fun `snapToCorner with no reference returns the target`() {
        val target = Vec2(0.5f, 0.5f)
        assertEquals(target, CropGeometry.snapToCorner(target, reference = null, radius = 0.03f))
    }
}
