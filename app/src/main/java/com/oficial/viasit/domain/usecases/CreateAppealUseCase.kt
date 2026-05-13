package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.repository.IAdminRepository

class CreateAppealUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(userId: String, name: String, reason: String, lineaId: String): Result<Unit> =
        repository.createAppeal(userId, name, reason, lineaId)
}
