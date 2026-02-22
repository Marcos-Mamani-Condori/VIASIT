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
     */
    fun isExpired(): Boolean {
        if (expiresAt.isEmpty()) return false
        return try {
            val expireDate = java.time.OffsetDateTime.parse(expiresAt)
            val now = java.time.OffsetDateTime.now()
            now.isAfter(expireDate)
        } catch (e: Exception) {
            false
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
