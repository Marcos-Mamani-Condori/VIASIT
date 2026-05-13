package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.repository.IAdminRepository

class GetInvitationCodesUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(): Result<List<InvitationCode>> = repository.getInvitationCodes()
}

class GenerateInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(role: String, lineaId: String, hours: Int, adminName: String): Result<InvitationCode> =
        repository.generateInvitationCode(role, lineaId, hours, adminName)
}

class DeleteInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteInvitationCode(id)
}

class ValidateInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(code: String): Result<InvitationCode?> =
        repository.validateInvitationCode(code)
}

class UseInvitationCodeUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(codeId: String, userId: String): Result<Unit> =
        repository.useInvitationCode(codeId, userId)
}
