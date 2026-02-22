package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para Logs del sistema
 * Colección PocketBase: logs
 * 
 * Usado para registrar eventos y acciones del sistema
 * 
 * Schema:
 * - userid: text
 * - description: text
 */
@Serializable
data class Log(
    @SerialName("id")
    val id: String = "",

    // ID del usuario que generó el log
    @SerialName("userid")
    val userId: String = "",

    // Descripción del evento
    @SerialName("description")
    val description: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "logs"
)

/**
 * Alias para compatibilidad con código existente
 */
typealias LogEntry = Log

