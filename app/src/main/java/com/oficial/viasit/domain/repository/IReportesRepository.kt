package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Reporte

interface IReportesRepository {
    suspend fun submitReporte(descripcion: String, reporterId: String, reporterName: String, driverId: String): Result<Unit>
    suspend fun getReportes(placa: String): Result<List<Reporte>>
    suspend fun responderReporte(reporteId: String, respuesta: String): Result<Unit>
}
