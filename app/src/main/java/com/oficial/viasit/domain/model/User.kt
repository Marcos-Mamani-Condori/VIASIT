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
    val role: List<String> = listOf(UserRole.usuario.name),
    
    @SerialName("phone")
    val phone: String = "",
    
    @SerialName("lineId")
    val lineaId: String = "",
    
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
            if (role.isNotEmpty()) {
                UserRole.valueOf(role.first())
            } else {
                UserRole.usuario
            }
        } catch (e: Exception) {
            UserRole.usuario
        }
    
    val isDriver: Boolean
        get() = role.contains(UserRole.conductor.name)
    
    val isGuest: Boolean
        get() = role.contains(UserRole.invitado.name)
    
    val canSendLocation: Boolean
        get() = role.contains(UserRole.usuario.name) || role.contains(UserRole.conductor.name)
    
    val canToggleService: Boolean
        get() = role.contains(UserRole.conductor.name)
    
    val isAdmin: Boolean
        get() = role.contains(UserRole.ADMIN_LINEA.name) || role.contains(UserRole.ADMIN_PRINCIPAL.name)
    
    val isAdminPrincipal: Boolean
        get() = role.contains(UserRole.ADMIN_PRINCIPAL.name)
    
    val canManageLines: Boolean
        get() = role.contains(UserRole.ADMIN_PRINCIPAL.name)
}

@Serializable
sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    @SerialName("authenticated")
    data class Authenticated(val user: User) : AuthState()
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
