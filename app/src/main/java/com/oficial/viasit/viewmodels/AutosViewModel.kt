package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.AutoRepository
import com.oficial.viasit.model.Auto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutosViewModel(private val repository: AutoRepository) : ViewModel() {

    val autosUiState: StateFlow<List<Auto>> = repository.autos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        iniciarActualizacionAutomatica()
    }

    private fun iniciarActualizacionAutomatica() {
        viewModelScope.launch {
            while (true) {
                repository.refreshAutos()
                delay(10000)
            }
        }
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