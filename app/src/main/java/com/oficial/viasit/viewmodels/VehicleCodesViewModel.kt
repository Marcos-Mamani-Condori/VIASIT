package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VehicleCodesViewModel : ViewModel()

internal class VehicleCodesHandler(
    private val scope: CoroutineScope,
    private val getInvitationCodesUseCase: GetInvitationCodesUseCase,
    private val generateInvitationCodeUseCase: GenerateInvitationCodeUseCase,
    private val deleteInvitationCodeUseCase: DeleteInvitationCodeUseCase
) {
    fun generateVehicleInvitationCode(
        lineaId: String, adminName: String, expiresInHours: Int = 72,
        currentUserId: String = "", state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null, generatedCode = null)
            generateInvitationCodeUseCase("DRIVER", lineaId, expiresInHours, adminName).fold(
                onSuccess = { code ->
                    state.value = state.value.copy(isLoading = false, generatedCode = code.code, successMessage = "Código de vehículo generado: ${code.code}")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Generó código de vehículo: ${code.code} (línea: $lineaId)")
                    loadVehicleInvitationCodes(lineaId, state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al generar código") }
            )
        }
    }

    fun loadVehicleInvitationCodes(lineaId: String, state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getInvitationCodesUseCase().fold(
                onSuccess = { codes -> 
                    val filtered = codes.filter { it.lineaId == lineaId && it.role == "DRIVER" }
                    state.value = state.value.copy(isLoading = false, invitationCodes = filtered) 
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar códigos") }
            )
        }
    }

    fun deleteVehicleInvitationCode(
        codeId: String, lineaId: String, currentUserId: String = "", adminName: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            deleteInvitationCodeUseCase(codeId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Código eliminado")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Eliminó código de vehículo: $codeId")
                    loadVehicleInvitationCodes(lineaId, state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar código") }
            )
        }
    }
}
