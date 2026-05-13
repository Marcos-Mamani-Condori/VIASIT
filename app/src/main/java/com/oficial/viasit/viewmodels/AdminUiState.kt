package com.oficial.viasit.viewmodels

import com.oficial.viasit.domain.model.*

data class AdminUiState(
    val isLoading: Boolean = false,
    val lineas: List<Linea> = emptyList(),
    val invitationCodes: List<InvitationCode> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val rutas: List<Ruta> = emptyList(),
    val users: List<User> = emptyList(), // Lista de conductores/usuarios
    val reportes: List<Reporte> = emptyList(), // Lista de reportes/quejas
    val appeals: List<LogEntry> = emptyList(), // Lista de apelaciones
    val generatedCode: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)
