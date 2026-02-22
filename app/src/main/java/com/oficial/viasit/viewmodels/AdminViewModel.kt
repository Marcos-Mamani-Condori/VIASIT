package com.oficial.viasit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.data.repository.AdminRepository
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Log
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val invitationCodes: List<InvitationCode> = emptyList(),
    val lineas: List<Linea> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val rutas: List<Ruta> = emptyList(),
    val vehicleCodes: List<VehicleInvitationCode> = emptyList(),
    val generatedCode: String? = null,
    val generatedVehicleCode: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val adminRepository = AdminRepository()

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    /**
     * Generate a new invitation code
     */
    fun generateInvitationCode(role: String = "ADMIN_LINEA", lineaId: String = "", expiresInHours: Int = 24, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, generatedCode = null)

            adminRepository.generateInvitationCode(role, lineaId, expiresInHours).fold(
                onSuccess = { code ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generatedCode = code.code,
                        successMessage = "Código generado: ${code.code}"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Generó código de invitación: ${code.code} (rol: $role)")
                    }
                    loadInvitationCodes() // Refresh list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al generar código"
                    )
                }
            )
        }
    }

    /**
     * Load all invitation codes
     */
    fun loadInvitationCodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getInvitationCodes().fold(
                onSuccess = { codes ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        invitationCodes = codes
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar códigos"
                    )
                }
            )
        }
    }

    /**
     * Delete an invitation code
     */
    fun deleteInvitationCode(codeId: String, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.deleteInvitationCode(codeId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Código eliminado"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Eliminó código de invitación: $codeId")
                    }
                    loadInvitationCodes() // Refresh list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al eliminar código"
                    )
                }
            )
        }
    }

    /**
     * Load all lines
     */
    fun loadLineas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("AdminViewModel", "Cargando líneas...")

            adminRepository.getLineas().fold(
                onSuccess = { lineas ->
                    android.util.Log.d("AdminViewModel", "Líneas cargadas exitosamente: ${lineas.size}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lineas = lineas
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("AdminViewModel", "Error cargando líneas: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar líneas"
                    )
                }
            )
        }
    }

    /**
     * Create a new line
     */
    fun createLinea(
        name: String, 
        code: String,
        rutaId: String = "",
        currentUserId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.createLinea(name, code, rutaId).fold(
                onSuccess = { linea ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Línea creada: $name"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Creó línea: $name (código: $code)")
                    }
                    loadLineas() // Refresh list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al crear línea"
                    )
                }
            )
        }
    }

    /**
     * Update a line
     */
    fun updateLinea(
        lineaId: String, 
        name: String, 
        code: String,
        rutaId: String = "",
        currentUserId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.updateLinea(lineaId, name, code, rutaId).fold(
                onSuccess = { linea ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Línea actualizada: $name"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Actualizó línea: $name (código: $code)")
                    }
                    loadLineas() // Refresh list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al actualizar línea"
                    )
                }
            )
        }
    }



    /**
     * Delete a line
     */
    fun deleteLinea(lineaId: String, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.deleteLinea(lineaId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Línea eliminada"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Eliminó línea: $lineaId")
                    }
                    loadLineas() // Refresh list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al eliminar línea"
                    )
                }
            )
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null, generatedCode = null)
    }

    /**
     * Load all logs
     */
    fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getLogs().fold(
                onSuccess = { logs ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = logs
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar logs"
                    )
                }
            )
        }
    }

    /**
     * Create a log entry
     */
    fun createLog(
        userId: String,
        description: String
    ) {
        viewModelScope.launch {
            adminRepository.createLog(
                userId = userId,
                description = description
            )
        }
    }

    // ============ Rutas ============

    /**
     * Crear una nueva ruta
     */
    fun createRuta(
        name: String,
        description: String,
        startPoint: String = "",
        endPoint: String = "",
        lineaId: String = "",
        currentUserId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Ruta creada: $name"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Creó ruta: $name")
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al crear ruta"
                    )
                }
            )
        }
    }

    /**
     * Crear una nueva ruta con coordenadas
     */
    fun createRutaWithCoords(
        name: String,
        description: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        lineaId: String,
        currentUserId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Format coordinates as "lat,lng" string
            val startPoint = if (startLat != 0.0 && startLng != 0.0) {
                "$startLat,$startLng"
            } else {
                ""
            }
            val endPoint = if (endLat != 0.0 && endLng != 0.0) {
                "$endLat,$endLng"
            } else {
                ""
            }

            adminRepository.createRuta(name, description, startPoint, endPoint, lineaId).fold(
                onSuccess = { ruta ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Ruta creada: $name"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Creó ruta: $name (con coordenadas: $startPoint -> $endPoint)")
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al crear ruta"
                    )
                }
            )
        }
    }

    // ============ Vehicle Invitation Codes ============

    /**
     * Generar código de invitación para vehículo
     */
    fun generateVehicleInvitationCode(lineaId: String, creadoPor: String, expiresInHours: Int = 72, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, generatedVehicleCode = null)

            adminRepository.generateVehicleInvitationCode(lineaId, creadoPor, expiresInHours).fold(
                onSuccess = { code ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generatedVehicleCode = code.code,
                        successMessage = "Código de vehículo generado: ${code.code}"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Generó código de vehículo: ${code.code} (línea: $lineaId)")
                    }
                    loadVehicleInvitationCodes(lineaId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al generar código de vehículo"
                    )
                }
            )
        }
    }

    /**
     * Cargar códigos de invitación de vehículo por línea
     */
    fun loadVehicleInvitationCodes(lineaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getVehicleInvitationCodesByLinea(lineaId).fold(
                onSuccess = { codes ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        vehicleCodes = codes
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar códigos de vehículo"
                    )
                }
            )
        }
    }

    /**
     * Eliminar código de invitación de vehículo
     */
    fun deleteVehicleInvitationCode(codeId: String, lineaId: String, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.deleteVehicleInvitationCode(codeId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Código de vehículo eliminado"
                    )
                    // Crear log de la acción
                    if (currentUserId.isNotEmpty()) {
                        createLog(currentUserId, "Eliminó código de vehículo: $codeId")
                    }
                    loadVehicleInvitationCodes(lineaId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al eliminar código de vehículo"
                    )
                }
            )
        }
    }

    /**
     * Limpiar código de vehículo generado
     */
    fun clearVehicleCode() {
        _uiState.value = _uiState.value.copy(generatedVehicleCode = null)
    }
}
