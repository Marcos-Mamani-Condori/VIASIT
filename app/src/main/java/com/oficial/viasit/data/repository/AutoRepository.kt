package com.oficial.viasit.data.repository

import android.util.Log
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.data.local.AutoEntity
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Repository simplificado que usa PocketBase SDK con Realtime
 * Mantiene sincronización con Room para caché offline
 */
class AutoRepository(
    private val pocketBaseClient: PocketBaseRealtimeClient,
    private val dao: AutoData
) {
    companion object {
        private const val TAG = "AutoRepository"
    }

    // Única fuente de verdad: PocketBase con caché en Room
    val autos: StateFlow<List<Auto>> = pocketBaseClient.autos

    /**
     * Iniciar suscripción realtime
     */
    fun startRealtimeSubscription() {
        pocketBaseClient.startRealtimeSubscription()
        Log.d(TAG, "Realtime iniciado")
    }

    /**
     * Detener suscripción realtime
     */
    fun stopRealtimeSubscription() {
        pocketBaseClient.stopRealtimeSubscription()
    }

    /**
     * Forzar refresh manual de datos
     */
    suspend fun refreshAutos() {
        pocketBaseClient.fetchAutos()
    }

    /**
     * Sincronizar con caché local (para uso offline)
     */
    suspend fun syncWithLocalCache() {
        val cachedAutos = dao.getAllAutos().first()
        if (cachedAutos.isNotEmpty()) {
            Log.d(TAG, "Cargando ${cachedAutos.size} autos desde caché local")
        }
    }

    /**
     * Registrar un nuevo auto
     */
    suspend fun registerAuto(
        userId: String,
        placa: String,
        linea: String
    ): Result<Auto> {
        return pocketBaseClient.registerAuto(
            userId = userId,
            placa = placa,
            linea = linea,
            lat = 0.0,
            lng = 0.0
        )
    }

    /**
     * Obtener el auto de un conductor específico
     */
    suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        return pocketBaseClient.getAutoByUserId(userId)
    }

    /**
     * Actualizar ubicación del auto
     */
    suspend fun updateAutoLocation(
        autoId: String,
        lat: Double,
        lng: Double,
        angulo: Double
    ): Result<Auto> {
        return pocketBaseClient.updateAutoLocation(autoId, lat, lng, angulo)
    }
}
