package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode

/**
 * Utilidades de parseo JSON para los repositorios de administración.
 *
 * Todas las funciones son internas (internal) para que solo los repositorios
 * del mismo paquete puedan usarlas. No deben ser llamadas desde la UI o ViewModels.
 */
internal object AdminJsonParsers {

    /** Extrae un campo String del JSON de PocketBase por nombre de campo */
    fun extractStringField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""

    /**
     * Extrae los bloques JSON {} individuales del array "items" de PocketBase.
     * Usa conteo de llaves para soportar cualquier orden de campos.
     */
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

    // ── Parsers por entidad ──────────────────────────────────────────────────

    fun parseInvitationCodesFromJson(json: String): List<InvitationCode> =
        extractItemBlocks(json).map { block ->
            InvitationCode(
                id        = extractStringField(block, "id"),
                code      = extractStringField(block, "code"),
                role      = extractStringField(block, "role"),
                lineaId   = extractStringField(block, "linea_id"),
                isUsed    = block.contains("\"isUsed\":true") || block.contains("\"isUsed\": true"),
                expiresAt = extractStringField(block, "expiresAt")
            )
        }.filter { it.id.isNotEmpty() }

    fun parseLineasFromJson(json: String): List<Linea> {
        val regex = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*"name"\s*:\s*"([^"]*)"[^}]*\}""")
        return regex.findAll(json).map { match ->
            val block = match.value
            Linea(id = extractStringField(block, "id"), name = extractStringField(block, "name"),
                code = extractStringField(block, "code"), rutaId = extractStringField(block, "ruta_id"))
        }.toList()
    }

    fun parseLogsFromJson(json: String): List<LogEntry> {
        val regex = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*\}""")
        return regex.findAll(json).map { match ->
            val block = match.value
            LogEntry(id = extractStringField(block, "id"), userId = extractStringField(block, "userid"),
                description = extractStringField(block, "description"), created = extractStringField(block, "created"))
        }.toList()
    }

    fun parseRutaFromJson(json: String): Ruta = Ruta(
        id          = extractStringField(json, "id"),
        name        = extractStringField(json, "name"),
        description = extractStringField(json, "description"),
        startPoint  = extractStringField(json, "start_point"),
        endPoint    = extractStringField(json, "end_point"),
        waypoints   = extractStringField(json, "waypoints")
    )

    fun parseVehicleCodesFromJson(json: String): List<VehicleInvitationCode> =
        extractItemBlocks(json).map { block ->
            VehicleInvitationCode(
                id        = extractStringField(block, "id"),
                code      = extractStringField(block, "code"),
                lineaId   = extractStringField(block, "linea_id"),
                creadoPor = extractStringField(block, "creado_por"),
                usadoPor  = extractStringField(block, "usadoPor"),
                expiresAt = extractStringField(block, "expires_at")
            )
        }.filter { it.id.isNotEmpty() }
}
