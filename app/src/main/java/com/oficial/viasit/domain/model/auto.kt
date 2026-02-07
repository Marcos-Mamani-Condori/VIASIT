package com.oficial.viasit.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "autos")
data class Auto(
    @PrimaryKey
    @SerialName("id")
    val id: String = "",

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
    val collectionId: String = "",

    @SerialName("collectionName")
    val collectionName: String = "autos"
)


