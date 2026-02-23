package com.oficial.viasit.viewmodels

/**
 * Estado del formulario de registro de vehículo.
 *
 * Usado por [AutosViewModel] para gestionar la información
 * que el conductor introduce al registrar su vehículo:
 *
 *  - [placa]      → matrícula del vehículo
 *  - [linea]      → código de invitación de la línea
 *  - [isLoading]  → petición en curso
 *  - [isSuccess]  → registro completado exitosamente
 *  - [error]      → mensaje de error si falló el registro
 */
data class VehicleFormState(
    val placa: String = "",
    val linea: String = "",
    val placaError: String? = null,
    val lineaError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
