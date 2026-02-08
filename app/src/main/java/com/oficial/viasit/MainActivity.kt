@file:OptIn(ExperimentalMaterial3Api::class)

package com.oficial.viasit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.*
import com.oficial.viasit.service.LocationTrackingService
import com.oficial.viasit.ui.auth.AuthScreen
import com.oficial.viasit.ui.map.AutosViewModel
import com.oficial.viasit.ui.theme.VIASITTheme
import com.oficial.viasit.ui.vehicle.DriverDashboardScreen
import com.oficial.viasit.ui.vehicle.RegisterVehicleScreen
import com.oficial.viasit.ui.vehicle.DriverWelcomeScreen
import com.oficial.viasit.viewmodels.AuthViewModel
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
import com.google.gson.JsonObject

// Estados de navegación
sealed class Screen {
    data object Auth : Screen()
    data object Map : Screen()
    data object DriverDashboard : Screen()
    data object DriverVehicleRegistration : Screen()
}
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "La app necesita ubicación para funcionar", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }

        setContent {
            VIASITTheme {
                VIASITApp()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VIASITApp() {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // Estados de navegación
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
    
    // Verificación inicial del estado de autenticación
    LaunchedEffect(Unit) {
        // Verificar inmediatamente si hay una sesión activa
        val currentState = authViewModel.authState.value
        android.util.Log.d("VIASIT", "VIASITApp inicializado - estado: ${currentState.javaClass.simpleName}")
        if (currentState is AuthState.Authenticated) {
            val role = currentState.user.role
            android.util.Log.d("VIASIT", "Usuario autenticado - role: $role")
            val isDriver = role.lowercase() == "conductor"
            android.util.Log.d("VIASIT", "Es conductor: $isDriver")
            currentScreen = if (isDriver) Screen.DriverDashboard else Screen.Map
        } else {
            android.util.Log.d("VIASIT", "No autenticado - mostrando AuthScreen")
            currentScreen = Screen.Auth
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                val role = state.user.role
                android.util.Log.d("VIASIT", "authState cambiado a Authenticated - role: $role")
                val isDriver = role.lowercase() == "conductor"
                android.util.Log.d("VIASIT", "Es conductor: $isDriver")
                currentScreen = if (isDriver) Screen.DriverDashboard else Screen.Map
            }
            is AuthState.Unauthenticated -> {
                currentScreen = Screen.Auth
            }
            else -> {}
        }
    }

    when (currentScreen) {
        Screen.Auth -> {
            AuthScreen(
                onAuthSuccess = {
                    // La navegación se maneja automáticamente por LaunchedEffect(authState)
                    // que verifica el rol del usuario
                }
            )
        }
        Screen.Map -> {
            MainMapScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    currentScreen = Screen.Auth
                }
            )
        }
        Screen.DriverDashboard -> {
            DriverDashboardHandler(
                authViewModel = authViewModel,
                onNavigateToMap = {
                    currentScreen = Screen.Map
                },
                onNavigateToRegisterVehicle = {
                    currentScreen = Screen.DriverVehicleRegistration
                },
                onLogout = {
                    authViewModel.logout()
                    currentScreen = Screen.Auth
                },
                onVehicleNotFound = {
                    // Si no tiene vehículo, ir directamente a registrar
                    currentScreen = Screen.DriverVehicleRegistration
                }
            )
        }
        Screen.DriverVehicleRegistration -> {
            DriverVehicleRegistrationScreen(
                authViewModel = authViewModel,
                onVehicleRegistered = {
                    // Después de registrar, ir al dashboard
                    currentScreen = Screen.DriverDashboard
                },
                onBackToDashboard = {
                    currentScreen = Screen.DriverDashboard
                }
            )
        }
    }
}

