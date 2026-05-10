package com.oficial.viasit.data.repository

import android.util.Log
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class AutoRepository(
    private val pocketBaseClient: PocketBaseRealtimeClient,
    private val dao: AutoData
) {
    companion object {
        private const val TAG = "AutoRepository"
    }

    val autos: StateFlow<List<Auto>> = pocketBaseClient.autos

    fun startRealtimeSubscription() {
        pocketBaseClient.startRealtimeSubscription()
        Log.d(TAG, "Realtime iniciado")
    }

    fun stopRealtimeSubscription() {
        pocketBaseClient.stopRealtimeSubscription()
    }

    suspend fun refreshAutos() {
        pocketBaseClient.fetchAutos()
    }

    suspend fun syncWithLocalCache() {
        val cachedAutos = dao.getAllAutos().first()
        if (cachedAutos.isNotEmpty()) {
            Log.d(TAG, "Cargando ${cachedAutos.size} autos desde caché local")
        }
    }

    suspend fun registerAuto(userId: String, placa: String, lineaCode: String): Result<Auto> {
        return pocketBaseClient.registerAuto(
            userId = userId,
            placa = placa,
            lineaCode = lineaCode,
            lat = 0.0,
            lng = 0.0
        )
    }

    suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return pocketBaseClient.getAutoByUserId(userId)
    }

    suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto> {
        return pocketBaseClient.updateAutoLocation(autoId, lat, lng, angulo)
    }

    suspend fun submitReporte(descripcion: String, userId: String): Result<Unit> =
        pocketBaseClient.createRecord(
            "reports",
            mapOf("description" to descripcion, "users" to userId),
            null
        ).map { }

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getReportes(placa: String): Result<List<Reporte>> = try {
        pocketBaseClient.getList("reports", perPage = 100, sort = "-created", authToken = null)
            .map { jsonStr ->
                val jsonObject = jsonParser.decodeFromString<JsonObject>(jsonStr)
                val items = jsonObject["items"]?.jsonArray ?: return@map emptyList()
                val reportes = items.map { jsonParser.decodeFromJsonElement<Reporte>(it) }
                reportes.filter { it.descripcion.contains("[Bus: $placa]") }
            }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun responderReporte(reporteId: String, respuesta: String, authToken: String?): Result<Unit> =
        pocketBaseClient.updateRecord(
            "reports", reporteId,
            mapOf("response" to respuesta),
            authToken
        ).map { }

}

