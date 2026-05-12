package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reporte(
    @SerialName("id")
    val id: String = "",

    @SerialName("description")
    val descripcion: String = "",

    @SerialName("response")
    val respuesta: String = "",

    @SerialName("users")
    val users: List<String> = emptyList(),

    @SerialName("userid")
    val userid: String = "",

    @SerialName("reporterName")
    val reporterName: String = "",

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "reports"
)
