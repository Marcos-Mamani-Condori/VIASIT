package com.oficial.viasit.data.repository

import android.util.Log
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repositorio de datos del módulo de administración.
 *
 * Este archivo sirve como punto de entrada único para toda la capa de datos
 * de administración. Internamente delega a sub-repositorios especializados:
 *
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │                   AdminRepository                           │
 *  │ (coordina autenticación, fechas y código seguro compartidos) │
 *  └──────┬────────────────┬────────────────┬────────────────────┘
 *         │                │                │
 *       InvitacionesRepo  LineasRepo       LogsRepo
 *       VehicleCodesRepo  RutasRepo
 *
 * Si quieres entender cómo funciona una operación específica, lee el
 * sub-repositorio correspondiente en data/repository/:
 *  → InvitationCodesRepository.kt  (códigos de invitación para admins)
 *  → LineasRepository.kt           (gestión de líneas de transporte)
 *  → LogsRepository.kt             (registros de auditoría)
 *  → RutasRepository.kt            (creación y consulta de rutas)
 *  → VehicleCodesRepository.kt     (códigos de invitación para conductores)
 *
 * Nota: Este archivo NO fue dividido todavía para no romper el código
 * existente de una vez. La separación se puede hacer cuando sea necesario
 * extender o testear cada dominio por separado.
 */
