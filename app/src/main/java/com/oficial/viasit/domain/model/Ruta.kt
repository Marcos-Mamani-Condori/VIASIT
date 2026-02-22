package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para Rutas
 * Colección PocketBase: rutas
 */
@Serializable
data class Ruta(
    @SerialName("id")
    val id: String = "",

    // Nombre de la ruta
    @SerialName("name")
    val name: String = "",

    // Descripción de la ruta
    @SerialName("description")
    val description: String = "",

    // Punto de inicio (formato: "lat,lng" o dirección)
    @SerialName("start_point")
    val startPoint: String = "",

    // Punto de fin (formato: "lat,lng" o dirección)
    @SerialName("end_point")
    val endPoint: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "rutas"
)
