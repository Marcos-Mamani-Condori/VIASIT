package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class AdminUsersHandler(
    private val scope: CoroutineScope,
    private val getUsersByLineaUseCase: GetUsersByLineaUseCase,
    private val setUserActiveStatusUseCase: SetUserActiveStatusUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getAllReportesUseCase: GetAllReportesUseCase,
    private val getAppealsUseCase: GetAppealsUseCase,
    private val resolveAppealUseCase: ResolveAppealUseCase,
    private val responderReporteUseCase: ResponderReporteUseCase
) {
    fun loadUsers(lineaId: String, isAdminPrincipal: Boolean, state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getUsersByLineaUseCase(lineaId).fold(
                onSuccess = { users -> 
                    val filteredUsers = if (isAdminPrincipal) {
                        users.filter { !it.isAdminPrincipal }
                    } else {
                        users.filter { it.isDriver }
                    }
                    state.value = state.value.copy(isLoading = false, users = filteredUsers)
                },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar usuarios") }
            )
        }
    }

    fun setUserActiveStatus(userId: String, active: Boolean, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            setUserActiveStatusUseCase(userId, active).fold(
                onSuccess = {
                    val action = if (active) "reactivado" else "suspendido"
                    state.value = state.value.copy(isLoading = false, successMessage = "Usuario $action correctamente")
                    onLog(currentUserId, adminName, "Cambió estado de usuario $userId a active=$active")
                },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cambiar estado") }
            )
        }
    }

    fun loadAllReportes(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getAllReportesUseCase().fold(
                onSuccess = { reportes -> state.value = state.value.copy(isLoading = false, reportes = reportes) },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar reportes") }
            )
        }
    }

    fun loadAppeals(state: MutableStateFlow<AdminUiState>) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            getAppealsUseCase().fold(
                onSuccess = { appeals -> state.value = state.value.copy(isLoading = false, appeals = appeals) },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar apelaciones") }
            )
        }
    }

    fun resolveAppeal(appealId: String, userId: String, accept: Boolean, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            resolveAppealUseCase(appealId, userId, accept).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Apelación resuelta")
                    onLog(currentUserId, adminName, "Resolvió apelación $appealId para usuario $userId (aceptada: $accept)")
                    loadAppeals(state)
                },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al resolver apelación") }
            )
        }
    }

    fun responderReporte(reporteId: String, respuesta: String, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            responderReporteUseCase(reporteId, respuesta).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, successMessage = "Reporte respondido")
                    onLog(currentUserId, adminName, "Respondió reporte $reporteId")
                    loadAllReportes(state)
                },
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al responder reporte") }
            )
        }
    }
}
