package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LogEntry(
    @SerialName("id")
    val id: String = "",

    @SerialName("userid")
    val userId: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("type")
    val type: String = "info", // info (verde), warning (amarillo), danger (rojo)

    @SerialName("created")
    val created: String = "",

    @SerialName("updated")
    val updated: String = "",

    @SerialName("collectionId")
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "logs"
)
