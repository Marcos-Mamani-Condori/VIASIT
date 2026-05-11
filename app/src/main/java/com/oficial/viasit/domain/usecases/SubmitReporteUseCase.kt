package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.repository.IReportesRepository

class SubmitReporteUseCase(private val repository: IReportesRepository) {
    suspend operator fun invoke(descripcion: String, userId: String): Result<Unit> =
        repository.submitReporte(descripcion, userId)
}
