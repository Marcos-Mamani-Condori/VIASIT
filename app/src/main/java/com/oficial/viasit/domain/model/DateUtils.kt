package com.oficial.viasit.domain.model

import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun parsePocketBaseDate(dateStr: String): Instant {
    return try {
        OffsetDateTime.parse(dateStr).toInstant()
    } catch (e1: Exception) {
        try {
            Instant.parse(dateStr)
        } catch (e2: Exception) {
            try {
                val normalized = dateStr.replace(" ", "T").replace(".000Z", "Z").replace(".00Z", "Z")
                Instant.parse(normalized)
            } catch (e3: Exception) {
                LocalDateTime.parse(
                    dateStr.substringBefore(".").replace(" ", "T"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                ).atZone(ZoneId.of("UTC")).toInstant()
            }
        }
    }
}
