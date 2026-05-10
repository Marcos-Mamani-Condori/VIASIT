@file:OptIn(ExperimentalMaterial3Api::class)

package com.oficial.viasit.ui.map

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.ui.routes.GeocodingService
import com.oficial.viasit.ui.routes.SearchResult
import com.oficial.viasit.viewmodels.AuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onBackToDashboard: (() -> Unit)? = null
) {
    val context      = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos        by autosViewModel.autosUiState.collectAsState()
    val authState    by authViewModel.authState.collectAsState()
    val scope        = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var mapInstance  by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedCar  by remember { mutableStateOf<Auto?>(null) }

    // Search state
    val geocodingService                         = remember { GeocodingService() }
    var searchQuery  by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching  by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob    by remember { mutableStateOf<Job?>(null) }

    val currentUser = (authState as? AuthState.Authenticated)?.user
    val isDriver    = currentUser?.userRole == UserRole.conductor
    val isInService by authViewModel.isInService.collectAsState()

    // Tu vehículo propio (si eres conductor y estás en servicio)
    val myVehicle: Auto? = if (isDriver && isInService) {
        autos.find { it.userId.contains(currentUser?.id ?: "") }
    } else null

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    addCarIconToStyle(style, context)
                    addCarsLayer(style)
                    // Solo mostrar vehículos activos (actualizados en los últimos 2 minutos)
                    val activeAutos = autos.filter { it.isActive() }
                    updateCarsSource(style, activeAutos)
                    enableLocationComponent(style, map, context)
                }
                map.addOnMapClickListener { latLng ->
                    val screenPoint = map.projection.toScreenLocation(latLng)
                    val features    = map.queryRenderedFeatures(screenPoint, "cars-layer")
                    selectedCar = if (features.isNotEmpty()) {
                        autos.find { it.placa == features[0].getStringProperty("placa") }
                    } else null
                    true
                }
            }
        }
    }

    // Ciclo de vida del MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE  -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Actualizar marcadores cuando cambian los autos (SSE → recomposición automática)
    // Solo mostrar vehículos activos (actualizados en los últimos 2 minutos)
    LaunchedEffect(autos, mapInstance) {
        val activeAutos = autos.filter { it.isActive() }
        mapInstance?.getStyle { style -> updateCarsSource(style, activeAutos) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        // ── Top bar + buscador ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "VIASIT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        currentUser?.let { user ->
                            Text(
                                text = user.name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (isDriver && isInService) {
                                Text(
                                    text = "🟢 En servicio",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (onBackToDashboard != null) {
                        IconButton(onClick = onBackToDashboard) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver al dashboard")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )

            // Barra de búsqueda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    TextField(
                        value = searchQuery,
                        onValueChange = { newQuery ->
                            searchQuery = newQuery
                            searchJob?.cancel()
                            if (newQuery.length >= 3) {
                                searchJob = scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    isSearching       = true
                                    showSearchResults = true
                                    geocodingService.search(newQuery).fold(
                                        onSuccess = { results ->
                                            searchResults = results
                                            isSearching   = false
                                        },
                                        onFailure = {
                                            searchResults = emptyList()
                                            isSearching   = false
                                        }
                                    )
                                }
                            } else {
                                searchResults     = emptyList()
                                showSearchResults = false
                            }
                        },
                        placeholder = {
                            Text("Buscar lugar...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor    = Color.Transparent,
                            unfocusedContainerColor  = Color.Transparent,
                            focusedTextColor         = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor       = MaterialTheme.colorScheme.onSurface,
                            cursorColor              = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor    = Color.Transparent,
                            unfocusedIndicatorColor  = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery       = ""
                            searchResults     = emptyList()
                            showSearchResults = false
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp).padding(end = 8.dp),
                            color       = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Resultados de búsqueda
            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mapInstance?.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(result.lat, result.lon), 16.0
                                            )
                                        )
                                        showSearchResults = false
                                        searchQuery       = ""
                                        focusManager.clearFocus()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    result.name,
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Botón de ubicación ────────────────────────────────────────────
        LargeFloatingActionButton(
            onClick  = { mapInstance?.let { centerOnUser(it, context) } },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 200.dp, end = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Mi posición")
        }

        // ── Panel inferior: info del auto seleccionado o lista ────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.8f)
        ) {
            if (selectedCar != null) {
                CarInfoCard(car = selectedCar!!, onClose = { selectedCar = null })
            } else if (autos.isNotEmpty()) {
                Column {
                    // Banner "TU AUTO" si el conductor está en servicio
                    if (myVehicle != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text  = "TU AUTO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = myVehicle.placa, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Lista de otros vehículos activos
                    val vehiclesToShow = if (isDriver && isInService) {
                        autos.filter { it.id != myVehicle?.id && it.isActive() }
                    } else {
                        autos.filter { it.isActive() }
                    }

                    if (vehiclesToShow.isNotEmpty()) {
                        CarListSimple(autos = vehiclesToShow, onCarClick = { car ->
                            selectedCar = car
                            mapInstance?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(car.lat, car.lng), 17.0),
                                1000
                            )
                        })
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text  = if (isDriver && isInService) "Solo estás tú en servicio"
                                        else "No hay vehículos activos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
