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

    override suspend fun submitReporte(
        descripcion: String,
        reporterId: String,
        reporterName: String,
        driverId: String,
        category: String
    ): Result<Unit> =
        client.createRecord(
            "reports",
            mapOf(
                "description" to descripcion,
                "userid" to driverId,
                "reporterName" to reporterName,
                "category" to category,
                "status" to "pendiente",
                "users" to listOf(reporterId)
            ),
            null
        ).map { }

    override suspend fun getReportes(placa: String): Result<List<Reporte>> = try {
        val token = authRepository.getAuthToken()
        // Expandimos userid (conductor) y users (reportero) para tener nombres reales
        client.getList("reports", perPage = 100, sort = "-created", expand = "userid,users", authToken = token)
            .map { jsonStr ->
                val jsonObject = jsonParser.decodeFromString<JsonObject>(jsonStr)
                val items = jsonObject["items"]?.jsonArray ?: return@map emptyList()
                val reportes = items.map { jsonParser.decodeFromJsonElement<Reporte>(it) }
                
                if (placa.isBlank()) {
                    reportes
                } else {
                    val cleanPlaca = placa.trim().lowercase()
                    reportes.filter { 
                        it.descripcion.lowercase().contains(cleanPlaca) || 
                        it.descripcion.lowercase().contains(cleanPlaca.replace("-", ""))
                    }
                }
            }
    } catch (e: Exception) { 
        android.util.Log.e("ReportesRepo", "Error al obtener reportes: ${e.message}")
        Result.failure(e) 
    }

    override suspend fun responderReporte(reporteId: String, respuesta: String): Result<Unit> {
        val token = authRepository.getAuthToken()
        return client.updateRecord(
            "reports", reporteId,
            mapOf(
                "response" to respuesta,
                "status" to "resuelto"
            ),
            token
        ).map { }
    }

    override suspend fun responderReporteConductor(reporteId: String, respuesta: String): Result<Unit> {
        val token = authRepository.getAuthToken()
        return client.updateRecord(
            "reports", reporteId,
            mapOf(
                "driver_response" to respuesta
            ),
            token
        ).map { }
    }

    override suspend fun actualizarEstadoReporte(reporteId: String, nuevoEstado: String): Result<Unit> {
        val token = authRepository.getAuthToken()
        return client.updateRecord(
            "reports", reporteId,
            mapOf("status" to nuevoEstado),
            token
        ).map { }
    }
}
