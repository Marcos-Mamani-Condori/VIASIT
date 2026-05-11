package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class LogsHandler(
    private val scope: CoroutineScope,
    private val getLogsUseCase: GetLogsUseCase,
    private val createLogUseCase: CreateLogUseCase
) {
    fun loadLogs(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getLogsUseCase().fold(
                onSuccess = { logs -> state.value = state.value.copy(isLoading = false, logs = logs) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar logs") }
            )
        }
    }

    fun createLog(userId: String, description: String) {
        scope.launch {
            createLogUseCase(userId = userId, description = description)
        }
    }
}
