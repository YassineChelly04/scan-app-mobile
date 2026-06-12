package com.scanni.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuadStabilizerTest {

    private val config = QuadStabilizer.Config(
        smoothing = 0.5f,
        steadyMaxDelta = 0.01f,
        steadyDurationMs = 1000,
        lostGraceMs = 300,
    )

    private val quad = Quad(
        Vec2(0.1f, 0.1f),
        Vec2(0.9f, 0.1f),
        Vec2(0.9f, 0.9f),
        Vec2(0.1f, 0.9f),
    )

    @Test
    fun `searching when nothing detected`() {
        val stabilizer = QuadStabilizer(config)
        assertEquals(QuadStabilizer.State.Searching, stabilizer.onFrame(null, 0))
    }

    @Test
    fun `locks after the steady duration`() {
        val stabilizer = QuadStabilizer(config)
        var time = 0L
        var state: QuadStabilizer.State = QuadStabilizer.State.Searching
        while (time <= 1200) {
            state = stabilizer.onFrame(quad, time)
            time += 50
        }
        assertTrue("expected Locked, got $state", state is QuadStabilizer.State.Locked)
    }

    @Test
    fun `progress grows while steady`() {
        val stabilizer = QuadStabilizer(config)
        stabilizer.onFrame(quad, 0)
        val state = stabilizer.onFrame(quad, 500)
        assertTrue(state is QuadStabilizer.State.Tracking)
        val progress = (state as QuadStabilizer.State.Tracking).steadyProgress
        assertTrue("progress $progress should be ~0.5", progress in 0.4f..0.6f)
    }

    @Test
    fun `movement resets the steady timer`() {
        val stabilizer = QuadStabilizer(config)
        stabilizer.onFrame(quad, 0)
        stabilizer.onFrame(quad, 800)
        // Jump the quad far away — steadiness restarts.
        val moved = quad.lerp(Quad.FULL, 0.9f)
        stabilizer.onFrame(moved, 850)
        val state = stabilizer.onFrame(moved, 900)
        assertTrue(state is QuadStabilizer.State.Tracking)
        val progress = (state as QuadStabilizer.State.Tracking).steadyProgress
        assertTrue("progress $progress should be small after movement", progress < 0.2f)
    }

    @Test
    fun `short dropouts keep tracking`() {
        val stabilizer = QuadStabilizer(config)
        stabilizer.onFrame(quad, 0)
        stabilizer.onFrame(quad, 100)
        val during = stabilizer.onFrame(null, 250)
        assertTrue("grace period should keep tracking", during !is QuadStabilizer.State.Searching)
        val after = stabilizer.onFrame(null, 600)
        assertEquals(QuadStabilizer.State.Searching, after)
    }

    @Test
    fun `rearm unlocks and allows a second lock`() {
        val stabilizer = QuadStabilizer(config)
        var time = 0L
        while (time <= 1100) {
            stabilizer.onFrame(quad, time)
            time += 50
        }
        assertTrue(stabilizer.onFrame(quad, time) is QuadStabilizer.State.Locked)

        stabilizer.rearm()
        val tracking = stabilizer.onFrame(quad, time + 50)
        assertTrue("after rearm should track again, got $tracking", tracking is QuadStabilizer.State.Tracking)

        var t = time + 100
        var state: QuadStabilizer.State = tracking
        while (t <= time + 1400) {
            state = stabilizer.onFrame(quad, t)
            t += 50
        }
        assertTrue("expected second lock, got $state", state is QuadStabilizer.State.Locked)
    }

    @Test
    fun `reset clears everything`() {
        val stabilizer = QuadStabilizer(config)
        stabilizer.onFrame(quad, 0)
        stabilizer.reset()
        assertEquals(QuadStabilizer.State.Searching, stabilizer.onFrame(null, 10))
    }
}
