package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.repository.IAdminRepository
import com.oficial.viasit.domain.repository.ILogsRepository

class GetLogsUseCase(private val repository: ILogsRepository) {
    suspend operator fun invoke(limit: Int = 50, filter: String = "", timeFilter: String = "todos", lineId: String = ""): Result<List<LogEntry>> =
        repository.getAll(limit, filter, timeFilter, lineId)
}

class CreateLogUseCase(private val repository: ILogsRepository) {
    suspend operator fun invoke(userId: String, userName: String, description: String, type: String = "info"): Result<Unit> =
        repository.create(userId, userName, description, type)
}
