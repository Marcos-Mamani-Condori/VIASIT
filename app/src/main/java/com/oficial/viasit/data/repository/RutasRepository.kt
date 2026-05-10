package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.extractItemBlocks
import com.oficial.viasit.data.repository.AdminJsonParsers.parseRutaFromJson
import com.oficial.viasit.domain.repository.IRutasRepository

internal class RutasRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : IRutasRepository {
    suspend fun create(
        name: String, description: String,
        startPoint: String = "", endPoint: String = "",
        lineaId: String = "", waypoints: String = ""
    ): Result<Ruta> = try {
        val data = mutableMapOf<String, Any>("name" to name, "description" to description)
        if (startPoint.isNotEmpty()) data["startPoint"] = startPoint
        if (endPoint.isNotEmpty())   data["endPoint"]   = endPoint
        if (waypoints.isNotEmpty())  data["waypoints"]   = waypoints
        client.createRecord("routes", data, shared.authToken()).map { json ->
            Ruta(id = AdminJsonParsers.extractStringField(json, "id"), name = name,
                description = description, startPoint = startPoint, endPoint = endPoint, waypoints = waypoints)
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getById(rutaId: String): Result<Ruta> = try {
        client.getRecord("routes", rutaId, shared.authToken()).map { parseRutaFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getAll(): Result<List<Ruta>> = try {
        client.getList("routes", perPage = 100, sort = "name", authToken = shared.authToken())
            .map { json -> extractItemBlocks(json).map { parseRutaFromJson(it) }.filter { it.id.isNotEmpty() } }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun delete(rutaId: String): Result<Unit> =
        client.deleteRecord("routes", rutaId, shared.authToken()).map { }
}
