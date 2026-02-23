package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class InvitacionesViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository: AdminRepository = (application as AutosAplicacion).adminRepository

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                InvitacionesViewModel(AutosAplicacion.instance) as T
        }
    }

    fun generateInvitationCode(
        role: String = "ADMIN_LINEA", lineaId: String = "", expiresInHours: Int = 24,
        currentUserId: String = "", state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null, generatedCode = null)
            adminRepository.generateInvitationCode(role, lineaId, expiresInHours).fold(
                onSuccess = { code ->
                    state.value = state.value.copy(isLoading = false, generatedCode = code.code, successMessage = "Código generado: ${code.code}")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Generó código: ${code.code} (rol: $role)")
                    loadInvitationCodes(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al generar código") }
            )
        }
    }

    fun loadInvitationCodes(state: MutableStateFlow<AdminUiState>) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.getInvitationCodes().fold(
                onSuccess = { codes -> state.value = state.value.copy(isLoading = false, invitationCodes = codes) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar códigos") }
            )
        }
    }

    fun deleteInvitationCode(
        codeId: String, currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.deleteInvitationCode(codeId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Código eliminado")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Eliminó código: $codeId")
                    loadInvitationCodes(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar código") }
            )
        }
    }
}
