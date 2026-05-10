package com.oficial.viasit.ui.passenger

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.ui.map.RoutePolyline
import com.oficial.viasit.ui.map.addCarsLayer
import com.oficial.viasit.ui.map.addCarIconToStyle
import com.oficial.viasit.ui.map.addRoutesLayer
import com.oficial.viasit.ui.map.centerOnUser
import com.oficial.viasit.ui.map.enableLocationComponent
import com.oficial.viasit.ui.map.updateCarsSource
import com.oficial.viasit.ui.map.clearRoutesSource
import com.oficial.viasit.ui.map.updateRoutesSource
import com.oficial.viasit.ui.routes.GeocodingService
import com.oficial.viasit.ui.routes.SearchResult
import com.oficial.viasit.viewmodels.PassengerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerMapScreen(passengerViewModel: PassengerViewModel) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos          by autosViewModel.autosUiState.collectAsState()
    val rutas          by passengerViewModel.rutas.collectAsState()
    val lineas         by passengerViewModel.lineas.collectAsState()
    val scope          = rememberCoroutineScope()
    val focusManager   = LocalFocusManager.current

    var mapInstance  by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle      by remember { mutableStateOf<Style?>(null) }  // se asigna cuando el estilo termina de cargar

    val geocodingService   = remember { GeocodingService() }
    var searchQuery        by remember { mutableStateOf("") }
    var searchResults      by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching        by remember { mutableStateOf(false) }
    var showSearchResults  by remember { mutableStateOf(false) }
    var searchJob          by remember { mutableStateOf<Job?>(null) }

    val selectedLineaId by passengerViewModel.selectedLineaId.collectAsState()

    val routePolylines: List<RoutePolyline> = remember(rutas, lineas, selectedLineaId) {
        if (selectedLineaId == null) return@remember emptyList()
        val linea = lineas.find { it.id == selectedLineaId } ?: return@remember emptyList()
        val ruta = rutas[linea.rutaId] ?: return@remember emptyList()
        if (ruta.startPoint.isBlank() || ruta.endPoint.isBlank()) return@remember emptyList()
        listOf(
            RoutePolyline(
                id         = ruta.id,
                lineaName  = linea.name,
                startPoint = ruta.startPoint,
                endPoint   = ruta.endPoint,
                waypoints  = ruta.waypoints
            )
        )
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    addCarIconToStyle(style, context)
                    addCarsLayer(style)
                    addRoutesLayer(style)
                    enableLocationComponent(style, map, context)
                    mapStyle = style  // ✅ estilo listo — ahora sí se puede dibujar
                }
            }
        }
    }

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

    LaunchedEffect(autos, mapStyle) {
        val activeAutos = autos.filter { it.isActive() }
        mapStyle?.let { style -> updateCarsSource(style, activeAutos) }
    }

    LaunchedEffect(routePolylines, mapStyle) {
        val style = mapStyle ?: return@LaunchedEffect
        if (routePolylines.isNotEmpty()) {
            updateRoutesSource(style, routePolylines)
            val firstRoute = routePolylines.first()
            val parts = firstRoute.startPoint.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null) {
                    mapInstance?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14.0)
                    )
                }
            }
        } else {
            clearRoutesSource(style)
        }
    }

    val selectedAuto by passengerViewModel.selectedAuto.collectAsState()
    LaunchedEffect(selectedAuto, mapStyle) {
        val auto = selectedAuto ?: return@LaunchedEffect
        if (mapStyle == null) return@LaunchedEffect
        if (auto.lat != 0.0 || auto.lng != 0.0) {
            mapInstance?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(auto.lat, auto.lng), 16.0)
            )
        }
        passengerViewModel.clearSelectedAuto()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mapa en vivo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedLineaId != null && routePolylines.isNotEmpty()) {
                        Text(
                            "Ruta: ${routePolylines.first().lineaName} · ${autos.count { it.isActive() }} bus(es)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            "${autos.count { it.isActive() }} bus(es) activo(s) · Selecciona una ruta",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = {
                    scope.launch { autosViewModel.refresh() }
                }) {
                    Icon(
                        Icons.Default.Refresh, "Actualizar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (selectedLineaId != null) {
                    IconButton(onClick = { passengerViewModel.clearSelectedLinea() }) {
                        Icon(
                            Icons.Default.Close, "Quitar ruta",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
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
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()
                            if (query.length >= 3) {
                                searchJob = scope.launch {
                                    kotlinx.coroutines.delay(400)
                                    isSearching = true
                                    showSearchResults = true
                                    geocodingService.search(query).fold(
                                        onSuccess = { results ->
                                            searchResults = results
                                            isSearching = false
                                        },
                                        onFailure = {
                                            searchResults = emptyList()
                                            isSearching = false
                                        }
                                    )
                                }
                            } else {
                                searchResults = emptyList()
                                showSearchResults = false
                            }
                        },
                        placeholder = {
                            Text(
                                "Buscar en La Paz...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        singleLine  = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResults = emptyList()
                            showSearchResults = false
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .heightIn(max = 200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
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
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        result.displayName.split(",").take(3).joinToString(","),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }
                            if (searchResults.last() != result) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mapInstance?.let { centerOnUser(it, context) } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, "Mi ubicación", modifier = Modifier.size(22.dp))
        }

        if (routePolylines.isNotEmpty()) {
            RoutePolylineLegend(
                routes = routePolylines,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun RoutePolylineLegend(
    routes: List<RoutePolyline>,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        "#3D5AFE", "#06B6D4", "#10B981", "#FFB300", "#F43F5E",
        "#8B5CF6", "#EC4899", "#14B8A6", "#F97316", "#84CC16"
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            routes.take(6).forEachIndexed { i, route ->  // Máximo 6 en leyenda
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 18.dp, height = 4.dp)
                            .background(
                                try { Color(android.graphics.Color.parseColor(palette[i % palette.size])) }
                                catch (e: Exception) { Color(0xFF3D5AFE) },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        route.lineaName.take(20),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                }
            }
            if (routes.size > 6) {
                Text(
                    "+${routes.size - 6} más",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
