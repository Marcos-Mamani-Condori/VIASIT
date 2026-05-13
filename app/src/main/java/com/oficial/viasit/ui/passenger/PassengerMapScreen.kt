package com.oficial.viasit.ui.passenger

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.UserRole
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
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.ui.theme.Rose500
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
fun PassengerMapScreen(
    passengerViewModel: PassengerViewModel,
    showTailsButton: Boolean = true,
    currentUser: com.oficial.viasit.domain.model.User? = null
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos          by autosViewModel.autosUiState.collectAsState()
    val tails          by autosViewModel.tails.collectAsState()
    val rutas          by passengerViewModel.rutas.collectAsState()
    val lineas         by passengerViewModel.lineas.collectAsState()
    val scope          = rememberCoroutineScope()
    val focusManager   = LocalFocusManager.current

    var mapInstance  by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle      by remember { mutableStateOf<Style?>(null) } 

    var showVehicleSheet by remember { mutableStateOf(false) }
    var showTails by remember { mutableStateOf(true) }
    var selectedVehicleIdForSheet by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    val selectedVehicleForSheet = remember(selectedVehicleIdForSheet, autos) {
        autos.find { it.id == selectedVehicleIdForSheet }
    }

    val geocodingService   = remember { GeocodingService() }
    var searchQuery        by remember { mutableStateOf("") }
    var searchResults      by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching        by remember { mutableStateOf(false) }
    var showSearchResults  by remember { mutableStateOf(false) }
    var searchJob          by remember { mutableStateOf<Job?>(null) }

    val selectedLineaId by passengerViewModel.selectedLineaId.collectAsState()
    val userRoleStr = currentUser?.role ?: ""
    val isLineAdmin = userRoleStr.equals(UserRole.ADMIN_LINEA.name, ignoreCase = true)
    val isPrincipalAdmin = userRoleStr.equals(UserRole.ADMIN_PRINCIPAL.name, ignoreCase = true)

    val puedeReportar = currentUser != null && !currentUser.isGuest

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
                    mapStyle = style
                }

                map.addOnMapClickListener { point ->
                    val pixel = map.projection.toScreenLocation(point)
                    
                    val clusters = map.queryRenderedFeatures(pixel, "clusters-layer")
                    if (clusters.isNotEmpty()) {
                        val currentZoom = map.cameraPosition.zoom
                        map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + 2.0))
                        return@addOnMapClickListener true
                    }

                    val features = map.queryRenderedFeatures(pixel, "cars-layer")
                    if (features.isNotEmpty()) {
                        val feature = features[0]
                        val autoId = feature.getStringProperty("id") ?: ""
                        val placa = feature.getStringProperty("placa") ?: ""
                        selectedVehicleIdForSheet = autoId
                        if (placa.isNotEmpty()) {
                            passengerViewModel.loadReportesForVehicle(placa)
                        }
                        showVehicleSheet = true
                        true
                    } else false
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

    LaunchedEffect(autos, mapStyle, selectedLineaId) {
        val activeAutos = autos.filter { it.isActive() }
        val filteredAutos = if (selectedLineaId != null) {
            activeAutos.filter { it.lineaId == selectedLineaId }
        } else {
            activeAutos
        }
        mapStyle?.let { style -> updateCarsSource(style, filteredAutos) }
    }

    LaunchedEffect(tails, mapStyle, showTails) {
        mapStyle?.let { style ->
            if (showTails) {
                com.oficial.viasit.ui.map.updateTailsSource(style, tails)
            } else {
                com.oficial.viasit.ui.map.updateTailsSource(style, emptyMap())
            }
        }
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

        // Top Search & Info Bar
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
                        "Transporte en vivo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedLineaId != null && routePolylines.isNotEmpty()) {
                        Text(
                            "Ruta: ${routePolylines.first().lineaName} · ${autos.count { it.isActive() }} unidad(es)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            "${autos.count { it.isActive() }} unidades activas · Selecciona una ruta",
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
                    IconButton(onClick = { 
                        passengerViewModel.clearSelectedLinea()
                        searchQuery = ""
                        searchResults = emptyList()
                        showSearchResults = false
                    }) {
                        Icon(
                            Icons.Default.Close, "Quitar ruta",
                            tint = Rose500, // Cambiado a rojo para mejor visibilidad
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar & Filter Chips (Enabled for everyone except simple guests if needed, but definitely for admins)
            if (true) {
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

                // Line Filter Chips (Visible to everyone, restricted for Line Admins)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLineAdmin) {
                            // Line Admin solo ve su propia línea
                            val miLinea = lineas.find { it.id == currentUser?.lineaId }
                            miLinea?.let { linea ->
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { /* Ya está seleccionada y restringida */ },
                                        label = { Text(linea.name) }
                                    )
                                }
                            }
                        } else {
                            item {
                                FilterChip(
                                    selected = selectedLineaId == null,
                                    onClick = { passengerViewModel.clearSelectedLinea() },
                                    label = { Text("Todas") }
                                )
                            }
                            items(lineas) { linea ->
                                FilterChip(
                                    selected = selectedLineaId == linea.id,
                                    onClick = { passengerViewModel.selectLinea(linea.id) },
                                    label = { Text(linea.name) }
                                )
                            }
                        }
                    }

                    if (selectedLineaId != null && puedeReportar) {
                        var showRouteReportDialog by remember { mutableStateOf(false) }
                        var routeReportText by remember { mutableStateOf("") }
                        val linea = lineas.find { it.id == selectedLineaId }

                        IconButton(
                            onClick = { showRouteReportDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Rose500.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ReportProblem,
                                "Reportar Ruta",
                                tint = Rose500,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showRouteReportDialog) {
                            AlertDialog(
                                onDismissRequest = { showRouteReportDialog = false },
                                title = { Text("Reportar Ruta ${linea?.name}") },
                                text = {
                                    OutlinedTextField(
                                        value = routeReportText,
                                        onValueChange = { routeReportText = it },
                                        placeholder = { Text("Ej: No está pasando, cambió de ruta...") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (routeReportText.isNotBlank()) {
                                                passengerViewModel.reportarProblemaRuta(linea?.id ?: "", linea?.name ?: "", routeReportText)
                                                showRouteReportDialog = false
                                                routeReportText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                                    ) { Text("Enviar") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRouteReportDialog = false }) { Text("Cancelar") }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showTailsButton) {
            SmallFloatingActionButton(
                onClick = { showTails = !showTails },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 75.dp),
                containerColor = if (showTails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (showTails) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    if (showTails) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                    "Ver rastro", 
                    modifier = Modifier.size(18.dp)
                )
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

        if (showVehicleSheet && selectedVehicleForSheet != null) {
            val auto = selectedVehicleForSheet!!
            val linea = lineas.find { it.id == auto.lineaId }
            val reportes by passengerViewModel.selectedVehicleReportes.collectAsState()

            ModalBottomSheet(
                onDismissRequest = {
                    showVehicleSheet = false
                    selectedVehicleIdForSheet = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                VehicleInfoContent(
                    auto = auto,
                    lineaName = linea?.name ?: "Desconocida",
                    reportes = reportes,
                    puedeReportar = puedeReportar,
                    onReportClick = { desc ->
                        passengerViewModel.reportarMalServicio(auto.placa, auto.userId, auto.lineaId, desc)
                    }
                )
            }
        }
    }
}

@Composable
fun VehicleInfoContent(
    auto: Auto,
    lineaName: String,
    reportes: List<com.oficial.viasit.domain.model.Reporte>,
    puedeReportar: Boolean = false,
    onReportClick: (String) -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = auto.placa,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Línea: $lineaName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Reputation Badge (Muro de la Verdad)
            Surface(
                color = if (reportes.isEmpty()) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF43F5E).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (reportes.isEmpty()) Icons.Default.Shield else Icons.Default.Warning,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (reportes.isEmpty()) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (reportes.isEmpty()) "Sin denuncias" else "${reportes.size} Reportes",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reportes.isEmpty()) Color(0xFF10B981) else Color(0xFFF43F5E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoItem(
                label = "Conductor",
                value = auto.driverName.ifBlank { "Asignando..." },
                icon = Icons.Default.Person
            )
            InfoItem(
                label = "Actualizado",
                value = auto.getLastUpdateText(),
                icon = Icons.Default.Update,
                alignment = Alignment.End
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))

        if (reportes.isNotEmpty()) {
            Text(
                "Muro de la Verdad (Reportes Recientes)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reportes.take(3).forEach { reporte ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                reporte.description.replace("[Unidad: ${auto.placa}]", "").trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Reportado por: ${reporte.reporterName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                            if (reporte.driverResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Respuesta Conductor: ${reporte.driverResponse}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                            }
                            if (reporte.adminResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Respuesta Admin: ${reporte.adminResponse}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (puedeReportar) {
            Button(
                onClick = { showReportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.ReportProblem, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reportar Incidente o Mal Trato", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Reportar Vehículo ${auto.placa}") },
            text = {
                Column {
                    Text("Tu reporte ayuda a mejorar el servicio. Sé específico.", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Ej: Exceso de velocidad, mal trato...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            onReportClick(reportText)
                            showReportDialog = false
                            reportText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Enviar Reporte")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.15f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignment == Alignment.Start) {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (alignment == Alignment.End) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            routes.take(6).forEachIndexed { i, route ->
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
