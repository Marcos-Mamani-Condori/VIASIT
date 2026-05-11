package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.repository.IAdminRepository

class GetLogsUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(): Result<List<LogEntry>> = repository.getLogs()
}

class CreateLogUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(userId: String, description: String): Result<Unit> =
        repository.createLog(userId, description)
}
