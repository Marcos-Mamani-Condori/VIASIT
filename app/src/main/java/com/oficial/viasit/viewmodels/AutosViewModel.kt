package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AutoRepository
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la lista de autos en tiempo real (PocketBase Realtime/SSE).
 *
 * Responsabilidades:
 *  - Suscribirse a los cambios del servidor en tiempo real
 *  - Exponer la lista de autos actualizada automáticamente
 *  - Gestionar el registro de nuevos vehículos por parte del conductor
 *
 * Anteriormente estaba en ui/map/ — movido a viewmodels/ porque
 * es lógica de negocio que no depende de la UI.
 *
 * Estado del formulario de registro: ver [VehicleFormState.kt]
 */
class AutosViewModel(private val repository: AutoRepository) : ViewModel() {

    /** Lista de autos actualizada en tiempo real desde PocketBase */
    val autosUiState: StateFlow<List<Auto>> = repository.autos

    /** Estado del formulario de registro de vehículo */
    private val _vehicleForm = MutableStateFlow(VehicleFormState())
    val vehicleFormState: StateFlow<VehicleFormState> = _vehicleForm.asStateFlow()

    init {
        // Iniciar suscripción SSE al crear el ViewModel
        viewModelScope.launch {
            repository.startRealtimeSubscription()
        }
    }

    /** Fuerza una recarga manual de todos los autos */
    fun refresh() {
        viewModelScope.launch { repository.refreshAutos() }
    }

    /** Sincroniza con el caché local (permite uso sin conexión) */
    fun syncWithCache() {
        viewModelScope.launch { repository.syncWithLocalCache() }
    }

    // ── Formulario de registro ──────────────────────────────────────────────

    /** Actualiza el campo de placa y limpia su error */
    fun updatePlaca(placa: String) {
        _vehicleForm.update { it.copy(placa = placa, placaError = null) }
    }

    /** Actualiza el campo de código de línea y limpia su error */
    fun updateLinea(linea: String) {
        _vehicleForm.update { it.copy(linea = linea, lineaError = null) }
    }

    /** Establece un error general en el formulario */
    fun setError(error: String) {
        _vehicleForm.update { it.copy(error = error) }
    }

    /**
     * Registra el vehículo del conductor en la línea indicada.
     * Valida los campos antes de enviar la petición.
     */
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

    /**
     * Busca el auto asignado a un conductor por su userId.
     * El resultado llega por callback para no bloquear la UI.
     */
    fun getDriverAuto(userId: String, callback: (Auto?) -> Unit) {
        viewModelScope.launch {
            repository.getAutoByUserId(userId).fold(
                onSuccess = { auto -> callback(auto) },
                onFailure = { callback(null) }
            )
        }
    }

    /** Reinicia la suscripción SSE (por ejemplo, al volver a la pantalla) */
    fun startRealtimeSubscription() {
        viewModelScope.launch { repository.startRealtimeSubscription() }
    }

    /** Detiene la suscripción SSE explícitamente */
    fun stopRealtimeSubscription() {
        repository.stopRealtimeSubscription()
    }

    override fun onCleared() {
        super.onCleared()
        // Liberar la conexión SSE al destruir el ViewModel
        repository.stopRealtimeSubscription()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutosAplicacion
                AutosViewModel(app.repository)
            }
        }
    }
}
