package com.oficial.viasit.ui.routes

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class SearchResult(
    val name: String,
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val type: String = ""
)

class GeocodingService {

    companion object {
        private const val TAG = "GeocodingService"
        // Bounding box de La Paz: sur-oeste, nor-este
        private const val VIEWBOX = "-68.25,-16.60,-67.95,-16.40"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun search(
        query: String,
        limit: Int = 5
    ): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        try {
            // Si la búsqueda ya tiene "la paz" o "bolivia" no agregar, si no, añadirlo
            val enrichedQuery = if (
                query.contains("la paz", ignoreCase = true) ||
                query.contains("bolivia", ignoreCase = true)
            ) query else "$query, La Paz, Bolivia"

            val url = buildString {
                append("https://nominatim.openstreetmap.org/search?")
                append("format=json")
                append("&q=${URLEncoder.encode(enrichedQuery, "UTF-8")}")
                append("&limit=$limit")
                append("&countrycodes=bo")
                append("&viewbox=$VIEWBOX")
                append("&bounded=0")          // bounded=0: prioriza el viewbox pero no limita
                append("&addressdetails=1")
                append("&accept-language=es")
            }

            Log.d(TAG, "Search URL: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VIASIT-Android/1.0")
                .header("Accept-Language", "es")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Respuesta vacía"))

            Log.d(TAG, "Response: ${body.take(200)}")

            val results = parseWithKotlinxSerialization(body)
            Result.success(results)

        } catch (e: Exception) {
            Log.e(TAG, "Error en búsqueda: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseWithKotlinxSerialization(jsonString: String): List<SearchResult> {
        return try {
            val array = json.parseToJsonElement(jsonString) as? JsonArray
                ?: return emptyList()

            array.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val lat = obj["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                    val lon = obj["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                    val displayName = obj["display_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""

                    // Intentar obtener el nombre corto del campo address
                    val addressObj = obj["address"]?.jsonObject
                    val shortName = addressObj?.let {
                        it["suburb"]?.jsonPrimitive?.content
                            ?: it["neighbourhood"]?.jsonPrimitive?.content
                            ?: it["road"]?.jsonPrimitive?.content
                            ?: it["amenity"]?.jsonPrimitive?.content
                            ?: it["leisure"]?.jsonPrimitive?.content
                    } ?: obj["name"]?.jsonPrimitive?.content
                        ?: displayName.split(",").firstOrNull()?.trim()
                        ?: "Sin nombre"

                    SearchResult(
                        name        = shortName,
                        displayName = displayName,
                        lat         = lat,
                        lon         = lon,
                        type        = type
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando resultado: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando JSON: ${e.message}")
            emptyList()
        }
    }
}
