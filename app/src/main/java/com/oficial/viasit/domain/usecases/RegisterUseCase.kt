package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAdminRepository
import com.oficial.viasit.domain.repository.IAuthRepository

class RegisterUseCase(
    private val authRepository: IAuthRepository,
    private val adminRepository: IAdminRepository
) {
    suspend operator fun invoke(request: RegisterRequest): Result<User> {
        var modifiedRequest = request
        if (request.role == "conductor" && request.invitationCode.isNotBlank()) {
            val validationResult = adminRepository.validateVehicleInvitationCode(request.invitationCode)
            if (validationResult.isFailure) {
                return Result.failure(Exception("Código de invitación inválido o expirado"))
            }
            val code = validationResult.getOrNull()
            if (code == null) {
                return Result.failure(Exception("Código de invitación inválido o expirado"))
            }
            if (code.expiresAt.isNotEmpty()) {
                try {
                    val exp = java.time.OffsetDateTime.parse(code.expiresAt).toInstant()
                    if (exp.isBefore(java.time.Instant.now())) {
                        return Result.failure(Exception("El código de invitación ha expirado"))
                    }
                } catch (e: Exception) { }
            }
        }
        return authRepository.register(modifiedRequest)
    }
}
