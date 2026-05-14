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
        targetName: String,
        lineId: String,
        category: String
    ): Result<Unit> {
        val fullDescription = "[$category] $descripcion | Reportado: $targetName"
        
        val mapData = mutableMapOf<String, Any>(
            "description" to fullDescription,
            "userid" to driverId,
            "status" to "pendiente",
            "users" to listOf(reporterId)
        )
        if (lineId.isNotBlank()) {
            mapData["lineId"] = lineId
        }

        val result = client.createRecord(
            "reports",
            mapData,
            null
        ).map { }

        if (result.isSuccess) {
            val logDescription = "REPORTE: $reporterName reportó a $targetName ($category): $descripcion"
            client.createRecord(
                "logs",
                mapOf(
                    "userid" to reporterId,
                    "description" to logDescription,
                    "type" to "warning"
                ),
                null
            )
        }
        
        return result
    }

    override suspend fun getReportes(query: String): Result<List<Reporte>> = try {
        val token = authRepository.getAuthToken()
        
        val filterQuery = if (query.isNotBlank()) {
            // Si parece un ID de PocketBase (aprox 15 caracteres) o si queremos buscar por texto
            if (query.length >= 15) {
                "userid = '$query' || description ~ '$query'"
            } else {
                "description ~ '$query'"
            }
        } else ""

        client.getList(
            "reports", 
            perPage = 100, 
            sort = "-created", 
            expand = "userid,users", 
            filter = filterQuery,
            authToken = token
        ).map { jsonStr ->
            val jsonObject = jsonParser.decodeFromString<JsonObject>(jsonStr)
            val items = jsonObject["items"]?.jsonArray ?: return@map emptyList()
            items.map { jsonParser.decodeFromJsonElement<Reporte>(it) }
        }
    } catch (e: Exception) { 
        Result.failure(e) 
    }

    override suspend fun responderReporte(reporteId: String, respuesta: String): Result<Unit> {
        val token = authRepository.getAuthToken()
        return client.updateRecord(
            "reports", reporteId,
            mapOf(
                "adminResponse" to respuesta,
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
                "driverResponse" to respuesta
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
