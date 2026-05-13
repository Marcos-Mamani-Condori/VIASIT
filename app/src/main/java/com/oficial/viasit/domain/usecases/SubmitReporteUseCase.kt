package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.repository.IReportesRepository

class SubmitReporteUseCase(private val repository: IReportesRepository) {
    suspend operator fun invoke(
        descripcion: String,
        reporterId: String,
        reporterName: String,
        driverId: String,
        targetName: String,
        lineId: String = "",
        category: String = "general"
    ): Result<Unit> =
        repository.submitReporte(descripcion, reporterId, reporterName, driverId, targetName, lineId, category)
}
