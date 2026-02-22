package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.local.toAutos
import com.oficial.viasit.data.local.toEntities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * Cliente PocketBase usando solo OkHttp + SSE
 * Mucho más eficiente que polling - solo consume recursos cuando hay cambios
 */
class PocketBaseRealtimeClient(
    private val autoDao: AutoData? = null
) {
    companion object {
        private const val TAG = "PocketBaseClient"
        private const val COLLECTION = "autos"
        
        // Usar BuildConfig para environment
        private val BASE_URL: String
            get() = BuildConfig.POCKETBASE_URL
    }

    // CoroutineScope con Job para poder cancelarlo
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Cliente OkHttp para todas las operaciones
    private val okHttpclient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Sin timeout para SSE
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Estado de autos
    private val _autos = MutableStateFlow<List<Auto>>(emptyList())
    val autos: StateFlow<List<Auto>> = _autos.asStateFlow()

    private var eventSource: EventSource? = null
    private var clientId: String? = null

    /**
     * Respuesta de lista de PocketBase
     */
    @Serializable
    data class ListResponse(
        val page: Int = 1,
        val perPage: Int = 30,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<Auto> = emptyList()
    )

    /**
     * Modelo para eventos SSE de PocketBase
     */
    @Serializable
    data class RealtimeEvent(
        val action: String,
        val record: Auto? = null
    )

    /**
     * Iniciar suscripción realtime con SSE
     */
    fun startRealtimeSubscription() {
        if (eventSource != null) {
            Log.d(TAG, "SSE ya está activo")
            return
        }

        scope.launch {
            try {
                // 1. Cargar datos iniciales
                fetchAutos()

                // 2. Conectar a SSE
                connectSSE()
                
                Log.d(TAG, "SSE Realtime iniciado")
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando SSE", e)
                // Reintentar después de 5 segundos
                delay(5000)
                startRealtimeSubscription()
            }
        }
    }

    /**
     * Conectar al endpoint SSE de PocketBase
     */
    private fun connectSSE() {
        // URL del endpoint SSE de PocketBase con suscripción a la colección
        val sseUrl = "$BASE_URL/api/realtime?clientId=${System.currentTimeMillis()}"
        
        val request = Request.Builder()
            .url(sseUrl)
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE conectado exitosamente")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                scope.launch {
                    handleSSEEvent(type, data)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE cerrado")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                Log.e(TAG, "SSE error: ${t?.message}", t)
                // Reconectar después de 5 segundos
                scope.launch {
                    delay(5000)
                    if (this@PocketBaseRealtimeClient.eventSource != null) {
                        connectSSE()
                    }
                }
            }
        }

        eventSource = EventSources.createFactory(okHttpclient)
            .newEventSource(request, listener)
    }

    /**
     * Manejar eventos SSE
     */
    private suspend fun handleSSEEvent(type: String?, data: String) {
        try {
            when (type) {
                "PB_CONNECT" -> {
                    // Obtener clientId y suscribirse a la colección
                    val connectData = json.decodeFromString<Map<String, String>>(data)
                    clientId = connectData["clientId"]
                    clientId?.let { subscribeToCollection(it) }
                }
                null -> {
                    // Evento de cambio en la colección
                    val event = json.decodeFromString<RealtimeEvent>(data)
                    handleRealtimeEvent(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando evento SSE: $data", e)
        }
    }

    /**
     * Suscribirse a una colección específica
     */
    private suspend fun subscribeToCollection(clientId: String) {
        try {
            // Enviar petición POST para suscribirse
            val subscribeUrl = "$BASE_URL/api/realtime"
            
            val requestBody = FormBody.Builder()
                .add("clientId", clientId)
                .add("subscriptions", "$COLLECTION/*")
                .build()
            
            val request = Request.Builder()
                .url(subscribeUrl)
                .post(requestBody)
                .build()
            
            withContext(Dispatchers.IO) {
                okHttpclient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Suscrito a colección: $COLLECTION")
                    } else {
                        Log.e(TAG, "Error suscribiéndose: ${response.code}")
                    }
                }
            }
            
            // Refrescar datos después de suscribirse
            fetchAutos()
        } catch (e: Exception) {
            Log.e(TAG, "Error suscribiéndose a colección", e)
        }
    }

    /**
     * Manejar eventos realtime de cambios en registros
     */
    private suspend fun handleRealtimeEvent(event: RealtimeEvent) {
        Log.d(TAG, "Evento realtime: ${event.action}")
        
        val currentList = _autos.value.toMutableList()
        
        when (event.action) {
            "create" -> {
                event.record?.let { newAuto ->
                    currentList.add(newAuto)
                    Log.d(TAG, "Auto creado: ${newAuto.placa}")
                }
            }
            "update" -> {
                event.record?.let { updatedAuto ->
                    val index = currentList.indexOfFirst { it.id == updatedAuto.id }
                    if (index != -1) {
                        currentList[index] = updatedAuto
                        Log.d(TAG, "Auto actualizado: ${updatedAuto.placa}")
                    }
                }
            }
            "delete" -> {
                event.record?.let { deletedAuto ->
                    currentList.removeIf { it.id == deletedAuto.id }
                    Log.d(TAG, "Auto eliminado: ${deletedAuto.placa}")
                }
            }
        }
        
        _autos.value = currentList
        
        // Sincronizar con Room (convertir Auto a AutoEntity)
        autoDao?.insertOrUpdateAutos(currentList.toEntities())
    }

    /**
     * Detener suscripción SSE y limpiar recursos
     */
    fun stopRealtimeSubscription() {
        eventSource?.cancel()
        eventSource = null
        clientId = null
        Log.d(TAG, "SSE detenido")
    }

    /**
     * Liberar todos los recursos del cliente
     * Debe llamarse cuando ya no se necesite el cliente
     */
    fun release() {
        stopRealtimeSubscription()
        scope.cancel("Liberando recursos PocketBaseClient")
        Log.d(TAG, "Recursos liberados")
    }

    /**
     * Obtener todos los autos desde PocketBase usando HTTP directo
     */
    suspend fun fetchAutos() {
        try {
            val url = "$BASE_URL/api/collections/$COLLECTION/records?perPage=200"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val responseBody = withContext(Dispatchers.IO) {
                okHttpclient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else {
                        Log.e(TAG, "Error HTTP: ${response.code}")
                        null
                    }
                }
            }
            
            responseBody?.let { body ->
                val listResponse = json.decodeFromString<ListResponse>(body)
                _autos.value = listResponse.items
                
                // Guardar en Room para caché offline (convertir Auto a AutoEntity)
                autoDao?.insertOrUpdateAutos(listResponse.items.toEntities())
                
                Log.d(TAG, "Autos cargados: ${listResponse.items.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetchAutos: ${e.message}", e)
            // En caso de error, intentar cargar desde caché
            loadFromCache()
        }
    }

    /**
     * Cargar datos desde caché local cuando falla la conexión
     */
    private suspend fun loadFromCache() {
        try {
            autoDao?.getAllAutos()?.first()?.let { cachedAutos ->
                if (cachedAutos.isNotEmpty()) {
                    // Convertir AutoEntity a Auto
                    _autos.value = cachedAutos.toAutos()
                    Log.d(TAG, "Datos cargados desde caché: ${cachedAutos.size} autos")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando desde caché", e)
        }
    }

    fun isConnected(): Boolean = eventSource != null

    // ============ Métodos Genéricos para Admin ============

    /**
     * Crear un registro en cualquier colección
     */
    suspend fun createRecord(collection: String, data: Map<String, Any>, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = buildJsonString(data)
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records")
                    .post(requestBody)
                
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }

                val response = okHttpclient.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Result.success(body ?: "{}")
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Result.failure(Exception("Error creando registro: ${response.code} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error createRecord", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualizar un registro
     */
    suspend fun updateRecord(collection: String, recordId: String, data: Map<String, Any>, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = buildJsonString(data)
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records/$recordId")
                    .patch(requestBody)
                
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }

                val response = okHttpclient.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Result.success(body ?: "{}")
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Result.failure(Exception("Error actualizando registro: ${response.code} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updateRecord", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Eliminar un registro
     */
    suspend fun deleteRecord(collection: String, recordId: String, authToken: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/collections/$collection/records/$recordId")
                    .delete()
                
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }

                val response = okHttpclient.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Result.failure(Exception("Error eliminando registro: ${response.code} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleteRecord", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener un registro por ID
     */
    suspend fun getRecord(collection: String, recordId: String, authToken: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/api/collections/$collection/records/$recordId"

                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }

                val response = okHttpclient.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Result.success(body ?: "{}")
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Result.failure(Exception("Error obteniendo registro: ${response.code} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getRecord", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener lista de registros con filtros opcionales
     */
    suspend fun getList(
        collection: String,
        page: Int = 1,
        perPage: Int = 30,
        filter: String = "",
        sort: String = "",
        authToken: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var url = "$BASE_URL/api/collections/$collection/records?page=$page&perPage=$perPage"
                if (filter.isNotEmpty()) {
                    url += "&filter=${java.net.URLEncoder.encode(filter, "UTF-8")}"
                }
                if (sort.isNotEmpty()) {
                    url += "&sort=${java.net.URLEncoder.encode(sort, "UTF-8")}"
                }

                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer $authToken")
                }

                val response = okHttpclient.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Result.success(body ?: "{}")
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Result.failure(Exception("Error obtiendo lista: ${response.code} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getList", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Construir string JSON desde Map
     */
    private fun buildJsonString(data: Map<String, Any>): String {
        return data.entries.joinToString(",", "{", "}") { (key, value) ->
            "\"$key\": ${when (value) {
                is String -> "\"$value\""
                is Boolean -> value.toString()
                is Number -> value.toString()
                else -> "\"$value\""
            }}"
        }
    }

    /**
     * Registrar un nuevo auto
     * NOTA: La línea se asigna al usuario (linea_id en users), no al vehículo
     */
    suspend fun registerAuto(
        userId: String,
        placa: String,
        lineaCode: String,
        lat: Double,
        lng: Double
    ): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                // Primero validar el código de invitación y obtener el ID de la línea
                val lineaId = validateInvitationCode(lineaCode)
                if (lineaId == null) {
                    return@withContext Result.failure(Exception("Codigo de invitacion invalido o expirado"))
                }

                // Crear el vehículo SIN linea_id (se obtiene del usuario)
                val requestBody = """
                    {
                        "userid": "$userId",
                        "placa": "$placa",
                        "lat": $lat,
                        "lng": $lng,
                        "angulo": 0
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/$COLLECTION/records")
                    .post(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val auto = json.decodeFromString<Auto>(body ?: "")
                    Log.d(TAG, "Auto registrado: ${auto.placa}")
                    
                    // Actualizar el linea_id del usuario
                    updateUserLinea(userId, lineaId)
                    
                    Result.success(auto)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error registerAuto: ${response.code} - $error")
                    Result.failure(Exception("Error al registrar auto: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registerAuto", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Actualizar el linea_id del usuario
     */
    private suspend fun updateUserLinea(userId: String, lineaId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """
                    {
                        "linea_id": "$lineaId"
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/users/records/$userId")
                    .patch(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Usuario actualizado con linea_id: $lineaId")
                    true
                } else {
                    Log.e(TAG, "Error actualizando usuario: ${response.code}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updateUserLinea", e)
                false
            }
        }
    }

    /**
     * Validar código de invitación y obtener el ID de la línea
     */
    private suspend fun validateInvitationCode(code: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Filtro combinado: code y isUsed=false en una sola query
                val filter = java.net.URLEncoder.encode("code=\"$code\" && isUsed=false", "UTF-8")
                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/vehicle_invitation_codes/records?perPage=1&filter=$filter")
                    .get()
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val jsonResponse = json.decodeFromString<JsonObject>(body ?: "{}")
                    val items = jsonResponse["items"]?.jsonArray
                    
                    if (items != null && items.isNotEmpty()) {
                        val invitation = items[0].jsonObject
                        // Verificar si no ha expirado
                        val expiresAt = invitation["expiresAt"]?.jsonPrimitive?.content
                        if (expiresAt != null) {
                            val expirationDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(expiresAt)
                            if (expirationDate != null && expirationDate.before(java.util.Date())) {
                                Log.w(TAG, "Codigo de invitacion expirado")
                                return@withContext null
                            }
                        }
                        // Retornar el ID de la línea (campo linea_id en PocketBase)
                        val lineaId = invitation["linea_id"]?.jsonPrimitive?.content
                        Log.d(TAG, "Codigo valido para linea: $lineaId")
                        return@withContext lineaId
                    }
                    Log.w(TAG, "Codigo de invitacion no encontrado")
                    null
                } else {
                    Log.e(TAG, "Error validating invitation code: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validateInvitationCode", e)
                null
            }
        }
    }

    /**
     * Obtener el auto de un conductor específico
     */
    suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return withContext(Dispatchers.IO) {
            try {
                // Filtrar por userid (relación simple) - usar = para coincidencia exacta
                val url = "$BASE_URL/api/collections/$COLLECTION/records?perPage=1&filter=userid%3D%27$userId%27"
                Log.d(TAG, "getAutoByUserId URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val listResponse = json.decodeFromString<ListResponse>(body ?: "")
                    val auto = listResponse.items.firstOrNull()
                    Log.d(TAG, "Auto del usuario $userId: ${auto?.placa ?: "no encontrado"}")
                    Result.success(auto)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error getAutoByUserId: ${response.code} - $error")
                    Result.failure(Exception("Error al buscar auto: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getAutoByUserId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualizar ubicación del auto por ID
     */
    suspend fun updateAutoLocation(
        autoId: String,
        lat: Double,
        lng: Double,
        angulo: Double
    ): Result<Auto> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """
                    {
                        "lat": $lat,
                        "lng": $lng,
                        "angulo": $angulo
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/$COLLECTION/records/$autoId")
                    .patch(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val auto = json.decodeFromString<Auto>(body ?: "")
                    Log.d(TAG, "Ubicación actualizada: ${auto.placa}")
                    Result.success(auto)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error updateAutoLocation: ${response.code} - $error")
                    Result.failure(Exception("Error al actualizar ubicación: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updateAutoLocation", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualizar ubicación del auto buscando por userId
     * Este método es usado por LocationTrackingService
     */
    suspend fun updateAutoLocation(
        userId: String,
        lat: Double,
        lng: Double,
        angulo: Float
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Primero obtener el auto del usuario
                val getResult = getAutoByUserId(userId)
                val autoId = getResult.getOrNull()?.id
                
                if (autoId == null) {
                    Log.w(TAG, "No se encontró auto para userId: $userId")
                    return@withContext Result.failure(Exception("Auto no encontrado"))
                }
                
                val requestBody = """
                    {
                        "lat": $lat,
                        "lng": $lng,
                        "angulo": $angulo
                    }
                """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/collections/$COLLECTION/records/$autoId")
                    .patch(requestBody)
                    .build()

                val response = okHttpclient.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d(TAG, "Ubicación actualizada para userId $userId: ($lat, $lng)")
                    Result.success(Unit)
                } else {
                    val error = response.body?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error updateAutoLocation: ${response.code} - $error")
                    Result.failure(Exception("Error al actualizar ubicación: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updateAutoLocation", e)
                Result.failure(e)
            }
        }
    }
}
