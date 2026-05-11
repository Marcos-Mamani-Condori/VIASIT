package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.data.local.AutoData
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.repository.IAutoRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class AutoRepository(
    private val pocketBaseClient: PocketBaseRealtimeClient,
    private val dao: AutoData,
    private val authRepository: com.oficial.viasit.domain.repository.IAuthRepository
) : IAutoRepository {

    override val autos: StateFlow<List<Auto>> = pocketBaseClient.autos

    override fun startRealtimeSubscription() {
        pocketBaseClient.startRealtimeSubscription()
    }

    override fun stopRealtimeSubscription() {
        pocketBaseClient.stopRealtimeSubscription()
    }

    override suspend fun refreshAutos() {
        pocketBaseClient.fetchAutos()
    }

    override suspend fun syncWithLocalCache() {
        dao.getAllAutos().first()
    }

    override suspend fun registerAuto(userId: String, placa: String, lineaCode: String): Result<Auto> {
        pocketBaseClient.authToken = authRepository.getAuthToken()
        return pocketBaseClient.registerAuto(
            userId = userId,
            placa = placa,
            lineaCode = lineaCode,
            lat = 0.0,
            lng = 0.0
        )
    }

    override suspend fun getAutoByUserId(userId: String): Result<Auto?> {
        pocketBaseClient.authToken = authRepository.getAuthToken()
        return pocketBaseClient.getAutoByUserId(userId)
    }

    override suspend fun updateAutoLocation(autoId: String, lat: Double, lng: Double, angulo: Double): Result<Auto> {
        pocketBaseClient.authToken = authRepository.getAuthToken()
        return pocketBaseClient.updateAutoLocation(autoId, lat, lng, angulo)
    }
}
