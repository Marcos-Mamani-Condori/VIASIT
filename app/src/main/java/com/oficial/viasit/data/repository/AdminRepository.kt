package com.oficial.viasit.data.repository

import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.domain.repository.IAdminRepository
import kotlinx.serialization.Serializable

class AdminRepository(
    private val client: PocketBaseRealtimeClient,
    private val authRepository: com.oficial.viasit.domain.repository.IAuthRepository? = null
) : IAdminRepository {
    @Serializable
    data class PocketBaseListResponse<T>(
        val page: Int = 1, val perPage: Int = 30,
        val totalItems: Int = 0, val totalPages: Int = 0,
        val items: List<T> = emptyList()
    )

    private fun getAuthToken() = authRepository?.getAuthToken() ?: client.authToken

    private val shared = AdminRepositoryShared(authRepository) { getAuthToken() }
    
    // Internal/private helpers instead of public
    internal fun formatNowPlus(hours: Int) = shared.formatNowPlus(hours)
    internal fun formatNow()               = shared.formatNow()
    internal fun generateSecureCode()      = shared.generateSecureCode()
    internal fun extractStringField(json: String, field: String) = AdminJsonParsers.extractStringField(json, field)
    internal fun extractItemBlocks(json: String)                 = AdminJsonParsers.extractItemBlocks(json)

    private val invitaciones   = InvitationCodesRepository(client, shared)
    private val lineas         = LineasRepository(client, shared)
    private val rutas          = RutasRepository(client, shared)
    private val logs           = LogsRepository(client, shared)
    private val vehicleCodes   = VehicleCodesRepository(client, shared)

    override suspend fun generateInvitationCode(role: String, lineaId: String, expiresInHours: Int): Result<InvitationCode> =
        invitaciones.generate(role, lineaId, expiresInHours)
    override suspend fun getInvitationCodes(): Result<List<InvitationCode>>                    = invitaciones.getAll()
    override suspend fun validateInvitationCode(code: String): Result<InvitationCode>          = invitaciones.validate(code)
    override suspend fun deleteInvitationCode(codeId: String): Result<Unit>                    = invitaciones.delete(codeId)
    override suspend fun useInvitationCode(codeId: String, userId: String): Result<Unit>       = invitaciones.markUsed(codeId, userId)

    override suspend fun createLinea(name: String, code: String, rutaId: String): Result<Linea>                         = lineas.create(name, code, rutaId)
    override suspend fun getLineas(): Result<List<Linea>>                                                                = lineas.getAll()
    override suspend fun getLineasByRuta(rutaId: String): Result<List<Linea>>                                           = lineas.getByRuta(rutaId)
    override suspend fun updateLinea(lineaId: String, name: String, code: String, rutaId: String): Result<Linea>        = lineas.update(lineaId, name, code, rutaId)
    override suspend fun deleteLinea(lineaId: String): Result<Unit>                                                      = lineas.delete(lineaId)
    override suspend fun updateLineaRuta(lineaId: String, rutaId: String): Result<Unit>                                  = lineas.updateRuta(lineaId, rutaId)

    override suspend fun createRuta(name: String, description: String, startPoint: String, endPoint: String,
                                    lineaId: String, waypoints: String): Result<Ruta>                                    = rutas.create(name, description, startPoint, endPoint, lineaId, waypoints)
    override suspend fun getRuta(rutaId: String): Result<Ruta>                                                           = rutas.getById(rutaId)
    override suspend fun getRutas(): Result<List<Ruta>>                                                                  = rutas.getAll()
    override suspend fun getRutaByLinea(linea: Linea): Result<Ruta?> =
        if (linea.rutaId.isBlank()) Result.success(null) else rutas.getById(linea.rutaId).map { it }
    override suspend fun deleteRuta(rutaId: String): Result<Unit>                                                        = rutas.delete(rutaId)

    override suspend fun createLog(userId: String, description: String, type: String): Result<Unit> =
        logs.create(userId, description, type)
    override suspend fun getLogs(limit: Int, filter: String): Result<List<LogEntry>>                                     = logs.getAll(limit, filter)
    override suspend fun getLogsByUser(userId: String): Result<List<LogEntry>>                                           = logs.getByUser(userId)

    override suspend fun getUsersByLinea(lineaId: String): Result<List<com.oficial.viasit.domain.model.User>> = try {
        client.getList("users", filter = "lineId='$lineaId'", authToken = getAuthToken())
            .map { AdminJsonParsers.parseUsersFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun setUserActiveStatus(userId: String, active: Boolean): Result<Unit> =
        client.updateRecord("users", userId, mapOf("active" to active), getAuthToken()).map { }

    override suspend fun deleteUser(userId: String): Result<Unit> =
        client.deleteRecord("users", userId, getAuthToken()).map { }

    override suspend fun createAppeal(userId: String, name: String, reason: String, lineaId: String): Result<Unit> =
        client.createRecord("appeals", mapOf(
            "userId" to userId,
            "userName" to name,
            "reason" to reason,
            "lineId" to lineaId,
            "status" to "pending"
        ), null).map { }

    override suspend fun getAppeals(): Result<List<LogEntry>> = try {
        client.getList("appeals", sort = "-created", authToken = getAuthToken())
            .map { AdminJsonParsers.parseAppealsAsLogs(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun resolveAppeal(appealId: String, userId: String, accept: Boolean): Result<Unit> = try {
        val status = if (accept) "accepted" else "rejected"
        client.updateRecord("appeals", appealId, mapOf("status" to status), getAuthToken()).getOrThrow()
        if (accept) {
            setUserActiveStatus(userId, true).getOrThrow()
        }
        val desc = "Apelación $status para usuario ID: $userId"
        createLog(userId, desc, if (accept) "info" else "danger").getOrThrow()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, expiresInHours: Int): Result<VehicleInvitationCode> =
        vehicleCodes.generate(lineaId, creadoPor, expiresInHours)
    override suspend fun getVehicleInvitationCodesByLinea(lineaId: String): Result<List<VehicleInvitationCode>>          = vehicleCodes.getByLinea(lineaId)
    override suspend fun validateVehicleInvitationCode(code: String): Result<VehicleInvitationCode>                      = vehicleCodes.validate(code)
    override suspend fun useVehicleInvitationCode(codeId: String, usadoPor: String): Result<Unit>                        = vehicleCodes.markUsed(codeId, usadoPor)
    override suspend fun deleteVehicleInvitationCode(codeId: String): Result<Unit>                                       = vehicleCodes.delete(codeId)
}
