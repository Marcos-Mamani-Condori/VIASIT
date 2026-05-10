package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.parseLineasFromJson
import com.oficial.viasit.domain.repository.ILineasRepository

internal class LineasRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : ILineasRepository {
    override suspend fun create(name: String, code: String, rutaId: String): Result<Linea> = try {
        val data = mutableMapOf<String, Any>("name" to name, "code" to code)
        if (rutaId.isNotEmpty()) data["routeId"] = rutaId
        client.createRecord("lines", data, shared.authToken()).map { json ->
            Linea(id = AdminJsonParsers.extractStringField(json, "id"), name = name, code = code, rutaId = rutaId)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAll(): Result<List<Linea>> = try {
        client.getList("lines", perPage = 100, sort = "name", authToken = shared.authToken())
            .map { parseLineasFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getByRuta(rutaId: String): Result<List<Linea>> = try {
        client.getList("lines", filter = "routeId='$rutaId'", authToken = shared.authToken())
            .map { parseLineasFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun update(lineaId: String, name: String, code: String, rutaId: String): Result<Linea> = try {
        val data = mutableMapOf<String, Any>("name" to name, "code" to code)
        if (rutaId.isNotEmpty()) data["routeId"] = rutaId
        client.updateRecord("lines", lineaId, data, shared.authToken())
            .map { Linea(lineaId, name, code, rutaId) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun delete(lineaId: String): Result<Unit> =
        client.deleteRecord("lines", lineaId, shared.authToken()).map { }

    override suspend fun updateRuta(lineaId: String, rutaId: String): Result<Unit> =
        client.updateRecord("lines", lineaId, mapOf("routeId" to rutaId), shared.authToken()).map { }
}
