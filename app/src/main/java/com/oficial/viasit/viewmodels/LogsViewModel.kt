package com.oficial.viasit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.usecases.GetLogsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
class LogsViewModel(
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _timeFilter = MutableStateFlow("todos") // todos, hoy, semana, mes
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    init {
        // Debounce search queries to avoid excessive API calls
        viewModelScope.launch {
            combine(_searchQuery, _timeFilter) { query, filter ->
                Pair(query, filter)
            }
            .debounce(500)
            .collect { (query, filter) ->
                performLoadLogs(query, filter)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
    }

    fun loadLogs() {
        viewModelScope.launch {
            performLoadLogs(_searchQuery.value, _timeFilter.value)
        }
    }

    private suspend fun performLoadLogs(query: String, timeFilter: String) {
        _isLoading.value = true
        
        // Solo enviamos filtro de texto si el usuario realmente escribió algo
        val textFilter = if (query.isNotBlank()) query else ""
        
        getLogsUseCase(filter = textFilter, timeFilter = timeFilter).fold(
            onSuccess = { _logs.value = it },
            onFailure = { _logs.value = emptyList() }
        )
        _isLoading.value = false
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
