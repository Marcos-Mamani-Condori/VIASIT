@file:OptIn(ExperimentalMaterial3Api::class)

package com.oficial.viasit.ui.routes

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oficial.viasit.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Pantalla para seleccionar el punto de inicio y fin de una ruta en el mapa.
 *
 * Este archivo solo contiene la UI y el estado de la pantalla.
 * La lógica auxiliar de MapLibre está en:
 *  → [RouteMapHelpers.kt]  (marcadores, iconos, centrar en usuario)
 *
 * Los modelos de datos están en:
 *  → [RouteModels.kt]      (RouteLocation, RouteData, PointSelectionMode)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapPickerScreen(
    lineaId: String,
    lineaName: String,
    onSaveRoute: (RouteData) -> Unit,
    onBack: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager   = LocalFocusManager.current
    val scope          = rememberCoroutineScope()

    // Inicializar MapLibre
    LaunchedEffect(Unit) { MapLibre.getInstance(context) }

    // ── Estado ──────────────────────────────────────────────────────────────
    val geocodingService = remember { GeocodingService() }

    var mapInstance  by remember { mutableStateOf<MapLibreMap?>(null) }
    var startPoint   by remember { mutableStateOf<RouteLocation?>(null) }
    var endPoint     by remember { mutableStateOf<RouteLocation?>(null) }
    var selectionMode by remember { mutableStateOf<PointSelectionMode>(PointSelectionMode.START) }

    var routeName        by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }
    var showSaveDialog   by remember { mutableStateOf(false) }

    var searchQuery      by remember { mutableStateOf("") }
    var searchResults    by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching      by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob        by remember { mutableStateOf<Job?>(null) }

    // ── MapView ─────────────────────────────────────────────────────────────
    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    addMarkerIconsToStyle(style, context)
                    addMarkerLayer(style, "start-marker-source", "start-marker-layer", "start-marker-icon")
                    addMarkerLayer(style, "end-marker-source",   "end-marker-layer",   "end-marker-icon")
                    enableRouteLocationComponent(style, map, context)
                }
                // Cámara inicial: Bolivia
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(-16.2902, -63.5887)).zoom(5.0).build()

                // Clic en mapa → colocar marcador según el modo actual
                map.addOnMapClickListener { latLng ->
                    when (selectionMode) {
                        PointSelectionMode.START -> {
                            startPoint = RouteLocation(latLng.latitude, latLng.longitude, "Inicio")
                            updateMarker(map.style!!, "start-marker-source", latLng)
                            selectionMode = PointSelectionMode.END // auto-avanzar al siguiente
                        }
                        PointSelectionMode.END -> {
                            endPoint = RouteLocation(latLng.latitude, latLng.longitude, "Fin")
                            updateMarker(map.style!!, "end-marker-source", latLng)
                        }
                    }
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

    BackHandler(onBack = onBack)

    // ── Layout ───────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        // ── Top bar + buscador ─────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text("Crear Ruta", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Línea: $lineaName",
                            style = MaterialTheme.typography.labelSmall, color = Slate400)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950.copy(alpha = 0.95f)
                )
            )

            // Barra de búsqueda de lugares
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, "Buscar", tint = Slate400,
                        modifier = Modifier.padding(start = 12.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { newQuery ->
                            searchQuery = newQuery
                            searchJob?.cancel()
                            if (newQuery.length >= 3) {
                                searchJob = scope.launch {
                                    delay(500)
                                    isSearching = true; showSearchResults = true
                                    geocodingService.search(newQuery).fold(
                                        onSuccess = { searchResults = it; isSearching = false },
                                        onFailure = { searchResults = emptyList(); isSearching = false }
                                    )
                                }
                            } else {
                                searchResults = emptyList(); showSearchResults = false
                            }
                        },
                        placeholder = { Text("Buscar calle, ciudad o lugar...", color = Slate400) },
                        singleLine  = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                if (searchQuery.length >= 3) {
                                    scope.launch {
                                        isSearching = true; showSearchResults = true
                                        geocodingService.search(searchQuery).fold(
                                            onSuccess = { searchResults = it; isSearching = false },
                                            onFailure = { searchResults = emptyList(); isSearching = false }
                                        )
                                    }
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            cursorColor             = Brand500,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""; searchResults = emptyList()
                            showSearchResults = false; focusManager.clearFocus()
                        }) { Icon(Icons.Default.Clear, "Limpiar", tint = Slate400) }
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            color = Brand500, strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Resultados de búsqueda
            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 250.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mapInstance?.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(result.lat, result.lon), 16.0)
                                        )
                                        when (selectionMode) {
                                            PointSelectionMode.START -> {
                                                startPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let { updateMarker(it, "start-marker-source", LatLng(result.lat, result.lon)) }
                                                selectionMode = PointSelectionMode.END
                                            }
                                            PointSelectionMode.END -> {
                                                endPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let { updateMarker(it, "end-marker-source", LatLng(result.lat, result.lon)) }
                                            }
                                        }
                                        showSearchResults = false; searchQuery = ""; focusManager.clearFocus()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Brand400, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.name, style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White, fontWeight = FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(result.displayName, style = MaterialTheme.typography.bodySmall,
                                        color = Slate400, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (searchResults.indexOf(result) < searchResults.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Slate700)
                            }
                        }
                    }
                }
            }

            // Sin resultados
            if (showSearchResults && searchResults.isEmpty() && !isSearching && searchQuery.length >= 3) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SearchOff, null, tint = Slate400)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No se encontraron resultados", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                    }
                }
            }
        }

        // ── Panel inferior (indicador de modo + resumen + botones) ─────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador del modo actual
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (selectionMode) {
                        PointSelectionMode.START -> Brand500.copy(alpha = 0.9f)
                        PointSelectionMode.END   -> Emerald500.copy(alpha = 0.9f)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        when (selectionMode) {
                            PointSelectionMode.START -> Icons.Default.LocationOn
                            PointSelectionMode.END   -> Icons.Default.Flag
                        },
                        null, tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            when (selectionMode) {
                                PointSelectionMode.START -> "Toca el mapa para seleccionar el INICIO"
                                PointSelectionMode.END   -> "Toca el mapa para seleccionar el FIN"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
            }

            // Resumen de puntos seleccionados
            if (startPoint != null || endPoint != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.95f)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Brand500, RoundedCornerShape(50)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inicio", style = MaterialTheme.typography.labelMedium,
                                color = if (startPoint != null) Brand400 else Slate600)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Emerald500, RoundedCornerShape(50)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fin", style = MaterialTheme.typography.labelMedium,
                                color = if (endPoint != null) Emerald400 else Slate600)
                        }
                        IconButton(
                            onClick = {
                                startPoint = null; endPoint = null
                                selectionMode = PointSelectionMode.START
                                mapInstance?.style?.let {
                                    clearMarker(it, "start-marker-source")
                                    clearMarker(it, "end-marker-source")
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Limpiar", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Botones de acción
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        selectionMode = if (selectionMode == PointSelectionMode.START)
                            PointSelectionMode.END else PointSelectionMode.START
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Slate600)
                ) {
                    Icon(
                        if (selectionMode == PointSelectionMode.START) Icons.Default.Flag else Icons.Default.LocationOn,
                        null, modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectionMode == PointSelectionMode.START) "Sel. Fin" else "Sel. Inicio",
                        fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled  = startPoint != null && endPoint != null
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Botón de ubicación
        LargeFloatingActionButton(
            onClick        = { mapInstance?.let { centerOnUserRoute(it, context) } },
            modifier       = Modifier.align(Alignment.TopEnd).padding(top = 180.dp, end = 16.dp),
            containerColor = Slate800,
            contentColor   = Color.White,
            shape          = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, "Mi ubicación")
        }
    }

    // ── Diálogo guardar ruta ─────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor   = Slate800,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Guardar Ruta", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Resumen de los puntos
                    Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Brand500, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Inicio: ${startPoint!!.name.ifEmpty { "%.4f, %.4f".format(startPoint!!.lat, startPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall, color = Slate200
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Emerald500, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fin: ${endPoint!!.name.ifEmpty { "%.4f, %.4f".format(endPoint!!.lat, endPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall, color = Slate200
                                )
                            }
                        }
                    }
                    // Nombre y descripción
                    OutlinedTextField(
                        value         = routeName,
                        onValueChange = { routeName = it },
                        label         = { Text("Nombre de la ruta *") },
                        singleLine    = true,
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Brand500, unfocusedBorderColor  = Slate700,
                            focusedLabelColor       = Brand400, unfocusedLabelColor   = Slate400,
                            focusedTextColor        = Color.White, unfocusedTextColor = Slate200,
                            cursorColor             = Brand500,
                            focusedContainerColor   = Slate900, unfocusedContainerColor = Slate900
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value         = routeDescription,
                        onValueChange = { routeDescription = it },
                        label         = { Text("Descripción (opcional)") },
                        singleLine    = false, minLines = 2, maxLines = 3,
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Brand500, unfocusedBorderColor  = Slate700,
                            focusedLabelColor       = Brand400, unfocusedLabelColor   = Slate400,
                            focusedTextColor        = Color.White, unfocusedTextColor = Slate200,
                            cursorColor             = Brand500,
                            focusedContainerColor   = Slate900, unfocusedContainerColor = Slate900
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotBlank() && startPoint != null && endPoint != null) {
                            onSaveRoute(RouteData(routeName, routeDescription, startPoint, endPoint))
                            showSaveDialog = false
                        }
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled = routeName.isNotBlank()
                ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar", color = Slate400) }
            }
        )
    }
}
