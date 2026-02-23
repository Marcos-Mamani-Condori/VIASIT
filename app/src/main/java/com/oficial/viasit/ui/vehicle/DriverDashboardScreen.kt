package com.oficial.viasit.ui.vehicle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.ui.map.AutosViewModel
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AuthViewModel

// ─── Driver Dashboard ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    authViewModel: AuthViewModel,
    vehicles: List<Auto>,
    selectedVehicleId: String?,
    isInService: Boolean,
    onToggleInService: (Boolean) -> Unit,
    onSelectVehicle: (Auto) -> Unit,
    onNavigateToRegisterVehicle: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToManageVehicle: () -> Unit,
    onLogout: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? com.oficial.viasit.domain.model.AuthState.Authenticated)?.user
    var showVehicleSelector by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(Brand500, Brand400))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsBus, null,
                                tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("VIASIT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp)
                            Text("Panel de Conductor",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = Slate400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
                )
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Saludo ────────────────────────────────────────────────────────
            GreetingCard(
                name = currentUser?.name ?: "Conductor",
                vehicleCount = vehicles.size
            )

            // ── Selector de vehículo ──────────────────────────────────────────
            if (vehicles.isNotEmpty()) {
                VehicleSelectorCard(
                    vehicles = vehicles,
                    selectedVehicle = selectedVehicle,
                    lineaId = currentUser?.lineaId,
                    onShowSelector = { showVehicleSelector = true }
                )
            }

            // ── Estado / Toggle en servicio ───────────────────────────────────
            if (selectedVehicle != null) {
                ServiceToggleCard(
                    isInService = isInService,
                    onToggle = onToggleInService,
                    vehiclePlaca = selectedVehicle.placa
                )
            } else if (vehicles.isEmpty()) {
                StatusInfoCard(
                    icon = Icons.Default.DirectionsCar,
                    title = "Sin vehículo registrado",
                    message = "Registra tu vehículo para comenzar a operar",
                    accentColor = Amber400
                )
            }

            // ── Acciones ──────────────────────────────────────────────────────
            Text(
                "Acciones",
                style = MaterialTheme.typography.labelMedium,
                color = Slate600,
                modifier = Modifier.padding(top = 4.dp)
            )

            DashboardMenuItem(
                icon = Icons.Default.DirectionsCar,
                title = if (vehicles.isEmpty()) "Registrar vehículo" else "Gestionar vehículos",
                subtitle = if (vehicles.isEmpty()) "Ingresa los datos de tu unidad" else "${vehicles.size} vehículo(s) registrado(s)",
                iconColor = Brand500,
                onClick = onNavigateToRegisterVehicle
            )

            DashboardMenuItem(
                icon = Icons.Default.Map,
                title = "Ver mapa en vivo",
                subtitle = "Monitorea las unidades en tiempo real",
                iconColor = Emerald400,
                onClick = onNavigateToMap
            )

            // ── Info tip ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceTinted)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = Brand400,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Text(
                    text = if (selectedVehicle != null)
                        "Activa «En servicio» para transmitir tu ubicación en tiempo real"
                    else
                        "Selecciona un vehículo y activa «En servicio» para comenzar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Diálogo selector de vehículo
        if (showVehicleSelector) {
            VehicleSelectorDialog(
                vehicles = vehicles,
                selectedVehicle = selectedVehicle,
                lineaId = currentUser?.lineaId,
                onSelectVehicle = { vehicle ->
                    onSelectVehicle(vehicle)
                    showVehicleSelector = false
                },
                onDismiss = { showVehicleSelector = false }
            )
        }
    }
}
// Sub-componentes privados eliminados — ahora viven en DriverComponents.kt
// Ver: GreetingCard, VehicleSelectorCard, ServiceToggleCard,
//      StatusInfoCard, DashboardMenuItem, VehicleSelectorDialog
