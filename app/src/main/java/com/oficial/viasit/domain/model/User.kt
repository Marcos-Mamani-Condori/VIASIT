package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User roles in the VIASIT application
 */
enum class UserRole {
    INVITADO,   // Invitado - puede ver buses en el mapa
    USUARIO,    // Usuario - puede enviar su ubicación
    CONDUCTOR   // Conductor - puede activar "en servicio" y enviar ubicación
}

/**
 * Domain model for authenticated users
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String = "",
    
    @SerialName("email")
    val email: String = "",
    
    @SerialName("name")
    val name: String = "",
    
    @SerialName("role")
    val role: String = UserRole.USUARIO.name,
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("avatar")
    val avatar: String = "",
    
    @SerialName("isInService")
    val isInService: Boolean = false,
    
    @SerialName("isActive")
    val isActive: Boolean = true,
    
    @SerialName("created")
    val created: String = "",
    
    @SerialName("updated")
    val updated: String = ""
) {
    val userRole: UserRole
        get() = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.USUARIO
        }
    
    val isDriver: Boolean
        get() = userRole == UserRole.CONDUCTOR
    
    val isGuest: Boolean
        get() = userRole == UserRole.INVITADO
    
    val canSendLocation: Boolean
        get() = userRole == UserRole.USUARIO || userRole == UserRole.CONDUCTOR
    
    val canToggleService: Boolean
        get() = userRole == UserRole.CONDUCTOR
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
    val userRole: String = UserRole.USUARIO.name
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
    
    @SerialName("name")
    val name: String,
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("role")
    val role: String = "USUARIO"
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
