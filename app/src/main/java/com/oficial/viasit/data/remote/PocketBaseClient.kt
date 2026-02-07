package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.data.local.AutoData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
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
        
        // Sincronizar con Room
        autoDao?.insertOrUpdateAutos(currentList)
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
                
                // Guardar en Room para caché offline
                autoDao?.insertOrUpdateAutos(listResponse.items)
                
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
                    _autos.value = cachedAutos
                    Log.d(TAG, "Datos cargados desde caché: ${cachedAutos.size} autos")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando desde caché", e)
        }
    }

    fun isConnected(): Boolean = eventSource != null
}
