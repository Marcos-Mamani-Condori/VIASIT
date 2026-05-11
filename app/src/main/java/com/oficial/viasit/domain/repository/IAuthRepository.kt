package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.domain.model.RegisterRequest
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val authState: StateFlow<AuthState>
    val isInService: StateFlow<Boolean>
    
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(request: RegisterRequest): Result<User>
    fun enterAsGuest()
    fun logout()
    fun setInService(inService: Boolean, autoId: String = "")
    fun isLoggedIn(): Boolean
    fun isGuestMode(): Boolean
    fun isDriver(): Boolean
    fun getUserRole(): UserRole
    fun getUserId(): String?
    fun getAuthToken(): String?
    fun restoreSession()
    fun getCurrentUser(): User?
}
