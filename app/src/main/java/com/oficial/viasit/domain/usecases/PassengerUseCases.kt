package com.oficial.viasit.domain.usecases

import android.location.Location
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Ruta

class SearchLineasByDestinationUseCase {
    operator fun invoke(
        lat: Double,
        lng: Double,
        lineas: List<Linea>,
        rutasMap: Map<String, Ruta>,
        radioMetros: Double = 800.0
    ): List<Linea> {
        return lineas.filter { linea ->
            val ruta = rutasMap[linea.rutaId] ?: return@filter false
            rutaPasaCercaDe(ruta, lat, lng, radioMetros)
        }
    }

    private fun rutaPasaCercaDe(ruta: Ruta, lat: Double, lng: Double, radioMetros: Double): Boolean {
        val puntos = buildList {
            parsePunto(ruta.startPoint)?.let { add(it) }
            parsePunto(ruta.endPoint)?.let { add(it) }
            if (ruta.waypoints.isNotBlank()) {
                ruta.waypoints.split(";").forEach { seg ->
                    parsePunto(seg)?.let { add(it) }
                }
            }
        }
        return puntos.any { (pLat, pLng) ->
            val result = FloatArray(1)
            Location.distanceBetween(lat, lng, pLat, pLng, result)
            result[0] <= radioMetros
        }
    }

    private fun parsePunto(s: String): Pair<Double, Double>? {
        val parts = s.trim().split(",")
        if (parts.size < 2) return null
        val pLat = parts[0].trim().toDoubleOrNull() ?: return null
        val pLng = parts[1].trim().toDoubleOrNull() ?: return null
        return pLat to pLng
    }
}
