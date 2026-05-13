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

    @SerialName("userId")
    val userIdAlternative: String = "", // Fallback por si en DB es userId

    @SerialName("lineId")
    val lineId: String = "",

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
    /**
     * Devuelve el ID del conductor reportado intentando todas las posibles variantes de campo.
     */
    val targetUserId: String
        get() {
            // 1. Prioridad: Campos directos (userid o userIdAlternative)
            if (userid.isNotBlank()) return userid
            if (userIdAlternative.isNotBlank()) return userIdAlternative
            
            // 2. Fallback: Buscar en el objeto expandido (PocketBase suele incluir el 'id' dentro)
            expand?.let { exp ->
                listOf("userid", "userId", "user", "driver").forEach { key ->
                    try {
                        val entry = exp[key]
                        if (entry is JsonObject) {
                            val id = entry["id"]?.jsonPrimitive?.content
                            if (!id.isNullOrBlank()) return id
                        } else if (entry is JsonArray && entry.isNotEmpty()) {
                            val first = entry[0]
                            if (first is JsonObject) {
                                val id = first["id"]?.jsonPrimitive?.content
                                if (!id.isNullOrBlank()) return id
                            }
                        }
                    } catch (e: Exception) { /* ignore */ }
                }
            }
            return ""
        }

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
        // Prioridad: 1. Nombre completo, 2. Username técnico, 3. Email
        val name = obj["name"]?.jsonPrimitive?.content
        if (!name.isNullOrBlank()) return name
        
        val username = obj["username"]?.jsonPrimitive?.content
        if (!username.isNullOrBlank()) return username

        val email = obj["email"]?.jsonPrimitive?.content
        if (!email.isNullOrBlank()) return email
        return null
    }
}
