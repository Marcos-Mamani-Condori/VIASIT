package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.repository.IAdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class LogsHandler(
    private val scope: CoroutineScope,
    private val repository: IAdminRepository
) {
    fun loadLogs(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            repository.getLogs().fold(
                onSuccess = { logs -> state.value = state.value.copy(isLoading = false, logs = logs) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar logs") }
            )
        }
    }

    fun createLog(userId: String, description: String) {
        scope.launch {
            repository.createLog(userId = userId, description = description)
        }
    }
}
