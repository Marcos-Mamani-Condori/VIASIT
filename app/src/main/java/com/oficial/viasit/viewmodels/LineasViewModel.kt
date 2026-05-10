package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.repository.IAdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class LineasHandler(
    private val scope: CoroutineScope,
    private val repository: IAdminRepository
) {
    fun loadLineas(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.getLineas().fold(
                onSuccess = { lineas -> state.value = state.value.copy(isLoading = false, lineas = lineas) },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar líneas") }
            )
        }
    }

    fun createLinea(
        name: String, code: String, rutaId: String = "", currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.createLinea(name, code, rutaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea creada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Creó línea: $name (código: $code)")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear línea") }
            )
        }
    }

    fun updateLinea(
        lineaId: String, name: String, code: String, rutaId: String = "", currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.updateLinea(lineaId, name, code, rutaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea actualizada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Actualizó línea: $name")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al actualizar línea") }
            )
        }
    }

    fun deleteLinea(
        lineaId: String, currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.deleteLinea(lineaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Línea eliminada")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Eliminó línea: $lineaId")
                    loadLineas(state)
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar línea") }
            )
        }
    }
}
