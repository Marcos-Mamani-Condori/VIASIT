package com.oficial.viasit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.oficial.viasit.model.Auto
import com.oficial.viasit.ui.theme.AutosViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            MapScreen()
        }
    }
}

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos by viewModel.autosUiState.collectAsState()

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedCar by remember { mutableStateOf<Auto?>(null) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                setupMap(map, context) { pointF ->
                    val features = map.queryRenderedFeatures(pointF, "cars-layer")
                    if (features.isNotEmpty()) {
                        val clickedPlaca = features[0].getStringProperty("placa")
                        selectedCar = autos.find { it.placa == clickedPlaca }
                    } else {
                        selectedCar = null
                    }
                }
            }
        }
    }

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

    LaunchedEffect(autos, mapInstance) {
        val map = mapInstance
        if (map != null && autos.isNotEmpty()) {
            map.getStyle { style ->
                updateCarsSource(style, autos)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        MapOverlayUI(
            autos = autos,
            selectedCar = selectedCar,
            onFindClick = {
                val map = mapInstance
                if (map != null) {
                    centerOnNearestCar(map, autos, context)
                }
            },
            onCarSelected = { car ->
                selectedCar = car
                mapInstance?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(car.lat, car.lng), 17.0),
                    1000
                )
            },
            onCloseCarInfo = { selectedCar = null }
        )
    }
}

private fun setupMap(map: MapLibreMap, context: Context, onMapClick: (PointF) -> Unit) {
    // ESTILO CON MAS COLOR Y DETALLE DE TERRENO (Relieve y naturaleza)
    val styleUrl = "https://tiles.openfreemap.org/styles/bright"

    map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
        addCarIconToStyle(style, context)
        addCarsLayer(style)
        enableLocationComponent(style, map, context)
    }

    map.addOnMapClickListener { latLng ->
        val screenPoint = map.projection.toScreenLocation(latLng)
        onMapClick(screenPoint)
        true
    }
}

private fun addCarIconToStyle(style: Style, context: Context) {
    try {
        val carIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car_icon)
        if (carIcon != null) {
            style.addImage("car-icon", carIcon)
        }
    } catch (e: Exception) {
        Log.e("MapSetup", "Error cargando ic_car_icon")
    }
}

private fun addCarsLayer(style: Style) {
    if (style.getSource("cars-source") == null) {
        style.addSource(GeoJsonSource("cars-source"))
    }
    if (style.getLayer("cars-layer") == null) {
        val symbolLayer = SymbolLayer("cars-layer", "cars-source").withProperties(
            PropertyFactory.iconImage("car-icon"),
            PropertyFactory.iconSize(0.6f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
        style.addLayer(symbolLayer)
    }
}

private fun updateCarsSource(style: Style, autos: List<Auto>) {
    val source = style.getSource("cars-source") as? GeoJsonSource
    val features = autos.map { auto ->
        Feature.fromGeometry(Point.fromLngLat(auto.lng, auto.lat)).apply {
            addStringProperty("placa", auto.placa)
        }
    }
    source?.setGeoJson(FeatureCollection.fromFeatures(features))
}

@Composable
fun BoxScope.MapOverlayUI(
    autos: List<Auto>,
    selectedCar: Auto?,
    onFindClick: () -> Unit,
    onCarSelected: (Auto) -> Unit,
    onCloseCarInfo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 40.dp),
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Text(
            text = "VIASIT",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
    }

    LargeFloatingActionButton(
        onClick = onFindClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 30.dp, end = 20.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = "Mi ubicación")
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
            .fillMaxWidth(0.75f)
    ) {
        if (selectedCar != null) {
            CarInfoCard(car = selectedCar, onClose = onCloseCarInfo)
        } else if (autos.isNotEmpty()) {
            CarListSimple(autos = autos, onCarClick = onCarSelected)
        }
    }
}

@Composable
fun CarListSimple(autos: List<Auto>, onCarClick: (Auto) -> Unit) {
    Surface(
        modifier = Modifier.heightIn(max = 220.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Unidades en línea: ${autos.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(autos) { auto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarClick(auto) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50)
                        ) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(auto.placa, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(auto.linea, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun CarInfoCard(car: Auto, onClose: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Información", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Placa: ${car.placa}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Línea: ${car.linea}", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Volver al mapa")
            }
        }
    }
}

private fun enableLocationComponent(style: Style, map: MapLibreMap, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            val locationComponent = map.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
        } catch (e: Exception) { e.printStackTrace() }
    }
}

private fun centerOnNearestCar(map: MapLibreMap, autos: List<Auto>, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { userLocation ->
        if (userLocation != null && autos.isNotEmpty()) {
            var targetLatLng = LatLng(autos[0].lat, autos[0].lng)
            var minDistance = Float.MAX_VALUE
            for (auto in autos) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(userLocation.latitude, userLocation.longitude, auto.lat, auto.lng, results)
                if (results[0] < minDistance) {
                    minDistance = results[0]
                    targetLatLng = LatLng(auto.lat, auto.lng)
                }
            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15.0), 1500)
        } else {
            Toast.makeText(context, "Buscando tu ubicación...", Toast.LENGTH_SHORT).show()
        }
    }
}
