package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cliente de autenticación para PocketBase.
 *
 * FIX: buildJsonString() eliminada. Ahora se usan clases serializables con
 * kotlinx.serialization para construir los cuerpos JSON correctamente, escapando
 * cualquier carácter especial en contraseñas/emails.
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

    private var authToken: String? = null

    // ============ Clases de request serializables ============

    @Serializable
    private data class LoginRequest(
        val identity: String,
        val password: String
    )

    @Serializable
    private data class RegisterRequest(
        val email: String,
        val password: String,
        val passwordConfirm: String,
        val name: String,
        val phone: String,
        val role: List<String>,
        val lineaId: String? = null
    )

    // ============ Auth methods ============

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val body = json.encodeToString(LoginRequest(email, password))
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/auth-with-password")
                    .post(body)
                    .build()

                val response = okHttpclient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Login response: $responseBody")
                    val authResponse = json.decodeFromString<AuthResponse>(responseBody)
                    authToken = authResponse.token
                    val user = authResponse.user.toDomain()
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

    suspend fun register(
        email: String,
        password: String,
        passwordConfirm: String,
        name: String,
        phone: String = "",
        role: String = "usuario",
        lineaId: String = ""
    ): Result<User> = registerAdmin(email, password, passwordConfirm, name, phone, role, "", lineaId)

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
                val reqObj = RegisterRequest(
                    email = email,
                    password = password,
                    passwordConfirm = passwordConfirm,
                    name = name,
                    phone = phone,
                    role = listOf(role),
                    lineaId = lineaId.ifEmpty { null }
                )
                val body = json.encodeToString(reqObj).toRequestBody("application/json".toMediaType())
                
                Log.d(TAG, "Registrando usuario: email=$email, name=$name, role=$role, lineaId=$lineaId")

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/records")
                    .post(body)
                    .build()

                val response = okHttpclient.newCall(request).execute()
                if (response.isSuccessful) {
                    val userResponse = json.decodeFromString<UserResponse>(response.body?.string() ?: "")
                    val user = userResponse.toDomain()
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
                    val authResponse = json.decodeFromString<AuthResponse>(response.body?.string() ?: "")
                    Result.success(authResponse.user.toDomain())
                } else {
                    Result.failure(Exception("Error al obtener usuario: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getCurrentUser", e)
                Result.failure(e)
            }
        }
    }

    fun logout() {
        authToken = null
        Log.d(TAG, "Logout exitoso")
    }

    fun isAuthenticated(): Boolean = authToken != null
    fun getAuthToken(): String? = authToken
}

// ============ Modelos de respuesta ============

@Serializable
data class AuthResponse(
    @SerialName("token") val token: String = "",
    @SerialName("record") val user: UserResponse = UserResponse()
)

@Serializable
data class UserResponse(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val role: List<String> = listOf("usuario"),
    val lineaId: String? = null,
    val created: String = "",
    val updated: String = ""
) {
    fun toDomain() = com.oficial.viasit.domain.model.User(
        id = id,
        email = email,
        name = name,
        phone = phone,
        role = role,
        lineaId = lineaId ?: ""
    )
}
