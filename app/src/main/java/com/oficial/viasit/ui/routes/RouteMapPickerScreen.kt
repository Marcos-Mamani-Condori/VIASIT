@file:OptIn(ExperimentalMaterial3Api::class)

package com.oficial.viasit.ui.routes

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oficial.viasit.domain.model.PointSelectionMode
import com.oficial.viasit.domain.model.RouteData
import com.oficial.viasit.domain.model.RouteLocation
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
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

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

    LaunchedEffect(Unit) { MapLibre.getInstance(context) }

    val geocodingService = remember { GeocodingService() }
    val routingService   = remember { RoutingService() }

    var mapInstance    by remember { mutableStateOf<MapLibreMap?>(null) }
    var startPoint     by remember { mutableStateOf<RouteLocation?>(null) }
    var endPoint       by remember { mutableStateOf<RouteLocation?>(null) }
    val viaPoints       = remember { mutableStateListOf<RouteLocation>() } // puntos intermedios forzados
    var selectionMode  by remember { mutableStateOf<PointSelectionMode>(PointSelectionMode.START) }
    var viaMode        by remember { mutableStateOf(false) }   // tap = agregar punto vía
    var isRouting      by remember { mutableStateOf(false) }   // cargando OSRM
    var routingError   by remember { mutableStateOf<String?>(null) }
    var calculatedRoute by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }

    var routeName        by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }
    var showSaveDialog   by remember { mutableStateOf(false) }

    var searchQuery       by remember { mutableStateOf("") }
    var searchResults     by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching       by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob         by remember { mutableStateOf<Job?>(null) }
    var routingJob        by remember { mutableStateOf<Job?>(null) }


    fun drawPreviewLine(style: Style, points: List<Pair<Double, Double>>) {
        val src = style.getSource("preview-line-source") as? GeoJsonSource ?: return
        if (points.size >= 2) {
            val mapPoints = points.map { (lat, lng) -> Point.fromLngLat(lng, lat) }
            src.setGeoJson(FeatureCollection.fromFeatures(
                listOf(Feature.fromGeometry(LineString.fromLngLats(mapPoints)))
            ))
        } else {
            src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    fun recalculateRoute() {
        val start = startPoint ?: return
        val end   = endPoint   ?: return
        routingJob?.cancel()
        routingJob = scope.launch {
            isRouting = true
            routingError = null
            val allPts = listOf(Pair(start.lat, start.lng)) +
                    viaPoints.map { Pair(it.lat, it.lng) } +
                    listOf(Pair(end.lat, end.lng))

            routingService.getRoute(allPts).fold(
                onSuccess = { route ->
                    calculatedRoute = route
                    mapInstance?.getStyle { style -> drawPreviewLine(style, route) }
                },
                onFailure = { err ->
                    calculatedRoute = allPts
                    mapInstance?.getStyle { style -> drawPreviewLine(style, allPts) }
                    routingError = "Sin conexión, usando línea recta"
                }
            )
            isRouting = false
        }
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    addMarkerIconsToStyle(style, context)
                    addMarkerLayer(style, "start-marker-source", "start-marker-layer", "start-marker-icon")
                    addMarkerLayer(style, "end-marker-source",   "end-marker-layer",   "end-marker-icon")

                    if (style.getSource("preview-line-source") == null)
                        style.addSource(GeoJsonSource("preview-line-source"))
                    if (style.getLayer("preview-line-layer") == null) {
                        style.addLayer(
                            LineLayer("preview-line-layer", "preview-line-source").withProperties(
                                PropertyFactory.lineColor("#3D5AFE"),
                                PropertyFactory.lineWidth(4f),
                                PropertyFactory.lineOpacity(0.85f),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                            )
                        )
                    }
                    enableRouteLocationComponent(style, map, context)
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(-16.5000, -68.1500)).zoom(12.0).build()

                map.addOnMapClickListener { latLng ->
                    val mapStyle = map.style ?: return@addOnMapClickListener true
                    when {
                        viaMode -> {
                            viaPoints.add(RouteLocation(latLng.latitude, latLng.longitude, "Vía"))
                            recalculateRoute()
                        }
                        selectionMode == PointSelectionMode.START -> {
                            startPoint = RouteLocation(latLng.latitude, latLng.longitude, "Inicio")
                            updateMarker(mapStyle, "start-marker-source", latLng)
                            selectionMode = PointSelectionMode.END
                            if (endPoint != null) recalculateRoute()
                        }
                        selectionMode == PointSelectionMode.END -> {
                            endPoint = RouteLocation(latLng.latitude, latLng.longitude, "Fin")
                            updateMarker(mapStyle, "end-marker-source", latLng)
                            recalculateRoute() // ← auto-calcula la ruta por calles
                        }
                    }
                    true
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

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
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
                actions = {
                    if (isRouting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            color = Brand400, strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950.copy(alpha = 0.95f)
                )
            )

            AnimatedVisibility(visible = routingError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.9f)),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(routingError ?: "", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                        IconButton(onClick = { routingError = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, "Buscar", tint = Slate400,
                        modifier = Modifier.padding(start = 12.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q; searchJob?.cancel()
                            if (q.length >= 3) {
                                searchJob = scope.launch {
                                    delay(500); isSearching = true; showSearchResults = true
                                    geocodingService.search(q).fold(
                                        onSuccess = { searchResults = it; isSearching = false },
                                        onFailure = { searchResults = emptyList(); isSearching = false }
                                    )
                                }
                            } else { searchResults = emptyList(); showSearchResults = false }
                        },
                        placeholder  = { Text("Buscar calle o lugar en La Paz...", color = Slate400) },
                        singleLine   = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = Brand500, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""; searchResults = emptyList()
                            showSearchResults = false; focusManager.clearFocus()
                        }) { Icon(Icons.Default.Clear, "Limpiar", tint = Slate400) }
                    }
                    if (isSearching) CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp), color = Brand500, strokeWidth = 2.dp)
                }
            }

            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 240.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800), shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        mapInstance?.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(result.lat, result.lon), 16.0))
                                        when {
                                            viaMode -> {
                                                viaPoints.add(RouteLocation(result.lat, result.lon, result.name))
                                                recalculateRoute()
                                            }
                                            selectionMode == PointSelectionMode.START -> {
                                                startPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let {
                                                    updateMarker(it, "start-marker-source", LatLng(result.lat, result.lon))
                                                }
                                                selectionMode = PointSelectionMode.END
                                                if (endPoint != null) recalculateRoute()
                                            }
                                            else -> {
                                                endPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let {
                                                    updateMarker(it, "end-marker-source", LatLng(result.lat, result.lon))
                                                }
                                                recalculateRoute()
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
                                    Text(result.name, color = Color.White, fontWeight = FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(result.displayName, color = Slate400, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (searchResults.indexOf(result) < searchResults.lastIndex)
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Slate700)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isRouting                              -> Slate700.copy(alpha = 0.95f)
                        viaMode                               -> Color(0xFF8B5CF6).copy(alpha = 0.9f)
                        selectionMode == PointSelectionMode.START -> Brand500.copy(alpha = 0.9f)
                        else                                  -> Emerald500.copy(alpha = 0.9f)
                    }
                ), shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRouting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(when {
                            viaMode                               -> Icons.Default.AddLocation
                            selectionMode == PointSelectionMode.START -> Icons.Default.LocationOn
                            else                                  -> Icons.Default.Flag
                        }, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Text(when {
                        isRouting                              -> "Calculando ruta por calles..."
                        viaMode                               -> "Toca para agregar punto de paso"
                        selectionMode == PointSelectionMode.START -> "Toca el mapa para marcar el INICIO"
                        else                                  -> "Toca el mapa para marcar el FIN"
                    }, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (startPoint != null || endPoint != null || viaPoints.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.95f)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Brand500, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (startPoint != null) startPoint!!.name.ifEmpty { "Inicio" }.take(12)
                                else "Inicio",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (startPoint != null) Brand400 else Slate600
                            )
                        }
                        if (viaPoints.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFF8B5CF6), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${viaPoints.size} pasos", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFAD8EF0))
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Undo, "Quitar último paso",
                                    tint = Slate500, modifier = Modifier.size(14.dp).clickable {
                                        viaPoints.removeLastOrNull()
                                        if (startPoint != null && endPoint != null) recalculateRoute()
                                    })
                            }
                        }
                        if (calculatedRoute.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, null, tint = Slate400, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${calculatedRoute.size} pts",
                                    style = MaterialTheme.typography.labelSmall, color = Slate400)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Emerald500, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (endPoint != null) endPoint!!.name.ifEmpty { "Fin" }.take(12)
                                else "Fin",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (endPoint != null) Emerald400 else Slate600
                            )
                        }
                        // Limpiar todo
                        IconButton(onClick = {
                            startPoint = null; endPoint = null
                            viaPoints.clear(); calculatedRoute = emptyList()
                            viaMode = false; selectionMode = PointSelectionMode.START
                            mapInstance?.style?.let {
                                clearMarker(it, "start-marker-source")
                                clearMarker(it, "end-marker-source")
                                drawPreviewLine(it, emptyList())
                            }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, "Limpiar", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viaMode = false
                        selectionMode = if (selectionMode == PointSelectionMode.START)
                            PointSelectionMode.END else PointSelectionMode.START },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border   = androidx.compose.foundation.BorderStroke(1.dp,
                        if (!viaMode) Brand500 else Slate600)
                ) {
                    Icon(if (selectionMode == PointSelectionMode.START) Icons.Default.Flag else Icons.Default.LocationOn,
                        null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectionMode == PointSelectionMode.START) "Marcar Fin" else "Inicio",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick  = { viaMode = !viaMode },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor   = if (viaMode) Color(0xFFAD8EF0) else Slate400,
                        containerColor = if (viaMode) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp,
                        if (viaMode) Color(0xFF8B5CF6) else Slate600)
                ) {
                    Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (viaMode) "Paso ON" else "+ Paso", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick  = { showSaveDialog = true },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled  = startPoint != null && endPoint != null && !isRouting
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LargeFloatingActionButton(
            onClick = { mapInstance?.let { centerOnUserRoute(it, context) } },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 180.dp, end = 16.dp),
            containerColor = Slate800, contentColor = Color.White, shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Default.MyLocation, "Mi ubicación") }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor   = Slate800, shape = RoundedCornerShape(20.dp),
            title  = { Text("Guardar Ruta", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Slate900), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Brand500, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Inicio: ${startPoint!!.name.ifEmpty { "%.4f, %.4f".format(startPoint!!.lat, startPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall, color = Slate200)
                            }
                            if (calculatedRoute.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Route, null, tint = Brand400, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ruta calculada por calles (${calculatedRoute.size} puntos)",
                                        style = MaterialTheme.typography.bodySmall, color = Slate400)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Emerald500, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fin: ${endPoint!!.name.ifEmpty { "%.4f, %.4f".format(endPoint!!.lat, endPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall, color = Slate200)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = routeName, onValueChange = { routeName = it },
                        label = { Text("Nombre de la ruta *") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500, unfocusedBorderColor = Slate700,
                            focusedLabelColor  = Brand400, unfocusedLabelColor  = Slate400,
                            focusedTextColor   = Color.White, unfocusedTextColor = Slate200,
                            cursorColor = Brand500,
                            focusedContainerColor = Slate900, unfocusedContainerColor = Slate900
                        ), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = routeDescription, onValueChange = { routeDescription = it },
                        label = { Text("Descripción (opcional)") },
                        singleLine = false, minLines = 2, maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500, unfocusedBorderColor = Slate700,
                            focusedLabelColor  = Brand400, unfocusedLabelColor  = Slate400,
                            focusedTextColor   = Color.White, unfocusedTextColor = Slate200,
                            cursorColor = Brand500,
                            focusedContainerColor = Slate900, unfocusedContainerColor = Slate900
                        ), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotBlank() && startPoint != null && endPoint != null) {
                            val waypointList = if (calculatedRoute.size > 2) {
                                calculatedRoute.drop(1).dropLast(1)
                                    .map { (lat, lng) -> RouteLocation(lat, lng) }
                            } else emptyList()

                            onSaveRoute(RouteData(
                                name        = routeName,
                                description = routeDescription,
                                startPoint  = startPoint,
                                endPoint    = endPoint,
                                waypoints   = waypointList
                            ))
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
