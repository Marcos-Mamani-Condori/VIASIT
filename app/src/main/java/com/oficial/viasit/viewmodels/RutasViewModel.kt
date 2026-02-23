package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RutasViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository: AdminRepository = (application as AutosAplicacion).adminRepository

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                RutasViewModel(AutosAplicacion.instance) as T
        }
    }

    fun createRuta(
        name: String, description: String, startPoint: String = "", endPoint: String = "",
        lineaId: String = "", currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Ruta creada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Creó ruta: $name")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }

    // Crea una ruta desde coordenadas del mapa — convierte Double a "lat,lng"
    fun createRutaWithCoords(
        name: String, description: String,
        startLat: Double, startLng: Double, endLat: Double, endLng: Double,
        lineaId: String, currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            val startPoint = if (startLat != 0.0 && startLng != 0.0) "$startLat,$startLng" else ""
            val endPoint   = if (endLat != 0.0 && endLng != 0.0)     "$endLat,$endLng"     else ""
            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Ruta creada: $name")
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Creó ruta: $name ($startPoint → $endPoint)")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }
}
