package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAuthRepository

class RegisterUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(request: RegisterRequest): Result<User> {
        return repository.register(request)
    }
}
