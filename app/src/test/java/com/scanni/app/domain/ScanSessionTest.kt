package com.scanni.app.domain

import com.scanni.app.core.geometry.Quad
import com.scanni.app.domain.model.CapturedPage
import com.scanni.app.domain.model.ScanFilter
import com.scanni.app.domain.model.ScanMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanSessionTest {

    private fun page(id: String) = CapturedPage(
        id = id,
        originalPath = "/tmp/$id.jpg",
        widthPx = 100,
        heightPx = 100,
        detectedQuad = null,
        quad = Quad.FULL,
        rotationDeg = 0,
        filter = ScanFilter.AUTO,
    )

    @Test
    fun `add and clear`() {
        val session = ScanSession()
        session.add(page("a"))
        session.add(page("b"))
        assertEquals(listOf("a", "b"), session.pages.value.map { it.id })
        session.clear()
        assertEquals(emptyList<CapturedPage>(), session.pages.value)
    }

    @Test
    fun `remove returns the removed page`() {
        val session = ScanSession()
        session.add(page("a"))
        session.add(page("b"))
        val removed = session.remove("a")
        assertEquals("a", removed?.id)
        assertEquals(listOf("b"), session.pages.value.map { it.id })
        assertNull(session.remove("missing"))
    }

    @Test
    fun `update transforms one page`() {
        val session = ScanSession()
        session.add(page("a"))
        session.update("a") { it.copy(rotationDeg = 90) }
        assertEquals(90, session.pages.value.first().rotationDeg)
    }

    @Test
    fun `move reorders pages`() {
        val session = ScanSession()
        session.add(page("a"))
        session.add(page("b"))
        session.add(page("c"))
        session.move(2, 0)
        assertEquals(listOf("c", "a", "b"), session.pages.value.map { it.id })
    }

    @Test
    fun `mode defaults to document and can change`() {
        val session = ScanSession()
        assertEquals(ScanMode.DOCUMENT, session.mode.value)
        session.setMode(ScanMode.WHITEBOARD)
        assertEquals(ScanMode.WHITEBOARD, session.mode.value)
    }
}
