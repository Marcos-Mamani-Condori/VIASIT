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

class PassengerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PassengerVM"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PassengerViewModel(AutosAplicacion.instance) as T
        }
    }

    private val adminRepository = AutosAplicacion.instance.adminRepository

    private val _lineas = MutableStateFlow<List<Linea>>(emptyList())
    val lineas: StateFlow<List<Linea>> = _lineas.asStateFlow()

    private val _rutas = MutableStateFlow<Map<String, Ruta>>(emptyMap())
    val rutas: StateFlow<Map<String, Ruta>> = _rutas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedLineaId = MutableStateFlow<String?>(null)
    val selectedLineaId: StateFlow<String?> = _selectedLineaId.asStateFlow()

    private val _selectedAuto = MutableStateFlow<Auto?>(null)
    val selectedAuto: StateFlow<Auto?> = _selectedAuto.asStateFlow()

    private val _destinoLineas = MutableStateFlow<List<Linea>>(emptyList())
    val destinoLineas: StateFlow<List<Linea>> = _destinoLineas.asStateFlow()

    private val _destinoNombre = MutableStateFlow<String?>(null)
    val destinoNombre: StateFlow<String?> = _destinoNombre.asStateFlow()

    private val _destinoQuery = MutableStateFlow("")
    val destinoQuery: StateFlow<String> = _destinoQuery.asStateFlow()

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
            adminRepository.getLineas().fold(
                onSuccess = { lineas ->
                    lineas.forEach { linea ->
                        Log.d(TAG, "Linea: ${linea.name} | rutaId='${linea.rutaId}'")
                    }
                    _lineas.value = lineas
                    loadRutas()
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando líneas: ${e.message}")
                    _error.value = "No se pudieron cargar las líneas"
                    _isLoading.value = false
                }
            )
        }
    }

    private suspend fun loadRutas() {
        adminRepository.getRutas().fold(
            onSuccess = { rutas ->
                rutas.forEach { ruta ->
                    Log.d(TAG, "Ruta: ${ruta.name} | id='${ruta.id}'")
                }
                _rutas.value = rutas.associateBy { it.id }
            },
            onFailure = { Log.w(TAG, "No se pudieron cargar rutas") }
        )
        _isLoading.value = false
    }

    fun buscarLineasPorDestino(lat: Double, lng: Double, nombre: String) {
        val coincidentes = _lineas.value.filter { linea ->
            val ruta = _rutas.value[linea.rutaId] ?: return@filter false
            rutaPasaCercaDe(ruta, lat, lng, radioMetros = 800.0)
        }
        _destinoLineas.value = coincidentes
        _destinoNombre.value = nombre
    }

    fun limpiarDestino() {
        _destinoLineas.value = emptyList()
        _destinoNombre.value = null
        _destinoQuery.value = ""
    }

    fun setDestinoQuery(q: String) {
        _destinoQuery.value = q
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
            android.location.Location.distanceBetween(lat, lng, pLat, pLng, result)
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
