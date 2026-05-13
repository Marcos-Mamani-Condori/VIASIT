package com.oficial.viasit.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.oficial.viasit.data.remote.PocketBaseAuthClient
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.domain.repository.IAuthRepository
import com.oficial.viasit.service.LocationTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(context: Context) : IAuthRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState
    private val context: Context = context.applicationContext

    private val _isInService = MutableStateFlow(prefs.getBoolean("isInService", false))
    override val isInService: StateFlow<Boolean> = _isInService

    private val pocketBaseAuth = PocketBaseAuthClient()

    override suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthRepository", "Iniciando login para: $email")
            val result = pocketBaseAuth.login(email, password)
            result.fold(
                onSuccess = { user ->
                    android.util.Log.d("AuthRepository", "Login exitoso. User: ${user.name}, LineaId: ${user.lineaId}, Role: ${user.role}")
                    if (!user.active) {
                        _authState.value = AuthState.Suspended(user)
                        Result.failure(Exception("Cuenta inactiva"))
                    } else {
                        saveUser(user, isGuest = false)
                        _authState.value = AuthState.Authenticated(user)
                        Result.success(user)
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("AuthRepository", "Error en login para $email", error)
                    _authState.value = AuthState.Error(error.message ?: "Error login")
                    Result.failure(error)
                }
            )
        }
    }

    override suspend fun register(request: RegisterRequest): Result<User> {
        return withContext(Dispatchers.IO) {
            _authState.value = AuthState.Loading

            val result = pocketBaseAuth.register(
                email = request.email,
                password = request.password,
                passwordConfirm = request.passwordConfirm,
                name = request.name,
                phone = request.phone,
                role = request.role,
                lineaId = request.lineaId
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



    override fun enterAsGuest() {
        val guest = User(
            id = "guest",
            email = "guest@viasit.com",
            name = "Invitado",
            role = UserRole.invitado.name
        )
        saveUser(guest, isGuest = true)
        _authState.value = AuthState.Authenticated(guest)
    }

    override fun logout() {
        _isInService.value = false
        stopLocationTrackingService()
        pocketBaseAuth.logout()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    override fun setInService(inService: Boolean, autoId: String) {
        _isInService.value = inService
        val current = getCurrentUser()
        current?.let {
            prefs.edit().putBoolean("isInService", inService).apply()
            if (inService) {
                startLocationTrackingService(it.id, it.name, it.userRole, autoId)
            } else {
                stopLocationTrackingService()
            }
        }
    }

    private fun startLocationTrackingService(userId: String, userName: String, userRole: UserRole, autoId: String) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra("userId", userId)
            putExtra("userName", userName)
            putExtra("userRole", userRole.name)
            putExtra("isInService", true)
            putExtra("autoId", autoId)
        }
        context.startForegroundService(intent)
    }

    private fun stopLocationTrackingService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun getUserId(): String? = prefs.getString("userId", null)
    override fun isLoggedIn(): Boolean = prefs.getBoolean("loggedIn", false)
    override fun isGuestMode(): Boolean = prefs.getBoolean("isGuest", false)
    override fun isDriver(): Boolean = prefs.getString("role", null) == UserRole.conductor.name
    override fun getUserRole(): UserRole {
        val roleStr = prefs.getString("role", UserRole.usuario.name) ?: UserRole.usuario.name
        return try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.usuario }
    }
    override fun getAuthToken(): String? = pocketBaseAuth.getAuthToken()

    override fun restoreSession() {
        val user = getCurrentUser()
        if (user == null) {
            prefs.edit().clear().apply()
            return
        }
        when {
            isGuestMode() -> _authState.value = AuthState.Authenticated(user)
            isLoggedIn()  -> _authState.value = AuthState.Authenticated(user)
        }
    }

    override fun getCurrentUser(): User? {
        if (!isLoggedIn() && !isGuestMode()) return null
        val roleStr = prefs.getString("role", UserRole.usuario.name) ?: UserRole.usuario.name
        val user = User(
            id = prefs.getString("userId", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            name = prefs.getString("name", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            role = roleStr,
            lineaId = prefs.getString("lineaId", "") ?: "",
            lineName = prefs.getString("lineName", "") ?: "",
            token = prefs.getString("token", "") ?: ""
        )
        android.util.Log.d("AuthRepository", "getCurrentUser restaurado: ${user.name}, LineaId: ${user.lineaId}, LineName: ${user.lineName}")
        return user
    }

    private fun saveUser(user: User, isGuest: Boolean) {
        android.util.Log.d("AuthRepository", "Guardando usuario en SharedPreferences: ${user.name}, LineaId: ${user.lineaId}, LineName: ${user.lineName}")
        prefs.edit().apply {
            putString("userId", user.id)
            putString("email", user.email)
            putString("name", user.name)
            putString("phone", user.phone)
            putString("role", user.role)
            putString("lineaId", user.lineaId)
            putString("lineName", user.lineName)
            putString("token", user.token)
            putBoolean("loggedIn", !isGuest)
            putBoolean("isGuest", isGuest)
            apply()
        }
    }
}
