package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutosViewModel(
    private val getAutosUseCase: GetAutosUseCase,
    private val registerVehicleUseCase: RegisterVehicleUseCase,
    private val getAutoByUserIdUseCase: GetAutoByUserIdUseCase,
    private val startRealtimeAutosUseCase: StartRealtimeAutosUseCase,
    private val stopRealtimeAutosUseCase: StopRealtimeAutosUseCase,
    private val refreshAutosUseCase: RefreshAutosUseCase,
    private val syncAutosUseCase: SyncAutosUseCase,
    private val submitReporteUseCase: SubmitReporteUseCase,
    private val getReportesUseCase: GetReportesUseCase,
    private val responderReporteConductorUseCase: ResponderReporteConductorUseCase
) : ViewModel() {

    val autosUiState: StateFlow<List<Auto>> = getAutosUseCase()

    // Historial ligero en memoria (Breadcrumbs) - Máximo 15 puntos por bus
    private val _tails = MutableStateFlow<Map<String, List<Pair<Double, Double>>>>(emptyMap())
    val tails: StateFlow<Map<String, List<Pair<Double, Double>>>> = _tails.asStateFlow()

    private val _vehicleForm = MutableStateFlow(VehicleFormState())
    val vehicleFormState: StateFlow<VehicleFormState> = _vehicleForm.asStateFlow()

    init {
        startRealtimeSubscription()
        observeAutosForTails()
    }

    private fun observeAutosForTails() {
        viewModelScope.launch {
            autosUiState.collect { autos ->
                val currentTails = _tails.value.toMutableMap()
                var changed = false
                autos.forEach { auto ->
                    if (auto.lat != 0.0 && auto.lng != 0.0) {
                        val list = currentTails[auto.id]?.toMutableList() ?: mutableListOf()
                        val lastPos = list.lastOrNull()
                        // Solo agregamos si la posición cambió significativamente
                        if (lastPos == null || lastPos.first != auto.lat || lastPos.second != auto.lng) {
                            list.add(auto.lat to auto.lng)
                            if (list.size > 15) list.removeAt(0) // Límite de 15 puntos para no pesar
                            currentTails[auto.id] = list
                            changed = true
                        }
                    }
                }
                if (changed) {
                    _tails.value = currentTails
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshAutosUseCase() }
    }

    fun syncWithCache() {
        viewModelScope.launch { syncAutosUseCase() }
    }

    fun updatePlaca(placa: String) {
        _vehicleForm.update { it.copy(placa = placa, placaError = null) }
    }

    fun updateLinea(linea: String) {
        _vehicleForm.update { it.copy(linea = linea, lineaError = null) }
    }

    fun setError(error: String) {
        _vehicleForm.update { it.copy(error = error) }
    }

    fun registerVehicle(userId: String, placa: String, linea: String, onSuccess: () -> Unit) {
        if (placa.isBlank()) {
            _vehicleForm.update { it.copy(placaError = "Placa requerida") }
            return
        }
        if (linea.isBlank()) {
            _vehicleForm.update { it.copy(lineaError = "Código de invitación requerido") }
            return
        }
        _vehicleForm.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            registerVehicleUseCase(userId, placa, linea).fold(
                onSuccess = {
                    _vehicleForm.update { it.copy(isLoading = false, isSuccess = true) }
                    refreshAutosUseCase()
                    onSuccess()
                },
                onFailure = { e ->
                    _vehicleForm.update { it.copy(isLoading = false, error = e.message ?: "Error al registrar vehículo") }
                }
            )
        }
    }

    fun getDriverAuto(userId: String, callback: (Auto?) -> Unit) {
        viewModelScope.launch {
            getAutoByUserIdUseCase(userId).fold(
                onSuccess = { auto -> callback(auto) },
                onFailure = { callback(null) }
            )
        }
    }

    fun startRealtimeSubscription() {
        viewModelScope.launch { startRealtimeAutosUseCase() }
    }

    fun stopRealtimeSubscription() {
        stopRealtimeAutosUseCase()
    }

    fun submitReporte(
        descripcion: String,
        reporterId: String,
        reporterName: String,
        driverId: String,
        targetName: String,
        lineId: String = "",
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            submitReporteUseCase(descripcion, reporterId, reporterName, driverId, targetName, lineId).fold(
                onSuccess = { onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    fun fetchReportes(placa: String, callback: (List<Reporte>) -> Unit) {
        viewModelScope.launch {
            getReportesUseCase(placa).fold(
                onSuccess = { callback(it) },
                onFailure = { callback(emptyList()) }
            )
        }
    }

    fun responderReporte(reporteId: String, respuesta: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            responderReporteConductorUseCase(reporteId, respuesta).fold(
                onSuccess = { onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeAutosUseCase()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutosAplicacion
                AutosViewModel(
                    app.getAutosUseCase,
                    app.registerVehicleUseCase,
                    app.getAutoByUserIdUseCase,
                    app.startRealtimeAutosUseCase,
                    app.stopRealtimeAutosUseCase,
                    app.refreshAutosUseCase,
                    app.syncAutosUseCase,
                    app.submitReporteUseCase,
                    app.getReportesUseCase,
                    app.responderReporteConductorUseCase
                )
            }
        }
    }
}
