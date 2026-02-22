package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cliente de autenticación para PocketBase
 */
class PocketBaseAuthClient {
    companion object {
        private const val TAG = "PocketBaseAuth"
        private val BASE_URL: String = BuildConfig.POCKETBASE_URL
    }

    private val okHttpclient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Construir string JSON desde parámetros
     */
    private fun buildJsonString(
        email: String,
        password: String,
        passwordConfirm: String,
        name: String,
        phone: String,
        role: String,
        invitationCode: String = "",
        lineaId: String = ""
    ): String {
        val map = mutableMapOf<String, Any>(
            "email" to email,
            "password" to password,
            "passwordConfirm" to passwordConfirm,
            "name" to name,
            "phone" to phone,
            "role" to listOf(role)  // PocketBase espera un array de roles
        )
        if (invitationCode.isNotEmpty()) {
            map["invitationCode"] = invitationCode
        }
        if (lineaId.isNotEmpty()) {
            map["lineaId"] = lineaId  // PocketBase users collection usa camelCase
            Log.d(TAG, "Agregando lineaId al JSON: $lineaId")
        }
        
        val jsonString = map.entries.joinToString(",", "{", "}") { (key, value) ->
            "\"$key\": ${when (value) {
                is String -> "\"$value\""
                is List<*> -> "[${value.joinToString(",") { "\"$it\"" }}]"
                is Boolean -> value.toString()
                else -> value
            }}"
        }
        Log.d(TAG, "JSON construido: $jsonString")
        return jsonString
    }

    private var authToken: String? = null

    /**
     * Login con email y password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """
                    {
                        "identity": "$email",
                        "password": "$password"
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/auth-with-password")
                    .post(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    android.util.Log.d("PocketBaseAuth", "Login response: $body")
                    val authResponse = json.decodeFromString<AuthResponse>(body ?: "")
                    authToken = authResponse.token
                    
                    val user = User(
                        id = authResponse.user.id,
                        email = authResponse.user.email,
                        name = authResponse.user.name,
                        phone = authResponse.user.phone,
                        role = authResponse.user.role,
                        lineaId = authResponse.user.lineaId
                    )
                    Log.d(TAG, "Login exitoso: ${user.email}, role: ${user.role}, lineaId: ${user.lineaId}")
                    Result.success(user)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error login: ${response.code} - $error")
                    Result.failure(Exception("Error login: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error login", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Register con datos de usuario
     */
    suspend fun register(
        email: String,
        password: String,
        passwordConfirm: String,
        name: String,
        phone: String = "",
        role: String = "usuario"
    ): Result<User> {
        return registerAdmin(email, password, passwordConfirm, name, phone, role, "")
    }

    /**
     * Register de administrador con código de invitación
     */
    suspend fun registerAdmin(
        email: String,
        password: String,
        passwordConfirm: String,
        name: String,
        phone: String = "",
        role: String = "ADMIN_LINEA",
        invitationCode: String = "",
        lineaId: String = ""
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Construir JSON
                val jsonBody = buildJsonString(
                    email = email,
                    password = password,
                    passwordConfirm = passwordConfirm,
                    name = name,
                    phone = phone,
                    role = role,
                    invitationCode = invitationCode,
                    lineaId = lineaId
                )
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/records")
                    .post(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val userResponse = json.decodeFromString<UserResponse>(body ?: "")
                    
                    val user = User(
                        id = userResponse.id,
                        email = userResponse.email,
                        name = userResponse.name,
                        phone = userResponse.phone,
                        role = userResponse.role,
                        lineaId = userResponse.lineaId
                    )
                    Log.d(TAG, "Register exitoso: ${user.email}, lineaId: ${user.lineaId}")
                    Result.success(user)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error register: ${response.code} - $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error register", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener usuario actual
     */
    suspend fun getCurrentUser(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authToken ?: return@withContext Result.failure(Exception("No hay token"))

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/auth-refresh")
                    .post("".toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val authResponse = json.decodeFromString<AuthResponse>(body ?: "")
                    
                    val user = User(
                        id = authResponse.user.id,
                        email = authResponse.user.email,
                        name = authResponse.user.name,
                        phone = authResponse.user.phone,
                        role = authResponse.user.role
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception("Error al obtener usuario: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getCurrentUser", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Logout
     */
    fun logout() {
        authToken = null
        Log.d(TAG, "Logout exitoso")
    }

    /**
     * Verificar si está autenticado
     */
    fun isAuthenticated(): Boolean = authToken != null

    fun getAuthToken(): String? = authToken
}

/**
 * Respuesta de autenticación de PocketBase
 */
@Serializable
data class AuthResponse(
    @SerialName("token")
    val token: String = "",
    
    @SerialName("record")
    val user: UserResponse = UserResponse()
)

/**
 * Respuesta de usuario de PocketBase
 */
@Serializable
data class UserResponse(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val role: List<String> = listOf("usuario"),  // PocketBase devuelve como array
    val lineaId: String = "",  // PocketBase users usa camelCase, no snake_case
    val created: String = "",
    val updated: String = ""
)
