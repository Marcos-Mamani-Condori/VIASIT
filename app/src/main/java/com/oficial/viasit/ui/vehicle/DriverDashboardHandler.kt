package com.oficial.viasit.ui.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.viewmodels.AuthViewModel

@Composable
fun DriverDashboardHandler(
    authViewModel: AuthViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToRegisterVehicle: () -> Unit,
    onLogout: () -> Unit,
    onVehicleNotFound: () -> Unit
) {
    val authState      by authViewModel.authState.collectAsState()
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)

    var isAuthRestored    by remember { mutableStateOf(false) }
    val currentUser = (authState as? AuthState.Authenticated)?.user

    // Esperar a que authState esté disponible
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) isAuthRestored = true
    }

    var vehicles       by remember { mutableStateOf<List<Auto>>(emptyList()) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var isLoading      by remember { mutableStateOf(true) }
    
    // Flag para evitar redirección infinita después de registrar vehículo
    // Usar rememberSaveable para que sobreviva a recomposiciones
    var hasCheckedVehicle by rememberSaveable { mutableStateOf(false) }
    var hasNavigatedToRegister by rememberSaveable { mutableStateOf(false) }

    // StateFlow reactivo — no lee SharedPreferences en cada recomposición
    val isInService by authViewModel.isInService.collectAsState()

    // Cargar vehículo del conductor desde PocketBase
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentUser.userRole == UserRole.conductor) {
            android.util.Log.d("DriverDashboardHandler", "Buscando vehículo para usuario: ${currentUser.id}")
            autosViewModel.getDriverAuto(currentUser.id) { auto ->
                android.util.Log.d("DriverDashboardHandler", "Vehículo encontrado: ${auto?.id}, userId=${auto?.userId}")
                if (auto != null && auto.userId.isNotEmpty()) {
                    if (vehicles.none { it.id == auto.id }) vehicles = vehicles + auto
                    if (selectedVehicleId == null) selectedVehicleId = auto.id
                }
                // Marcamos carga completa; la verificación de "sin vehículo" se hace
                // en LaunchedEffect(isLoading, vehicles) para evitar race condition
                isLoading = false
                hasCheckedVehicle = true
            }
            autosViewModel.startRealtimeSubscription()
        } else {
            isLoading = false
            hasCheckedVehicle = true
        }
    }

    // Observar cambios via SSE para detectar vehículos del conductor
    val autos by autosViewModel.autosUiState.collectAsState()
    LaunchedEffect(autos) {
        if (currentUser != null) {
            val driverAutos = autos.filter { it.userId.isNotEmpty() && it.userId == currentUser.id }
            android.util.Log.d("DriverDashboardHandler", "Autos SSE: ${driverAutos.size} para usuario ${currentUser.id}")
            if (driverAutos.isNotEmpty()) {
                vehicles = driverAutos
                if (selectedVehicleId == null) selectedVehicleId = driverAutos[0].id
            }
        }
    }

    // FIX: verificar vehículo vacío solo cuando carga terminó (no en el callback async)
    // Solo redirigir una vez para evitar bucles infinitos
    LaunchedEffect(isLoading, vehicles.size, hasCheckedVehicle, hasNavigatedToRegister) {
        android.util.Log.d("DriverDashboardHandler", "isLoading=$isLoading, vehicles=${vehicles.size}, hasCheckedVehicle=$hasCheckedVehicle, hasNavigatedToRegister=$hasNavigatedToRegister")
        if (!isLoading && vehicles.isEmpty() && currentUser?.userRole == UserRole.conductor && hasCheckedVehicle && !hasNavigatedToRegister) {
            android.util.Log.d("DriverDashboardHandler", "No se encontró vehículo, redirigiendo a registro")
            hasNavigatedToRegister = true
            onVehicleNotFound()
        }
    }

    if (isLoading || !isAuthRestored || currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        DriverDashboardScreen(
            authViewModel     = authViewModel,
            vehicles          = vehicles,
            selectedVehicleId = selectedVehicleId,
            isInService       = isInService,
            onToggleInService = { enabled ->
                if (enabled && selectedVehicleId != null) {
                    authViewModel.setInService(true, selectedVehicleId!!)
                } else {
                    authViewModel.setInService(false)
                }
            },
            onSelectVehicle = { vehicle ->
                selectedVehicleId = vehicle.id
                if (isInService) {
                    authViewModel.setInService(false)
                    authViewModel.setInService(true, vehicle.id)
                }
            },
            onNavigateToRegisterVehicle = onNavigateToRegisterVehicle,
            onNavigateToMap             = onNavigateToMap,
            onNavigateToManageVehicle   = onNavigateToRegisterVehicle,
            onLogout                    = onLogout
        )
    }
}

@Composable
fun DriverVehicleRegistrationScreen(
    authViewModel: AuthViewModel,
    onVehicleRegistered: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)
    RegisterVehicleScreen(
        authViewModel    = authViewModel,
        autosViewModel   = autosViewModel,
        onVehicleRegistered = onVehicleRegistered,
        onGoToMap        = onBackToDashboard,
        onBack           = onBackToDashboard
    )
}
