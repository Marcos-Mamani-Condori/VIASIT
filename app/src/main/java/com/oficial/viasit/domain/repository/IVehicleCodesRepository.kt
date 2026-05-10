package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.VehicleInvitationCode

interface IVehicleCodesRepository {
    suspend fun generate(lineaId: String, creadoPor: String, expiresInHours: Int): Result<VehicleInvitationCode>
    suspend fun getByLinea(lineaId: String): Result<List<VehicleInvitationCode>>
    suspend fun validate(code: String): Result<VehicleInvitationCode>
    suspend fun markUsed(codeId: String, usadoPor: String): Result<Unit>
    suspend fun delete(codeId: String): Result<Unit>
}
