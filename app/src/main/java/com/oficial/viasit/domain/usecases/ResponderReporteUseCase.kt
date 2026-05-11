package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.repository.IReportesRepository

class ResponderReporteUseCase(private val repository: IReportesRepository) {
    suspend operator fun invoke(reporteId: String, respuesta: String): Result<Unit> =
        repository.responderReporte(reporteId, respuesta)
}
