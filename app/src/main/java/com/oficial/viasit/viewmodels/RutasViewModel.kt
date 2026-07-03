package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
        lineaId: String, currentUserId: String = "", adminName: String = "",
        waypoints: List<Pair<Double, Double>> = emptyList(),
        state: MutableStateFlow<AdminUiState>, 
        onLog: (String, String, String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            val startPoint   = if (startLat != 0.0 && startLng != 0.0) "$startLat,$startLng" else ""
            val endPoint     = if (endLat != 0.0 && endLng != 0.0)     "$endLat,$endLng"     else ""
            val waypointsStr = waypoints.joinToString(";") { (lat, lng) -> "$lat,$lng" }

            createRutaUseCase(name, description, startPoint, endPoint, lineaId, waypointsStr).fold(
                onSuccess = { ruta ->
                    scope.launch {
                        if (lineaId.isNotEmpty() && ruta.id.isNotEmpty()) {
                            // ESPERAMOS a que la asignación termine realmente
                            assignRutaToLineaUseCase(lineaId, ruta.id)
                            delay(500) // Pausa de seguridad para consistencia de DB
                        }
                        
                        val updatedLineas = getLineasUseCase().getOrNull() ?: state.value.lineas
                        val updatedRutas  = getRutasUseCase().getOrNull()  ?: state.value.rutas
                        
                        state.value = state.value.copy(
                            isLoading = false,
                            successMessage = "Ruta actualizada",
                            lineas = updatedLineas, 
                            rutas = updatedRutas
                        )
                        
                        if (currentUserId.isNotEmpty())
                            onLog(currentUserId, adminName, "Actualizó ruta de línea")
                        
                        onSuccess?.invoke()
                    }
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }

    fun createRuta(
        name: String, description: String,
        startPoint: String = "", endPoint: String = "",
        lineaId: String = "", currentUserId: String = "", adminName: String = "",
        state: MutableStateFlow<AdminUiState>, 
        onLog: (String, String, String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            createRutaUseCase(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    scope.launch {
                        if (lineaId.isNotEmpty() && ruta.id.isNotEmpty()) {
                            assignRutaToLineaUseCase(lineaId, ruta.id)
                            delay(500)
                        }
                        
                        val updatedLineas = getLineasUseCase().getOrNull() ?: state.value.lineas
                        val updatedRutas  = getRutasUseCase().getOrNull()  ?: state.value.rutas
                        
                        state.value = state.value.copy(
                            isLoading = false, 
                            successMessage = "Ruta creada",
                            lineas = updatedLineas, 
                            rutas = updatedRutas
                        )
                        
                        onSuccess?.invoke()
                        if (currentUserId.isNotEmpty()) onLog(currentUserId, adminName, "Creó ruta")
                    }
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al crear ruta") }
            )
        }
    }

    fun assignRutaToLinea(
        lineaId: String, rutaId: String, rutaName: String,
        currentUserId: String, adminName: String, 
        state: MutableStateFlow<AdminUiState>, 
        onLog: (String, String, String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            assignRutaToLineaUseCase(lineaId, rutaId).fold(
                onSuccess = {
                    scope.launch {
                        delay(500)
                        val updatedLineas = getLineasUseCase().getOrNull() ?: state.value.lineas
                        val updatedRutas  = getRutasUseCase().getOrNull()  ?: state.value.rutas
                        state.value = state.value.copy(
                            isLoading = false,
                            successMessage = "Ruta asignada",
                            lineas = updatedLineas, 
                            rutas = updatedRutas
                        )
                        onSuccess?.invoke()
                    }
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al asignar ruta") }
            )
        }
    }

    fun deleteRuta(
        rutaId: String, rutaName: String, lineaId: String = "",
        currentUserId: String, adminName: String, 
        state: MutableStateFlow<AdminUiState>, 
        onLog: (String, String, String) -> Unit,
        onSuccess: (() -> Unit)? = null
    ) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            if (lineaId.isNotEmpty()) assignRutaToLineaUseCase(lineaId, "")
            
            deleteRutaUseCase(rutaId).fold(
                onSuccess = {
                    scope.launch {
                        delay(500)
                        val updatedRutas  = getRutasUseCase().getOrNull()  ?: state.value.rutas
                        val updatedLineas = getLineasUseCase().getOrNull() ?: state.value.lineas
                        state.value = state.value.copy(
                            isLoading = false,
                            successMessage = "Ruta eliminada",
                            rutas = updatedRutas, 
                            lineas = updatedLineas
                        )
                        onSuccess?.invoke()
                    }
                },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al eliminar ruta") }
            )
        }
    }
}
