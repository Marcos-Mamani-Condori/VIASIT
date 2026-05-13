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
    onNavigateToMyRoute: (String) -> Unit,
    onNavigateToRegisterVehicle: () -> Unit,
    onLogout: () -> Unit,
    onVehicleNotFound: () -> Unit
) {
    val authState      by authViewModel.authState.collectAsState()
    val autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory)

    var isAuthRestored    by remember { mutableStateOf(false) }
    val currentUser = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) isAuthRestored = true
    }

    var vehicles       by remember { mutableStateOf<List<Auto>>(emptyList()) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var isLoading      by remember { mutableStateOf(true) }
    
    var hasCheckedVehicle by rememberSaveable { mutableStateOf(false) }
    var hasNavigatedToRegister by rememberSaveable { mutableStateOf(false) }

    val isInService by authViewModel.isInService.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null && currentUser.userRole == UserRole.conductor) {
            android.util.Log.d("DriverDashboardHandler", "Buscando vehículo para usuario: ${currentUser.id}")
            autosViewModel.getDriverAuto(currentUser.id) { auto ->
                android.util.Log.d("DriverDashboardHandler", "Vehículo encontrado: ${auto?.id}, userId='${auto?.userId}'")
                if (auto != null) {
                    // Si el userId viene vacío por algún error de serialización, lo rellenamos ya que sabemos que le pertenece a este conductor
                    val fixedAuto = if (auto.userId.isBlank()) auto.copy(userId = currentUser.id) else auto
                    if (vehicles.none { it.id == fixedAuto.id }) {
                        vehicles = vehicles + fixedAuto
                    }
                    if (selectedVehicleId == null) selectedVehicleId = fixedAuto.id
                }
                isLoading = false
                hasCheckedVehicle = true
            }
            autosViewModel.startRealtimeSubscription()
        } else {
            isLoading = false
            hasCheckedVehicle = true
        }
    }

    val autos by autosViewModel.autosUiState.collectAsState()
    LaunchedEffect(autos) {
        if (currentUser != null) {
            val driverAutos = autos.filter { 
                (it.userId.isNotEmpty() && it.userId == currentUser.id) ||
                (it.id == selectedVehicleId)
            }
            android.util.Log.d("DriverDashboardHandler", "Autos SSE filtrados: ${driverAutos.size} para usuario ${currentUser.id}")
            if (driverAutos.isNotEmpty()) {
                vehicles = driverAutos
                if (selectedVehicleId == null) selectedVehicleId = driverAutos[0].id
            }
        }
    }

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
                if (enabled) {
                    if (selectedVehicleId != null) {
                        authViewModel.setInService(true, selectedVehicleId!!)
                    } else {
                        // Aquí podrías mostrar un Toast o un mensaje de error
                        android.util.Log.w("DriverDashboardHandler", "Intento de activar servicio sin vehículo")
                    }
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
            onNavigateToMyRoute         = { 
                currentUser.lineaId.let { id ->
                    if (id.isNotBlank()) onNavigateToMyRoute(id)
                }
            },
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
