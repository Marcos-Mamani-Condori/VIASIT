package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UserIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = serialDescriptor<String>()
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> element.content
            is JsonObject -> element["id"]?.jsonPrimitive?.content ?: ""
            is JsonArray -> if (element.isNotEmpty()) (element[0] as? JsonObject)?.get("id")?.jsonPrimitive?.content ?: "" else ""
            else -> ""
        }
    }
}

@Serializable
data class Auto(
    @SerialName("id")
    val id: String = "",

    @Serializable(UserIdSerializer::class)
    @SerialName("userid")
    val userId: String = "",
    
    @SerialName("code")
    val code: String = "",

    @SerialName("plate")
    val placa: String = "",

    @SerialName("lineId")
    val lineaIdFromDb: String = "",

    @SerialName("lat")
    val lat: Double = 0.0,

    @SerialName("lng")
    val lng: Double = 0.0,

    @SerialName("angle")
    val angulo: Double = 0.0,

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("expand")
    val expand: JsonObject? = null,

    // Caching fields to preserve expanded data in Room/Realtime
    val driverNameCache: String? = null,
    val lineNameCache: String? = null,
    val lineIdCache: String? = null
) {
    val driverName: String
        get() = driverNameCache ?: parseDriverName()

    val lineName: String
        get() = lineNameCache ?: parseLineName()

    val lineaId: String
        get() = if (lineaIdFromDb.isNotBlank()) lineaIdFromDb else (lineIdCache ?: parseLineId())

    private fun parseDriverName(): String = try {
        val userExpand = expand?.get("userid")
        val userObj = when (userExpand) {
            is JsonObject -> userExpand
            is JsonArray -> if (userExpand.isNotEmpty()) userExpand[0] as? JsonObject else null
            else -> null
        }
        userObj?.get("name")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: userObj?.get("username")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: userObj?.get("email")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: "Conductor ($userId)".take(20)
    } catch (e: Exception) {
        "Conductor"
    }

    private fun parseLineName(): String = try {
        val userExpand = expand?.get("userid")
        val userObj = when (userExpand) {
            is JsonObject -> userExpand
            is JsonArray -> if (userExpand.isNotEmpty()) userExpand[0] as? JsonObject else null
            else -> null
        }
        val lineExpand = userObj?.get("expand")?.jsonObject?.get("lineId")
        val lineObj = when (lineExpand) {
            is JsonObject -> lineExpand
            is JsonArray -> if (lineExpand.isNotEmpty()) lineExpand[0] as? JsonObject else null
            else -> null
        }
        lineObj?.get("name")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "Sin Línea"
    } catch (e: Exception) {
        "Sin Línea"
    }

    private fun parseLineId(): String = try {
        val userExpand = expand?.get("userid")
        val userObj = when (userExpand) {
            is JsonObject -> userExpand
            is JsonArray -> if (userExpand.isNotEmpty()) userExpand[0] as? JsonObject else null
            else -> null
        }
        val lineExpand = userObj?.get("expand")?.jsonObject?.get("lineId")
        val lineObj = when (lineExpand) {
            is JsonObject -> lineExpand
            is JsonArray -> if (lineExpand.isNotEmpty()) lineExpand[0] as? JsonObject else null
            else -> null
        }
        lineObj?.get("id")?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) { "" }

    fun isActive(maxAgeMinutes: Int = 30): Boolean {
        return try {
            if (updated.isEmpty() || (lat == 0.0 && lng == 0.0)) return false
            val normalized = updated.replace(" ", "T")
            val updateTime = java.time.OffsetDateTime.parse(normalized)
            val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            java.time.Duration.between(updateTime, now).toMinutes() < maxAgeMinutes
        } catch (e: Exception) { false }
    }

    fun getLastUpdateText(): String {
        return try {
            if (updated.isEmpty()) return "Sin datos"
            val updateTime = java.time.OffsetDateTime.parse(updated.replace(" ", "T"))
            val minutes = java.time.Duration.between(updateTime, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)).toMinutes()
            when {
                minutes < 1 -> "En línea"
                minutes < 60 -> "Hace $minutes min"
                else -> "Hace ${minutes/60} h"
            }
        } catch (e: Exception) { "Desconocido" }
    }
}
