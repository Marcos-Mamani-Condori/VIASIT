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

    fun setUserActiveStatus(userId: String, userName: String, active: Boolean, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            if (userId.isBlank()) {
                state.value = state.value.copy(error = "Error: El reporte no contiene un ID de usuario válido para suspender.")
                return@launch
            }

            // Actualización optimista
            val oldUsers = state.value.users
            val updatedUsers = oldUsers.map { 
                if (it.id == userId) it.copy(active = active) else it 
            }
            state.value = state.value.copy(users = updatedUsers, error = null)

            setUserActiveStatusUseCase(userId, active).fold(
                onSuccess = {
                    val action = if (active) "reactivado" else "suspendido"
                    state.value = state.value.copy(
                        successMessage = "Usuario $action correctamente"
                    )
                    onLog(currentUserId, adminName, "${if(active) "Reactivó" else "Suspendió"} a usuario: $userName (ID: $userId)")
                },
                onFailure = { error -> 
                    // Revertir si falla
                    state.value = state.value.copy(users = oldUsers, error = error.message ?: "Error al cambiar estado")
                }
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

    fun resolveAppeal(appealId: String, userId: String, userName: String, accept: Boolean, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            // Actualización optimista: marcar como resuelto en la lista local
            val oldAppeals = state.value.appeals
            val updatedAppeals = oldAppeals.map {
                if (it.id == appealId) {
                    it.copy(type = if (accept) "success" else "error")
                } else it
            }
            state.value = state.value.copy(appeals = updatedAppeals, error = null)

            resolveAppealUseCase(appealId, userId, accept).fold(
                onSuccess = {
                    val action = if (accept) "Aceptó" else "Rechazó"
                    state.value = state.value.copy(successMessage = "Apelación resuelta")
                    onLog(currentUserId, adminName, "$action apelación de: $userName")
                },
                onFailure = { error -> 
                    // Revertir
                    state.value = state.value.copy(appeals = oldAppeals, error = error.message ?: "Error al resolver apelación")
                }
            )
        }
    }

    fun responderReporte(reporteId: String, respuesta: String, currentUserId: String, adminName: String, state: MutableStateFlow<AdminUiState>, onLog: (String, String, String) -> Unit) {
        scope.launch {
            // Actualización optimista
            val oldReportes = state.value.reportes
            val reporte = oldReportes.find { it.id == reporteId }
            val targetName = reporte?.driverName ?: "desconocido"
            val targetId = reporte?.targetUserId ?: ""
            
            // Log de depuración para ver qué ID estamos enviando
            android.util.Log.e("REPORT_SUSPEND", "Intentando suspender desde reporte. TargetId: '$targetId', TargetName: '$targetName'")

            val updatedReportes = oldReportes.map {
                if (it.id == reporteId) it.copy(status = "resuelto", adminResponse = respuesta) else it
            }
            state.value = state.value.copy(reportes = updatedReportes, error = null)

            responderReporteUseCase(reporteId, respuesta).fold(
                onSuccess = {
                    state.value = state.value.copy(successMessage = "Reporte respondido")
                    onLog(currentUserId, adminName, "Resolvió reporte contra: $targetName (ID: $targetId)")
                },
                onFailure = { error -> 
                    // Revertir
                    state.value = state.value.copy(reportes = oldReportes, error = error.message ?: "Error al responder reporte")
                }
            )
        }
    }
}
