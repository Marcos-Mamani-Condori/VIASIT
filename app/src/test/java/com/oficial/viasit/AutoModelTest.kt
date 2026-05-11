package com.oficial.viasit

import com.oficial.viasit.domain.model.Auto
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para el modelo Auto
 */
class AutoModelTest {

    @Test
    fun `auto creation with default values`() {
        val auto = Auto()

        assertEquals("", auto.id)
        assertEquals("", auto.placa)
        assertEquals("", auto.code)
        assertEquals(0.0, auto.lat, 0.0001)
        assertEquals(0.0, auto.lng, 0.0001)
        assertEquals(0.0, auto.angulo, 0.0001)
    }

    @Test
    fun `auto creation with custom values`() {
        val auto = Auto(
            id = "rec123",
            placa = "ABC-123",
            code = "L01",
            lat = -17.7833,
            lng = -63.1821,
            angulo = 45.0,
        )

        assertEquals("rec123", auto.id)
        assertEquals("ABC-123", auto.placa)
        assertEquals("L01", auto.code)
        assertEquals(-17.7833, auto.lat, 0.0001)
        assertEquals(-63.1821, auto.lng, 0.0001)
        assertEquals(45.0, auto.angulo, 0.0001)
    }

    @Test
    fun `auto copy creates new instance with modified values`() {
        val original = Auto(
            id = "rec123",
            placa = "ABC-123",
            lat = -17.7833,
            lng = -63.1821
        )

        val modified = original.copy(placa = "XYZ-999", lat = -17.8)

        assertEquals("ABC-123", original.placa)
        assertEquals(-17.7833, original.lat, 0.0001)
        assertEquals("XYZ-999", modified.placa)
        assertEquals(-17.8, modified.lat, 0.0001)
    }

    @Test
    fun `auto equality based on all properties`() {
        val auto1 = Auto(id = "rec1", placa = "ABC-123")
        val auto2 = Auto(id = "rec1", placa = "ABC-123")
        val auto3 = Auto(id = "rec2", placa = "ABC-123")

        assertEquals(auto1, auto2)
        assertNotEquals(auto1, auto3)
    }
}
