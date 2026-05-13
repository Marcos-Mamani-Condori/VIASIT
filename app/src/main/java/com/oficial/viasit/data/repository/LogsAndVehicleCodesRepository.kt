package com.oficial.viasit.data.repository

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminJsonParsers.parseLogsFromJson
import com.oficial.viasit.domain.repository.ILogsRepository

internal class LogsRepository(
    private val client: PocketBaseRealtimeClient,
    private val shared: AdminRepositoryShared
) : ILogsRepository {
    override suspend fun create(userId: String, userName: String, description: String, type: String): Result<Unit> = try {
        val finalUserName = userName.ifEmpty { shared.getCurrentUserName() ?: "Sistema" }
        client.createRecord("logs",
            mapOf("userid" to userId, "userName" to finalUserName, "description" to description, "type" to type),
            shared.authToken()).map { }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAll(limit: Int, filter: String, timeFilter: String, lineId: String): Result<List<LogEntry>> = try {
        var pbFilter = ""
        
        // Limpieza profunda del filtro: si es vacío o "todos", no filtramos por texto
        val cleanFilter = filter.trim().lowercase()
        if (cleanFilter.isNotEmpty() && cleanFilter != "todos") {
            pbFilter = "description ~ '$filter'"
        }

        // Filtro por LineId (Jurisdicción)
        if (lineId.isNotBlank()) {
            val linePart = "description ~ '$lineId'"
            pbFilter = if (pbFilter.isNotEmpty()) "($pbFilter) && ($linePart)" else linePart
        }

        val timePart = when (timeFilter.lowercase()) {
            "hoy" -> "created >= '${shared.getStartDateForFilter("today")}'"
            "ayer" -> "created >= '${shared.getStartDateForFilter("yesterday")}' && created < '${shared.getStartDateForFilter("today")}'"
            "semana" -> "created >= '${shared.getStartDateForFilter("last_week")}'"
            "mes" -> "created >= '${shared.getStartDateForFilter("last_month")}'"
            else -> ""
        }

        if (timePart.isNotEmpty()) {
            pbFilter = if (pbFilter.isNotEmpty()) "($pbFilter) && ($timePart)" else timePart
        }

        println("DEBUG: LogsRepository - Filtro PB: $pbFilter")

        client.getList("logs", perPage = limit, sort = "-created", filter = pbFilter, expand = "userid", authToken = shared.authToken())
            .map { json ->
                println("DEBUG: LogsRepository - Respuesta Cruda: $json")
                parseLogsFromJson(json)
            }.onFailure { 
                println("DEBUG: LogsRepository - Error en getList: ${it.message}")
            }
    } catch (e: Exception) { 
        println("DEBUG: LogsRepository - Excepción: ${e.message}")
        Result.failure(e) 
    }

    override suspend fun getByUser(userId: String): Result<List<LogEntry>> = try {
        client.getList("logs", filter = "userid='$userId'", sort = "-created", expand = "userid", authToken = shared.authToken())
            .map { parseLogsFromJson(it) }
    } catch (e: Exception) { Result.failure(e) }
}
