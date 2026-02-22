package com.oficial.viasit.ui.routes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Resultado de búsqueda de ubicación
 */
@Serializable
data class SearchResult(
    val name: String,
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val type: String = ""
)

/**
 * Servicio de geocoding usando Nominatim (OpenStreetMap)
 * API gratuita con límite de uso razonable
 */
class GeocodingService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Buscar ubicaciones por texto
     * @param query Texto de búsqueda (calle, ciudad, lugar)
     * @param countryCodes Códigos de país para limitar búsqueda (ej: "bo" para Bolivia)
     * @return Lista de resultados
     */
    suspend fun search(
        query: String,
        countryCodes: String = "bo",
        limit: Int = 5
    ): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://nominatim.openstreetmap.org/search?")
                append("format=json")
                append("&q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                append("&limit=$limit")
                if (countryCodes.isNotEmpty()) {
                    append("&countrycodes=$countryCodes")
                }
                append("&addressdetails=1")
            }
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VIASIT App")
                .header("Accept-Language", "es")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                
                // Parse JSON array manually
                val results = parseSearchResults(body)
                Result.success(results)
            } else {
                Result.failure(Exception("Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseSearchResults(jsonString: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        try {
            // Simple JSON parsing
            val items = jsonString.trim()
                .removeSurrounding("[", "]")
                .split("},{")
            
            for (item in items) {
                try {
                    val cleanItem = item.trim().removeSurrounding("{", "}")
                    
                    // Extract display_name
                    val displayNameMatch = """display_name"\s*:\s*"([^"]+)"""".toRegex().find(cleanItem)
                    val displayName = displayNameMatch?.groupValues?.get(1) ?: continue
                    
                    // Extract lat
                    val latMatch = """lat"\s*:\s*"([^"]+)"""".toRegex().find(cleanItem)
                    val lat = latMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
                    
                    // Extract lon
                    val lonMatch = """lon"\s*:\s*"([^"]+)"""".toRegex().find(cleanItem)
                    val lon = lonMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
                    
                    // Extract name (short name)
                    val nameMatch = """"name"\s*:\s*"([^"]+)"""".toRegex().find(cleanItem)
                    val name = nameMatch?.groupValues?.get(1) ?: displayName.split(",").firstOrNull()?.trim() ?: "Sin nombre"
                    
                    // Extract type
                    val typeMatch = """type"\s*:\s*"([^"]+)"""".toRegex().find(cleanItem)
                    val type = typeMatch?.groupValues?.get(1) ?: ""
                    
                    results.add(SearchResult(
                        name = name,
                        displayName = displayName,
                        lat = lat,
                        lon = lon,
                        type = type
                    ))
                } catch (e: Exception) {
                    // Skip this item
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results
    }
}
