package com.oficial.viasit.ui.routes

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oficial.viasit.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Data class to hold route location data
 */
data class RouteLocation(
    val lat: Double,
    val lng: Double,
    val name: String = ""
)

/**
 * Data class to hold the complete route data
 */
data class RouteData(
    val name: String,
    val description: String,
    val startPoint: RouteLocation?,
    val endPoint: RouteLocation?
)

/**
 * Screen for selecting route start and end points on a map
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapPickerScreen(
    lineaId: String,
    lineaName: String,
    onSaveRoute: (RouteData) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    // Geocoding service
    val geocodingService = remember { GeocodingService() }
    
    // Map state
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    
    // Selection state
    var startPoint by remember { mutableStateOf<RouteLocation?>(null) }
    var endPoint by remember { mutableStateOf<RouteLocation?>(null) }
    var selectionMode by remember { mutableStateOf<PointSelectionMode>(PointSelectionMode.START) }
    
    // Route details
    var routeName by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    // Initialize MapLibre
    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }
    
    // Create map view
    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    // Add marker icons
                    addMarkerIconsToStyle(style, context)
                    // Add layers for start and end markers
                    addMarkerLayer(style, "start-marker-source", "start-marker-layer", "start-marker-icon")
                    addMarkerLayer(style, "end-marker-source", "end-marker-layer", "end-marker-icon")
                    // Enable location
                    enableLocationComponent(style, map, context)
                }
                
                // Set initial camera position (Bolivia center)
                map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                    .target(LatLng(-16.2902, -63.5887))
                    .zoom(5.0)
                    .build()
                
                // Handle map clicks
                map.addOnMapClickListener { latLng ->
                    when (selectionMode) {
                        PointSelectionMode.START -> {
                            startPoint = RouteLocation(latLng.latitude, latLng.longitude, "Inicio")
                            updateMarker(style = map.style!!, sourceId = "start-marker-source", latLng = latLng)
                            // Auto switch to end point selection
                            selectionMode = PointSelectionMode.END
                        }
                        PointSelectionMode.END -> {
                            endPoint = RouteLocation(latLng.latitude, latLng.longitude, "Fin")
                            updateMarker(style = map.style!!, sourceId = "end-marker-source", latLng = latLng)
                        }
                    }
                    true
                }
            }
        }
    }
    
    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Back handler
    BackHandler(onBack = onBack)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())
        
        // Top bar with search
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Crear Ruta",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Línea: $lineaName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
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
            
            // Search bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Slate400,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { newQuery ->
                            searchQuery = newQuery
                            // Debounce search
                            searchJob?.cancel()
                            if (newQuery.length >= 3) {
                                searchJob = scope.launch {
                                    delay(500) // Wait 500ms before searching
                                    isSearching = true
                                    showSearchResults = true
                                    geocodingService.search(newQuery).fold(
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
                            Text("Buscar calle, ciudad o lugar...", color = Slate400) 
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                if (searchQuery.length >= 3) {
                                    scope.launch {
                                        isSearching = true
                                        showSearchResults = true
                                        geocodingService.search(searchQuery).fold(
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
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Brand500,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                showSearchResults = false
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(Icons.Default.Clear, "Limpiar", tint = Slate400)
                        }
                    }
                    
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = Brand500,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Search results dropdown
            if (showSearchResults && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 250.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Navigate to location
                                        mapInstance?.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(result.lat, result.lon),
                                                16.0
                                            )
                                        )
                                        // Set as selected point
                                        when (selectionMode) {
                                            PointSelectionMode.START -> {
                                                startPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let { style ->
                                                    updateMarker(style, "start-marker-source", LatLng(result.lat, result.lon))
                                                }
                                                selectionMode = PointSelectionMode.END
                                            }
                                            PointSelectionMode.END -> {
                                                endPoint = RouteLocation(result.lat, result.lon, result.name)
                                                mapInstance?.style?.let { style ->
                                                    updateMarker(style, "end-marker-source", LatLng(result.lat, result.lon))
                                                }
                                            }
                                        }
                                        showSearchResults = false
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Brand400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        result.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (searchResults.indexOf(result) < searchResults.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
            }
            
            // No results message
            if (showSearchResults && searchResults.isEmpty() && !isSearching && searchQuery.length >= 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Slate400
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "No se encontraron resultados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    }
                }
            }
        }
        
        // Selection mode indicator and controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current selection mode card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (selectionMode) {
                        PointSelectionMode.START -> Brand500.copy(alpha = 0.9f)
                        PointSelectionMode.END -> Emerald500.copy(alpha = 0.9f)
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
                            PointSelectionMode.END -> Icons.Default.Flag
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            when (selectionMode) {
                                PointSelectionMode.START -> "Toca el mapa para seleccionar el INICIO"
                                PointSelectionMode.END -> "Toca el mapa para seleccionar el FIN"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (startPoint != null && selectionMode == PointSelectionMode.START) {
                            Text(
                                startPoint!!.name.ifEmpty { "Inicio: ${String.format("%.4f", startPoint!!.lat)}, ${String.format("%.4f", startPoint!!.lng)}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        if (endPoint != null && selectionMode == PointSelectionMode.END) {
                            Text(
                                endPoint!!.name.ifEmpty { "Fin: ${String.format("%.4f", endPoint!!.lat)}, ${String.format("%.4f", endPoint!!.lng)}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // Selected points summary
            if (startPoint != null || endPoint != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start point
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Brand500, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Inicio",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (startPoint != null) Brand400 else Slate600
                            )
                        }
                        
                        // End point
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Emerald500, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Fin",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (endPoint != null) Emerald400 else Slate600
                            )
                        }
                        
                        // Clear button
                        IconButton(
                            onClick = {
                                startPoint = null
                                endPoint = null
                                selectionMode = PointSelectionMode.START
                                mapInstance?.style?.let { style ->
                                    clearMarker(style, "start-marker-source")
                                    clearMarker(style, "end-marker-source")
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                "Limpiar",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Switch mode button
                OutlinedButton(
                    onClick = {
                        selectionMode = if (selectionMode == PointSelectionMode.START) 
                            PointSelectionMode.END 
                        else 
                            PointSelectionMode.START
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate600)
                ) {
                    Icon(
                        if (selectionMode == PointSelectionMode.START) Icons.Default.Flag else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectionMode == PointSelectionMode.START) "Sel. Fin" else "Sel. Inicio",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Continue button
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand500
                    ),
                    enabled = startPoint != null && endPoint != null
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Center on location button
        LargeFloatingActionButton(
            onClick = { 
                mapInstance?.let { centerOnUser(it, context) }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 180.dp, end = 16.dp),
            containerColor = Slate800,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
        }
    }
    
    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { 
                Text(
                    "Guardar Ruta",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Route info summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Brand500, RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Inicio: ${startPoint!!.name.ifEmpty { String.format("%.4f", startPoint!!.lat) + ", " + String.format("%.4f", startPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Emerald500, RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fin: ${endPoint!!.name.ifEmpty { String.format("%.4f", endPoint!!.lat) + ", " + String.format("%.4f", endPoint!!.lng) }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200
                                )
                            }
                        }
                    }
                    
                    // Route name
                    OutlinedTextField(
                        value = routeName,
                        onValueChange = { routeName = it },
                        label = { Text("Nombre de la ruta *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = Brand400,
                            unfocusedLabelColor = Slate400,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200,
                            cursorColor = Brand500,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Route description
                    OutlinedTextField(
                        value = routeDescription,
                        onValueChange = { routeDescription = it },
                        label = { Text("Descripción (opcional)") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = Brand400,
                            unfocusedLabelColor = Slate400,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200,
                            cursorColor = Brand500,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotBlank() && startPoint != null && endPoint != null) {
                            onSaveRoute(
                                RouteData(
                                    name = routeName,
                                    description = routeDescription,
                                    startPoint = startPoint,
                                    endPoint = endPoint
                                )
                            )
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled = routeName.isNotBlank()
                ) { 
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar", color = Slate400)
                }
            }
        )
    }
}

/**
 * Selection mode enum
 */
