package com.oficial.viasit.domain.repository

import com.oficial.viasit.domain.model.Reporte

interface IReportesRepository {
    suspend fun submitReporte(
        descripcion: String,
        reporterId: String,
        reporterName: String,
        driverId: String,
        targetName: String,
        lineId: String = "",
        category: String = "general"
    ): Result<Unit>
    suspend fun getReportes(query: String = ""): Result<List<Reporte>>
    suspend fun responderReporte(reporteId: String, respuesta: String): Result<Unit>
    suspend fun responderReporteConductor(reporteId: String, respuesta: String): Result<Unit>
    suspend fun actualizarEstadoReporte(reporteId: String, nuevoEstado: String): Result<Unit>
}
