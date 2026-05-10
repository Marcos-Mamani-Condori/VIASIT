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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState
    private val context: Context = context.applicationContext

    private val _isInService = MutableStateFlow(prefs.getBoolean("isInService", false))
    val isInService: StateFlow<Boolean> = _isInService

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

            var lineaId = ""
            if (request.role == "conductor" && request.invitationCode.isNotBlank()) {
                val lineaResult = validateVehicleInvitationCode(request.invitationCode)
                if (lineaResult == null) {
                    _authState.value = AuthState.Error("Código de invitación inválido o expirado")
                    return@withContext Result.failure(Exception("Código de invitación inválido o expirado"))
                }
                lineaId = lineaResult
            }

            val result = pocketBaseAuth.register(
                email = request.email,
                password = request.password,
                passwordConfirm = request.passwordConfirm,
                name = request.name,
                phone = request.phone,
                role = request.role,
                lineaId = lineaId
            )

            result.fold(
                onSuccess = { user ->
                    if (lineaId.isNotEmpty()) {
                        markVehicleCodeAsUsed(request.invitationCode, request.email)
                    }
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

    private val httpClient by lazy { com.oficial.viasit.data.remote.PocketBaseHttpClient.create() }

    private suspend fun validateVehicleInvitationCode(code: String): String? {
        return try {
            val filter = "code=\"$code\" && isUsed=false"
            android.util.Log.d("AuthRepository", "Validando código: $code con filtro: $filter")
            
            val result = httpClient.getList(
                collection = "vehicle_invitation_codes",
                perPage = 1,
                filter = filter
            )
            
            result.fold(
                onSuccess = { body ->
                    android.util.Log.d("AuthRepository", "Respuesta del servidor: $body")
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val obj = json.decodeFromString<kotlinx.serialization.json.JsonObject>(body)
                    val items = obj["items"]?.jsonArray ?: return@fold null
                    if (items.isEmpty()) {
                        android.util.Log.w("AuthRepository", "No se encontraron códigos con ese filtro")
                        return@fold null
                    }
                    
                    val inv = items[0].jsonObject
                    android.util.Log.d("AuthRepository", "Código encontrado: ${inv}")
                    
                    val expiresAt = inv["expiresAt"]?.jsonPrimitive?.content
                    
                    val lineaId = try {
                        val lineaElement = inv["lineId"]
                        when {
                            lineaElement == null -> {
                                android.util.Log.w("AuthRepository", "linea_id es null en el JSON")
                                null
                            }
                            lineaElement.jsonPrimitive.isString -> {
                                lineaElement.jsonPrimitive.content.also {
                                    android.util.Log.d("AuthRepository", "linea_id como string: $it")
                                }
                            }
                            else -> {
                                lineaElement.jsonObject["id"]?.jsonPrimitive?.content.also {
                                    android.util.Log.d("AuthRepository", "linea_id como objeto, ID: $it")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Error extrayendo linea_id: ${e.message}")
                        null
                    }
                    
                    android.util.Log.d("AuthRepository", "expiresAt: $expiresAt, lineaId: $lineaId")
                    
                    if (lineaId == null) {
                        android.util.Log.w("AuthRepository", "linea_id es null")
                        return@fold null
                    }
                    
                    if (expiresAt != null && expiresAt.isNotEmpty()) {
                        try {
                            val exp = java.time.OffsetDateTime.parse(expiresAt).toInstant()
                            if (exp.isBefore(java.time.Instant.now())) {
                                android.util.Log.w("AuthRepository", "Código expirado: $expiresAt")
                                return@fold null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AuthRepository", "Error parseando fecha: $expiresAt", e)
                        }
                    }
                    
                    android.util.Log.d("AuthRepository", "Código válido, lineaId: $lineaId")
                    lineaId
                },
                onFailure = { error ->
                    android.util.Log.e("AuthRepository", "Error en la petición", error)
                    null
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error validando código de vehículo", e)
            null
        }
    }

    private suspend fun markVehicleCodeAsUsed(code: String, userEmail: String) {
        try {
            val filter = "code=\"$code\""
            android.util.Log.d("AuthRepository", "Marcando código como usado: $code")
            
            val result = httpClient.getList(
                collection = "vehicle_invitation_codes",
                perPage = 1,
                filter = filter
            )
            
            result.fold(
                onSuccess = { body ->
                    android.util.Log.d("AuthRepository", "Respuesta: $body")
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val obj = json.decodeFromString<kotlinx.serialization.json.JsonObject>(body)
                    val items = obj["items"]?.jsonArray ?: return@fold
                    if (items.isEmpty()) {
                        android.util.Log.w("AuthRepository", "No se encontró el código para marcar")
                        return@fold
                    }
                    
                    val codeId = items[0].jsonObject["id"]?.jsonPrimitive?.content ?: return@fold
                    android.util.Log.d("AuthRepository", "ID del código: $codeId")
                    
                    val updateResult = httpClient.updateRecord(
                        collection = "vehicle_invitation_codes",
                        recordId = codeId,
                        data = mapOf("isUsed" to true, "usedBy" to userEmail)
                    )
                    android.util.Log.d("AuthRepository", "Código marcado como usado: $updateResult")
                },
                onFailure = { error ->
                    android.util.Log.e("AuthRepository", "Error buscando código: ${error.message}")
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error marcando código como usado", e)
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
        _isInService.value = false
        stopLocationTrackingService()
        pocketBaseAuth.logout()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun setInService(inService: Boolean, autoId: String = "") {
        _isInService.value = inService
        val current = getCurrentUser()
        current?.let {
            android.util.Log.d("AuthRepository", "setInService: $inService, autoId: $autoId")
            prefs.edit().putBoolean("isInService", inService).apply()

            if (inService) {
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
    fun getAuthToken(): String? = pocketBaseAuth.getAuthToken()

    fun restoreSession() {
        val user = getCurrentUser()
        android.util.Log.d("AuthRepository", "restoreSession - isLoggedIn: ${isLoggedIn()}, isGuest: ${isGuestMode()}")
        android.util.Log.d("AuthRepository", "restoreSession - user: ${user?.email}, role: ${user?.role}")
        if (user == null) {
            android.util.Log.w("AuthRepository", "restoreSession: user es null, limpiando sesión corrupta")
            prefs.edit().clear().apply()
            return
        }
        when {
            isGuestMode() -> _authState.value = AuthState.Authenticated(user)
            isLoggedIn()  -> _authState.value = AuthState.Authenticated(user)
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
            putString("role", user.role.firstOrNull() ?: UserRole.usuario.name)
            putString("lineaId", user.lineaId)
            putBoolean("loggedIn", !isGuest)
            putBoolean("isGuest", isGuest)
            apply()
        }
    }
}
