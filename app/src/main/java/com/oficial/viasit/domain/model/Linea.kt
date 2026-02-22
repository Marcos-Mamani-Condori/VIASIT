package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para Líneas de transporte
 * Colección PocketBase: lineas
 */
@Serializable
data class Linea(
    @SerialName("id")
    val id: String = "",

    // Código único de la línea (ej: "L001")
    @SerialName("code")
    val code: String = "",

    // Nombre de la línea (ej: "Línea 1 - Centro")
    @SerialName("name")
    val name: String = "",

    // Relación con la ruta asignada (relation a rutas)
    @SerialName("ruta_id")
    val rutaId: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "lineas"
)
