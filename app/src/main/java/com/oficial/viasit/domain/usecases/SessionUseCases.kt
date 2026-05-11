package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.StateFlow

class LogoutUseCase(private val repository: IAuthRepository) {
    operator fun invoke() = repository.logout()
}

class EnterAsGuestUseCase(private val repository: IAuthRepository) {
    operator fun invoke() = repository.enterAsGuest()
}

class SetDriverInServiceUseCase(private val repository: IAuthRepository) {
    operator fun invoke(inService: Boolean, autoId: String = "") = 
        repository.setInService(inService, autoId)
}

class GetAuthStateUseCase(private val repository: IAuthRepository) {
    operator fun invoke(): StateFlow<AuthState> = repository.authState
}

class GetIsInServiceUseCase(private val repository: IAuthRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.isInService
}

class RestoreSessionUseCase(private val repository: IAuthRepository) {
    operator fun invoke() = repository.restoreSession()
}
