package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.usecases.GetLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _timeFilter = MutableStateFlow("todos") // todos, hoy, ayer, semana, mes
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadLogs()
    }

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val filter = if (_timeFilter.value != "todos") _timeFilter.value else _searchQuery.value
            // Nota: El repositorio actual usa un solo campo de 'filter' para ambos. 
            // Si hay búsqueda y filtro de tiempo, priorizamos el de tiempo según la implementación actual del repo.
            // Idealmente el repo debería soportar ambos.
            
            getLogsUseCase(filter = filter).fold(
                onSuccess = { _logs.value = it },
                onFailure = { _logs.value = emptyList() }
            )
            _isLoading.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return LogsViewModel(AutosAplicacion.instance.getLogsUseCase) as T
            }
        }
    }
}
