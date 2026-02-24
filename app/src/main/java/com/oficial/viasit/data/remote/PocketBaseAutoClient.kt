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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class PocketBaseAutoClient(
    private val okHttpClient: OkHttpClient,
    private val autoDao: AutoData?,
    private val _autos: MutableStateFlow<List<Auto>>
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val TAG = "PocketBaseAutoClient"
        private const val COLLECTION = "autos"
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
                .url("$BASE_URL/api/collections/$COLLECTION/records?perPage=200")
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
                // Evitar duplicados: si ya existe un auto con mismo id o mismo userId, actualizar
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
                Log.d(TAG, "registerAuto: userId=$userId, placa=$placa, lineaCode=$lineaCode")

                val lineaId = if (lineaCode.length == 15 && lineaCode.all { it.isLetterOrDigit() }) {
                    lineaCode
                } else {
                    validateInvitationCode(lineaCode)
                        ?: return@withContext Result.failure(Exception("Codigo de invitacion invalido o expirado"))
                }

                // ✅ UPSERT: buscar si ya existe un auto para este conductor
                val existingAuto = getAutoByUserId(userId).getOrNull()
                if (existingAuto != null) {
                    Log.d(TAG, "Auto existente encontrado (id=${existingAuto.id}), actualizando en lugar de crear")
                    // Reactivar: solo actualizar lat/lng para marcar como activo
                    val body = """{"lat":$lat,"lng":$lng,"angulo":0}"""
                        .toRequestBody("application/json".toMediaType())
                    val resp = okHttpClient.newCall(
                        Request.Builder().url("$BASE_URL/api/collections/$COLLECTION/records/${existingAuto.id}").patch(body).build()
                    ).execute()
                    return@withContext if (resp.isSuccessful) {
                        val updated = json.decodeFromString<Auto>(resp.body?.string() ?: "")
                        updateUserLinea(userId, lineaId)
                        Result.success(updated)
                    } else {
                        Result.failure(Exception("Error al reactivar auto: ${resp.code}"))
                    }
                }

                // Crear nuevo registro solo si no existe
                val body = """{"userid":"$userId","placa":"$placa","lat":$lat,"lng":$lng,"angulo":0}"""
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

    private suspend fun validateInvitationCode(code: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Usar isUsed (camelCase) que es como está en PocketBase según el schema
                val filter = java.net.URLEncoder.encode("code=\"$code\" && isUsed=false", "UTF-8")
                val resp = okHttpClient.newCall(
                    Request.Builder()
                        .url("$BASE_URL/api/collections/vehicle_invitation_codes/records?perPage=1&filter=$filter")
                        .get().build()
                ).execute()
                if (!resp.isSuccessful) return@withContext null
                val obj = json.decodeFromString<JsonObject>(resp.body?.string() ?: "{}")
                val items = obj["items"]?.jsonArray ?: return@withContext null
                if (items.isEmpty()) return@withContext null
                val inv = items[0].jsonObject
                val expiresAt = inv["expiresAt"]?.jsonPrimitive?.content
                if (expiresAt != null) {
                    try {
                        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
                        val exp = java.time.LocalDateTime.parse(expiresAt, fmt).atZone(ZoneId.systemDefault()).toInstant()
                        if (exp.isBefore(Instant.now())) return@withContext null
                    } catch (_: Exception) {}
                }
                inv["linea_id"]?.jsonPrimitive?.content
            } catch (e: Exception) { Log.e(TAG, "validateInvitationCode", e); null }
        }
    }

    suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return withContext(Dispatchers.IO) {
            try {
                // Para relaciones en PocketBase, usar userid.id para filtrar por ID
                // Esto funciona tanto para relaciones simples como múltiples (arrays)
                val filter = "userid.id=\"$userId\""
                val encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8")
                val url = "$BASE_URL/api/collections/$COLLECTION/records?perPage=1&filter=$encodedFilter"
                Log.d(TAG, "Buscando auto con URL: $url")
                val resp = okHttpClient.newCall(Request.Builder().url(url).get().build()).execute()
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    Log.d(TAG, "Respuesta búsqueda auto: $responseBody")
                    val list = json.decodeFromString<ListResponse>(responseBody)
                    Log.d(TAG, "Autos encontrados: ${list.items.size}")
                    Result.success(list.items.firstOrNull())
                } else {
                    Log.e(TAG, "Error al buscar auto: ${resp.code}")
                    Result.failure(Exception("Error al buscar auto: ${resp.code}"))
                }
            } catch (e: Exception) { Log.e(TAG, "getAutoByUserId", e); Result.failure(e) }
        }
    }

    suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = """{"lat":$lat,"lng":$lng,"angulo":$angulo}"""
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
                val body = """{"lat":$lat,"lng":$lng,"angulo":$angulo}"""
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
