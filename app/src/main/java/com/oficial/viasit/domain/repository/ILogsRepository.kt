package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.LogEntry

interface ILogsRepository {
    suspend fun create(userId: String, description: String): Result<Unit>
    suspend fun getAll(limit: Int = 50): Result<List<LogEntry>>
    suspend fun getByUser(userId: String): Result<List<LogEntry>>
}
