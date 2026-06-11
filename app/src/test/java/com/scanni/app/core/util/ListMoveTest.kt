package com.scanni.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListMoveTest {

    @Test
    fun `moves an element forward`() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").move(0, 2))
    }

    @Test
    fun `moves an element backward`() {
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").move(2, 0))
    }

    @Test
    fun `same index is a no-op`() {
        val list = listOf("a", "b", "c")
        assertSame(list, list.move(1, 1))
    }

    @Test
    fun `out of range indices are a no-op`() {
        val list = listOf("a", "b", "c")
        assertSame(list, list.move(-1, 2))
        assertSame(list, list.move(0, 3))
    }
}