private enum class PointSelectionMode {
    START,
    END
}

/**
 * Add marker icons to map style
 */
private fun addMarkerIconsToStyle(mapStyle: Style, context: android.content.Context) {
    // Create start marker (green/blue)
    val startDrawable = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
    if (startDrawable != null) {
        val bitmap = android.graphics.Bitmap.createBitmap(
            48, 48,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#3B82F6") // Brand500
            setStyle(android.graphics.Paint.Style.FILL)
        }
        canvas.drawCircle(24f, 24f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(24f, 24f, 8f, paint)
        mapStyle.addImage("start-marker-icon", bitmap)
    }
    
    // Create end marker (green)
    val endDrawable = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
    if (endDrawable != null) {
        val bitmap = android.graphics.Bitmap.createBitmap(
            48, 48,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#10B981") // Emerald500
            setStyle(android.graphics.Paint.Style.FILL)
        }
        canvas.drawCircle(24f, 24f, 20f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(24f, 24f, 8f, paint)
        mapStyle.addImage("end-marker-icon", bitmap)
    }
}

/**
 * Add marker layer to map
 */
private fun addMarkerLayer(style: Style, sourceId: String, layerId: String, iconImage: String) {
    if (style.getSource(sourceId) == null) {
        style.addSource(GeoJsonSource(sourceId))
    }
    if (style.getLayer(layerId) == null) {
        val symbolLayer = SymbolLayer(layerId, sourceId).withProperties(
            PropertyFactory.iconImage(iconImage),
            PropertyFactory.iconSize(1.0f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconAnchor("center")
        )
        style.addLayer(symbolLayer)
    }
}

/**
 * Update marker position
 */
private fun updateMarker(style: Style, sourceId: String, latLng: LatLng) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    val feature = Feature.fromGeometry(
        Point.fromLngLat(latLng.longitude, latLng.latitude)
    )
    source.setGeoJson(FeatureCollection.fromFeatures(listOf(feature)))
}

/**
 * Clear marker from map
 */
private fun clearMarker(style: Style, sourceId: String) {
    val source = style.getSource(sourceId) as? GeoJsonSource ?: return
    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
}

/**
 * Center map on user location
 */
private fun centerOnUser(map: MapLibreMap, context: android.content.Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        map.locationComponent.lastKnownLocation?.let { location ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 
                    15.0
                )
            )
        }
    }
}

/**
 * Enable location component on map
 */
private fun enableLocationComponent(style: Style, map: MapLibreMap, context: android.content.Context) {
    try {
        val locationComponent = map.locationComponent
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.renderMode = RenderMode.COMPASS
            locationComponent.cameraMode = CameraMode.NONE
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
