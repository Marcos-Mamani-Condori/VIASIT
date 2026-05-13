package com.oficial.viasit

import org.junit.Test
import org.junit.Assert.assertEquals

class RegexTest {
    fun extractStringField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"([^"]*)"|"$field"\s*:\s*(\w+)|"$field"\s*:\s*\[([^\]]*)\]""").find(json)?.let { 
            it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3].replace("\"", "").trim() }
        } ?: ""

    @Test
    fun testRegex() {
        val json = """{"id": "test_id", "role": ["conductor"], "active": true, "userid": ""}"""
        assertEquals("test_id", extractStringField(json, "id"))
        assertEquals("conductor", extractStringField(json, "role"))
        assertEquals("true", extractStringField(json, "active"))
        assertEquals("", extractStringField(json, "userid"))
    }
}
