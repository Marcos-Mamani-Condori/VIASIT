package com.oficial.viasit.ui.map

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
 * ViewModel con soporte Realtime de PocketBase
 * Los datos se actualizan automáticamente cuando hay cambios en el servidor
 */
class AutosViewModel(private val repository: AutoRepository) : ViewModel() {

    // Flow de autos desde PocketBase Realtime
    val autosUiState: StateFlow<List<Auto>> = repository.autos

    // Estado del formulario de registro de vehículo
    private val _vehicleForm = MutableStateFlow(VehicleFormState())
    val vehicleFormState: StateFlow<VehicleFormState> = _vehicleForm.asStateFlow()

    // Inicializar suscripción realtime
    init {
        viewModelScope.launch {
            repository.startRealtimeSubscription()
        }
    }

    /**
     * Forzar refresh manual de datos
     */
    fun refresh() {
        viewModelScope.launch {
            repository.refreshAutos()
        }
    }

    /**
     * Sincronizar con caché local (para uso offline)
     */
    fun syncWithCache() {
        viewModelScope.launch {
            repository.syncWithLocalCache()
        }
    }

    /**
     * Actualizar placa
     */
    fun updatePlaca(placa: String) {
        _vehicleForm.update { it.copy(placa = placa, placaError = null) }
    }

    /**
     * Actualizar línea
     */
    fun updateLinea(linea: String) {
        _vehicleForm.update { it.copy(linea = linea, lineaError = null) }
    }

    /**
     * Registrar vehículo
     */
    fun registerVehicle(
        userId: String,
        placa: String,
        linea: String,
        onSuccess: () -> Unit
    ) {
        // Validar
        if (placa.isBlank()) {
            _vehicleForm.update { it.copy(placaError = "Placa requerida") }
            return
        }
        if (linea.isBlank()) {
            _vehicleForm.update { it.copy(lineaError = "Codigo de invitacion requerido") }
            return
        }

        _vehicleForm.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = repository.registerAuto(userId, placa, linea)

            result.fold(
                onSuccess = { auto ->
                    _vehicleForm.update { it.copy(isLoading = false, isSuccess = true) }
                    // Refrescar lista de autos
                    repository.refreshAutos()
                    onSuccess()
                },
                onFailure = { e ->
                    _vehicleForm.update {
                        it.copy(isLoading = false, error = e.message ?: "Error al registrar vehículo")
                    }
                }
            )
        }
    }

    /**
     * Obtener auto del conductor
     */
    fun getDriverAuto(userId: String, callback: (Auto?) -> Unit) {
        viewModelScope.launch {
            val result = repository.getAutoByUserId(userId)
            result.fold(
                onSuccess = { auto -> callback(auto) },
                onFailure = { callback(null) }
            )
        }
    }

    /**
     * Reiniciar suscripción realtime
     */
    fun startRealtimeSubscription() {
        viewModelScope.launch {
            repository.startRealtimeSubscription()
        }
    }

    /**
     * Detener suscripción realtime
     */
    fun stopRealtimeSubscription() {
        repository.stopRealtimeSubscription()
    }

    override fun onCleared() {
        super.onCleared()
        // Detener polling al destruir ViewModel
        repository.stopRealtimeSubscription()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutosAplicacion)
                AutosViewModel(application.repository)
            }
        }
    }
}

/**
 * Estado del formulario de registro de vehículo
 */
data class VehicleFormState(
    val placa: String = "",
    val linea: String = "",
    val placaError: String? = null,
    val lineaError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
