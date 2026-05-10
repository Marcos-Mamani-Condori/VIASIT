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

import com.oficial.viasit.domain.repository.IAutoRepository

class AutoRepository(
    private val pocketBaseClient: PocketBaseRealtimeClient,
    private val dao: AutoData
) : IAutoRepository {
    companion object {
        private const val TAG = "AutoRepository"
    }

    override val autos: StateFlow<List<Auto>> = pocketBaseClient.autos

    override fun startRealtimeSubscription() {
        pocketBaseClient.startRealtimeSubscription()
        Log.d(TAG, "Realtime iniciado")
    }

    override fun stopRealtimeSubscription() {
        pocketBaseClient.stopRealtimeSubscription()
    }

    override suspend fun refreshAutos() {
        pocketBaseClient.fetchAutos()
    }

    override suspend fun syncWithLocalCache() {
        val cachedAutos = dao.getAllAutos().first()
        if (cachedAutos.isNotEmpty()) {
            Log.d(TAG, "Cargando ${cachedAutos.size} autos desde caché local")
        }
    }

    override suspend fun registerAuto(userId: String, placa: String, lineaCode: String): Result<Auto> {
        return pocketBaseClient.registerAuto(
            userId = userId,
            placa = placa,
            lineaCode = lineaCode,
            lat = 0.0,
            lng = 0.0
        )
    }

    override suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return pocketBaseClient.getAutoByUserId(userId)
    }

    override suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto> {
        return pocketBaseClient.updateAutoLocation(autoId, lat, lng, angulo)
    }

}
