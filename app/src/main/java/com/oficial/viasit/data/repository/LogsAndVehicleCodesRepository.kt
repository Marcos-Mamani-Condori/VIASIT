package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.parseLogsFromJson
import com.oficial.viasit.data.repository.AdminJsonParsers.parseVehicleCodesFromJson

/**
 * Repositorio de logs de auditoría.
 */
internal class LogsRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) {
    suspend fun create(userEmail: String, description: String): Result<Unit> = try {
        client.createRecord("logs",
            mapOf("user_id" to userEmail, "description" to description, "created_at" to shared.formatNow()),
            shared.authToken()).map { }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getAll(limit: Int = 50): Result<List<LogEntry>> = try {
        client.getList("logs", perPage = limit, sort = "-created", authToken = shared.authToken())
            .map { parseLogsFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getByUser(userId: String): Result<List<LogEntry>> = try {
        client.getList("logs", filter = "user_id='$userId'", sort = "-created", authToken = shared.authToken())
            .map { parseLogsFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }
}

/**
 * Repositorio de códigos de vehículo (para conductores).
 */
internal class VehicleCodesRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) {
    suspend fun generate(lineaId: String, creadoPor: String, expiresInHours: Int = 72): Result<VehicleInvitationCode> = try {
        val code = shared.generateSecureCode()
        val data = mapOf("code" to code, "linea_id" to lineaId, "creado_por" to creadoPor,
            "expires_at" to shared.formatNowPlus(expiresInHours), "usadoPor" to "")
        client.createRecord("vehicle_invitation_codes", data, shared.authToken()).map { json ->
            VehicleInvitationCode(id = AdminJsonParsers.extractStringField(json, "id"),
                code = code, lineaId = lineaId, creadoPor = creadoPor,
                usadoPor = "", expiresAt = shared.formatNowPlus(expiresInHours))
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getByLinea(lineaId: String): Result<List<VehicleInvitationCode>> = try {
        client.getList("vehicle_invitation_codes", filter = "linea_id='$lineaId'",
            sort = "-created", authToken = shared.authToken()).map { parseVehicleCodesFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun validate(code: String): Result<VehicleInvitationCode> = try {
        client.getList("vehicle_invitation_codes", filter = "code='$code'&&usadoPor=''",
            authToken = shared.authToken()).fold(
            onSuccess = { json ->
                val codes = parseVehicleCodesFromJson(json)
                if (codes.isNotEmpty()) Result.success(codes.first())
                else Result.failure(Exception("Código de vehículo no válido o ya usado"))
            },
            onFailure = { Result.failure(it) }
        )
    } catch (e: Exception) { Result.failure(e) }

    suspend fun markUsed(codeId: String, usadoPor: String): Result<Unit> =
        client.updateRecord("vehicle_invitation_codes", codeId, mapOf("usadoPor" to usadoPor), shared.authToken()).map { }

    suspend fun delete(codeId: String): Result<Unit> =
        client.deleteRecord("vehicle_invitation_codes", codeId, shared.authToken()).map { }
}