@Composable
fun DriverDashboardHandler(
    authViewModel: AuthViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToRegisterVehicle: () -> Unit,
    onLogout: () -> Unit,
    onVehicleNotFound: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    
    val currentUser = (authState as? AuthState.Authenticated)?.user
    var vehicles by remember { mutableStateOf<List<Auto>>(emptyList()) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isInService by remember { mutableStateOf(false) }

    // Cargar vehículos del conductor
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentUser.role.lowercase() == "conductor") {
            autosViewModel.getDriverAuto(currentUser.id) { auto ->
                if (auto != null) {
                    // Agregar a la lista si no existe
                    if (vehicles.none { it.id == auto.id }) {
                        vehicles = vehicles + auto
                    }
                    // Si no hay vehículo seleccionado, seleccionar este
                    if (selectedVehicleId == null) {
                        selectedVehicleId = auto.id
                    }
                }
                isLoading = false
                // Si no tiene ningún vehículo, ir directamente a registrar
                if (vehicles.isEmpty()) {
                    onVehicleNotFound()
                }
            }
            // También suscribirse a todos los cambios de autos para detectar vehículos del conductor
            autosViewModel.startRealtimeSubscription()
        } else {
            isLoading = false
        }
    }

    // Observar cambios en la lista de autos para detectar vehículos del conductor
    val autos by autosViewModel.autosUiState.collectAsState()
    LaunchedEffect(autos) {
        if (currentUser != null) {
            // Filtrar autos que pertenecen a este conductor
            val driverAutos = autos.filter { auto -> 
                auto.userId.contains(currentUser.id)
            }
            if (driverAutos.isNotEmpty()) {
                vehicles = driverAutos
                // Si no hay vehículo seleccionado, seleccionar el primero
                if (selectedVehicleId == null && driverAutos.isNotEmpty()) {
                    selectedVehicleId = driverAutos[0].id
                }
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        DriverDashboardScreen(
            authViewModel = authViewModel,
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            isInService = isInService,
            onToggleInService = { enabled ->
                isInService = enabled
                if (enabled && selectedVehicleId != null) {
                    // Iniciar servicio de tracking con el vehículo seleccionado
                    authViewModel.setInService(true)
                } else {
                    // Detener servicio de tracking
                    authViewModel.setInService(false)
                }
            },
            onSelectVehicle = { vehicle ->
                selectedVehicleId = vehicle.id
                // Si está en servicio, reiniciar con el nuevo vehículo
                if (isInService) {
                    authViewModel.setInService(false)
                    authViewModel.setInService(true)
                }
            },
            onNavigateToRegisterVehicle = onNavigateToRegisterVehicle,
            onNavigateToMap = onNavigateToMap,
            onNavigateToManageVehicle = onNavigateToRegisterVehicle,
            onLogout = onLogout
        )
    }
}

/**
 * Pantalla de registro de vehículo para conductores
 */
@Composable
fun DriverVehicleRegistrationScreen(
    authViewModel: AuthViewModel,
    onVehicleRegistered: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    
    RegisterVehicleScreen(
        authViewModel = authViewModel,
        autosViewModel = autosViewModel,
        onVehicleRegistered = onVehicleRegistered,
        onGoToMap = onVehicleRegistered,
        onBack = onBackToDashboard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos by autosViewModel.autosUiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedCar by remember { mutableStateOf<Auto?>(null) }

    val currentUser = (authState as? AuthState.Authenticated)?.user

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
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        LargeFloatingActionButton(
            onClick = { mapInstance?.let { centerOnUser(it, context) } },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 150.dp, end = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Mi posición")
        }

        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(0.8f)) {
            if (selectedCar != null) {
                CarInfoCard(car = selectedCar!!, onClose = { selectedCar = null })
            } else if (autos.isNotEmpty()) {
                CarListSimple(autos = autos, onCarClick = { car ->
                    selectedCar = car
                    mapInstance?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(car.lat, car.lng), 17.0),
                        1000
                    )
                })
            }
        }
    }
}

@Composable
private fun DriverControlsCard(
    isInService: Boolean,
    onToggleService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Modo Conductor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isInService) "En servicio - Enviando ubicación" else "Fuera de servicio",
                    fontSize = 12.sp,
                    color = if (isInService) Color.Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = isInService,
                onCheckedChange = onToggleService,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun CarListSimple(autos: List<Auto>, onCarClick: (Auto) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${autos.size} vehículos activos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(autos.take(5)) { auto ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarClick(auto) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = auto.placa,
                            style = MaterialTheme.typography.bodyMedium
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = car.placa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Línea: ${car.linea}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "ID: ${car.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun addCarIconToStyle(style: Style, context: android.content.Context) {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_car_icon)
    if (drawable != null) {
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        style.addImage("car-icon", bitmap)
    }
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
                org.maplibre.android.style.expressions.Expression.get("angulo")
            )
        )
        style.addLayer(symbolLayer)
    }
}

private fun updateCarsSource(style: Style, autos: List<Auto>) {
    val source = style.getSource("cars-source") as? GeoJsonSource ?: return
    val features = autos.map { auto ->
        val properties = JsonObject().apply {
            addProperty("placa", auto.placa)
            addProperty("angulo", auto.angulo)
        }
        Feature.fromGeometry(
            Point.fromLngLat(auto.lng, auto.lat),
            properties
        )
    }
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun centerOnUser(map: MapLibreMap, context: android.content.Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        map.locationComponent.lastKnownLocation?.let { location ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16.0))
        }
    }
}

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
            locationComponent.cameraMode = CameraMode.TRACKING
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
