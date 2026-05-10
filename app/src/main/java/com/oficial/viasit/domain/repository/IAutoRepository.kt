package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Auto
import kotlinx.coroutines.flow.StateFlow

interface IAutoRepository {
    val autos: StateFlow<List<Auto>>
    fun startRealtimeSubscription()
    fun stopRealtimeSubscription()
    suspend fun refreshAutos()
    suspend fun syncWithLocalCache()
    suspend fun registerAuto(userId: String, placa: String, lineaCode: String): Result<Auto>
    suspend fun getAutoByUserId(userId: String): Result<Auto?>
    suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto>
}
