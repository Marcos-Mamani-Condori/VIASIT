package com.oficial.viasit.data.repository

import android.util.Log
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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
            "reportes",
            mapOf("descripcion" to descripcion, "users" to userId),
            null
        ).map { }

    data class Reporte(val id: String, val descripcion: String, val respuesta: String)

    suspend fun getReportes(placa: String): Result<List<Reporte>> = try {
        pocketBaseClient.getList("reportes", perPage = 100, sort = "-created", authToken = null)
            .map { json ->
                val idRgx   = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
                val descRgx = Regex("\"descripcion\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*?)\"")
                val respRgx = Regex("\"respuesta\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*?)\"")
                val itemRgx = Regex("\\{[^}]*\"descripcion\"[^}]*\\}")
                itemRgx.findAll(json).mapNotNull { item ->
                    val desc = descRgx.find(item.value)?.groupValues?.get(1) ?: return@mapNotNull null
                    if (!desc.contains("[Bus: $placa]")) return@mapNotNull null
                    val id   = idRgx.find(item.value)?.groupValues?.get(1) ?: ""
                    val resp = respRgx.find(item.value)?.groupValues?.get(1) ?: ""
                    Reporte(id, desc, resp)
                }.toList()
            }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun responderReporte(reporteId: String, respuesta: String, authToken: String?): Result<Unit> =
        pocketBaseClient.updateRecord(
            "reportes", reporteId,
            mapOf("respuesta" to respuesta),
            authToken
        ).map { }

}

