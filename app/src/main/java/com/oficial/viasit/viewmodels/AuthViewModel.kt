package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.data.repository.AuthRepository
import com.oficial.viasit.domain.usecases.LoginUseCase
import com.oficial.viasit.domain.usecases.RegisterUseCase
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.authState

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    init { authRepository.restoreSession() }

    fun updateLoginEmail(email: String) { _loginForm.update { it.copy(email = email) } }
    fun updateLoginPassword(password: String) { _loginForm.update { it.copy(password = password) } }
    fun updateRegisterEmail(email: String) { _registerForm.update { it.copy(email = email) } }
    fun updateRegisterPassword(password: String) { _registerForm.update { it.copy(password = password) } }
    fun updateRegisterConfirmPassword(cp: String) { _registerForm.update { it.copy(confirmPassword = cp) } }
    fun updateRegisterName(name: String) { _registerForm.update { it.copy(name = name) } }
    fun updateRegisterPhone(phone: String) { _registerForm.update { it.copy(phone = phone) } }
    fun updateRegisterRole(role: String) { _registerForm.update { it.copy(role = role) } }
    fun updateRegisterInvitationCode(code: String) { _registerForm.update { it.copy(invitationCode = code) } }

    fun login() {
        val form = _loginForm.value
        if (!validateLogin(form)) return
        _loginForm.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = loginUseCase(form.email, form.password)
            result.fold(
                onSuccess = { user ->
                    _loginForm.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { e ->
                    _loginForm.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun register() {
        val form = _registerForm.value
        if (!validateRegister(form)) return
        _registerForm.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = registerUseCase(RegisterRequest(
                email = form.email,
                password = form.password,
                passwordConfirm = form.confirmPassword,
                name = form.name,
                phone = form.phone,
                role = form.role,
                invitationCode = form.invitationCode
            ))
            result.fold(
                onSuccess = { user ->
                    _registerForm.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { e ->
                    _registerForm.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun enterAsGuest() { authRepository.enterAsGuest() }
    fun logout() { authRepository.logout() }
    fun setInService(isInService: Boolean, autoId: String = "") { authRepository.setInService(isInService, autoId) }
    // StateFlow reactivo — no lee disco en cada recomposición
    val isInService: StateFlow<Boolean> = authRepository.isInService
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
    fun isGuest(): Boolean = authRepository.isGuestMode()
    fun isDriver(): Boolean = authRepository.isDriver()
    fun getUserRole(): UserRole = authRepository.getUserRole()
    fun getUserId(): String? = authRepository.getUserId()

    private fun validateLogin(form: LoginFormState): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            form.email.isBlank() -> { _loginForm.update { it.copy(emailError = "Email requerido") }; false }
            !emailRegex.matches(form.email) -> { _loginForm.update { it.copy(emailError = "Email invalido") }; false }
            form.password.isBlank() -> { _loginForm.update { it.copy(passwordError = "Contrasena requerida") }; false }
            form.password.length < 6 -> { _loginForm.update { it.copy(passwordError = "Minimo 6 caracteres") }; false }
            else -> { _loginForm.update { it.copy(emailError = null, passwordError = null) }; true }
        }
    }

    private fun validateRegister(form: RegisterFormState): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            form.email.isBlank() -> { _registerForm.update { it.copy(emailError = "Email requerido") }; false }
            !emailRegex.matches(form.email) -> { _registerForm.update { it.copy(emailError = "Email invalido") }; false }
            form.password.isBlank() -> { _registerForm.update { it.copy(passwordError = "Contrasena requerida") }; false }
            form.password.length < 8 -> { _registerForm.update { it.copy(passwordError = "Minimo 8 caracteres") }; false }
            form.confirmPassword != form.password -> { _registerForm.update { it.copy(confirmPasswordError = "No coinciden") }; false }
            form.name.isBlank() -> { _registerForm.update { it.copy(nameError = "Nombre requerido") }; false }
            form.role == "conductor" && form.invitationCode.isBlank() -> {
                _registerForm.update { it.copy(error = "Los conductores necesitan un código de invitación") }; false
            }
            else -> {
                _registerForm.update {
                    it.copy(emailError = null, passwordError = null, confirmPasswordError = null, nameError = null, error = null)
                }
                true
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = com.oficial.viasit.AutosAplicacion.instance
                return AuthViewModel(
                    authRepository = app.authRepository,
                    loginUseCase = app.loginUseCase,
                    registerUseCase = app.registerUseCase
                ) as T
            }
        }
    }
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class RegisterFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "usuario",
    val invitationCode: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
