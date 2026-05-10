package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleInvitationCode(
    @SerialName("id")
    val id: String = "",

    @SerialName("code")
    val code: String = "",

    @SerialName("expiresAt")
    val expiresAt: String = "",

    @SerialName("isUsed")
    val isUsed: Boolean = false,

    @SerialName("lineId")
    val lineaId: String = "",

    @SerialName("usedBy")
    val usedBy: String = "",

    @SerialName("createdBy")
    val createdBy: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "vehicle_invitation_codes"
) {
    fun isExpired(): Boolean {
        if (expiresAt.isEmpty()) return false
        return try {
            val expireDate = parsePocketBaseDate(expiresAt)
            val now = java.time.Instant.now()
            now.isAfter(expireDate)
        } catch (e: Exception) {
            false
        }
    }

    fun isValid(): Boolean = !isUsed && !isExpired()
}
