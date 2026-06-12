package com.scanni.app.core.text

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Generates default document titles like "Scan Jun 11, 14:05". */
class DocumentNamer(private val clock: () -> Long = System::currentTimeMillis) {

    fun defaultName(
        prefix: String,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", locale)
        val stamp = formatter.format(Instant.ofEpochMilli(clock()).atZone(zone))
        return "$prefix $stamp"
    }
}
