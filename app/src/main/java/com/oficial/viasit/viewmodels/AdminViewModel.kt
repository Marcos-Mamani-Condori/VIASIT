package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.usecases.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                onFailure = { error -> state.value = state.value.copy(isLoading = false, error = error.message ?: "Error al cargar logs") }
            )
        }
    }

    fun createLog(userId: String, userName: String, description: String) {
        scope.launch {
            createLogUseCase(userId, userName, description)
        }
    }
}

class AdminViewModel(
    application: Application,
    private val getLineasUseCase: GetLineasUseCase,
    private val createLineaUseCase: CreateLineaUseCase,
    private val updateLineaUseCase: UpdateLineaUseCase,
    private val deleteLineaUseCase: DeleteLineaUseCase,
    private val getInvitationCodesUseCase: GetInvitationCodesUseCase,
    private val generateInvitationCodeUseCase: GenerateInvitationCodeUseCase,
    private val deleteInvitationCodeUseCase: DeleteInvitationCodeUseCase,
    private val getRutasUseCase: GetRutasUseCase,
    private val createRutaUseCase: CreateRutaUseCase,
    private val deleteRutaUseCase: DeleteRutaUseCase,
    private val assignRutaToLineaUseCase: AssignRutaToLineaUseCase,
    private val getLogsUseCase: GetLogsUseCase,
    private val createLogUseCase: CreateLogUseCase,
    private val getUsersByLineaUseCase: GetUsersByLineaUseCase,
    private val setUserActiveStatusUseCase: SetUserActiveStatusUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val getAllReportesUseCase: GetAllReportesUseCase,
    private val getAppealsUseCase: GetAppealsUseCase,
    private val resolveAppealUseCase: ResolveAppealUseCase,
    private val responderReporteUseCase: ResponderReporteUseCase,
    private val getAutosUseCase: GetAutosUseCase
) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val app = AutosAplicacion.instance
                return AdminViewModel(
                    app,
                    app.getLineasUseCase, app.createLineaUseCase, app.updateLineaUseCase, app.deleteLineaUseCase,
                    app.getInvitationCodesUseCase, app.generateInvitationCodeUseCase, app.deleteInvitationCodeUseCase,
                    app.getRutasUseCase, app.createRutaUseCase, app.deleteRutaUseCase, app.assignRutaToLineaUseCase,
                    app.getLogsUseCase, app.createLogUseCase,
                    app.getUsersByLineaUseCase, app.setUserActiveStatusUseCase, app.deleteUserUseCase,
                    app.getAllReportesUseCase, app.getAppealsUseCase, app.resolveAppealUseCase,
                    app.responderReporteUseCase,
                    app.getAutosUseCase
                ) as T
            }
        }
    }

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    val autosUiState = getAutosUseCase()

    private val lineasHandler       = LineasHandler(viewModelScope, getLineasUseCase, createLineaUseCase, updateLineaUseCase, deleteLineaUseCase)
    private val invitacionesHandler = InvitacionesHandler(viewModelScope, getInvitationCodesUseCase, generateInvitationCodeUseCase, deleteInvitationCodeUseCase)
    private val logsHandler         = LogsHandler(viewModelScope, getLogsUseCase, createLogUseCase)
    private val rutasHandler        = RutasHandler(viewModelScope, getRutasUseCase, createRutaUseCase, deleteRutaUseCase, assignRutaToLineaUseCase, getLineasUseCase)
    private val vehicleCodesHandler = VehicleCodesHandler(viewModelScope, getInvitationCodesUseCase, generateInvitationCodeUseCase, deleteInvitationCodeUseCase)
    private val usersHandler        = AdminUsersHandler(viewModelScope, getUsersByLineaUseCase, setUserActiveStatusUseCase, deleteUserUseCase, getAllReportesUseCase, getAppealsUseCase, resolveAppealUseCase, responderReporteUseCase)

    private val logAction: (String, String, String) -> Unit = { userId, userName, desc -> logsHandler.createLog(userId, userName, desc) }

    fun loadLineas()                                                               = lineasHandler.loadLineas(_uiState)
    fun createLinea(n: String, c: String, r: String, userId: String, adminName: String) = lineasHandler.createLinea(n, c, r, userId, adminName, _uiState, logAction)
    fun updateLinea(id: String, n: String, c: String, r: String, userId: String, adminName: String) = lineasHandler.updateLinea(id, n, c, r, userId, adminName, _uiState, logAction)
    fun deleteLinea(id: String, name: String, userId: String, adminName: String)                 = lineasHandler.deleteLinea(id, name, userId, adminName, _uiState, logAction)

    fun loadInvitationCodes()                                                      = invitacionesHandler.loadInvitationCodes(_uiState)
    fun generateInvitationCode(role: String, lineaId: String, hours: Int = 24, userId: String = "", adminName: String = "") =
        invitacionesHandler.generateInvitationCode(role, lineaId, hours, userId, adminName, _uiState, logAction)
    fun deleteInvitationCode(id: String, userId: String, adminName: String)       = invitacionesHandler.deleteInvitationCode(id, userId, adminName, _uiState, logAction)

    fun loadLogs()                                                                 = logsHandler.loadLogs(_uiState)
    fun createLog(userId: String, userName: String, description: String)           = logsHandler.createLog(userId, userName, description)

    fun loadRutas() = rutasHandler.loadRutas(_uiState)
    fun createRuta(n: String, d: String, s: String, e: String, lineaId: String, userId: String, adminName: String, onSuccess: (() -> Unit)? = null) =
        rutasHandler.createRuta(n, d, s, e, lineaId, userId, adminName, _uiState, logAction, onSuccess)
    fun createRutaWithCoords(n: String, d: String, sLat: Double, sLng: Double, eLat: Double, eLng: Double,
                             lineaId: String, userId: String, adminName: String, waypoints: List<Pair<Double, Double>> = emptyList(), onSuccess: (() -> Unit)? = null) =
        rutasHandler.createRutaWithCoords(n, d, sLat, sLng, eLat, eLng, lineaId, userId, adminName, waypoints, _uiState, logAction, onSuccess)
    fun assignRutaToLinea(lineaId: String, rutaId: String, rutaName: String, userId: String, adminName: String, onSuccess: (() -> Unit)? = null) =
        rutasHandler.assignRutaToLinea(lineaId, rutaId, rutaName, userId, adminName, _uiState, logAction, onSuccess)
    fun deleteRuta(rutaId: String, rutaName: String, lineaId: String = "", userId: String, adminName: String, onSuccess: (() -> Unit)? = null) =
        rutasHandler.deleteRuta(rutaId, rutaName, lineaId, userId, adminName, _uiState, logAction, onSuccess)

    fun loadVehicleInvitationCodes(lineaId: String)                               = vehicleCodesHandler.loadVehicleInvitationCodes(lineaId, _uiState)
    fun generateVehicleInvitationCode(lineaId: String, adminName: String, hours: Int = 72, userId: String = "") =
        vehicleCodesHandler.generateVehicleInvitationCode(lineaId, adminName, hours, userId, _uiState, logAction)
    fun deleteVehicleInvitationCode(id: String, lineaId: String, userId: String, adminName: String) =
        vehicleCodesHandler.deleteVehicleInvitationCode(id, lineaId, userId, adminName, _uiState, logAction)
    fun clearVehicleCode() { _uiState.value = _uiState.value.copy(generatedCode = null) }

    // Admin Users, Reports & Appeals
    fun loadUsers(lineaId: String, isAdminPrincipal: Boolean = false) = usersHandler.loadUsers(lineaId, isAdminPrincipal, _uiState)
    fun setUserActiveStatus(targetUserId: String, targetUserName: String, active: Boolean, adminId: String, adminName: String) = 
        usersHandler.setUserActiveStatus(targetUserId, targetUserName, active, adminId, adminName, _uiState, logAction)
    fun loadAllReportes() = usersHandler.loadAllReportes(_uiState)
    fun loadAppeals() = usersHandler.loadAppeals(_uiState)
    fun resolveAppeal(appealId: String, targetUserId: String, targetUserName: String, accept: Boolean, adminId: String, adminName: String) =
        usersHandler.resolveAppeal(appealId, targetUserId, targetUserName, accept, adminId, adminName, _uiState, logAction)

    fun responderReporte(reporteId: String, respuesta: String, adminId: String, adminName: String) =
        usersHandler.responderReporte(reporteId, respuesta, adminId, adminName, _uiState, logAction)

    fun clearMessages() { _uiState.value = _uiState.value.copy(error = null, successMessage = null, generatedCode = null) }
}
