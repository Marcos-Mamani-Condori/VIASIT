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
import com.oficial.viasit.ui.admin.AdminDashboardScreen
import com.oficial.viasit.ui.admin.RegisterAdminScreen
import com.oficial.viasit.ui.routes.RouteMapPickerScreen
import com.oficial.viasit.ui.routes.RouteData
import com.oficial.viasit.ui.routes.GeocodingService
import com.oficial.viasit.ui.routes.SearchResult
import kotlinx.coroutines.Job
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
import com.google.gson.JsonObject

// Estados de navegación
sealed class Screen {
    data object Auth : Screen()
    data object Map : Screen()
    data object DriverDashboard : Screen()
    data object DriverVehicleRegistration : Screen()
    data object AdminDashboard : Screen()
    data object RegisterAdmin : Screen()
    data class RouteMapPicker(val lineaId: String, val lineaName: String) : Screen()
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
        val currentState = authViewModel.authState.value
        android.util.Log.d("VIASIT", "VIASITApp inicializado - estado: ${currentState.javaClass.simpleName}")
        if (currentState is AuthState.Authenticated) {
            val userRole = currentState.user.userRole
            android.util.Log.d("VIASIT", "Usuario autenticado - role: $userRole")
            currentScreen = when (userRole) {
                UserRole.conductor -> Screen.DriverDashboard
                UserRole.ADMIN_PRINCIPAL, UserRole.ADMIN_LINEA -> Screen.AdminDashboard
                else -> Screen.Map
            }
        } else {
            android.util.Log.d("VIASIT", "No autenticado - mostrando AuthScreen")
            currentScreen = Screen.Auth
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                val userRole = state.user.userRole
                android.util.Log.d("VIASIT", "authState cambiado a Authenticated - role: $userRole")
                currentScreen = when (userRole) {
                    UserRole.conductor -> Screen.DriverDashboard
                    UserRole.ADMIN_PRINCIPAL, UserRole.ADMIN_LINEA -> Screen.AdminDashboard
                    else -> Screen.Map
                }
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
                },
                onRegisterAdmin = {
                    currentScreen = Screen.RegisterAdmin
                }
            )
        }
        Screen.Map -> {
            MainMapScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    currentScreen = Screen.Auth
                },
                onBackToDashboard = if (authState is AuthState.Authenticated &&
                    (authState as AuthState.Authenticated).user.userRole == UserRole.conductor) {
                    { currentScreen = Screen.DriverDashboard }
                } else null
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
        Screen.AdminDashboard -> {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            if (currentUser != null && currentUser.isAdmin) {
                AdminDashboardScreen(
                    currentUser = currentUser,
                    onLogout = {
                        authViewModel.logout()
                        currentScreen = Screen.Auth
                    },
                    onNavigateToRoutePicker = { lineaId, lineaName ->
                        currentScreen = Screen.RouteMapPicker(lineaId, lineaName)
                    }
                )
            } else {
                // No tiene acceso, volver al mapa
                currentScreen = Screen.Map
            }
        }
        Screen.RegisterAdmin -> {
            RegisterAdminScreen(
                onRegisterSuccess = {
                    // Después de registrar, ir al dashboard de admin
                    currentScreen = Screen.AdminDashboard
                },
                onNavigateBack = {
                    currentScreen = Screen.Auth
                },
                authState = authState
            )
        }
        is Screen.RouteMapPicker -> {
            val adminViewModel: com.oficial.viasit.viewmodels.AdminViewModel = viewModel()
            val screen = currentScreen as Screen.RouteMapPicker
            val currentUser = (authState as? AuthState.Authenticated)?.user
            RouteMapPickerScreen(
                lineaId = screen.lineaId,
                lineaName = screen.lineaName,
                onSaveRoute = { routeData ->
                    // Create route with coordinates
                    adminViewModel.createRutaWithCoords(
                        name = routeData.name,
                        description = routeData.description,
                        startLat = routeData.startPoint?.lat ?: 0.0,
                        startLng = routeData.startPoint?.lng ?: 0.0,
                        endLat = routeData.endPoint?.lat ?: 0.0,
                        endLng = routeData.endPoint?.lng ?: 0.0,
                        lineaId = screen.lineaId,
                        currentUserId = currentUser?.id ?: ""
                    )
                    // Go back to admin dashboard
                    currentScreen = Screen.AdminDashboard
                },
                onBack = {
                    currentScreen = Screen.AdminDashboard
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
    
    // Esperar a que el estado de autenticación esté disponible
    var isAuthRestored by remember { mutableStateOf(false) }
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            isAuthRestored = true
        }
    }
    
    val currentUser = (authState as? AuthState.Authenticated)?.user
    var vehicles by remember { mutableStateOf<List<Auto>>(emptyList()) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // El estado de servicio viene de SharedPreferences (local), no del modelo User
    val isInService: Boolean = authViewModel.isInService()

    // Cargar vehículos del conductor
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentUser.userRole == UserRole.conductor) {
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

    // Mostrar loading hasta que la autenticación esté restaurada
    if (isLoading || !isAuthRestored || currentUser == null) {
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
                // isInService es solo lectura, viene del usuario
                if (enabled && selectedVehicleId != null) {
                    // Iniciar servicio de tracking con el vehículo seleccionado
                    authViewModel.setInService(true, selectedVehicleId!!)
                } else {
                    // Detener servicio de tracking
                    authViewModel.setInService(false)
                }
            },
            onSelectVehicle = { vehicle ->
                val previousVehicleId = selectedVehicleId
                selectedVehicleId = vehicle.id
                // Si está en servicio, reiniciar con el nuevo vehículo
                if (isInService) {
                    authViewModel.setInService(false)
                    authViewModel.setInService(true, vehicle.id)
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
    onLogout: () -> Unit,
    onBackToDashboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    val autos by autosViewModel.autosUiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedCar by remember { mutableStateOf<Auto?>(null) }
    
    // Search state
    val geocodingService = remember { GeocodingService() }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearchResults by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val currentUser = (authState as? AuthState.Authenticated)?.user
    val isDriver = currentUser?.userRole == UserRole.conductor
    val isInService = authViewModel.isInService()
    
    // Tu vehículo propio (si eres conductor y estás en servicio)
    val myVehicle: Auto? = if (isDriver && isInService) {
        autos.find { auto -> 
            auto.userId.contains(currentUser?.id ?: "")
        }
    } else null

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

        // Top section with search
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
                        IconButton(onClick = onBackToDashboard!!) {
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
            
            // Search bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
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
                        placeholder = { Text("Buscar lugar...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
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
                            Icon(Icons.Default.Clear, "Limpiar", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
            
            // Search results
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
                                                LatLng(result.lat, result.lon),
                                                16.0
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    result.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        LargeFloatingActionButton(
            onClick = { mapInstance?.let { centerOnUser(it, context) } },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 200.dp, end = 20.dp),
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
                // Banner indicando que es tu vehículo
                if (myVehicle != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TU AUTO",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = myVehicle.placa,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                // Lista de vehículos (excluyendo el propio si es conductor, solo activos)
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
                    // Mensaje cuando no hay vehículos activos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isDriver && isInService) "Solo estás tú en servicio" else "No hay vehículos activos",
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
                    CarListItem(auto = auto, onClick = { onCarClick(auto) })
                }
            }
        }
    }
}

@Composable
private fun CarListItem(auto: Auto, onClick: () -> Unit) {
    val isActive = auto.isActive()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador de estado (punto verde/rojo)
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935),
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = auto.placa,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = auto.getLastUpdateText(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
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
                text = "Placa: ${car.placa}",
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
