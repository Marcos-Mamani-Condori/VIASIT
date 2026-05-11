package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.local.toAutos
import com.oficial.viasit.data.local.toEntities
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PocketBaseAutoClient(
    private val okHttpClient: OkHttpClient,
    private val autoDao: AutoData?,
    private val _autos: MutableStateFlow<List<Auto>>
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val TAG = "PocketBaseAutoClient"
        private const val COLLECTION = "vehicles"
        private val BASE_URL: String get() = BuildConfig.POCKETBASE_URL

        fun create(autoDao: AutoData?, autos: MutableStateFlow<List<Auto>>): PocketBaseAutoClient =
            PocketBaseAutoClient(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build(),
                autoDao,
                autos
            )
    }

    @Serializable
    data class ListResponse(
        val page: Int = 1,
        val perPage: Int = 30,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<Auto> = emptyList()
    )

    suspend fun fetchAutos() {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/collections/$COLLECTION/records?perPage=200&expand=userid")
                .get().build()
            val responseBody = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string()
                    else { Log.e(TAG, "fetchAutos HTTP ${resp.code}"); null }
                }
            }
            responseBody?.let { body ->
                val list = json.decodeFromString<ListResponse>(body)
                _autos.value = list.items
                autoDao?.insertOrUpdateAutos(list.items.toEntities())
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAutos: ${e.message}", e)
            loadFromCache()
        }
    }

    suspend fun loadFromCache() {
        try {
            autoDao?.getAllAutos()?.collect { cached ->
                if (cached.isNotEmpty()) _autos.value = cached.toAutos()
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadFromCache: ${e.message}", e)
        }
    }

    fun applyRealtimeUpdate(action: String, record: Auto?) {
        if (record == null) return
        val list = _autos.value.toMutableList()
        when (action) {
            "create" -> {
                val existingIdx = list.indexOfFirst { it.id == record.id || it.userId == record.userId }
                if (existingIdx != -1) list[existingIdx] = record
                else list.add(record)
            }
            "update" -> {
                val i = list.indexOfFirst { it.id == record.id }
                if (i != -1) list[i] = record else list.add(record)
            }
            "delete" -> list.removeIf { it.id == record.id }
        }
        _autos.value = list
    }

    suspend fun registerAuto(userId: String, placa: String, lineaCode: String, lat: Double, lng: Double): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "registerAuto: userId=$userId, placa=$placa, lineaId=$lineaCode")
                
                // Usamos el lineaId directamente (asignado previamente al usuario)
                val lineaId = lineaCode

                val existingAuto = getAutoByUserId(userId).getOrNull()
                if (existingAuto != null) {
                    Log.d(TAG, "Re-vinculando auto existente: id=${existingAuto.id}")
                    val body = """{"plate":"$placa","lat":$lat,"lng":$lng,"angle":0,"lineaId":"$lineaId"}"""
                        .toRequestBody("application/json".toMediaType())
                    val resp = okHttpClient.newCall(
                        Request.Builder().url("$BASE_URL/api/collections/$COLLECTION/records/${existingAuto.id}").patch(body).build()
                    ).execute()
                    return@withContext if (resp.isSuccessful) {
                        val updated = json.decodeFromString<Auto>(resp.body?.string() ?: "")
                        updateUserLinea(userId, lineaId)
                        Result.success(updated)
                    } else {
                        Result.failure(Exception("Error al actualizar vehículo: ${resp.code}"))
                    }
                }

                val body = "{\"userid\":\"$userId\",\"plate\":\"$placa\",\"lat\":$lat,\"lng\":$lng,\"angle\":0,\"lineaId\":\"$lineaId\"}"
                    .toRequestBody("application/json".toMediaType())
                Log.d(TAG, "Creando nuevo auto...")
                val resp = okHttpClient.newCall(
                    Request.Builder().url("$BASE_URL/api/collections/$COLLECTION/records").post(body).build()
                ).execute()

                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val auto = json.decodeFromString<Auto>(responseBody)
                    updateUserLinea(userId, lineaId)
                    Result.success(auto)
                } else {
                    val errorBody = resp.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Error al registrar auto: ${resp.code} - $errorBody")
                    Result.failure(Exception("Error al registrar auto: ${resp.code}"))
                }
            } catch (e: Exception) { Log.e(TAG, "registerAuto", e); Result.failure(e) }
        }
    }

    private suspend fun updateUserLinea(userId: String, lineaId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = """{"lineaId":"$lineaId"}""".toRequestBody("application/json".toMediaType())
                val resp = okHttpClient.newCall(
                    Request.Builder().url("$BASE_URL/api/collections/users/records/$userId").patch(body).build()
                ).execute()
                resp.isSuccessful
            } catch (e: Exception) { Log.e(TAG, "updateUserLinea", e); false }
        }
    }

    suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return withContext(Dispatchers.IO) {
            try {
                val filter = "userid.id=\"$userId\""
                val encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8")
                val url = "$BASE_URL/api/collections/$COLLECTION/records?perPage=1&filter=$encodedFilter&expand=userid"
                val resp = okHttpClient.newCall(Request.Builder().url(url).get().build()).execute()
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val list = json.decodeFromString<ListResponse>(responseBody)
                    Result.success(list.items.firstOrNull())
                } else {
                    Result.failure(Exception("Error al buscar auto: ${resp.code}"))
                }
            } catch (e: Exception) { Log.e(TAG, "getAutoByUserId", e); Result.failure(e) }
        }
    }

    suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = """{"lat":$lat,"lng":$lng,"angle":$angulo}"""
                    .toRequestBody("application/json".toMediaType())
                val resp = okHttpClient.newCall(
                    Request.Builder().url("$BASE_URL/api/collections/$COLLECTION/records/$autoId").patch(body).build()
                ).execute()
                if (resp.isSuccessful)
                    Result.success(json.decodeFromString(resp.body?.string() ?: ""))
                else
                    Result.failure(Exception("Error al actualizar ubicación: ${resp.code}"))
            } catch (e: Exception) { Log.e(TAG, "updateAutoLocation(id)", e); Result.failure(e) }
        }
    }

    suspend fun updateAutoLocation(userId: String, lat: Double, lng: Double, angulo: Float): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val autoId = getAutoByUserId(userId).getOrNull()?.id
                    ?: return@withContext Result.failure(Exception("Auto no encontrado"))
                val body = """{"lat":$lat,"lng":$lng,"angle":$angulo}"""
                    .toRequestBody("application/json".toMediaType())
                val resp = okHttpClient.newCall(
                    Request.Builder().url("$BASE_URL/api/collections/$COLLECTION/records/$autoId").patch(body).build()
                ).execute()
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Error al actualizar ubicación: ${resp.code}"))
            } catch (e: Exception) { Log.e(TAG, "updateAutoLocation(userId)", e); Result.failure(e) }
        }
    }
}
