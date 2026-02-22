package com.oficial.viasit.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.oficial.viasit.data.remote.PocketBaseAuthClient
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.service.LocationTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState
    private val context: Context = context.applicationContext

    // Estado local de "en servicio" (no se guarda en PocketBase, solo local)
    private var _isInService: Boolean = false

    private val pocketBaseAuth = PocketBaseAuthClient()

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _authState.value = AuthState.Loading

            android.util.Log.d("AuthRepository", "login() llamado con: $email")
            val result = pocketBaseAuth.login(email, password)

            result.fold(
                onSuccess = { user ->
                    android.util.Log.d("AuthRepository", "login onSuccess - user: ${user.email}, role: ${user.role}")
                    saveUser(user, isGuest = false)
                    _authState.value = AuthState.Authenticated(user)
                    Result.success(user)
                },
                onFailure = { error ->
                    android.util.Log.d("AuthRepository", "login onFailure - ${error.message}")
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
                // Nota: invitationCode no se envía al registrar usuarios normales (usuario/conductor)
                // Solo se usa en RegisterAdminScreen para ADMIN_LINEA
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
            role = listOf(UserRole.invitado.name)
        )
        saveUser(guest, isGuest = true)
        _authState.value = AuthState.Authenticated(guest)
    }

    fun logout() {
        // Detener servicio de tracking
        _isInService = false
        stopLocationTrackingService()
        pocketBaseAuth.logout()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun setInService(isInService: Boolean, autoId: String = "") {
        _isInService = isInService
        val current = getCurrentUser()
        current?.let {
            android.util.Log.d("AuthRepository", "setInService: $isInService, autoId: $autoId")
            prefs.edit().putBoolean("isInService", isInService).apply()

            // Iniciar o detener el servicio de tracking
            if (isInService) {
                startLocationTrackingService(
                    userId = it.id,
                    userName = it.name,
                    userRole = it.userRole,
                    autoId = autoId
                )
            } else {
                stopLocationTrackingService()
            }
        }
    }

    private fun startLocationTrackingService(
        userId: String,
        userName: String,
        userRole: UserRole,
        autoId: String
    ) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra("userId", userId)
            putExtra("userName", userName)
            putExtra("userRole", userRole.name)
            putExtra("isInService", true)
            putExtra("autoId", autoId)
        }
        context.startForegroundService(intent)
        android.util.Log.d("AuthRepository", "Iniciando servicio de tracking para usuario: $userName (role: $userRole)")
    }

    private fun stopLocationTrackingService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
        android.util.Log.d("AuthRepository", "Deteniendo servicio de tracking")
    }

    fun getUserId(): String? = prefs.getString("userId", null)
    fun isLoggedIn(): Boolean = prefs.getBoolean("loggedIn", false)
    fun isGuestMode(): Boolean = prefs.getBoolean("isGuest", false)
    fun isDriver(): Boolean = prefs.getString("role", null) == UserRole.conductor.name
    fun getUserRole(): UserRole {
        val roleStr = prefs.getString("role", UserRole.usuario.name) ?: UserRole.usuario.name
        return try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.usuario
        }
    }
    fun isInService(): Boolean = prefs.getBoolean("isInService", false)

    fun restoreSession() {
        val user = getCurrentUser()
        android.util.Log.d("AuthRepository", "restoreSession - isLoggedIn: ${isLoggedIn()}, isGuest: ${isGuestMode()}")
        android.util.Log.d("AuthRepository", "restoreSession - user: ${user?.email}, role: ${user?.role}")
        when {
            isGuestMode() -> _authState.value = AuthState.Authenticated(user!!)
            isLoggedIn() -> _authState.value = AuthState.Authenticated(user!!)
        }
    }

    fun getCurrentUser(): User? {
        if (!isLoggedIn() && !isGuestMode()) return null
        val roleStr = prefs.getString("role", UserRole.usuario.name) ?: UserRole.usuario.name
        return User(
            id = prefs.getString("userId", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            name = prefs.getString("name", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            role = listOf(roleStr),
            lineaId = prefs.getString("lineaId", "") ?: ""
        )
    }

    private fun saveUser(user: User, isGuest: Boolean) {
        prefs.edit().apply {
            putString("userId", user.id)
            putString("email", user.email)
            putString("name", user.name)
            putString("phone", user.phone)
            // Guardar el primer rol de la lista
            putString("role", user.role.firstOrNull() ?: UserRole.usuario.name)
            putString("lineaId", user.lineaId)
            putBoolean("loggedIn", !isGuest)
            putBoolean("isGuest", isGuest)
            apply()
        }
    }
}
