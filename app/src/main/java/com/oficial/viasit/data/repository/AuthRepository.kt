package com.oficial.viasit.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.oficial.viasit.data.remote.PocketBaseAuthClient
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val pocketBaseAuth = PocketBaseAuthClient()

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _authState.value = AuthState.Loading

            val result = pocketBaseAuth.login(email, password)

            result.fold(
                onSuccess = { user ->
                    saveUser(user, isGuest = false)
                    _authState.value = AuthState.Authenticated(user)
                    Result.success(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Error login")
                    Result.failure(error)
                }
            )
        }
    }

    suspend fun register(request: RegisterRequest): Result<User> {
        return withContext(Dispatchers.IO) {
            _authState.value = AuthState.Loading

            val result = pocketBaseAuth.register(
                email = request.email,
                password = request.password,
                passwordConfirm = request.passwordConfirm,
                name = request.name,
                phone = request.phone,
                role = request.role
            )

            result.fold(
                onSuccess = { user ->
                    saveUser(user, isGuest = false)
                    _authState.value = AuthState.Authenticated(user)
                    Result.success(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Error registro")
                    Result.failure(error)
                }
            )
        }
    }

    fun enterAsGuest() {
        val guest = User(
            id = "guest",
            email = "guest@viasit.com",
            name = "Invitado",
            role = "invitado",
            isActive = true,
            isInService = false
        )
        saveUser(guest, isGuest = true)
        _authState.value = AuthState.Authenticated(guest)
    }

    fun logout() {
        pocketBaseAuth.logout()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun toggleInService(isInService: Boolean) {
        val current = getCurrentUser()
        current?.let {
            val updated = it.copy(isInService = isInService)
            saveUser(updated, isGuest = it.role.lowercase() == "invitado")
            _authState.value = AuthState.Authenticated(updated)
        }
    }

    fun getUserId(): String? = prefs.getString("userId", null)
    fun isLoggedIn(): Boolean = prefs.getBoolean("loggedIn", false)
    fun isGuestMode(): Boolean = prefs.getBoolean("isGuest", false)
    fun isDriver(): Boolean = prefs.getString("role", null) == "conductor"
    fun getUserRole(): UserRole {
        val role = prefs.getString("role", "usuario") ?: "usuario"
        return when (role.lowercase()) {
            "conductor" -> UserRole.CONDUCTOR
            "invitado" -> UserRole.INVITADO
            else -> UserRole.USUARIO
        }
    }
    fun isInService(): Boolean = prefs.getBoolean("isInService", false)

    fun restoreSession() {
        when {
            isGuestMode() -> _authState.value = AuthState.Authenticated(getCurrentUser()!!)
            isLoggedIn() -> _authState.value = AuthState.Authenticated(getCurrentUser()!!)
        }
    }

    fun getCurrentUser(): User? {
        if (!isLoggedIn() && !isGuestMode()) return null
        return User(
            id = prefs.getString("userId", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            name = prefs.getString("name", "") ?: "",
            role = prefs.getString("role", "usuario") ?: "usuario",
            isActive = true,
            isInService = prefs.getBoolean("isInService", false)
        )
    }

    private fun saveUser(user: User, isGuest: Boolean) {
        prefs.edit().apply {
            putString("userId", user.id)
            putString("email", user.email)
            putString("name", user.name)
            putString("role", user.role)
            putBoolean("loggedIn", !isGuest)
            putBoolean("isGuest", isGuest)
            putBoolean("isInService", user.isInService)
            apply()
        }
    }
}
