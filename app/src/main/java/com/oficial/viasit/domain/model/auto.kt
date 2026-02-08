package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para vehículos (Auto)
 * Usado para comunicación con PocketBase
 * Room tiene su propia entidad en AutoData.kt (AutoEntity)
 */
@Serializable
data class Auto(
    @SerialName("id")
    val id: String = "",

    // Campo para PocketBase - relación múltiple como array
    @SerialName("userid")
    val userId: List<String> = emptyList(),

    @SerialName("placa")
    val placa: String = "",

    @SerialName("linea")
    val linea: String = "",

    @SerialName("lat")
    val lat: Double = 0.0,

    @SerialName("lng")
    val lng: Double = 0.0,

    @SerialName("angulo")
    val angulo: Double = 0.0,

    @SerialName("colectivoid")
    val colectivoid: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = ""
)
