package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.repository.IAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AdminViewModel(AutosAplicacion.instance) as T
        }
    }

    private val adminRepository: IAdminRepository = (application as AutosAplicacion).adminRepository

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    private val lineasHandler       = LineasHandler(viewModelScope, adminRepository)
    private val invitacionesHandler = InvitacionesHandler(viewModelScope, adminRepository)
    private val logsHandler         = LogsHandler(viewModelScope, adminRepository)
    private val rutasHandler        = RutasHandler(viewModelScope, adminRepository)
    private val vehicleCodesHandler = VehicleCodesHandler(viewModelScope, adminRepository)

    private val logAction: (String, String) -> Unit = { userId, desc -> logsHandler.createLog(userId, desc) }

    fun loadLineas()                                                               = lineasHandler.loadLineas(_uiState)
    fun createLinea(n: String, c: String, r: String, userId: String)              = lineasHandler.createLinea(n, c, r, userId, _uiState, logAction)
    fun updateLinea(id: String, n: String, c: String, r: String, userId: String)  = lineasHandler.updateLinea(id, n, c, r, userId, _uiState, logAction)
    fun deleteLinea(id: String, userId: String)                                    = lineasHandler.deleteLinea(id, userId, _uiState, logAction)

    fun loadInvitationCodes()                                                      = invitacionesHandler.loadInvitationCodes(_uiState)
    fun generateInvitationCode(role: String, lineaId: String, hours: Int = 24, userId: String = "") =
        invitacionesHandler.generateInvitationCode(role, lineaId, hours, userId, _uiState, logAction)
    fun deleteInvitationCode(id: String, userId: String)                          = invitacionesHandler.deleteInvitationCode(id, userId, _uiState, logAction)

    fun loadLogs()                                                                 = logsHandler.loadLogs(_uiState)
    fun createLog(userId: String, description: String)                            = logsHandler.createLog(userId, description)

    fun loadRutas() = rutasHandler.loadRutas(_uiState)
    fun createRuta(n: String, d: String, s: String, e: String, lineaId: String, userId: String) =
        rutasHandler.createRuta(n, d, s, e, lineaId, userId, _uiState, logAction)
    fun createRutaWithCoords(n: String, d: String, sLat: Double, sLng: Double, eLat: Double, eLng: Double,
                             lineaId: String, userId: String, waypoints: List<Pair<Double,Double>> = emptyList()) =
        rutasHandler.createRutaWithCoords(n, d, sLat, sLng, eLat, eLng, lineaId, userId, waypoints, _uiState, logAction)
    fun assignRutaToLinea(lineaId: String, rutaId: String, rutaName: String, userId: String) =
        rutasHandler.assignRutaToLinea(lineaId, rutaId, rutaName, userId, _uiState, logAction)
    fun deleteRuta(rutaId: String, rutaName: String, lineaId: String = "", userId: String) =
        rutasHandler.deleteRuta(rutaId, rutaName, lineaId, userId, _uiState, logAction)

    fun loadVehicleInvitationCodes(lineaId: String)                               = vehicleCodesHandler.loadVehicleInvitationCodes(lineaId, _uiState)
    fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, hours: Int = 72, userId: String = "") =
        vehicleCodesHandler.generateVehicleInvitationCode(lineaId, creadoPor, hours, userId, _uiState, logAction)
    fun deleteVehicleInvitationCode(id: String, lineaId: String, userId: String) =
        vehicleCodesHandler.deleteVehicleInvitationCode(id, lineaId, userId, _uiState, logAction)
    fun clearVehicleCode() { _uiState.value = _uiState.value.copy(generatedVehicleCode = null) }

    fun clearMessages() { _uiState.value = _uiState.value.copy(error = null, successMessage = null, generatedCode = null) }
}
