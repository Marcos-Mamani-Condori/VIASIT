package com.oficial.viasit.data.repository

import android.util.Log
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.parseLineasFromJson

/**
 * Repositorio de líneas de transporte.
 */
internal class LineasRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) {
    suspend fun create(name: String, code: String, rutaId: String = ""): Result<Linea> = try {
        val data = mutableMapOf<String, Any>("name" to name, "code" to code)
        if (rutaId.isNotEmpty()) data["ruta_id"] = rutaId
        client.createRecord("lineas", data, shared.authToken()).map { json ->
            Linea(id = AdminJsonParsers.extractStringField(json, "id"), name = name, code = code, rutaId = rutaId)
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getAll(): Result<List<Linea>> = try {
        Log.d("LineasRepository", "Consultando colección lineas...")
        client.getList("lineas", perPage = 100, sort = "name", authToken = shared.authToken()).fold(
            onSuccess = { json ->
                Log.d("LineasRepository", "Respuesta lineas OK")
                Result.success(parseLineasFromJson(json))
            },
            onFailure = { e ->
                Log.e("LineasRepository", "Error getLineas: ${e.message}")
                Result.failure(e)
            }
        )
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getByRuta(rutaId: String): Result<List<Linea>> = try {
        client.getList("lineas", filter = "ruta_id='$rutaId'", authToken = shared.authToken())
            .map { parseLineasFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun update(lineaId: String, name: String, code: String, rutaId: String = ""): Result<Linea> = try {
        val data = mutableMapOf<String, Any>("name" to name, "code" to code)
        if (rutaId.isNotEmpty()) data["ruta_id"] = rutaId
        client.updateRecord("lineas", lineaId, data, shared.authToken())
            .map { Linea(lineaId, name, code, rutaId) }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun delete(lineaId: String): Result<Unit> =
        client.deleteRecord("lineas", lineaId, shared.authToken()).map { }

    suspend fun updateRuta(lineaId: String, rutaId: String): Result<Unit> =
        client.updateRecord("lineas", lineaId, mapOf("ruta_id" to rutaId), shared.authToken()).map { }
}
