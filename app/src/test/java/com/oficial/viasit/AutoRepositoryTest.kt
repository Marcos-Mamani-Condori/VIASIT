package com.oficial.viasit

import com.oficial.viasit.model.Auto
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios adicionales para lógica de dominio
 */
class AutoRepositoryTest {

    @Test
    fun `filter autos by placa prefix`() {
        val autos = listOf(
            Auto(id = "1", placa = "ABC-123"),
            Auto(id = "2", placa = "ABC-456"),
            Auto(id = "3", placa = "XYZ-789")
        )
        
        val abcAutos = autos.filter { it.placa.startsWith("ABC") }
        
        assertEquals(2, abcAutos.size)
        assertTrue(abcAutos.all { it.placa.startsWith("ABC") })
    }

    @Test
    fun `find auto by placa`() {
        val autos = listOf(
            Auto(id = "1", placa = "ABC-123"),
            Auto(id = "2", placa = "XYZ-789")
        )
        
        val found = autos.find { it.placa == "ABC-123" }
        
        assertNotNull(found)
        assertEquals("1", found?.id)
    }

    @Test
    fun `calculate distance between autos`() {
        // Santa Cruz, Bolivia coordinates
        val auto1 = Auto(id = "1", lat = -17.7833, lng = -63.1821)
        // Cochabamba, Bolivia coordinates  
        val auto2 = Auto(id = "2", lat = -17.3895, lng = -66.1568)
        
        // Simple distance calculation (Haversine would be more accurate)
        val latDiff = kotlin.math.abs(auto1.lat - auto2.lat)
        val lngDiff = kotlin.math.abs(auto1.lng - auto2.lng)
        val distance = kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
        
        // Should be approximately 0.4 degrees (~40km)
        assertTrue(distance > 0.3)
        assertTrue(distance < 0.5)
    }

    @Test
    fun `sort autos by angle`() {
        val autos = listOf(
            Auto(id = "1", angulo = 180.0),
            Auto(id = "2", angulo = 90.0),
            Auto(id = "3", angulo = 270.0),
            Auto(id = "4", angulo = 0.0)
        )
        
        val sorted = autos.sortedBy { it.angulo }
        
        assertEquals(0.0, sorted[0].angulo, 0.01)
        assertEquals(90.0, sorted[1].angulo, 0.01)
        assertEquals(180.0, sorted[2].angulo, 0.01)
        assertEquals(270.0, sorted[3].angulo, 0.01)
    }
}
