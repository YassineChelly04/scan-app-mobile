package com.scanni.app.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {

    @Test
    fun `single token becomes a prefix query`() {
        assertEquals("invoice*", FtsQuery.sanitize("invoice"))
    }

    @Test
    fun `multiple tokens quote all but the last`() {
        assertEquals("\"hello\" world*", FtsQuery.sanitize("hello world"))
    }

    @Test
    fun `fts operators are stripped`() {
        assertEquals("\"foo\" bar*", FtsQuery.sanitize("foo* -\"bar\""))
        // NEAR survives only as a quoted plain term, not as an operator.
        assertEquals("\"a\" \"NEAR\" b*", FtsQuery.sanitize("a NEAR b"))
    }

    @Test
    fun `unicode text is preserved`() {
        assertEquals("فاتورة*", FtsQuery.sanitize("فاتورة"))
    }

    @Test
    fun `blank or symbol-only input yields null`() {
        assertNull(FtsQuery.sanitize(""))
        assertNull(FtsQuery.sanitize("   "))
        assertNull(FtsQuery.sanitize("*()-\""))
    }

    @Test
    fun `like pattern escapes wildcards`() {
        assertEquals("%50\\% off\\_now%", FtsQuery.likePattern("50% off_now"))
        assertEquals("%a\\\\b%", FtsQuery.likePattern("a\\b"))
    }
}
