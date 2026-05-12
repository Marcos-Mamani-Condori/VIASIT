package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.domain.repository.IAdminRepository
import com.oficial.viasit.domain.repository.IReportesRepository

class GetAppealsUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(): Result<List<LogEntry>> = repository.getAppeals()
}

class ResolveAppealUseCase(private val repository: IAdminRepository) {
    suspend operator fun invoke(appealId: String, userId: String, accept: Boolean): Result<Unit> = 
        repository.resolveAppeal(appealId, userId, accept)
}

class GetAllReportesUseCase(private val repository: IReportesRepository) {
    suspend operator fun invoke(): Result<List<Reporte>> = repository.getReportes("") // Enviamos vacío para traer todos
}
