package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.data.repository.AuthRepository
import com.oficial.viasit.domain.usecases.LoginUseCase
import com.oficial.viasit.domain.usecases.RegisterUseCase
import com.oficial.viasit.domain.usecases.AuthValidators
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.domain.model.LoginFormState
import com.oficial.viasit.domain.model.RegisterFormState
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
        val form = AuthValidators.validateLogin(_loginForm.value)
        _loginForm.value = form
        if (form.emailError != null || form.passwordError != null) return

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
        val form = AuthValidators.validateRegister(_registerForm.value)
        _registerForm.value = form
        if (form.emailError != null || form.passwordError != null || form.confirmPasswordError != null || form.nameError != null || form.error != null) return

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
    val isInService: StateFlow<Boolean> = authRepository.isInService
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
    fun isGuest(): Boolean = authRepository.isGuestMode()
    fun isDriver(): Boolean = authRepository.isDriver()
    fun getUserRole(): UserRole = authRepository.getUserRole()
    fun getUserId(): String? = authRepository.getUserId()

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
