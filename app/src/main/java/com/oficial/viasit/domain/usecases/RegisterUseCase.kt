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
        if (request.password != request.passwordConfirm) {
            return Result.failure(Exception("Las contraseñas no coinciden"))
        }
        if (request.role == "conductor" && request.invitationCode.isNotBlank()) {
            val validationResult = adminRepository.validateInvitationCode(request.invitationCode)
            if (validationResult.isFailure) {
                return Result.failure(Exception("Código de invitación inválido o expirado"))
            }
            val code = validationResult.getOrNull()
                ?: return Result.failure(Exception("Código de invitación inválido o expirado"))
            
            if (code.role != "conductor") {
                return Result.failure(Exception("Este código no es válido para conductores"))
            }
            if (code.isExpired()) {
                return Result.failure(Exception("El código de invitación ha expirado"))
            }
        }
        return authRepository.register(request)
    }
}
