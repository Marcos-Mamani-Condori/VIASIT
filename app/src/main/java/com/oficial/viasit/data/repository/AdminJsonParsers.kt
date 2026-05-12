package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode

internal object AdminJsonParsers {

    fun extractStringField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""

    fun extractItemBlocks(json: String): List<String> {
        val arrayStart = json.indexOf('[', json.indexOf("\"items\":"))
        if (arrayStart == -1) return emptyList()
        val blocks = mutableListOf<String>()
        var depth = 0; var blockStart = -1; var i = arrayStart + 1
        while (i < json.length) {
            when (json[i]) {
                '{' -> { if (depth == 0) blockStart = i; depth++ }
                '}' -> { depth--
                    if (depth == 0 && blockStart != -1) { blocks.add(json.substring(blockStart, i + 1)); blockStart = -1 }
                }
                ']' -> if (depth == 0) break
            }
            i++
        }
        return blocks
    }

    fun parseInvitationCodesFromJson(json: String): List<InvitationCode> =
        extractItemBlocks(json).map { block ->
            InvitationCode(
                id        = extractStringField(block, "id"),
                code      = extractStringField(block, "code"),
                role      = extractStringField(block, "role"),
                lineaId   = extractStringField(block, "lineId"),
                isUsed    = block.contains("\"isUsed\":true") || block.contains("\"isUsed\": true"),
                expiresAt = extractStringField(block, "expiresAt")
            )
        }.filter { it.id.isNotEmpty() }

    fun parseLineasFromJson(json: String): List<Linea> {
        return extractItemBlocks(json).map { block ->
            Linea(
                id = extractStringField(block, "id"),
                name = extractStringField(block, "name"),
                code = extractStringField(block, "code"),
                rutaId = extractStringField(block, "routeId")
            )
        }.filter { it.id.isNotEmpty() }
    }

    fun parseLogsFromJson(json: String): List<LogEntry> {
        return extractItemBlocks(json).map { block ->
            LogEntry(
                id = extractStringField(block, "id"),
                userId = extractStringField(block, "userid"),
                description = extractStringField(block, "description"),
                type = extractStringField(block, "type").ifEmpty { "info" },
                created = extractStringField(block, "created")
            )
        }.filter { it.id.isNotEmpty() }
    }

    fun parseRutaFromJson(json: String): Ruta = Ruta(
        id          = extractStringField(json, "id"),
        name        = extractStringField(json, "name"),
        description = extractStringField(json, "description"),
        startPoint  = extractStringField(json, "startPoint"),
        endPoint    = extractStringField(json, "endPoint"),
        waypoints   = extractStringField(json, "waypoints")
    )

    fun parseVehicleCodesFromJson(json: String): List<VehicleInvitationCode> =
        extractItemBlocks(json).map { block ->
            VehicleInvitationCode(
                id        = extractStringField(block, "id"),
                code      = extractStringField(block, "code"),
                lineaId   = extractStringField(block, "lineId"),
                createdBy = extractStringField(block, "createdBy"),
                usedBy    = extractStringField(block, "usedBy"),
                expiresAt = extractStringField(block, "expiresAt")
            )
        }.filter { it.id.isNotEmpty() }

    fun parseUsersFromJson(json: String): List<com.oficial.viasit.domain.model.User> =
        extractItemBlocks(json).map { block ->
            com.oficial.viasit.domain.model.User(
                id = extractStringField(block, "id"),
                email = extractStringField(block, "email"),
                name = extractStringField(block, "name"),
                role = listOf(extractStringField(block, "role").replace("[", "").replace("]", "").replace("\"", "")),
                phone = extractStringField(block, "phone"),
                lineaId = extractStringField(block, "lineId"),
                created = extractStringField(block, "created")
            )
        }.filter { it.id.isNotEmpty() }

    fun parseAppealsAsLogs(json: String): List<LogEntry> =
        extractItemBlocks(json).map { block ->
            LogEntry(
                id = extractStringField(block, "id"),
                userId = extractStringField(block, "userId"),
                description = "APELACIÓN de ${extractStringField(block, "userName")}: ${extractStringField(block, "reason")}",
                type = "warning",
                created = extractStringField(block, "created")
            )
        }.filter { it.id.isNotEmpty() }
}
