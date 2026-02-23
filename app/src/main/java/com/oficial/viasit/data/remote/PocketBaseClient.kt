package com.oficial.viasit.data.remote

import android.util.Log
import com.oficial.viasit.BuildConfig
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class PocketBaseRealtimeClient(private val autoDao: AutoData? = null) {

    companion object {
        private const val TAG = "PocketBaseClient"
        private const val COLLECTION = "autos"
        private val BASE_URL: String get() = BuildConfig.POCKETBASE_URL
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpclient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _autos = MutableStateFlow<List<Auto>>(emptyList())
    val autos: StateFlow<List<Auto>> = _autos.asStateFlow()

    private var eventSource: EventSource? = null
    private var clientId: String? = null

    // Delegados
    private val autoClient = PocketBaseAutoClient(okHttpclient, autoDao, _autos)
    private val http = PocketBaseHttpClient(okHttpclient)

    var authToken: String? = null

    @Serializable
    data class RealtimeEvent(val action: String, val record: Auto? = null)

    fun startRealtimeSubscription() {
        if (eventSource != null) return
        scope.launch {
            try {
                autoClient.fetchAutos()
                connectSSE()
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando SSE", e)
                delay(5000)
                startRealtimeSubscription()
            }
        }
    }

    private fun connectSSE() {
        val request = Request.Builder()
            .url("$BASE_URL/api/realtime?clientId=${System.currentTimeMillis()}")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE conectado")
            }
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                scope.launch { handleSSEEvent(type, data) }
            }
            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE cerrado")
            }
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE error: ${t?.message}")
                scope.launch {
                    delay(5000)
                    if (this@PocketBaseRealtimeClient.eventSource != null) connectSSE()
                }
            }
        }

        eventSource = EventSources.createFactory(okHttpclient).newEventSource(request, listener)
    }

    private suspend fun handleSSEEvent(type: String?, data: String) {
        try {
            when (type) {
                "PB_CONNECT" -> {
                    val connectData = json.decodeFromString<Map<String, String>>(data)
                    clientId = connectData["clientId"]
                    clientId?.let { subscribeToCollection(it) }
                }
                null -> {
                    val event = json.decodeFromString<RealtimeEvent>(data)
                    autoClient.applyRealtimeUpdate(event.action, event.record)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando SSE: $data", e)
        }
    }

    private suspend fun subscribeToCollection(clientId: String) {
        try {
            val body = FormBody.Builder()
                .add("clientId", clientId)
                .add("subscriptions", "$COLLECTION/*")
                .build()
            val request = Request.Builder().url("$BASE_URL/api/realtime").post(body).build()
            withContext(Dispatchers.IO) {
                okHttpclient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) Log.e(TAG, "Error suscribiéndose: ${resp.code}")
                }
            }
            autoClient.fetchAutos()
        } catch (e: Exception) {
            Log.e(TAG, "Error suscribiéndose", e)
        }
    }

    fun stopRealtimeSubscription() {
        eventSource?.cancel()
        eventSource = null
        clientId = null
    }

    fun release() {
        stopRealtimeSubscription()
        scope.cancel()
    }

    fun isConnected(): Boolean = eventSource != null

    // Delegación a PocketBaseAutoClient
    suspend fun fetchAutos() = autoClient.fetchAutos()
    suspend fun registerAuto(userId: String, placa: String, lineaCode: String, lat: Double, lng: Double) =
        autoClient.registerAuto(userId, placa, lineaCode, lat, lng)
    suspend fun getAutoByUserId(userId: String) = autoClient.getAutoByUserId(userId)
    suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double) =
        autoClient.updateAutoLocation(autoId, lat, lng, angulo)
    suspend fun updateAutoLocation(userId: String, lat: Double, lng: Double, angulo: Float) =
        autoClient.updateAutoLocation(userId, lat, lng, angulo)

    // Delegación a PocketBaseHttpClient
    suspend fun createRecord(collection: String, data: Map<String, Any>, authToken: String? = null) =
        http.createRecord(collection, data, authToken)
    suspend fun updateRecord(collection: String, recordId: String, data: Map<String, Any>, authToken: String? = null) =
        http.updateRecord(collection, recordId, data, authToken)
    suspend fun deleteRecord(collection: String, recordId: String, authToken: String? = null) =
        http.deleteRecord(collection, recordId, authToken)
    suspend fun getRecord(collection: String, recordId: String, authToken: String? = null) =
        http.getRecord(collection, recordId, authToken)
    suspend fun getList(collection: String, page: Int = 1, perPage: Int = 30, filter: String = "", sort: String = "", authToken: String? = null) =
        http.getList(collection, page, perPage, filter, sort, authToken)
}
