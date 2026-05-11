package com.oficial.viasit.domain.usecases

import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.domain.repository.IReportesRepository

class GetReportesUseCase(private val repository: IReportesRepository) {
    suspend operator fun invoke(placa: String): Result<List<Reporte>> =
        repository.getReportes(placa)
}
