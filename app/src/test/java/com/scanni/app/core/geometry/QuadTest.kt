package com.scanni.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuadTest {

    private val square = Quad(
        Vec2(0.2f, 0.2f),
        Vec2(0.8f, 0.2f),
        Vec2(0.8f, 0.8f),
        Vec2(0.2f, 0.8f),
    )

    @Test
    fun `fromUnordered restores corner roles from shuffled input`() {
        val shuffled = listOf(square.bottomRight, square.topLeft, square.bottomLeft, square.topRight)
        val ordered = Quad.fromUnordered(shuffled)
        assertEquals(square, ordered)
    }

    @Test
    fun `fromUnordered handles a rotated document`() {
        // A quad tilted ~30 degrees, corners given in arbitrary order.
        val tl = Vec2(0.30f, 0.10f)
        val tr = Vec2(0.85f, 0.30f)
        val br = Vec2(0.65f, 0.85f)
        val bl = Vec2(0.10f, 0.65f)
        val ordered = Quad.fromUnordered(listOf(br, bl, tr, tl))
        assertEquals(tl, ordered.topLeft)
        assertEquals(tr, ordered.topRight)
        assertEquals(br, ordered.bottomRight)
        assertEquals(bl, ordered.bottomLeft)
    }

    @Test
    fun `area of a square`() {
        assertEquals(0.36f, square.area(), 1e-5f)
        assertEquals(1f, Quad.FULL.area(), 1e-5f)
    }

    @Test
    fun `convexity detection`() {
        assertTrue(square.isConvex())
        // Swapping two adjacent corners produces a self-intersecting bowtie.
        val bowtie = square.copy(topLeft = square.topRight, topRight = square.topLeft)
        assertFalse(bowtie.isConvex())
    }

    @Test
    fun `signed area is positive for canonical order and negative when flipped`() {
        assertTrue(square.signedArea() > 0f)
        val flipped = Quad(
            topLeft = square.bottomLeft,
            topRight = square.bottomRight,
            bottomRight = square.topRight,
            bottomLeft = square.topLeft,
        )
        assertTrue(flipped.signedArea() < 0f)
        assertEquals(square.area(), flipped.area(), 1e-5f)
    }

    @Test
    fun `lerp interpolates corners`() {
        val target = Quad(
            Vec2(0.4f, 0.4f),
            Vec2(1.0f, 0.4f),
            Vec2(1.0f, 1.0f),
            Vec2(0.4f, 1.0f),
        )
        val half = square.lerp(target, 0.5f)
        assertEquals(0.3f, half.topLeft.x, 1e-5f)
        assertEquals(0.3f, half.topLeft.y, 1e-5f)
        assertEquals(0.9f, half.bottomRight.x, 1e-5f)
    }

    @Test
    fun `maxCornerDistance is the largest corner displacement`() {
        val moved = square.withCorner(2, Vec2(0.9f, 0.8f))
        assertEquals(0.1f, square.maxCornerDistance(moved), 1e-5f)
    }

    @Test
    fun `rotatedClockwise 90 remaps corner roles`() {
        val quad = Quad(
            Vec2(0.1f, 0.2f),
            Vec2(0.9f, 0.2f),
            Vec2(0.9f, 0.7f),
            Vec2(0.1f, 0.7f),
        )
        val rotated = quad.rotatedClockwise(90)
        // Image rotated 90° CW: old bottom-left becomes the new top-left.
        assertEquals(Vec2(1f - 0.7f, 0.1f), rotated.topLeft)
        assertEquals(Vec2(1f - 0.2f, 0.1f), rotated.topRight)
        assertTrue(rotated.isConvex())
        // Four rotations are the identity.
        assertEquals(quad, quad.rotatedClockwise(360))
    }

    @Test
    fun `encode decode roundtrip`() {
        val decoded = Quad.decode(square.encode())
        assertEquals(square, decoded)
    }

    @Test
    fun `decode rejects malformed input`() {
        assertNull(Quad.decode(null))
        assertNull(Quad.decode(""))
        assertNull(Quad.decode("1,2;3,4"))
        assertNull(Quad.decode("a,b;c,d;e,f;g,h"))
    }

    @Test
    fun `edge midpoints`() {
        assertEquals(Vec2(0.5f, 0.2f), square.edgeMidpoint(0))
        assertEquals(Vec2(0.8f, 0.5f), square.edgeMidpoint(1))
        assertEquals(Vec2(0.5f, 0.8f), square.edgeMidpoint(2))
        assertEquals(Vec2(0.2f, 0.5f), square.edgeMidpoint(3))
    }
}
