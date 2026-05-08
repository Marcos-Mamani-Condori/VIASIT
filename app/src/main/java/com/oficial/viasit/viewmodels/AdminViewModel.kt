package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private val logAction: (String, String) -> Unit = { email, desc -> logsVM.createLog(email, desc) }

    fun loadLineas()                                                           = lineasVM.loadLineas(_uiState)
    fun createLinea(n: String, c: String, r: String, email: String)           = lineasVM.createLinea(n, c, r, email, _uiState, logAction)
    fun updateLinea(id: String, n: String, c: String, r: String, email: String) = lineasVM.updateLinea(id, n, c, r, email, _uiState, logAction)
    fun deleteLinea(id: String, email: String)                                = lineasVM.deleteLinea(id, email, _uiState, logAction)

    fun loadInvitationCodes()                                                  = invitacionesVM.loadInvitationCodes(_uiState)
    fun generateInvitationCode(role: String, lineaId: String, hours: Int = 24, email: String = "") =
        invitacionesVM.generateInvitationCode(role, lineaId, hours, email, _uiState, logAction)
    fun deleteInvitationCode(id: String, email: String)                       = invitacionesVM.deleteInvitationCode(id, email, _uiState, logAction)

    fun loadLogs()                                                             = logsVM.loadLogs(_uiState)
    fun createLog(email: String, description: String)                         = logsVM.createLog(email, description)

    fun loadRutas() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        getApplication<AutosAplicacion>().adminRepository.getRutas().fold(
            onSuccess = { rutas -> _uiState.value = _uiState.value.copy(rutas = rutas, isLoading = false) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }
    fun createRuta(n: String, d: String, s: String, e: String, lineaId: String, email: String) =
        rutasVM.createRuta(n, d, s, e, lineaId, email, _uiState, logAction)
    fun createRutaWithCoords(n: String, d: String, sLat: Double, sLng: Double, eLat: Double, eLng: Double,
                             lineaId: String, email: String, waypoints: List<Pair<Double,Double>> = emptyList()) =
        rutasVM.createRutaWithCoords(n, d, sLat, sLng, eLat, eLng, lineaId, email, waypoints, _uiState, logAction)
    fun assignRutaToLinea(lineaId: String, rutaId: String, rutaName: String, email: String) =
        rutasVM.assignRutaToLinea(lineaId, rutaId, rutaName, email, _uiState, logAction)
    fun deleteRuta(rutaId: String, rutaName: String, lineaId: String = "", email: String) =
        rutasVM.deleteRuta(rutaId, rutaName, lineaId, email, _uiState, logAction)

    fun loadVehicleInvitationCodes(lineaId: String)                           = vehicleCodesVM.loadVehicleInvitationCodes(lineaId, _uiState)
    fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, hours: Int = 72, email: String = "") =
        vehicleCodesVM.generateVehicleInvitationCode(lineaId, creadoPor, hours, email, _uiState, logAction)
    fun deleteVehicleInvitationCode(id: String, lineaId: String, email: String) =
        vehicleCodesVM.deleteVehicleInvitationCode(id, lineaId, email, _uiState, logAction)
    fun clearVehicleCode() { _uiState.value = _uiState.value.copy(generatedVehicleCode = null) }

    fun clearMessages() { _uiState.value = _uiState.value.copy(error = null, successMessage = null, generatedCode = null) }
}
