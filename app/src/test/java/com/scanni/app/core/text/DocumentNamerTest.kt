package com.scanni.app.core.text

import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentNamerTest {

    @Test
    fun `formats a deterministic default name`() {
        // 2026-06-11T14:05:00Z
        val namer = DocumentNamer(clock = { 1_781_186_700_000L })
        val name = namer.defaultName(prefix = "Scan", locale = Locale.US, zone = ZoneOffset.UTC)
        assertEquals("Scan Jun 11, 14:05", name)
    }

    @Test
    fun `prefix is localizable`() {
        val namer = DocumentNamer(clock = { 1_781_186_700_000L })
        val name = namer.defaultName(prefix = "مسح", locale = Locale.US, zone = ZoneOffset.UTC)
        assertEquals("مسح Jun 11, 14:05", name)
    }
}
