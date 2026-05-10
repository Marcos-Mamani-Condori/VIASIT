package com.oficial.viasit.viewmodels

import com.oficial.viasit.data.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class InvitacionesHandler(
    private val scope: CoroutineScope,
    private val repository: AdminRepository
) {
    fun generateInvitationCode(
        role: String = "ADMIN_LINEA", lineaId: String = "", expiresInHours: Int = 24,
        currentUserId: String = "", state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null, generatedCode = null)
            repository.generateInvitationCode(role, lineaId, expiresInHours).fold(
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
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.getInvitationCodes().fold(
                onSuccess = { codes -> state.value = state.value.copy(isLoading = false, invitationCodes = codes) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar códigos") }
            )
        }
    }

    fun deleteInvitationCode(
        codeId: String, currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.deleteInvitationCode(codeId).fold(
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
