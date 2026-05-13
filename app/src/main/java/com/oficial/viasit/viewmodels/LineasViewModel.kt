package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class LineasHandler(
    private val scope: CoroutineScope,
    private val getLineasUseCase: GetLineasUseCase,
    private val createLineaUseCase: CreateLineaUseCase,
    private val updateLineaUseCase: UpdateLineaUseCase,
    private val deleteLineaUseCase: DeleteLineaUseCase
) {
    fun loadLineas(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getLineasUseCase().fold(
                onSuccess = { lineas -> state.value = state.value.copy(isLoading = false, lineas = lineas) },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar líneas") }
            )
        }
    }

    fun createLinea(
        name: String, code: String, rutaId: String = "", currentUserId: String = "", adminName: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            createLineaUseCase(name, code, rutaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea creada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Creó línea: $name (código: $code)")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear línea") }
            )
        }
    }

    fun updateLinea(
        lineaId: String, name: String, code: String, rutaId: String = "", currentUserId: String = "", adminName: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            updateLineaUseCase(lineaId, name, code, rutaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea actualizada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Actualizó línea: $name")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al actualizar línea") }
            )
        }
    }

    fun deleteLinea(
        lineaId: String, name: String, currentUserId: String = "", adminName: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            deleteLineaUseCase(lineaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea eliminada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Eliminó línea: $name")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar línea") }
            )
        }
    }
}
