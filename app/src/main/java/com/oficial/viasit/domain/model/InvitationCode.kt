package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InvitationCode(
    @SerialName("id")
    val id: String = "",

    @SerialName("code")
    val code: String = "",

    @SerialName("role")
    val role: String = "",

    @SerialName("lineId")
    val lineaId: String = "",

    @SerialName("expiresAt")
    val expiresAt: String = "",

    @SerialName("isUsed")
    val isUsed: Boolean = false,

    @SerialName("usedBy")
    val usedBy: String = "",

    @SerialName("createdBy")
    val createdBy: String = "",

    @SerialName("adminName")
    val adminName: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "invitation_codes"
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
