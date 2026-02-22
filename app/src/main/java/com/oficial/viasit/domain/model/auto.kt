package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
