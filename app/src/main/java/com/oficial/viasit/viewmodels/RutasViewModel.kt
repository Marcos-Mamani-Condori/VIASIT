package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class RutasHandler(
    private val scope: CoroutineScope,
    private val getRutasUseCase: GetRutasUseCase,
    private val createRutaUseCase: CreateRutaUseCase,
    private val deleteRutaUseCase: DeleteRutaUseCase,
    private val assignRutaToLineaUseCase: AssignRutaToLineaUseCase,
    private val getLineasUseCase: GetLineasUseCase
) {
    fun loadRutas(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getRutasUseCase().fold(
                onSuccess = { rutas -> state.value = state.value.copy(isLoading = false, rutas = rutas) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

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

            createRutaUseCase(name, description, startPoint, endPoint, lineaId, waypointsStr).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        assignRutaToLineaUseCase(lineaId, ruta.id)
                    
                    val updatedLineas = getLineasUseCase().getOrElse { state.value.lineas }
                    val updatedRutas  = getRutasUseCase().getOrElse  { state.value.rutas  }
                    
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
            createRutaUseCase(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    if (lineaId.isNotEmpty() && ruta.id.isNotEmpty())
                        assignRutaToLineaUseCase(lineaId, ruta.id)
                    
                    val updatedLineas = getLineasUseCase().getOrElse { state.value.lineas }
                    val updatedRutas  = getRutasUseCase().getOrElse  { state.value.rutas  }
                    
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
            assignRutaToLineaUseCase(lineaId, rutaId).fold(
                onSuccess = {
                    val updatedLineas = getLineasUseCase().getOrElse { state.value.lineas }
                    val updatedRutas  = getRutasUseCase().getOrElse  { state.value.rutas  }
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
            if (lineaId.isNotEmpty()) assignRutaToLineaUseCase(lineaId, "")
            
            deleteRutaUseCase(rutaId).fold(
                onSuccess = {
                    val updatedRutas  = getRutasUseCase().getOrElse  { state.value.rutas  }
                    val updatedLineas = getLineasUseCase().getOrElse { state.value.lineas }
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
