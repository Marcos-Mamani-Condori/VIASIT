package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.domain.model.RegisterRequest
import com.oficial.viasit.domain.usecases.RegisterUseCase
import com.oficial.viasit.domain.usecases.ValidateInvitationCodeUseCase
import com.oficial.viasit.domain.usecases.UseInvitationCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterAdminViewModel(
    private val registerUseCase: RegisterUseCase,
    private val validateInvitationCodeUseCase: ValidateInvitationCodeUseCase,
    private val useInvitationCodeUseCase: UseInvitationCodeUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _codeValidated = MutableStateFlow(false)
    val codeValidated: StateFlow<Boolean> = _codeValidated.asStateFlow()

    private val _validatedCodeRole = MutableStateFlow("")
    val validatedCodeRole: StateFlow<String> = _validatedCodeRole.asStateFlow()

    private val _validatedCodeLineaId = MutableStateFlow("")
    val validatedCodeLineaId: StateFlow<String> = _validatedCodeLineaId.asStateFlow()

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun validateCode(code: String) {
        if (code.length < 8) {
            _codeValidated.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            validateInvitationCodeUseCase(code).fold(
                onSuccess = { validationCode ->
                    if (validationCode != null) {
                        _codeValidated.value = true
                        _validatedCodeRole.value = validationCode.role
                        _validatedCodeLineaId.value = validationCode.lineaId
                        _successMessage.value = "Código válido: ${validationCode.role}"
                    } else {
                        _codeValidated.value = false
                        _errorMessage.value = "Código no válido o ya utilizado"
                    }
                },
                onFailure = { error ->
                    _codeValidated.value = false
                    _errorMessage.value = error.message ?: "Error al validar código"
                }
            )
            _isLoading.value = false
        }
    }

    fun registerAdmin(
        request: RegisterRequest,
        invitationCode: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = registerUseCase(request)

            result.fold(
                onSuccess = { user ->
                    validateInvitationCodeUseCase(invitationCode).fold(
                        onSuccess = { code ->
                            if (code != null) useInvitationCodeUseCase(code.id, user.id)
                        },
                        onFailure = { }
                    )
                    _successMessage.value = "Registro exitoso"
                    onSuccess()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Error al registrar"
                }
            )
            _isLoading.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = com.oficial.viasit.AutosAplicacion.instance
                return RegisterAdminViewModel(
                    registerUseCase = app.registerUseCase,
                    validateInvitationCodeUseCase = app.validateInvitationCodeUseCase,
                    useInvitationCodeUseCase = app.useInvitationCodeUseCase
                ) as T
            }
        }
    }
}