class AdminRepository(
    private val client: PocketBaseRealtimeClient,
    private val authRepository: AuthRepository? = null
) {

    // ── Utilidades compartidas entre todos los sub-dominios ────────────────

    fun getAuthToken() = authRepository?.getCurrentUser()?.let { client.authToken } ?: client.authToken

    @Serializable
    data class PocketBaseListResponse<T>(
        val page: Int = 1,
        val perPage: Int = 30,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<T> = emptyList()
    )

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'")

    /** Fecha/hora + N horas en formato PocketBase */
    fun formatNowPlus(hours: Int): String =
        LocalDateTime.now(ZoneId.of("UTC")).plusHours(hours.toLong()).format(formatter)

    /** Fecha/hora actual en formato PocketBase */
    fun formatNow(): String =
        LocalDateTime.now(ZoneId.of("UTC")).format(formatter)

    /** Genera un código alfanumérico seguro de 8 caracteres */
    fun generateSecureCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }

    // ── Invitaciones ───────────────────────────────────────────────────────

    suspend fun generateInvitationCode(
        role: String = "ADMIN_LINEA",
        lineaId: String = "",
        expiresInHours: Int = 24
    ): Result<InvitationCode> {
        return try {
            val code = generateSecureCode()
            val data = mutableMapOf<String, Any>(
                "code"      to code,
                "role"      to role,
                "expiresAt" to formatNowPlus(expiresInHours),
                "isUsed"    to false
            )
            if (lineaId.isNotEmpty()) data["linea_id"] = lineaId

            val result = client.createRecord("invitation_codes", data, getAuthToken())
            result.fold(
                onSuccess = { recordJson ->
                    Result.success(InvitationCode(
                        id        = extractStringField(recordJson, "id"),
                        code      = code,
                        role      = role,
                        lineaId   = lineaId,
                        isUsed    = false,
                        expiresAt = formatNowPlus(expiresInHours)
                    ))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitationCodes(): Result<List<InvitationCode>> {
        return try {
            val result = client.getList("invitation_codes", perPage = 50, sort = "-created", authToken = getAuthToken())
            result.fold(
                onSuccess = { json ->
                    val codes = parseInvitationCodesFromJson(json)
                    Result.success(codes)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateInvitationCode(code: String): Result<InvitationCode> {
        return try {
            val result = client.getList(
                "invitation_codes",
                filter = "code='$code'&&isUsed=false",
                authToken = getAuthToken()
            )
            result.fold(
                onSuccess = { json ->
                    val codes = parseInvitationCodesFromJson(json)
                    if (codes.isNotEmpty()) Result.success(codes.first())
                    else Result.failure(Exception("Código no válido o ya usado"))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvitationCode(codeId: String): Result<Unit> =
        client.deleteRecord("invitation_codes", codeId, getAuthToken()).map { }

    suspend fun useInvitationCode(codeId: String, userEmail: String): Result<Unit> =
        client.updateRecord("invitation_codes", codeId, mapOf("isUsed" to true, "usedBy" to userEmail), getAuthToken()).map { }

    // ── Líneas ─────────────────────────────────────────────────────────────

    suspend fun createLinea(name: String, code: String, rutaId: String = ""): Result<Linea> {
        return try {
            val data = mutableMapOf<String, Any>("name" to name, "code" to code)
            if (rutaId.isNotEmpty()) data["ruta_id"] = rutaId
            val result = client.createRecord("lineas", data, getAuthToken())
            result.fold(
                onSuccess = { json ->
                    Result.success(Linea(
                        id     = extractStringField(json, "id"),
                        name   = name,
                        code   = code,
                        rutaId = rutaId
                    ))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getLineas(): Result<List<Linea>> {
        return try {
            Log.d("AdminRepository", "Consultando colección lineas...")
            val result = client.getList("lineas", perPage = 100, sort = "name", authToken = getAuthToken())
            result.fold(
                onSuccess = { json ->
                    Log.d("AdminRepository", "Respuesta lineas OK")
                    Result.success(parseLineasFromJson(json))
                },
                onFailure = { e ->
                    Log.e("AdminRepository", "Error getLineas: ${e.message}")
                    Result.failure(e)
                }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getLineasByRuta(rutaId: String): Result<List<Linea>> {
        return try {
            val result = client.getList("lineas", filter = "ruta_id='$rutaId'", authToken = getAuthToken())
            result.fold(
                onSuccess = { json -> Result.success(parseLineasFromJson(json)) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateLinea(lineaId: String, name: String, code: String, rutaId: String = ""): Result<Linea> {
        return try {
            val data = mutableMapOf<String, Any>("name" to name, "code" to code)
            if (rutaId.isNotEmpty()) data["ruta_id"] = rutaId
            client.updateRecord("lineas", lineaId, data, getAuthToken())
                .map { Linea(lineaId, name, code, rutaId) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteLinea(lineaId: String): Result<Unit> =
        client.deleteRecord("lineas", lineaId, getAuthToken()).map { }

    // ── Logs ───────────────────────────────────────────────────────────────

    suspend fun createLog(userId: String, description: String): Result<Unit> {
        return try {
            client.createRecord("logs", mapOf("user_id" to userId, "description" to description, "created_at" to formatNow()), getAuthToken()).map { }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getLogs(limit: Int = 50): Result<List<LogEntry>> {
        return try {
            val result = client.getList("logs", perPage = limit, sort = "-created", authToken = getAuthToken())
            result.fold(
                onSuccess = { json -> Result.success(parseLogsFromJson(json)) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getLogsByUser(userId: String): Result<List<LogEntry>> {
        return try {
            val result = client.getList("logs", filter = "user_id='$userId'", sort = "-created", authToken = getAuthToken())
            result.fold(
                onSuccess = { json -> Result.success(parseLogsFromJson(json)) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Rutas ──────────────────────────────────────────────────────────────

    suspend fun createRuta(name: String, description: String, startPoint: String = "", endPoint: String = "", lineaId: String = ""): Result<Ruta> {
        return try {
            val data = mutableMapOf<String, Any>("name" to name, "description" to description)
            if (startPoint.isNotEmpty()) data["start_point"] = startPoint
            if (endPoint.isNotEmpty())   data["end_point"]   = endPoint
            if (lineaId.isNotEmpty())    data["linea_id"]    = lineaId
            client.createRecord("rutas", data, getAuthToken())
                .map { json -> Ruta(id = extractStringField(json, "id"), name = name, description = description, startPoint = startPoint, endPoint = endPoint) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRuta(rutaId: String): Result<Ruta> {
        return try {
            client.getRecord("rutas", rutaId, getAuthToken())
                .map { json -> parseRutaFromJson(json) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateLineaRuta(lineaId: String, rutaId: String): Result<Unit> =
        client.updateRecord("lineas", lineaId, mapOf("ruta_id" to rutaId), getAuthToken()).map { }

    // ── Códigos de vehículo ────────────────────────────────────────────────

    suspend fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, expiresInHours: Int = 72): Result<VehicleInvitationCode> {
        return try {
            val code = generateSecureCode()
            val data = mapOf("code" to code, "linea_id" to lineaId, "creado_por" to creadoPor, "expires_at" to formatNowPlus(expiresInHours), "usadoPor" to "")
            client.createRecord("vehicle_invitation_codes", data, getAuthToken()).map { json ->
                VehicleInvitationCode(id = extractStringField(json, "id"), code = code, lineaId = lineaId, creadoPor = creadoPor, usadoPor = "", expiresAt = formatNowPlus(expiresInHours))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getVehicleInvitationCodesByLinea(lineaId: String): Result<List<VehicleInvitationCode>> {
        return try {
            client.getList("vehicle_invitation_codes", filter = "linea_id='$lineaId'", sort = "-created", authToken = getAuthToken())
                .map { json -> parseVehicleCodesFromJson(json) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun validateVehicleInvitationCode(code: String): Result<VehicleInvitationCode> {
        return try {
            val result = client.getList("vehicle_invitation_codes", filter = "code='$code'&&usadoPor=''", authToken = getAuthToken())
            result.fold(
                onSuccess = { json ->
                    val codes = parseVehicleCodesFromJson(json)
                    if (codes.isNotEmpty()) Result.success(codes.first())
                    else Result.failure(Exception("Código de vehículo no válido o ya usado"))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun useVehicleInvitationCode(codeId: String, usadoPor: String): Result<Unit> =
        client.updateRecord("vehicle_invitation_codes", codeId, mapOf("usadoPor" to usadoPor), getAuthToken()).map { }

    suspend fun deleteVehicleInvitationCode(codeId: String): Result<Unit> =
        client.deleteRecord("vehicle_invitation_codes", codeId, getAuthToken()).map { }

    // ── Parsers JSON ───────────────────────────────────────────────────────

    private fun extractStringField(json: String, field: String): String {
        return Regex(""""$field"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""
    }

    private fun parseInvitationCodesFromJson(json: String): List<InvitationCode> {
        return extractItemBlocks(json).map { block ->
            InvitationCode(
                id        = extractStringField(block, "id"),
                code      = extractStringField(block, "code"),
                role      = extractStringField(block, "role"),
                lineaId   = extractStringField(block, "linea_id"),
                isUsed    = block.contains("\"isUsed\":true") || block.contains("\"isUsed\": true"),
                expiresAt = extractStringField(block, "expiresAt")
            )
        }.filter { it.id.isNotEmpty() }
    }

    private fun parseLineasFromJson(json: String): List<Linea> {
        val regex = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*"name"\s*:\s*"([^"]*)"[^}]*\}""")
        return regex.findAll(json).map { match ->
            val block = match.value
            Linea(
                id     = extractStringField(block, "id"),
                name   = extractStringField(block, "name"),
                code   = extractStringField(block, "code"),
                rutaId = extractStringField(block, "ruta_id")
            )
        }.toList()
    }

    private fun parseLogsFromJson(json: String): List<LogEntry> {
        val regex = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*\}""")
        return regex.findAll(json).map { match ->
            val block = match.value
            LogEntry(
                id          = extractStringField(block, "id"),
                userId      = extractStringField(block, "userid"),
                description = extractStringField(block, "description"),
                created     = extractStringField(block, "created")
            )
        }.toList()
    }

    private fun parseRutaFromJson(json: String): Ruta = Ruta(
        id          = extractStringField(json, "id"),
        name        = extractStringField(json, "name"),
        description = extractStringField(json, "description"),
        startPoint  = extractStringField(json, "start_point"),
        endPoint    = extractStringField(json, "end_point")
    )

    /**
     * Extrae los bloques {} individuales del array "items" del JSON de PocketBase.
     * Usa conteo de llaves en lugar de regex para no depender del orden de los campos.
     */
    private fun extractItemBlocks(json: String): List<String> {
        // Busca el array "items":[...]
        val itemsStart = json.indexOf('"', json.indexOf("\"items\":")) + 1
        val arrayStart = json.indexOf('[', json.indexOf("\"items\":"))
        if (arrayStart == -1) return emptyList()

        val blocks = mutableListOf<String>()
        var depth = 0
        var blockStart = -1
        var i = arrayStart + 1
        while (i < json.length) {
            when (json[i]) {
                '{' -> { if (depth == 0) blockStart = i; depth++ }
                '}' -> {
                    depth--
                    if (depth == 0 && blockStart != -1) {
                        blocks.add(json.substring(blockStart, i + 1))
                        blockStart = -1
                    }
                }
                ']' -> if (depth == 0) break
            }
            i++
        }
        return blocks
    }

    private fun parseVehicleCodesFromJson(json: String): List<VehicleInvitationCode> {
        return extractItemBlocks(json).map { block ->
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
}
