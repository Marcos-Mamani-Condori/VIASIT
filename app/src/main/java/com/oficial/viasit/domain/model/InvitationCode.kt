package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para Códigos de Invitación de Administradores
 * Colección PocketBase: invitation_codes
 * 
 * Usado para invitar nuevos administradores (ADMIN_LINEA, ADMIN_PRINCIPAL)
 */
@Serializable
data class InvitationCode(
    @SerialName("id")
    val id: String = "",

    // Código de invitación único
    @SerialName("code")
    val code: String = "",

    // Rol que se asignará al usar el código
    @SerialName("role")
    val role: String = "", // "ADMIN_LINEA" o "ADMIN_PRINCIPAL"

    // ID de la línea (para ADMIN_LINEA)
    // En PocketBase el campo se llama "linea_id"
    @SerialName("linea_id")
    val lineaId: String = "",

    // Fecha de expiración
    @SerialName("expiresAt")
    val expiresAt: String = "",

    // Si ya fue usado
    @SerialName("isUsed")
    val isUsed: Boolean = false,

    // ID del usuario que usó el código
    @SerialName("usedBy")
    val usedBy: String = "",

    // Fecha cuando fue usado
    @SerialName("usedAt")
    val usedAt: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "invitation_codes"
) {
    /**
     * Verifica si el código ha expirado
     * Soporta múltiples formatos de fecha de PocketBase
     */
    fun isExpired(): Boolean {
        if (expiresAt.isEmpty()) return false
        return try {
            val expireDate = parsePocketBaseDate(expiresAt)
            val now = java.time.Instant.now()
            now.isAfter(expireDate)
        } catch (e: Exception) {
            android.util.Log.e("InvitationCode", "Error parseando fecha '$expiresAt': ${e.message}")
            false
        }
    }
    
    private fun parsePocketBaseDate(dateStr: String): java.time.Instant {
        // Intentar varios formatos que PocketBase puede devolver
        return try {
            // Formato ISO 8601 con offset (ej: 2024-01-15T10:30:00+00:00)
            java.time.OffsetDateTime.parse(dateStr).toInstant()
        } catch (e: Exception) {
            try {
                // Formato ISO 8601 con Z (ej: 2024-01-15T10:30:00Z)
                java.time.Instant.parse(dateStr)
            } catch (e2: Exception) {
                try {
                    // Formato PocketBase con espacio (ej: 2024-01-15 10:30:00.000Z)
                    val normalized = dateStr
                        .replace(" ", "T")
                        .replace(".000Z", "Z")
                        .replace(".00Z", "Z")
                    java.time.Instant.parse(normalized)
                } catch (e3: Exception) {
                    // Último intento: formato sin milisegundos
                    val formatter = java.time.format.DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .withZone(java.time.ZoneId.of("UTC"))
                    java.time.LocalDateTime.parse(
                        dateStr.substringBefore(".").replace(" ", "T"),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    ).atZone(java.time.ZoneId.of("UTC")).toInstant()
                }
            }
        }
    }

    /**
     * Verifica si el código es válido (no usado y no expirado)
     */
    fun isValid(): Boolean {
        return !isUsed && !isExpired()
    }
}
