package com.oficial.viasit.viewmodels

data class VehicleFormState(
    val placa: String = "",
    val linea: String = "",
    val placaError: String? = null,
    val lineaError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
