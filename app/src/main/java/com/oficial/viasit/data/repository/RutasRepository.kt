package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.repository.IRutasRepository

internal class RutasRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : IRutasRepository {
    override suspend fun create(
        name: String, description: String,
        startPoint: String, endPoint: String,
        lineaId: String, waypoints: String
    ): Result<Ruta> = try {
        android.util.Log.d("LLAMADOS", ">>> CREANDO RUTA: name=$name, line=$lineaId")
        val data = mutableMapOf<String, Any>(
            "name" to name, 
            "description" to description
        )
        if (lineaId.isNotEmpty()) data["lineId"] = lineaId // Solo enviar si no está vacío
        if (startPoint.isNotEmpty()) data["startPoint"] = startPoint
        if (endPoint.isNotEmpty())   data["endPoint"]   = endPoint
        if (waypoints.isNotEmpty())  data["waypoints"]   = waypoints
        
        val result = client.createRecord("routes", data, shared.authToken())
        
        result.fold(
            onSuccess = { json ->
                android.util.Log.d("LLAMADOS", ">>> RUTA CREADA EXITOSAMENTE: $json")
                Result.success(Ruta(
                    id = extractStringField(json, "id"), 
                    name = name,
                    description = description, 
                    startPoint = startPoint, 
                    endPoint = endPoint, 
                    waypoints = waypoints
                ))
            },
            onFailure = { e ->
                android.util.Log.e("LLAMADOS", ">>> ERROR AL CREAR RUTA: ${e.message}")
                Result.failure(e)
            }
        )
    } catch (e: Exception) { 
        android.util.Log.e("LLAMADOS", ">>> EXCEPCION AL CREAR RUTA: ${e.message}")
        Result.failure(e) 
    }

    override suspend fun getById(rutaId: String): Result<Ruta> = try {
        client.getRecord("routes", rutaId, shared.authToken()).map { parseRutaFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getByLinea(lineaId: String): Result<List<Ruta>> = try {
        client.getList("routes", filter = "lineId='$lineaId'", sort = "name", authToken = shared.authToken())
            .map { json -> extractItemBlocks(json).map { parseRutaFromJson(it) }.filter { it.id.isNotEmpty() } }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAll(): Result<List<Ruta>> = try {
        client.getList("routes", perPage = 100, sort = "name", authToken = shared.authToken())
            .map { json -> extractItemBlocks(json).map { parseRutaFromJson(it) }.filter { it.id.isNotEmpty() } }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun delete(rutaId: String): Result<Unit> =
        client.deleteRecord("routes", rutaId, shared.authToken()).map { }
}
