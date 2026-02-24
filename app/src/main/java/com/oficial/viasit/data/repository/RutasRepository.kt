package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.extractItemBlocks
import com.oficial.viasit.data.repository.AdminJsonParsers.parseRutaFromJson

/**
 * Repositorio de rutas (creación, consulta, eliminación).
 */
internal class RutasRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) {
    suspend fun create(
        name: String, description: String,
        startPoint: String = "", endPoint: String = "",
        lineaId: String = "", waypoints: String = ""
    ): Result<Ruta> = try {
        val data = mutableMapOf<String, Any>("name" to name, "description" to description)
        if (startPoint.isNotEmpty()) data["start_point"] = startPoint
        if (endPoint.isNotEmpty())   data["end_point"]   = endPoint
        if (lineaId.isNotEmpty())    data["linea_id"]    = lineaId
        if (waypoints.isNotEmpty())  data["waypoints"]   = waypoints
        client.createRecord("rutas", data, shared.authToken()).map { json ->
            Ruta(id = AdminJsonParsers.extractStringField(json, "id"), name = name,
                description = description, startPoint = startPoint, endPoint = endPoint, waypoints = waypoints)
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getById(rutaId: String): Result<Ruta> = try {
        client.getRecord("rutas", rutaId, shared.authToken()).map { parseRutaFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getAll(): Result<List<Ruta>> = try {
        client.getList("rutas", perPage = 100, sort = "name", authToken = shared.authToken())
            .map { json -> extractItemBlocks(json).map { parseRutaFromJson(it) }.filter { it.id.isNotEmpty() } }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun delete(rutaId: String): Result<Unit> =
        client.deleteRecord("rutas", rutaId, shared.authToken()).map { }
}
