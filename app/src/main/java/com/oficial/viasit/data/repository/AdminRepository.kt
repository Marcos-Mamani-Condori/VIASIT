package com.oficial.viasit.data.repository

import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode
import kotlinx.serialization.Serializable

class AdminRepository(
    private val client: PocketBaseRealtimeClient,
    private val authRepository: AuthRepository? = null
) {
    @Serializable
    data class PocketBaseListResponse<T>(
        val page: Int = 1, val perPage: Int = 30,
        val totalItems: Int = 0, val totalPages: Int = 0,
        val items: List<T> = emptyList()
    )

    fun getAuthToken() = authRepository?.getCurrentUser()?.let { client.authToken } ?: client.authToken

    private val shared = AdminRepositoryShared(authRepository) { getAuthToken() }
    fun formatNowPlus(hours: Int) = shared.formatNowPlus(hours)
    fun formatNow()               = shared.formatNow()
    fun generateSecureCode()      = shared.generateSecureCode()

    fun extractStringField(json: String, field: String) = AdminJsonParsers.extractStringField(json, field)
    fun extractItemBlocks(json: String)                 = AdminJsonParsers.extractItemBlocks(json)

    private val invitaciones   = InvitationCodesRepository(client, shared)
    private val lineas         = LineasRepository(client, shared)
    private val rutas          = RutasRepository(client, shared)
    private val logs           = LogsRepository(client, shared)
    private val vehicleCodes   = VehicleCodesRepository(client, shared)

    suspend fun generateInvitationCode(role: String = "ADMIN_LINEA", lineaId: String = "", expiresInHours: Int = 24): Result<InvitationCode> =
        invitaciones.generate(role, lineaId, expiresInHours)
    suspend fun getInvitationCodes(): Result<List<InvitationCode>>                     = invitaciones.getAll()
    suspend fun validateInvitationCode(code: String): Result<InvitationCode>           = invitaciones.validate(code)
    suspend fun deleteInvitationCode(codeId: String): Result<Unit>                     = invitaciones.delete(codeId)
    suspend fun useInvitationCode(codeId: String, userEmail: String): Result<Unit>     = invitaciones.markUsed(codeId, userEmail)

    suspend fun createLinea(name: String, code: String, rutaId: String = ""): Result<Linea>                          = lineas.create(name, code, rutaId)
    suspend fun getLineas(): Result<List<Linea>>                                                                      = lineas.getAll()
    suspend fun getLineasByRuta(rutaId: String): Result<List<Linea>>                                                 = lineas.getByRuta(rutaId)
    suspend fun updateLinea(lineaId: String, name: String, code: String, rutaId: String = ""): Result<Linea>         = lineas.update(lineaId, name, code, rutaId)
    suspend fun deleteLinea(lineaId: String): Result<Unit>                                                           = lineas.delete(lineaId)
    suspend fun updateLineaRuta(lineaId: String, rutaId: String): Result<Unit>                                       = lineas.updateRuta(lineaId, rutaId)

    suspend fun createRuta(name: String, description: String, startPoint: String = "", endPoint: String = "",
                           lineaId: String = "", waypoints: String = ""): Result<Ruta>                               = rutas.create(name, description, startPoint, endPoint, lineaId, waypoints)
    suspend fun getRuta(rutaId: String): Result<Ruta>                                                                = rutas.getById(rutaId)
    suspend fun getRutas(): Result<List<Ruta>>                                                                       = rutas.getAll()
    suspend fun getRutaByLinea(linea: Linea): Result<Ruta?> =
        if (linea.rutaId.isBlank()) Result.success(null) else rutas.getById(linea.rutaId).map { it }
    suspend fun deleteRuta(rutaId: String): Result<Unit>                                                             = rutas.delete(rutaId)

    suspend fun createLog(userId: String, description: String): Result<Unit>                                       = logs.create(userId, description)
    suspend fun getLogs(limit: Int = 50): Result<List<LogEntry>>                                                     = logs.getAll(limit)
    suspend fun getLogsByUser(userId: String): Result<List<LogEntry>>                                                = logs.getByUser(userId)

    suspend fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, expiresInHours: Int = 72): Result<VehicleInvitationCode> =
        vehicleCodes.generate(lineaId, creadoPor, expiresInHours)
    suspend fun getVehicleInvitationCodesByLinea(lineaId: String): Result<List<VehicleInvitationCode>>              = vehicleCodes.getByLinea(lineaId)
    suspend fun validateVehicleInvitationCode(code: String): Result<VehicleInvitationCode>                          = vehicleCodes.validate(code)
    suspend fun useVehicleInvitationCode(codeId: String, usadoPor: String): Result<Unit>                            = vehicleCodes.markUsed(codeId, usadoPor)
    suspend fun deleteVehicleInvitationCode(codeId: String): Result<Unit>                                           = vehicleCodes.delete(codeId)
}
