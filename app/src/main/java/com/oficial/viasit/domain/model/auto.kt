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

/**
 * Serializador personalizado para manejar userid que puede ser:
 * - String: "lvb2ka1s47ap07u"
 * - JsonObject: {"id": "lvb2ka1s47ap07u", ...} (relación expandida)
 * - JsonArray: ["lvb2ka1s47ap07u"] (relación múltiple con un elemento)
 */
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
                // Si es un array, tomar el primer elemento
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

/**
 * Modelo de datos para Vehículos (Autos)
 * Colección PocketBase: autos
 * 
 * Schema:
 * - placa: text
 * - lat: number
 * - lng: number
 * - angulo: number
 * - colectivoid: text
 * - userid: relation a users
 * - code: text
 * 
 * NOTA: La línea del vehículo se obtiene del conductor (userId -> lineaId en users)
 * No se almacena lineaId aquí para evitar redundancia.
 */
@Serializable
data class Auto(
    @SerialName("id")
    val id: String = "",

    // Relación con el usuario conductor (relation a users)
    // Puede ser un String (ID) o un JsonObject (relación expandida)
    @Serializable(UserIdSerializer::class)
    @SerialName("userid")
    val userId: String = "",
    
    // Código del vehículo
    @SerialName("code")
    val code: String = "",

    // Placa del vehículo
    @SerialName("placa")
    val placa: String = "",

    // Latitud actual
    @SerialName("lat")
    val lat: Double = 0.0,

    // Longitud actual
    @SerialName("lng")
    val lng: Double = 0.0,

    // Ángulo/Dirección del vehículo
    @SerialName("angulo")
    val angulo: Double = 0.0,

    // ID del colectivo (si aplica)
    @SerialName("colectivoid")
    val colectivoid: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "autos"
) {
    /**
     * Verifica si el vehículo está activo (actualizó en los últimos 2 minutos)
     */
    fun isActive(maxAgeMinutes: Int = 2): Boolean {
        return try {
            if (updated.isEmpty()) return false
            val updateTime = java.time.OffsetDateTime.parse(updated)
            val now = java.time.OffsetDateTime.now()
            val minutesSinceUpdate = java.time.Duration.between(updateTime, now).toMinutes()
            minutesSinceUpdate < maxAgeMinutes
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retorna hace cuánto se actualizó en formato legible
     */
    fun getLastUpdateText(): String {
        return try {
            if (updated.isEmpty()) return "Sin datos"
            val updateTime = java.time.OffsetDateTime.parse(updated)
            val now = java.time.OffsetDateTime.now()
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
