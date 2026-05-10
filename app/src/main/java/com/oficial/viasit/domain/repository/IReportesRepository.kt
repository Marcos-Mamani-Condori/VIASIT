package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Reporte

interface IReportesRepository {
    suspend fun submitReporte(descripcion: String, userId: String): Result<Unit>
    suspend fun getReportes(placa: String): Result<List<Reporte>>
    suspend fun responderReporte(reporteId: String, respuesta: String, authToken: String?): Result<Unit>
}
