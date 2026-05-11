package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.domain.repository.IAdminRepository

class GetVehicleInvitationCodesUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(lineaId: String): Result<List<VehicleInvitationCode>> =
        repository.getVehicleInvitationCodesByLinea(lineaId)
}

class GenerateVehicleInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(lineaId: String, creadoPor: String, hours: Int): Result<VehicleInvitationCode> =
        repository.generateVehicleInvitationCode(lineaId, creadoPor, hours)
}

class DeleteVehicleInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteVehicleInvitationCode(id)
}
