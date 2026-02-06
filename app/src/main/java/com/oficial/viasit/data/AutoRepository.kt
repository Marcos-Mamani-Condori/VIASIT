package com.oficial.viasit.data

import android.util.Log
import com.oficial.viasit.model.Auto
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
}

