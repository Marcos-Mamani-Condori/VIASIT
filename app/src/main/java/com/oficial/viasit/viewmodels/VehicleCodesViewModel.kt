package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class VehicleCodesHandler(
    private val scope: CoroutineScope,
    private val getVehicleInvitationCodesUseCase: GetVehicleInvitationCodesUseCase,
    private val generateVehicleInvitationCodeUseCase: GenerateVehicleInvitationCodeUseCase,
    private val deleteVehicleInvitationCodeUseCase: DeleteVehicleInvitationCodeUseCase
) {
    fun generateVehicleInvitationCode(
        lineaId: String, creadoPor: String, expiresInHours: Int = 72,
        currentUserId: String = "", state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null, generatedVehicleCode = null)
            generateVehicleInvitationCodeUseCase(lineaId, creadoPor, expiresInHours).fold(
                onSuccess = { code ->
                    state.value = state.value.copy(isLoading = false, generatedVehicleCode = code.code, successMessage = "Código generado: ${code.code}")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Generó código de vehículo: ${code.code} (línea: $lineaId)")
                    loadVehicleInvitationCodes(lineaId, state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al generar código") }
            )
        }
    }

    fun loadVehicleInvitationCodes(lineaId: String, state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getVehicleInvitationCodesUseCase(lineaId).fold(
                onSuccess = { codes -> state.value = state.value.copy(isLoading = false, vehicleCodes = codes) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar códigos") }
            )
        }
    }

    fun deleteVehicleInvitationCode(
        codeId: String, lineaId: String, currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            deleteVehicleInvitationCodeUseCase(codeId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Código eliminado")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Eliminó código de vehículo: $codeId")
                    loadVehicleInvitationCodes(lineaId, state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar código") }
            )
        }
    }
}
