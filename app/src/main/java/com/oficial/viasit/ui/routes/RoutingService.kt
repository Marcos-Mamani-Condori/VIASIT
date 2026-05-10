package com.oficial.viasit.ui.routes

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RoutingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRoute(points: List<Pair<Double, Double>>): Result<List<Pair<Double, Double>>> =
        withContext(Dispatchers.IO) {
            if (points.size < 2) return@withContext Result.failure(Exception("Se necesitan al menos 2 puntos"))
            try {
                // OSRM espera: lng,lat;lng,lat;...
                val coordsStr = points.joinToString(";") { (lat, lng) -> "$lng,$lat" }
                val url = "https://router.project-osrm.org/route/v1/driving/$coordsStr" +
                        "?overview=full&geometries=geojson&steps=false"

                Log.d("RoutingService", "Consultando ruta: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VIASIT-App/1.0")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Respuesta vacía del servidor"))

                if (!response.isSuccessful) {
                    Log.e("RoutingService", "Error HTTP ${response.code}: $body")
                    return@withContext Result.failure(Exception("Error HTTP ${response.code}"))
                }

                val routeCoords = parseOsrmResponse(body)
                if (routeCoords.isEmpty()) {
                    return@withContext Result.failure(Exception("No se encontró ruta"))
                }

                Log.d("RoutingService", "Ruta calculada: ${routeCoords.size} puntos")
                Result.success(routeCoords)
            } catch (e: Exception) {
                Log.e("RoutingService", "Error calculando ruta", e)
                // Fallback: devolver los puntos originales (línea recta)
                Result.failure(e)
            }
        }

    fun routeToWaypointsString(routePoints: List<Pair<Double, Double>>): String {
        if (routePoints.size <= 2) return ""
        // Excluir el primer y último punto (ya guardados como start_point/end_point)
        return routePoints.drop(1).dropLast(1)
            .joinToString(";") { (lat, lng) -> "$lat,$lng" }
    }


    private fun parseOsrmResponse(jsonStr: String): List<Pair<Double, Double>> {
        return try {
            val root      = json.parseToJsonElement(jsonStr).jsonObject
            val routes    = root["routes"]?.jsonArray
                ?: return emptyList()
            val geometry  = routes[0].jsonObject["geometry"]?.jsonObject
                ?: return emptyList()
            val coords    = geometry["coordinates"]?.jsonArray
                ?: return emptyList()

            coords.mapNotNull { coord ->
                val arr = coord.jsonArray
                if (arr.size < 2) return@mapNotNull null
                val lng = arr[0].jsonPrimitive.double
                val lat = arr[1].jsonPrimitive.double
                Pair(lat, lng)
            }
        } catch (e: Exception) {
            Log.e("RoutingService", "Error parseando respuesta OSRM", e)
            emptyList()
        }
    }
}
