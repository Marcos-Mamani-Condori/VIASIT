package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para Códigos de Invitación de Vehículos
 * Colección PocketBase: vehicle_invitation_codes
 * 
 * Usado para que conductores registren sus vehículos en una línea específica
 */
@Serializable
data class VehicleInvitationCode(
    @SerialName("id")
    val id: String = "",

    // Código de invitación único
    @SerialName("code")
    val code: String = "",

    // Fecha de expiración
    @SerialName("expiresAt")
    val expiresAt: String = "",

    // Si ya fue usado
    @SerialName("isUsed")
    val isUsed: Boolean = false,

    // Relación con la línea (relation a lineas)
    @SerialName("linea_id")
    val lineaId: String = "",

    // Si tiene máximo de usos
    @SerialName("max_usos")
    val maxUsos: Boolean = false,

    // IDs de usuarios que usaron el código (separados por coma si múltiples)
    @SerialName("usado_por")
    val usadoPor: String = "",

    // Fechas de uso (separados por coma si múltiples)
    @SerialName("usado_en")
    val usadoEn: String = "",

    // ID del admin que creó el código
    @SerialName("creado_por")
    val creadoPor: String = "",

    // Si el código está activo
    @SerialName("activo")
    val activo: Boolean = true,

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "vehicle_invitation_codes"
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
            android.util.Log.e("VehicleInvitationCode", "Error parseando fecha '$expiresAt': ${e.message}")
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
                    java.time.LocalDateTime.parse(
                        dateStr.substringBefore(".").replace(" ", "T"),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    ).atZone(java.time.ZoneId.of("UTC")).toInstant()
                }
            }
        }
    }

    /**
     * Verifica si el código es válido (activo, no usado y no expirado)
     */
    fun isValid(): Boolean {
        return activo && !isUsed && !isExpired()
    }

    /**
     * Retorna la lista de IDs de usuarios que usaron el código
     */
    fun getUsedByList(): List<String> {
        return if (usadoPor.isNotEmpty()) usadoPor.split(",").map { it.trim() } else emptyList()
    }
}
