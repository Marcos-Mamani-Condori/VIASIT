package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.domain.model.VehicleInvitationCode

data class AdminUiState(
    val isLoading: Boolean = false,
    val lineas: List<Linea> = emptyList(),
    val invitationCodes: List<InvitationCode> = emptyList(),
    val vehicleCodes: List<VehicleInvitationCode> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val rutas: List<Ruta> = emptyList(),
    val generatedCode: String? = null,
    val generatedVehicleCode: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)
