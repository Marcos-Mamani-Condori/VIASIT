package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.*
import kotlinx.serialization.json.*

private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

fun extractStringField(json: String, field: String): String {
    return try {
        val element = jsonParser.parseToJsonElement(json)
        if (element is JsonObject) {
            element[field]?.jsonPrimitive?.content ?: ""
        } else {
            Regex(""""$field"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
        }
    } catch (e: Exception) {
        Regex(""""$field"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
    }
}

fun extractItemBlocks(json: String): List<String> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        root["items"]?.jsonArray?.map { it.toString() } ?: emptyList()
    } catch (e: Exception) {
        val itemsMatch = Regex(""""items"\s*:\s*\[(.*)\]""").find(json) ?: return emptyList()
        val content = itemsMatch.groupValues[1]
        val blocks = mutableListOf<String>()
        var bracketCount = 0
        var currentBlock = StringBuilder()
        for (char in content) {
            if (char == '{') bracketCount++
            if (bracketCount > 0) currentBlock.append(char)
            if (char == '}') {
                bracketCount--
                if (bracketCount == 0) {
                    blocks.add(currentBlock.toString())
                    currentBlock = StringBuilder()
                }
            }
        }
        blocks
    }
}

fun parseLogsFromJson(json: String): List<LogEntry> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        items.map { it.jsonObject }.map { obj ->
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val userId = obj["userid"]?.jsonPrimitive?.content ?: ""
            val description = obj["description"]?.jsonPrimitive?.content ?: ""
            val type = obj["type"]?.jsonPrimitive?.content ?: ""
            val created = obj["created"]?.jsonPrimitive?.content ?: ""
            
            var displayName = ""
            val userExpand = obj["expand"]?.jsonObject?.get("userid")?.jsonObject
            if (userExpand != null) {
                displayName = userExpand["name"]?.jsonPrimitive?.content 
                    ?: userExpand["username"]?.jsonPrimitive?.content ?: ""
            }
            if (displayName.isEmpty()) displayName = "Usuario ($userId)"

            LogEntry(id = id, userId = userId, userName = displayName, description = description, type = type, created = created)
        }
    } catch (e: Exception) { emptyList() }
}

fun parseUsersFromJson(json: String): List<User> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        items.map { it.jsonObject }.map { obj ->
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val email = obj["email"]?.jsonPrimitive?.content ?: ""
            // Priorizamos 'name' (Nombre Completo) sobre 'username' (ID técnico)
            val name = obj["name"]?.jsonPrimitive?.content?.ifBlank { null } 
                ?: obj["username"]?.jsonPrimitive?.content 
                ?: ""
            val active = obj["active"]?.jsonPrimitive?.booleanOrNull ?: true
            val lineaId = obj["lineId"]?.jsonPrimitive?.content ?: ""
            
            val role = when (val r = obj["role"]) {
                is JsonArray -> r.firstOrNull()?.jsonPrimitive?.content ?: "usuario"
                is JsonPrimitive -> r.content
                else -> "usuario"
            }
            
            var lineName = ""
            val expand = obj["expand"]?.jsonObject
            val lineExpand = expand?.get("lineId")
            val lineObj = when (lineExpand) {
                is JsonObject -> lineExpand
                is JsonArray -> if (lineExpand.isNotEmpty()) lineExpand[0].jsonObject else null
                else -> null
            }
            lineName = lineObj?.get("name")?.jsonPrimitive?.content ?: ""

            User(
                id = id,
                email = email,
                name = name,
                role = role,
                active = active,
                lineaId = lineaId,
                lineName = lineName
            )
        }
    } catch (e: Exception) { emptyList() }
}

fun parseInvitationCodesFromJson(json: String): List<InvitationCode> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        items.map { it.jsonObject }.map { obj ->
            InvitationCode(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                code = obj["code"]?.jsonPrimitive?.content ?: "",
                role = obj["role"]?.jsonPrimitive?.content ?: "",
                expiresAt = obj["expiresAt"]?.jsonPrimitive?.content ?: "",
                isUsed = obj["isUsed"]?.jsonPrimitive?.booleanOrNull ?: false,
                lineaId = obj["lineId"]?.jsonPrimitive?.content ?: ""
            )
        }
    } catch (e: Exception) { emptyList() }
}

fun parseLineasFromJson(json: String): List<Linea> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        items.map { it.jsonObject }.map { obj ->
            Linea(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                code = obj["code"]?.jsonPrimitive?.content ?: "",
                rutaId = obj["routeId"]?.jsonPrimitive?.content ?: ""
            )
        }
    } catch (e: Exception) { emptyList() }
}

fun parseRutaFromJson(json: String): Ruta {
    return try {
        val obj = jsonParser.parseToJsonElement(json).jsonObject
        Ruta(
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            name = obj["name"]?.jsonPrimitive?.content ?: "",
            description = obj["description"]?.jsonPrimitive?.content ?: "",
            startPoint = obj["startPoint"]?.jsonPrimitive?.content ?: "",
            endPoint = obj["endPoint"]?.jsonPrimitive?.content ?: "",
            waypoints = obj["waypoints"]?.jsonPrimitive?.content ?: ""
        )
    } catch (e: Exception) { Ruta() }
}

fun parseAppealsAsLogs(json: String): List<LogEntry> {
    return try {
        val root = jsonParser.parseToJsonElement(json).jsonObject
        val items = root["items"]?.jsonArray ?: return emptyList()
        items.map { it.jsonObject }.map { obj ->
            val status = obj["status"]?.jsonPrimitive?.content ?: "pending"
            // Mapeamos status a type de LogEntry para persistir el estado visual
            val type = when(status) {
                "accepted" -> "success"
                "rejected" -> "error"
                else -> "warning"
            }
            LogEntry(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                userId = obj["userId"]?.jsonPrimitive?.content ?: "",
                userName = obj["userName"]?.jsonPrimitive?.content ?: "",
                description = "APELACIÓN: " + (obj["reason"]?.jsonPrimitive?.content ?: ""),
                type = type,
                created = obj["created"]?.jsonPrimitive?.content ?: ""
            )
        }
    } catch (e: Exception) { emptyList() }
}
