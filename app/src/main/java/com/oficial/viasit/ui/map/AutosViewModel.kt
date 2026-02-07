package com.oficial.viasit.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AutoRepository
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel con soporte Realtime de PocketBase
 * Los datos se actualizan automáticamente cuando hay cambios en el servidor
 */
class AutosViewModel(private val repository: AutoRepository) : ViewModel() {

    // Flow de autos desde PocketBase Realtime
    val autosUiState: StateFlow<List<Auto>> = repository.autos

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
