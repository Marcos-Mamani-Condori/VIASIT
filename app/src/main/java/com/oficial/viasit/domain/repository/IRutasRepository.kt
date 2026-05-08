package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Ruta

interface IRutasRepository {
    suspend fun getAll(): Result<List<Ruta>>
    suspend fun getById(rutaId: String): Result<Ruta>
    suspend fun create(
        name: String,
        description: String,
        startPoint: String = "",
        endPoint: String = "",
        lineaId: String = "",
        waypoints: String = ""
    ): Result<Ruta>
    suspend fun delete(rutaId: String): Result<Unit>
}
