package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAdminRepository

class GetUsersByLineaUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(lineaId: String): Result<List<User>> = repository.getUsersByLinea(lineaId)
}

class SetUserActiveStatusUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(userId: String, active: Boolean): Result<Unit> = repository.setUserActiveStatus(userId, active)
}

class DeleteUserUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(userId: String): Result<Unit> = repository.deleteUser(userId)
}
