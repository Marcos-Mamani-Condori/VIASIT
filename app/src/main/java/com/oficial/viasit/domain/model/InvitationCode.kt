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
     * Verifica si el código es válido (no usado y no expirado)
     */
    fun isValid(): Boolean {
        return !isUsed && !isExpired()
    }
}
