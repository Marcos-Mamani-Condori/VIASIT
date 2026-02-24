package com.oficial.viasit.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Ruta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * ViewModel para el dashboard de pasajeros.
 * Carga las líneas y rutas de PocketBase en modo solo-lectura.
 */
class PassengerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PassengerVM"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PassengerViewModel(AutosAplicacion.instance) as T
        }
    }

    private val pocketBase = AutosAplicacion.instance.pocketBaseClient
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _lineas = MutableStateFlow<List<Linea>>(emptyList())
    val lineas: StateFlow<List<Linea>> = _lineas.asStateFlow()

    private val _rutas = MutableStateFlow<Map<String, Ruta>>(emptyMap())
    val rutas: StateFlow<Map<String, Ruta>> = _rutas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** ID de la línea que el pasajero seleccionó para ver en el mapa */
    private val _selectedLineaId = MutableStateFlow<String?>(null)
    val selectedLineaId: StateFlow<String?> = _selectedLineaId.asStateFlow()

    /** Bus seleccionado desde la lista para centrar el mapa en él */
    private val _selectedAuto = MutableStateFlow<Auto?>(null)
    val selectedAuto: StateFlow<Auto?> = _selectedAuto.asStateFlow()

    init {
        loadLineas()
    }

    fun selectLinea(lineaId: String)  { _selectedLineaId.value = lineaId }
    fun clearSelectedLinea()           { _selectedLineaId.value = null }
    fun selectAuto(auto: Auto?)        { _selectedAuto.value = auto }
    fun clearSelectedAuto()            { _selectedAuto.value = null }

    fun loadLineas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = pocketBase.getList(
                    collection = "lineas",
                    perPage    = 100,
                    sort       = "name"
                )
                result.fold(
                    onSuccess = { body ->
                        val root  = json.parseToJsonElement(body).jsonObject
                        val items = root["items"]?.jsonArray ?: return@fold
                        val parsed = items.map { json.decodeFromJsonElement(Linea.serializer(), it) }
                        // 🔍 DEBUG: mostrar qué ruta_id recibe cada línea
                        parsed.forEach { linea ->
                            Log.d(TAG, "Linea: ${linea.name} | rutaId='${linea.rutaId}'")
                        }
                        _lineas.value = parsed
                        loadRutas()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error cargando líneas: ${e.message}")
                        _error.value = "No se pudieron cargar las líneas"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception en loadLineas: ${e.message}")
                _error.value = "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadRutas() {
        try {
            val result = pocketBase.getList(collection = "rutas", perPage = 100)
            result.fold(
                onSuccess = { body ->
                    val root  = json.parseToJsonElement(body).jsonObject
                    val items = root["items"]?.jsonArray ?: return
                    val parsed = items.map { json.decodeFromJsonElement(Ruta.serializer(), it) }
                    // 🔍 DEBUG: mostrar rutas cargadas
                    parsed.forEach { ruta ->
                        Log.d(TAG, "Ruta: ${ruta.name} | id='${ruta.id}'")
                    }
                    _rutas.value = parsed.associateBy { it.id }
                },
                onFailure = { Log.w(TAG, "No se pudieron cargar rutas") }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error cargando rutas: ${e.message}")
        }
    }
}
