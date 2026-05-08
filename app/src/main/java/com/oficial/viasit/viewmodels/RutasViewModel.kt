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


    fun createRutaWithCoords(
        name: String, description: String,
        startLat: Double, startLng: Double, endLat: Double, endLng: Double,
        lineaId: String, currentUserEmail: String = "",
        waypoints: List<Pair<Double, Double>> = emptyList(),
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            val startPoint   = if (startLat != 0.0 && startLng != 0.0) "$startLat,$startLng" else ""
            val endPoint     = if (endLat != 0.0 && endLng != 0.0)     "$endLat,$endLng"     else ""
            val waypointsStr = waypoints.joinToString(";") { (lat, lng) -> "$lat,$lng" }

            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId, waypointsStr).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        adminRepository.updateLineaRuta(lineaId, ruta.id)
                    val updatedLineas = adminRepository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = adminRepository.getRutas().getOrElse  { state.value.rutas  }
                    val wptInfo = if (waypoints.isNotEmpty()) " con ${waypoints.size} puntos de recorrido" else ""
                    state.value = state.value.copy(
                        isLoading = false,
                        successMessage = "Ruta \"$name\" creada y asignada",
                        lineas = updatedLineas, rutas = updatedRutas
                    )
                    if (currentUserEmail.isNotEmpty())
                        onLog(currentUserEmail, "Creó y asignó ruta: $name ($startPoint → $endPoint)$wptInfo")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }


    fun createRuta(
        name: String, description: String,
        startPoint: String = "", endPoint: String = "",
        lineaId: String = "", currentUserEmail: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        adminRepository.updateLineaRuta(lineaId, ruta.id)
                    val updatedLineas = adminRepository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = adminRepository.getRutas().getOrElse  { state.value.rutas  }
                    state.value = state.value.copy(isLoading = false, successMessage = "Ruta creada: $name",
                        lineas = updatedLineas, rutas = updatedRutas)
                    if (currentUserEmail.isNotEmpty()) onLog(currentUserEmail, "Creó ruta: $name")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }


    fun assignRutaToLinea(
        lineaId: String, rutaId: String, rutaName: String,
        currentUserEmail: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.updateLineaRuta(lineaId, rutaId).fold(
                onSuccess = {
                    val updatedLineas = adminRepository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = adminRepository.getRutas().getOrElse  { state.value.rutas  }
                    state.value = state.value.copy(isLoading = false,
                        successMessage = "Ruta \"$rutaName\" asignada",
                        lineas = updatedLineas, rutas = updatedRutas)
                    if (currentUserEmail.isNotEmpty())
                        onLog(currentUserEmail, "Asignó ruta \"$rutaName\" a línea $lineaId")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al asignar ruta") }
            )
        }
    }


    fun deleteRuta(
        rutaId: String, rutaName: String, lineaId: String = "",
        currentUserEmail: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            if (lineaId.isNotEmpty()) adminRepository.updateLineaRuta(lineaId, "")
            adminRepository.deleteRuta(rutaId).fold(
                onSuccess = {
                    val updatedRutas  = adminRepository.getRutas().getOrElse  { state.value.rutas  }
                    val updatedLineas = adminRepository.getLineas().getOrElse { state.value.lineas }
                    state.value = state.value.copy(isLoading = false,
                        successMessage = "Ruta \"$rutaName\" eliminada",
                        rutas = updatedRutas, lineas = updatedLineas)
                    if (currentUserEmail.isNotEmpty())
                        onLog(currentUserEmail, "Eliminó ruta: $rutaName")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar ruta") }
            )
        }
    }
}
