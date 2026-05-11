package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.domain.usecases.*
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
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val enterAsGuestUseCase: EnterAsGuestUseCase,
    private val setDriverInServiceUseCase: SetDriverInServiceUseCase,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val getIsInServiceUseCase: GetIsInServiceUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase
) : ViewModel() {

    val authState: StateFlow<AuthState> = getAuthStateUseCase()
    val isInService: StateFlow<Boolean> = getIsInServiceUseCase()

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    init { restoreSessionUseCase() }

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
                onSuccess = { _loginForm.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _loginForm.update { it.copy(isLoading = false, error = e.message) } }
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
                onSuccess = { _registerForm.update { it.copy(isLoading = false, isSuccess = true) } },
                onFailure = { e -> _registerForm.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun enterAsGuest() { enterAsGuestUseCase() }
    fun logout() { logoutUseCase() }
    fun setInService(isInService: Boolean, autoId: String = "") { setDriverInServiceUseCase(isInService, autoId) }

    fun isLoggedIn(): Boolean = authState.value is AuthState.Authenticated
    fun isGuest(): Boolean = (authState.value as? AuthState.Authenticated)?.user?.isGuest == true
    fun isDriver(): Boolean = (authState.value as? AuthState.Authenticated)?.user?.userRole == UserRole.conductor
    fun getUserRole(): UserRole = (authState.value as? AuthState.Authenticated)?.user?.userRole ?: UserRole.usuario
    fun getUserId(): String? = (authState.value as? AuthState.Authenticated)?.user?.id
    fun getAuthToken(): String? = (authState.value as? AuthState.Authenticated)?.user?.token

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as com.oficial.viasit.AutosAplicacion
                AuthViewModel(
                    loginUseCase = app.loginUseCase,
                    registerUseCase = app.registerUseCase,
                    logoutUseCase = app.logoutUseCase,
                    enterAsGuestUseCase = app.enterAsGuestUseCase,
                    setDriverInServiceUseCase = app.setDriverInServiceUseCase,
                    getAuthStateUseCase = app.getAuthStateUseCase,
                    getIsInServiceUseCase = app.getIsInServiceUseCase,
                    restoreSessionUseCase = app.restoreSessionUseCase
                )
            }
        }
    }
}
