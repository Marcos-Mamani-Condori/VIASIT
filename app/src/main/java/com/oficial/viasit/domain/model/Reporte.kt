package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Reporte(
    @SerialName("id")
    val id: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("adminResponse")
    val adminResponse: String = "",

    @SerialName("driverResponse")
    val driverResponse: String = "",

    @SerialName("users")
    val users: List<String> = emptyList(), // ID del Pasajero

    @SerialName("userid")
    val userid: String = "", // ID del Conductor

    @SerialName("status")
    val status: String = "pendiente",

    @SerialName("created")
    val created: String = "",

    @SerialName("reporterName")
    val reporterNameFromDb: String = "",

    @SerialName("targetName")
    val targetNameFromDb: String = "",

    @SerialName("expand")
    val expand: JsonObject? = null
) {
    val driverName: String
        get() {
            val nameFromExpand = getNameFromExpand("userid")
            return if (!nameFromExpand.isNullOrBlank()) nameFromExpand
            else if (targetNameFromDb.isNotBlank()) targetNameFromDb
            else "Conductor ($userid)"
        }

    val reporterName: String
        get() {
            val nameFromExpand = getNameFromExpand("users")
            return if (!nameFromExpand.isNullOrBlank()) nameFromExpand
            else if (reporterNameFromDb.isNotBlank()) reporterNameFromDb
            else "Pasajero"
        }

    private fun getNameFromExpand(key: String): String? {
        return try {
            val expandObj = expand?.get(key)
            if (expandObj is JsonObject) return extractName(expandObj)
            if (expandObj is JsonArray && expandObj.isNotEmpty()) {
                val first = expandObj[0]
                if (first is JsonObject) return extractName(first)
            }
            null
        } catch (e: Exception) { null }
    }

    private fun extractName(obj: JsonObject): String? {
        val username = obj["username"]?.jsonPrimitive?.content
        if (!username.isNullOrBlank()) return username
        val name = obj["name"]?.jsonPrimitive?.content
        if (!name.isNullOrBlank()) return name
        val email = obj["email"]?.jsonPrimitive?.content
        if (!email.isNullOrBlank()) return email
        return null
    }
}
