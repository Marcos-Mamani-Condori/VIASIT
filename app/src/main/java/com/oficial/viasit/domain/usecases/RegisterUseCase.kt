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
        if (request.role == "conductor" && request.invitationCode.isNotBlank()) {
            val validationResult = adminRepository.validateVehicleInvitationCode(request.invitationCode)
            if (validationResult.isFailure) {
                return Result.failure(Exception("Código de invitación inválido o expirado"))
            }
            val code = validationResult.getOrNull()
                ?: return Result.failure(Exception("Código de invitación inválido o expirado"))
            if (code.expiresAt.isNotEmpty()) {
                val expired = try {
                    val exp = java.time.OffsetDateTime.parse(code.expiresAt).toInstant()
                    exp.isBefore(java.time.Instant.now())
                } catch (e: Exception) {
                    true
                }
                if (expired) return Result.failure(Exception("El código de invitación ha expirado"))
            }
        }
        return authRepository.register(request)
    }
}
