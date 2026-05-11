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
import com.oficial.viasit.domain.usecases.GetLineasUseCase
import com.oficial.viasit.domain.usecases.GetRutasUseCase
import com.oficial.viasit.domain.usecases.SearchLineasByDestinationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PassengerViewModel(
    application: Application,
    private val getLineasUseCase: GetLineasUseCase,
    private val getRutasUseCase: GetRutasUseCase,
    private val searchLineasByDestinationUseCase: SearchLineasByDestinationUseCase
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PassengerVM"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val app = AutosAplicacion.instance
                return PassengerViewModel(
                    app,
                    app.getLineasUseCase,
                    app.getRutasUseCase,
                    app.searchLineasByDestinationUseCase
                ) as T
            }
        }
    }

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
            getLineasUseCase().fold(
                onSuccess = { lineas ->
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
        getRutasUseCase().fold(
            onSuccess = { rutas ->
                _rutas.value = rutas.associateBy { it.id }
            },
            onFailure = { Log.w(TAG, "No se pudieron cargar rutas") }
        )
        _isLoading.value = false
    }

    fun buscarLineasPorDestino(lat: Double, lng: Double, nombre: String) {
        _destinoLineas.value = searchLineasByDestinationUseCase(
            lat = lat,
            lng = lng,
            lineas = _lineas.value,
            rutasMap = _rutas.value
        )
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
}
