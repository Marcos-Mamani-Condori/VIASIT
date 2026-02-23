package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.oficial.viasit.AutosAplicacion
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

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    private val lineasVM       = LineasViewModel(application)
    private val invitacionesVM = InvitacionesViewModel(application)
    private val logsVM         = LogsViewModel(application)
    private val rutasVM        = RutasViewModel(application)
    private val vehicleCodesVM = VehicleCodesViewModel(application)

    private val logAction: (String, String) -> Unit = { uid, desc -> logsVM.createLog(uid, desc) }

    // Líneas
    fun loadLineas()                                                           = lineasVM.loadLineas(_uiState)
    fun createLinea(n: String, c: String, r: String, uid: String)              = lineasVM.createLinea(n, c, r, uid, _uiState, logAction)
    fun updateLinea(id: String, n: String, c: String, r: String, uid: String)  = lineasVM.updateLinea(id, n, c, r, uid, _uiState, logAction)
    fun deleteLinea(id: String, uid: String)                                   = lineasVM.deleteLinea(id, uid, _uiState, logAction)

    // Códigos de invitación (admin)
    fun loadInvitationCodes()                                                  = invitacionesVM.loadInvitationCodes(_uiState)
    fun generateInvitationCode(role: String, lineaId: String, hours: Int = 24, uid: String = "") =
        invitacionesVM.generateInvitationCode(role, lineaId, hours, uid, _uiState, logAction)
    fun deleteInvitationCode(id: String, uid: String)                          = invitacionesVM.deleteInvitationCode(id, uid, _uiState, logAction)

    // Logs
    fun loadLogs()                                                             = logsVM.loadLogs(_uiState)
    fun createLog(userId: String, description: String)                         = logsVM.createLog(userId, description)

    // Rutas
    fun createRuta(n: String, d: String, s: String, e: String, lineaId: String, uid: String) =
        rutasVM.createRuta(n, d, s, e, lineaId, uid, _uiState, logAction)
    fun createRutaWithCoords(n: String, d: String, sLat: Double, sLng: Double, eLat: Double, eLng: Double, lineaId: String, uid: String) =
        rutasVM.createRutaWithCoords(n, d, sLat, sLng, eLat, eLng, lineaId, uid, _uiState, logAction)

    // Códigos de vehículo
    fun loadVehicleInvitationCodes(lineaId: String)                            = vehicleCodesVM.loadVehicleInvitationCodes(lineaId, _uiState)
    fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, hours: Int = 72, uid: String = "") =
        vehicleCodesVM.generateVehicleInvitationCode(lineaId, creadoPor, hours, uid, _uiState, logAction)
    fun deleteVehicleInvitationCode(id: String, lineaId: String, uid: String)  =
        vehicleCodesVM.deleteVehicleInvitationCode(id, lineaId, uid, _uiState, logAction)
    fun clearVehicleCode() { _uiState.value = _uiState.value.copy(generatedVehicleCode = null) }

    // UI
    fun clearMessages() { _uiState.value = _uiState.value.copy(error = null, successMessage = null, generatedCode = null) }
}
