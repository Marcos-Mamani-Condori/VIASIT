package com.oficial.viasit.data.repository

import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.domain.repository.IReportesRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

class ReportesRepository(
    private val client: PocketBaseRealtimeClient,
    private val authRepository: com.oficial.viasit.domain.repository.IAuthRepository
) : IReportesRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun submitReporte(descripcion: String, userId: String): Result<Unit> =
        client.createRecord(
            "reports",
            mapOf("description" to descripcion, "users" to listOf(userId)),
            null
        ).map { }

    override suspend fun getReportes(placa: String): Result<List<Reporte>> = try {
        client.getList("reports", perPage = 100, sort = "-created", authToken = null)
            .map { jsonStr ->
                val jsonObject = jsonParser.decodeFromString<JsonObject>(jsonStr)
                val items = jsonObject["items"]?.jsonArray ?: return@map emptyList()
                val reportes = items.map { jsonParser.decodeFromJsonElement<Reporte>(it) }
                reportes.filter { it.descripcion.contains("[Bus: $placa]") }
            }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun responderReporte(reporteId: String, respuesta: String): Result<Unit> {
        val token = authRepository.getAuthToken()
        return client.updateRecord(
            "reports", reporteId,
            mapOf("response" to respuesta),
            token
        ).map { }
    }
}
