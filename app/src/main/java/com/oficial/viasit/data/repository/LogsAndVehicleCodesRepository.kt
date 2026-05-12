package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.parseLogsFromJson
import com.oficial.viasit.data.repository.AdminJsonParsers.parseVehicleCodesFromJson
import com.oficial.viasit.domain.repository.ILogsRepository
import com.oficial.viasit.domain.repository.IVehicleCodesRepository

internal class LogsRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : ILogsRepository {
    override suspend fun create(userId: String, description: String, type: String): Result<Unit> = try {
        client.createRecord("logs",
            mapOf("userid" to userId, "description" to description, "type" to type),
            shared.authToken()).map { }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAll(limit: Int, filter: String, timeFilter: String): Result<List<LogEntry>> = try {
        var pbFilter = ""
        
        // Limpieza profunda del filtro: si es vacío o "todos", no filtramos por texto
        val cleanFilter = filter.trim().lowercase()
        if (cleanFilter.isNotEmpty() && cleanFilter != "todos") {
            pbFilter = "description ~ '$filter'"
        }

        val timePart = when (timeFilter.lowercase()) {
            "hoy" -> "created >= '${shared.getStartDateForFilter("today")}'"
            "ayer" -> "created >= '${shared.getStartDateForFilter("yesterday")}' && created < '${shared.getStartDateForFilter("today")}'"
            "semana" -> "created >= '${shared.getStartDateForFilter("last_week")}'"
            "mes" -> "created >= '${shared.getStartDateForFilter("last_month")}'"
            else -> ""
        }

        if (timePart.isNotEmpty()) {
            pbFilter = if (pbFilter.isNotEmpty()) "($pbFilter) && ($timePart)" else timePart
        }

        println("DEBUG: LogsRepository - Filtro PB: $pbFilter")

        client.getList("logs", perPage = limit, sort = "-created", filter = pbFilter, expand = "userid", authToken = shared.authToken())
            .map { json ->
                println("DEBUG: LogsRepository - Respuesta Cruda: $json")
                parseLogsFromJson(json)
            }.onFailure { 
                println("DEBUG: LogsRepository - Error en getList: ${it.message}")
            }
    } catch (e: Exception) { 
        println("DEBUG: LogsRepository - Excepción: ${e.message}")
        Result.failure(e) 
    }

    override suspend fun getByUser(userId: String): Result<List<LogEntry>> = try {
        client.getList("logs", filter = "userid='$userId'", sort = "-created", expand = "userid", authToken = shared.authToken())
            .map { parseLogsFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }
}

internal class VehicleCodesRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : IVehicleCodesRepository {
    override suspend fun generate(lineaId: String, creadoPor: String, expiresInHours: Int): Result<VehicleInvitationCode> = try {
        val code = shared.generateSecureCode()
        val data = mapOf("code" to code, "lineId" to lineaId, "createdBy" to creadoPor,
            "expiresAt" to shared.formatNowPlus(expiresInHours), "usedBy" to "")
        client.createRecord("vehicle_invitation_codes", data, shared.authToken()).map { json ->
            VehicleInvitationCode(id = AdminJsonParsers.extractStringField(json, "id"),
                code = code, lineaId = lineaId, createdBy = creadoPor,
                usedBy = "", expiresAt = shared.formatNowPlus(expiresInHours))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getByLinea(lineaId: String): Result<List<VehicleInvitationCode>> = try {
        client.getList("vehicle_invitation_codes", filter = "lineId='$lineaId'",
            sort = "-created", authToken = shared.authToken()).map { parseVehicleCodesFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun validate(code: String): Result<VehicleInvitationCode> = try {
        client.getList("vehicle_invitation_codes", filter = "code='$code'&&usedBy=''",
            authToken = shared.authToken()).fold(
            onSuccess = { json ->
                val codes = parseVehicleCodesFromJson(json)
                if (codes.isNotEmpty()) Result.success(codes.first())
                else Result.failure(Exception("Código de vehículo no válido o ya usado"))
            },
            onFailure = { Result.failure(it) }
        )
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun markUsed(codeId: String, usadoPor: String): Result<Unit> =
        client.updateRecord("vehicle_invitation_codes", codeId, mapOf("usedBy" to usadoPor, "isUsed" to true), shared.authToken()).map { }

    override suspend fun delete(codeId: String): Result<Unit> =
        client.deleteRecord("vehicle_invitation_codes", codeId, shared.authToken()).map { }
}
