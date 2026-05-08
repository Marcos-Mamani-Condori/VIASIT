package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.repository.IAuthRepository

class LoginUseCase(private val repository: IAuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return repository.login(email, password)
    }
}
