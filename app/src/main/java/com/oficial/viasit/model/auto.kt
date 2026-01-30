package com.oficial.viasit.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "autos")
data class Auto(
    @PrimaryKey
    @SerializedName("id")
    val id: String = "",

    @SerializedName("placa")
    val placa: String = "",

    @SerializedName("linea")
    val linea: String = "",

    @SerializedName("lat")
    val lat: Double = 0.0,

    @SerializedName("lng")
    val lng: Double = 0.0,

    @SerializedName("angulo")
    val angulo: Float = 0f,

    @SerializedName("updated")
    val updated: String = ""
)