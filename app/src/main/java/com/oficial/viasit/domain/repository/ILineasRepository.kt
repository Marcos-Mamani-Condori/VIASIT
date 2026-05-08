package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Linea

interface ILineasRepository {
    suspend fun getAll(): Result<List<Linea>>
    suspend fun getByRuta(rutaId: String): Result<List<Linea>>
    suspend fun create(name: String, code: String, rutaId: String = ""): Result<Linea>
    suspend fun update(lineaId: String, name: String, code: String, rutaId: String = ""): Result<Linea>
    suspend fun delete(lineaId: String): Result<Unit>
    suspend fun updateRuta(lineaId: String, rutaId: String): Result<Unit>
}
