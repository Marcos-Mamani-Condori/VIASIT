package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AutoRepository
import com.oficial.viasit.data.repository.ReportesRepository
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.Reporte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutosViewModel(
    private val repository: AutoRepository,
    private val reportesRepository: ReportesRepository
) : ViewModel() {

    val autosUiState: StateFlow<List<Auto>> = repository.autos

    private val _vehicleForm = MutableStateFlow(VehicleFormState())
    val vehicleFormState: StateFlow<VehicleFormState> = _vehicleForm.asStateFlow()

    init {
        viewModelScope.launch {
            repository.startRealtimeSubscription()
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshAutos() }
    }

    fun syncWithCache() {
        viewModelScope.launch { repository.syncWithLocalCache() }
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
            repository.registerAuto(userId, placa, linea).fold(
                onSuccess = {
                    _vehicleForm.update { it.copy(isLoading = false, isSuccess = true) }
                    repository.refreshAutos()
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
            repository.getAutoByUserId(userId).fold(
                onSuccess = { auto -> callback(auto) },
                onFailure = { callback(null) }
            )
        }
    }

    fun startRealtimeSubscription() {
        viewModelScope.launch { repository.startRealtimeSubscription() }
    }

    fun stopRealtimeSubscription() {
        repository.stopRealtimeSubscription()
    }

    fun submitReporte(descripcion: String, userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            reportesRepository.submitReporte(descripcion, userId).fold(
                onSuccess = { onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    fun fetchReportes(placa: String, callback: (List<Reporte>) -> Unit) {
        viewModelScope.launch {
            reportesRepository.getReportes(placa).fold(
                onSuccess = { callback(it) },
                onFailure = { callback(emptyList()) }
            )
        }
    }

    fun responderReporte(reporteId: String, respuesta: String, authToken: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            reportesRepository.responderReporte(reporteId, respuesta, authToken).fold(
                onSuccess = { onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopRealtimeSubscription()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutosAplicacion
                AutosViewModel(app.repository, app.reportesRepository)
            }
        }
    }
}
