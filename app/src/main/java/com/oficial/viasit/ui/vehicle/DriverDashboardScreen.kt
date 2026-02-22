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

// ─── Greeting Card ────────────────────────────────────────────────────────────
@Composable
private fun GreetingCard(name: String, vehicleCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF283593))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brand500.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Brand300,
                    modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Hola, $name 👋",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (vehicleCount) {
                        0 -> "Sin vehículos registrados"
                        1 -> "1 vehículo registrado"
                        else -> "$vehicleCount vehículos registrados"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand300
                )
            }
        }
    }
}

// ─── Vehicle Selector Card ────────────────────────────────────────────────────
@Composable
private fun VehicleSelectorCard(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    lineaId: String?,
    onShowSelector: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onShowSelector)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Amber500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DirectionsCar, null, tint = Amber400,
                modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Vehículo activo",
                style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(
                selectedVehicle?.placa ?: "Seleccionar vehículo",
                style = MaterialTheme.typography.titleMedium,
                color = if (selectedVehicle != null) Color.White else Slate400,
                fontWeight = FontWeight.SemiBold
            )
            // La línea se obtiene del perfil del conductor, no del vehículo
            if (!lineaId.isNullOrBlank()) {
                Text("Línea: $lineaId",
                    style = MaterialTheme.typography.labelSmall, color = Slate600)
            }
        }
        Icon(Icons.Default.UnfoldMore, null, tint = Slate600,
            modifier = Modifier.size(20.dp))
    }
}

// ─── Service Toggle Card ──────────────────────────────────────────────────────
@Composable
private fun ServiceToggleCard(
    isInService: Boolean,
    onToggle: (Boolean) -> Unit,
    vehiclePlaca: String = ""
) {
    val bgColor = if (isInService)
        Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF065F46)))
    else
        Brush.linearGradient(listOf(SurfaceElevated, SurfaceElevated))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isInService) Emerald400.copy(0.2f) else Slate700.copy(0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isInService) Icons.Default.RadioButtonChecked
                        else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isInService) Emerald400 else Slate500,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        if (isInService) "En servicio" else "Fuera de servicio",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isInService) Emerald400 else Slate200,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isInService) "Transmitiendo ubicación — $vehiclePlaca"
                        else "Activa para comenzar a operar",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isInService) Emerald400.copy(0.7f) else Slate600
                    )
                }
            }
            Switch(
                checked = isInService,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Emerald500,
                    checkedBorderColor = Emerald400,
                    uncheckedThumbColor = Slate400,
                    uncheckedTrackColor = Slate800,
                    uncheckedBorderColor = Slate700
                )
            )
        }
    }
}

// ─── Status Info Card ─────────────────────────────────────────────────────────
@Composable
private fun StatusInfoCard(
    icon: ImageVector,
    title: String,
    message: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
    }
}

// ─── Dashboard Menu Item ──────────────────────────────────────────────────────
@Composable
private fun DashboardMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Slate700,
            modifier = Modifier.size(20.dp))
    }
}

// ─── Vehicle Selector Dialog ──────────────────────────────────────────────────
@Composable
private fun VehicleSelectorDialog(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    lineaId: String?,
    onSelectVehicle: (Auto) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Seleccionar vehículo",
                style = MaterialTheme.typography.headlineSmall, color = Color.White)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vehicles) { vehicle ->
                    val isSelected = vehicle.id == selectedVehicle?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Brand500.copy(0.15f) else SurfaceTinted)
                            .clickable { onSelectVehicle(vehicle) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null,
                            tint = if (isSelected) Brand400 else Slate400,
                            modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vehicle.placa,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) Brand300 else Color.White,
                                fontWeight = FontWeight.SemiBold)
                            // La línea se muestra desde el perfil del conductor
                            if (!lineaId.isNullOrBlank()) {
                                Text("Línea: $lineaId",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = Brand400,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Brand400)
            }
        }
    )
}

// Nota: RegisterVehicleScreen y DriverWelcomeScreen están definidos en RegisterVehicleScreen.kt
// para evitar duplicados

// Colores auxiliares
private val Slate500 = Color(0xFF64748B)
