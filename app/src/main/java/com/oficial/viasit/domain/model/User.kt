package com.oficial.viasit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole {
    invitado,
    usuario,
    conductor,
    ADMIN_LINEA,
    ADMIN_PRINCIPAL
}

@Serializable
data class User(
    @SerialName("id")
    val id: String = "",
    
    @SerialName("email")
    val email: String = "",
    
    @SerialName("name")
    val name: String = "",
    
    @SerialName("role")
    val role: String = UserRole.usuario.name,
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("lineId")
    val lineaId: String = "",

    @SerialName("lineName")
    val lineName: String = "",
    
    @SerialName("token")
    val token: String = "",

    @SerialName("active")
    val active: Boolean = true,

    @SerialName("created")
    val created: String = "",
    
    @SerialName("updated")
    val updated: String = "",
    
    @SerialName("collectionId")
    val collectionId: String = "",
    
    @SerialName("collectionName")
    val collectionName: String = "users"
) {
    val userRole: UserRole
        get() = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.usuario
        }
    
    val isDriver: Boolean
        get() = role.equals(UserRole.conductor.name, ignoreCase = true) || role.equals("DRIVER", ignoreCase = true)
    
    val isGuest: Boolean
        get() = role.equals(UserRole.invitado.name, ignoreCase = true)
    
    val canSendLocation: Boolean
        get() = role.equals(UserRole.usuario.name, ignoreCase = true) || role.equals(UserRole.conductor.name, ignoreCase = true)
    
    val canToggleService: Boolean
        get() = role.equals(UserRole.conductor.name, ignoreCase = true)
    
    val isAdmin: Boolean
        get() = role.equals(UserRole.ADMIN_LINEA.name, ignoreCase = true) || role.equals(UserRole.ADMIN_PRINCIPAL.name, ignoreCase = true)
    
    val isAdminPrincipal: Boolean
        get() = role.equals(UserRole.ADMIN_PRINCIPAL.name, ignoreCase = true)
    
    val canManageLines: Boolean
        get() = role.equals(UserRole.ADMIN_PRINCIPAL.name, ignoreCase = true)
}

@Serializable
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    @SerialName("authenticated")
    data class Authenticated(val user: User) : AuthState()
    data class Suspended(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Serializable
data class LoginRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String
)

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
    val role: String = "usuario",
    
    @SerialName("invitationCode")
    val invitationCode: String = "",

    @SerialName("lineId")
    val lineaId: String = ""
)

@Serializable
data class AuthResponse(
    @SerialName("token")
    val token: String = "",
    
    @SerialName("user")
    val user: User = User()
)

@Serializable
data class AdminRegisterRequest(
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
    val role: String = "ADMIN_LINEA",
    
    @SerialName("invitationCode")
    val invitationCode: String
)
