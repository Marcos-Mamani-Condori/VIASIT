package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.LogEntry

interface ILogsRepository {
    suspend fun create(userId: String, userName: String, description: String, type: String = "info"): Result<Unit>
    suspend fun getAll(limit: Int = 50, filter: String = "", timeFilter: String = "todos", lineId: String = ""): Result<List<LogEntry>>
    suspend fun getByUser(userId: String): Result<List<LogEntry>>
}
