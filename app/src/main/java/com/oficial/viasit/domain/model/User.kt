package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User roles in the VIASIT application
 * IMPORTANTE: Los valores deben coincidir exactamente con PocketBase schema
 * Schema: maxSelect: 2, values: ["conductor", "usuario", "invitado", "ADMIN_PRINCIPAL", "ADMIN_LINEA"]
 */
enum class UserRole {
    invitado,       // Invitado - puede ver buses en el mapa
    usuario,        // Usuario - puede enviar su ubicación
    conductor,      // Conductor - puede activar "en servicio" y enviar ubicación
    ADMIN_LINEA,    // Admin de Línea - gestiona una línea específica
    ADMIN_PRINCIPAL // Admin Principal - gestiona todas las líneas
}

/**
 * Modelo de datos para Usuarios
 * Colección PocketBase: users (auth collection)
 * 
 * Schema:
 * - phone: text
 * - role: select (maxSelect: 2)
 * - lineaId: relation a lineas (maxSelect: 1)
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String = "",
    
    @SerialName("email")
    val email: String = "",
    
    @SerialName("username")
    val name: String = "",
    
    // PocketBase devuelve role como array con maxSelect: 2
    @SerialName("role")
    val role: List<String> = listOf(UserRole.usuario.name),
    
    // Teléfono del usuario
    @SerialName("phone")
    val phone: String = "",
    
    // Línea asignada (relation a lineas) - para ADMIN_LINEA y conductores
    // En PocketBase el campo se llama "lineaId"
    @SerialName("lineaId")
    val lineaId: String = "",
    
    @SerialName("created")
    val created: String = "",
    
    @SerialName("updated")
    val updated: String = "",
    
    @SerialName("collectionId")
    val collectionId: String = "",
    
    @SerialName("collectionName")
    val collectionName: String = "users"
) {
    // Obtener el rol principal (primer elemento de la lista)
    val userRole: UserRole
        get() = try {
            if (role.isNotEmpty()) {
                UserRole.valueOf(role.first())
            } else {
                UserRole.usuario
            }
        } catch (e: Exception) {
            UserRole.usuario
        }
    
    val isDriver: Boolean
        get() = userRole == UserRole.conductor
    
    val isGuest: Boolean
        get() = userRole == UserRole.invitado
    
    val canSendLocation: Boolean
        get() = userRole == UserRole.usuario || userRole == UserRole.conductor
    
    val canToggleService: Boolean
        get() = userRole == UserRole.conductor
    
    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN_LINEA || userRole == UserRole.ADMIN_PRINCIPAL
    
    val isAdminPrincipal: Boolean
        get() = userRole == UserRole.ADMIN_PRINCIPAL
    
    val canManageLines: Boolean
        get() = userRole == UserRole.ADMIN_PRINCIPAL
}

/**
 * Domain model for user location
 */
@Serializable
data class UserLocation(
    @SerialName("id")
    val id: String = "",
    
    @SerialName("userId")
    val userId: String = "",
    
    @SerialName("userName")
    val userName: String = "",
    
    @SerialName("lat")
    val lat: Double = 0.0,
    
    @SerialName("lng")
    val lng: Double = 0.0,
    
    @SerialName("accuracy")
    val accuracy: Float = 0f,
    
    @SerialName("altitude")
    val altitude: Double = 0.0,
    
    @SerialName("speed")
    val speed: Float = 0f,
    
    @SerialName("bearing")
    val bearing: Double = 0.0,
    
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerialName("isInService")
    val isInService: Boolean = false,
    
    @SerialName("userRole")
    val userRole: String = UserRole.usuario.name
)

/**
 * Authentication state
 */
@Serializable
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    @SerialName("authenticated")
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Login request
 */
@Serializable
data class LoginRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String
)

/**
 * Register request
 */
@Serializable
data class RegisterRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String,
    
    @SerialName("passwordConfirm")
    val passwordConfirm: String,
    
    @SerialName("username")
    val name: String,
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("role")
    val role: String = "usuario",
    
    @SerialName("invitationCode")
    val invitationCode: String = ""
)

/**
 * Auth response from PocketBase
 */
@Serializable
data class AuthResponse(
    @SerialName("token")
    val token: String = "",
    
    @SerialName("user")
    val user: User = User()
)

/**
 * Admin registration request with invitation code
 */
@Serializable
data class AdminRegisterRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String,
    
    @SerialName("passwordConfirm")
    val passwordConfirm: String,
    
    @SerialName("username")
    val name: String,
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("role")
    val role: String = "ADMIN_LINEA",
    
    @SerialName("invitationCode")
    val invitationCode: String
)
