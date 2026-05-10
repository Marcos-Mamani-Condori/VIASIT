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
    val creadoPor: String = "",

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
            android.util.Log.e("InvitationCode", "Error parseando fecha '$expiresAt': ${e.message}")
            false
        }
    }
    
    private fun parsePocketBaseDate(dateStr: String): java.time.Instant {
    
        return try {
           
            java.time.OffsetDateTime.parse(dateStr).toInstant()
        } catch (e: Exception) {
            try {
             
                java.time.Instant.parse(dateStr)
            } catch (e2: Exception) {
                try {
               
                    val normalized = dateStr
                        .replace(" ", "T")
                        .replace(".000Z", "Z")
                        .replace(".00Z", "Z")
                    java.time.Instant.parse(normalized)
                } catch (e3: Exception) {
                   
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


    fun isValid(): Boolean {
        return !isUsed && !isExpired()
    }
}
