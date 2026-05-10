package com.oficial.viasit.viewmodels

import com.oficial.viasit.data.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class RutasHandler(
    private val scope: CoroutineScope,
    private val repository: AdminRepository
) {
    fun createRutaWithCoords(
        name: String, description: String,
        startLat: Double, startLng: Double, endLat: Double, endLng: Double,
        lineaId: String, currentUserId: String = "",
        waypoints: List<Pair<Double, Double>> = emptyList(),
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            val startPoint   = if (startLat != 0.0 && startLng != 0.0) "$startLat,$startLng" else ""
            val endPoint     = if (endLat != 0.0 && endLng != 0.0)     "$endLat,$endLng"     else ""
            val waypointsStr = waypoints.joinToString(";") { (lat, lng) -> "$lat,$lng" }

            repository.createRuta(name, description, startPoint, endPoint, lineaId, waypointsStr).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        repository.updateLineaRuta(lineaId, ruta.id)
                    val updatedLineas = repository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = repository.getRutas().getOrElse  { state.value.rutas  }
                    val wptInfo = if (waypoints.isNotEmpty()) " con ${waypoints.size} puntos de recorrido" else ""
                    state.value = state.value.copy(
                        isLoading = false,
                        successMessage = "Ruta \"$name\" creada y asignada",
                        lineas = updatedLineas, rutas = updatedRutas
                    )
                    if (currentUserId.isNotEmpty())
                        onLog(currentUserId, "Creó y asignó ruta: $name ($startPoint → $endPoint)$wptInfo")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }

    fun createRuta(
        name: String, description: String,
        startPoint: String = "", endPoint: String = "",
        lineaId: String = "", currentUserId: String = "",
        state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        repository.updateLineaRuta(lineaId, ruta.id)
                    val updatedLineas = repository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = repository.getRutas().getOrElse  { state.value.rutas  }
                    state.value = state.value.copy(isLoading = false, successMessage = "Ruta creada: $name",
                        lineas = updatedLineas, rutas = updatedRutas)
                    if (currentUserId.isNotEmpty()) onLog(currentUserId, "Creó ruta: $name")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }

    fun assignRutaToLinea(
        lineaId: String, rutaId: String, rutaName: String,
        currentUserId: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.updateLineaRuta(lineaId, rutaId).fold(
                onSuccess = {
                    val updatedLineas = repository.getLineas().getOrElse { state.value.lineas }
                    val updatedRutas  = repository.getRutas().getOrElse  { state.value.rutas  }
                    state.value = state.value.copy(isLoading = false,
                        successMessage = "Ruta \"$rutaName\" asignada",
                        lineas = updatedLineas, rutas = updatedRutas)
                    if (currentUserId.isNotEmpty())
                        onLog(currentUserId, "Asignó ruta \"$rutaName\" a línea $lineaId")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al asignar ruta") }
            )
        }
    }

    fun deleteRuta(
        rutaId: String, rutaName: String, lineaId: String = "",
        currentUserId: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String) -> Unit
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            if (lineaId.isNotEmpty()) repository.updateLineaRuta(lineaId, "")
            repository.deleteRuta(rutaId).fold(
                onSuccess = {
                    val updatedRutas  = repository.getRutas().getOrElse  { state.value.rutas  }
                    val updatedLineas = repository.getLineas().getOrElse { state.value.lineas }
                    state.value = state.value.copy(isLoading = false,
                        successMessage = "Ruta \"$rutaName\" eliminada",
                        rutas = updatedRutas, lineas = updatedLineas)
                    if (currentUserId.isNotEmpty())
                        onLog(currentUserId, "Eliminó ruta: $rutaName")
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar ruta") }
            )
        }
    }
}
