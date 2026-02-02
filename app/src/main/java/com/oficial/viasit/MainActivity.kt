package com.oficial.viasit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.google.android.gms.location.Priority
import com.oficial.viasit.model.Auto
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.ui.theme.VIASITTheme
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
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            recreate()
        } else {
            Toast.makeText(this, "La app necesita ubicación para funcionar correctamente", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            VIASITTheme {
                MapScreen()
            }
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
                map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")) { style ->
                    addCarIconToStyle(style, context)
                    addCarsLayer(style)
                    updateCarsSource(style, autos)
                    enableLocationComponent(style, map, context)
                }
                
                map.addOnMapClickListener { latLng ->
                    val screenPoint = map.projection.toScreenLocation(latLng)
                    val features = map.queryRenderedFeatures(screenPoint, "cars-layer")
                    if (features.isNotEmpty()) {
                        val clickedPlaca = features[0].getStringProperty("placa")
                        selectedCar = autos.find { it.placa == clickedPlaca }
                    } else {
                        selectedCar = null
                    }
                    true
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
        mapInstance?.getStyle { style -> updateCarsSource(style, autos) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())

        MapOverlayUI(
            autos = autos,
            selectedCar = selectedCar,
            onFindClick = { mapInstance?.let { centerOnUser(it, context) } },
            onCarSelected = { car ->
                selectedCar = car
                mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(car.lat, car.lng), 17.0), 1000)
            },
            onCloseCarInfo = { selectedCar = null }
        )
    }
}

private fun vectorToBitmap(context: Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun addCarIconToStyle(style: Style, context: Context) {
    try {
        val carIcon = vectorToBitmap(context, R.drawable.ic_car_icon)
        if (carIcon != null) style.addImage("car-icon", carIcon)
    } catch (e: Exception) { Log.e("MapSetup", "Error icono: ${e.message}") }
}

private fun updateCarIcon(style: Style, context: Context) {
    try {
        val carIcon = vectorToBitmap(context, R.drawable.ic_car_icon)
        if (carIcon != null) style.addImage("car-icon", carIcon, true)
    } catch (e: Exception) { Log.e("MapSetup", "Error actualizar icono: ${e.message}") }
}

private fun addCarsLayer(style: Style) {
    if (style.getSource("cars-source") == null) style.addSource(GeoJsonSource("cars-source"))
    if (style.getLayer("cars-layer") == null) {
        val symbolLayer = SymbolLayer("cars-layer", "cars-source").withProperties(
            PropertyFactory.iconImage("car-icon"),
            PropertyFactory.iconSize(0.5f),
            PropertyFactory.iconAllowOverlap(false),
            PropertyFactory.iconIgnorePlacement(false),
            PropertyFactory.iconAnchor("center"),
            PropertyFactory.iconRotate(
                interpolate(
                    linear(),
                    get("angulo"),
                    stop(0f, 0f),
                    stop(360f, 360f)
                )
            )
        )
        style.addLayer(symbolLayer)
    }
}

private fun updateCarsSource(style: Style, autos: List<Auto>) {
    val source = style.getSource("cars-source") as? GeoJsonSource ?: return
    val features = autos.map { auto ->
        Feature.fromGeometry(Point.fromLngLat(auto.lng, auto.lat)).apply {
            addStringProperty("placa", auto.placa)
            addNumberProperty("angulo", auto.angulo.toDouble())
        }
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
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
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "VIASIT",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    LargeFloatingActionButton(
        onClick = onFindClick,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 20.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = "Mi posición")
    }

    Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(0.8f)) {
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
        modifier = Modifier.heightIn(max = 200.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Unidades: ${autos.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(autos) { auto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarClick(auto) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            auto.placa,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (auto != autos.last()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CarInfoCard(car: Auto, onClose: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Detalle del Auto",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Placa: ${car.placa}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Línea: ${car.linea}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar", fontSize = 16.sp)
            }
        }
    }
}

private fun enableLocationComponent(style: Style, map: MapLibreMap, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            val locationComponent = map.locationComponent
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        } catch (e: Exception) { Log.e("Map", "Error localizacion") }
    }
}

private fun centerOnUser(map: MapLibreMap, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                val userPos = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 16.0), 1000)
            } else {
                map.locationComponent.lastKnownLocation?.let { loc ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16.0), 1000)
                }
            }
        }
}
