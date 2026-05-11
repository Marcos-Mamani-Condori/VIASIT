package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UserIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = serialDescriptor<String>()
    
    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
    
    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder 
            ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> element.content
            is JsonObject -> element["id"]?.let { 
                (it as? JsonPrimitive)?.content ?: ""
            } ?: ""
            is kotlinx.serialization.json.JsonArray -> {
                if (element.isEmpty()) ""
                else {
                    val first = element[0]
                    when (first) {
                        is JsonPrimitive -> first.content
                        is JsonObject -> first["id"]?.let { 
                            (it as? JsonPrimitive)?.content ?: ""
                        } ?: ""
                        else -> ""
                    }
                }
            }
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

    @SerialName("lineaId")
    val lineaId: String = "",

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

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "vehicles",

    @SerialName("expand")
    val expand: JsonObject? = null
) {
    val driverName: String
        get() = try {
            val userObj = expand?.get("userid") as? JsonObject
            userObj?.get("name")?.let { 
                if (it is JsonPrimitive) it.content else "Conductor desconocido"
            } ?: "Conductor"
        } catch (e: Exception) {
            "Conductor"
        }

    fun isActive(maxAgeMinutes: Int = 10): Boolean {
        return try {
            if (updated.isEmpty() || (lat == 0.0 && lng == 0.0)) return false
            val normalized = updated.replace(" ", "T")
            val updateTime = java.time.OffsetDateTime.parse(normalized)
            val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            val minutesSinceUpdate = java.time.Duration.between(updateTime, now).toMinutes()
            
            // Un vehículo es "activo" si se ha actualizado en los últimos 10 min
            // y no está en coordenadas 0,0 (que indica error de GPS)
            minutesSinceUpdate < maxAgeMinutes
        } catch (e: Exception) {
            false
        }
    }

    fun getLastUpdateText(): String {
        return try {
            if (updated.isEmpty()) return "Sin datos"
            val normalized = updated.replace(" ", "T")
            val updateTime = java.time.OffsetDateTime.parse(normalized)
            val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            val minutesSinceUpdate = java.time.Duration.between(updateTime, now).toMinutes()
            when {
                minutesSinceUpdate < 1 -> "Hace un momento"
                minutesSinceUpdate < 60 -> "Hace ${minutesSinceUpdate.toInt()} min"
                else -> "Hace ${(minutesSinceUpdate / 60).toInt()} h"
            }
        } catch (e: Exception) {
            "Desconocido"
        }
    }
}
