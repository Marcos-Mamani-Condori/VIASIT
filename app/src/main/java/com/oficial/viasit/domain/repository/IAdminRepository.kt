package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode

interface IAdminRepository {

    suspend fun generateInvitationCode(role: String = "ADMIN_LINEA", lineaId: String = "", expiresInHours: Int = 24): Result<InvitationCode>
    suspend fun getInvitationCodes(): Result<List<InvitationCode>>
    suspend fun validateInvitationCode(code: String): Result<InvitationCode>
    suspend fun deleteInvitationCode(codeId: String): Result<Unit>
    suspend fun useInvitationCode(codeId: String, userId: String): Result<Unit>

    suspend fun createLinea(name: String, code: String, rutaId: String = ""): Result<Linea>
    suspend fun getLineas(): Result<List<Linea>>
    suspend fun getLineasByRuta(rutaId: String): Result<List<Linea>>
    suspend fun updateLinea(lineaId: String, name: String, code: String, rutaId: String = ""): Result<Linea>
    suspend fun deleteLinea(lineaId: String): Result<Unit>
    suspend fun updateLineaRuta(lineaId: String, rutaId: String): Result<Unit>

    suspend fun createRuta(name: String, description: String, startPoint: String = "", endPoint: String = "", lineaId: String = "", waypoints: String = ""): Result<Ruta>
    suspend fun getRuta(rutaId: String): Result<Ruta>
    suspend fun getRutas(): Result<List<Ruta>>
    suspend fun getRutaByLinea(linea: Linea): Result<Ruta?>
    suspend fun deleteRuta(rutaId: String): Result<Unit>

    suspend fun createLog(userId: String, description: String): Result<Unit>
    suspend fun getLogs(limit: Int = 50): Result<List<LogEntry>>
    suspend fun getLogsByUser(userId: String): Result<List<LogEntry>>

    suspend fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, expiresInHours: Int = 72): Result<VehicleInvitationCode>
    suspend fun getVehicleInvitationCodesByLinea(lineaId: String): Result<List<VehicleInvitationCode>>
    suspend fun validateVehicleInvitationCode(code: String): Result<VehicleInvitationCode>
    suspend fun useVehicleInvitationCode(codeId: String, usadoPor: String): Result<Unit>
    suspend fun deleteVehicleInvitationCode(codeId: String): Result<Unit>
}
