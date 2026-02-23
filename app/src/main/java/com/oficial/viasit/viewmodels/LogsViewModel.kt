package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository: AdminRepository = (application as AutosAplicacion).adminRepository

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                LogsViewModel(AutosAplicacion.instance) as T
        }
    }

    fun loadLogs(state: MutableStateFlow<AdminUiState>) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            adminRepository.getLogs().fold(
                onSuccess = { logs -> state.value = state.value.copy(isLoading = false, logs = logs) },
                onFailure = { state.value = state.value.copy(isLoading = false, error = it.message ?: "Error al cargar logs") }
            )
        }
    }

    fun createLog(userId: String, description: String) {
        viewModelScope.launch {
            adminRepository.createLog(userId = userId, description = description)
        }
    }
}
