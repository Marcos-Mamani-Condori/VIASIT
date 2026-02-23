package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AdminRepository
import com.oficial.viasit.domain.model.Linea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LineasViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository: AdminRepository = (application as AutosAplicacion).adminRepository

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                LineasViewModel(AutosAplicacion.instance) as T
        }
    }

    fun loadLineas(state: MutableStateFlow<AdminUiState>) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.getLineas().fold(
                onSuccess = { lineas ->
                    state.value = state.value.copy(isLoading = false, lineas = lineas)
                },
                onFailure = { error ->
                    state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar líneas")
                }
            )
        }
    }

    fun createLinea(
        name: String, code: String, rutaId: String = "", currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.createLinea(name, code, rutaId).fold(
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
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.updateLinea(lineaId, name, code, rutaId).fold(
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
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.deleteLinea(lineaId).fold(
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
