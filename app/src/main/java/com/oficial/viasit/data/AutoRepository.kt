package com.oficial.viasit.data

import android.util.Log
import com.oficial.viasit.model.Auto
import kotlinx.coroutines.flow.Flow

class AutoRepository(
    private val api: PocketBaseService,
    private val dao: AutoData
) {
    val autos: Flow<List<Auto>> = dao.getAllAutos()

    suspend fun refreshAutos() {
        try {
            val response = api.getAutos()

            if (response.items.isNotEmpty()) {
                dao.insertOrUpdateAutos(response.items)
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                 android.widget.Toast.makeText(
                     com.oficial.viasit.AutosAplicacion.instance, 
                     "Error de conexión: ${e.message}", 
                     android.widget.Toast.LENGTH_LONG
                 ).show()
            }
        }
    }
}
