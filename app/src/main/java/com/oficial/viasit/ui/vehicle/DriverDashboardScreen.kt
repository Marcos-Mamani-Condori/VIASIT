package com.oficial.viasit.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.viewmodels.AuthViewModel
 
/**
 * Panel intermedio para conductores después del login
 * Permite al conductor elegir entre: registrar vehículo, ver mapa, gestionar vehículo, o cerrar sesión
 * Incluye toggle "En servicio" para activar tracking de ubicación
 * Soporta múltiples vehículos con selector de vehículo activo
 */
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
                    Column {
                        Text(
                            "VIASIT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Panel de Conductor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Saludo al conductor
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¡Bienvenido, ${currentUser?.name ?: "Conductor"}!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = when {
                                vehicles.isEmpty() -> "Sin vehículos registrados"
                                vehicles.size == 1 -> "Vehículo: ${vehicles[0].placa}"
                                else -> "${vehicles.size} vehículos registrados"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Selector de vehículo activo (si tiene más de uno)
                if (vehicles.isNotEmpty()) {
                    VehicleSelectorCard(
                        vehicles = vehicles,
                        selectedVehicle = selectedVehicle,
                        onShowSelector = { showVehicleSelector = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Estado del vehículo
                DashboardCard(
                    title = when {
                        vehicles.isEmpty() -> "Sin Vehículo"
                        selectedVehicle != null -> "Vehículo Activo"
                        else -> "Selecciona un Vehículo"
                    },
                    subtitle = when {
                        vehicles.isEmpty() -> "Registra tu vehículo para comenzar a operar"
                        selectedVehicle != null -> "Línea: ${selectedVehicle.linea} - ${selectedVehicle.placa}"
                        else -> "Tienes ${vehicles.size} vehículos. Selecciona uno para comenzar"
                    },
                    icon = when {
                        vehicles.isEmpty() -> Icons.Default.Warning
                        selectedVehicle != null -> Icons.Default.CheckCircle
                        else -> Icons.Default.DirectionsCar
                    },
                    iconTint = when {
                        vehicles.isEmpty() -> MaterialTheme.colorScheme.error
                        selectedVehicle != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    backgroundColor = when {
                        vehicles.isEmpty() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        selectedVehicle != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Toggle "En servicio" - Solo visible si hay un vehículo seleccionado
                if (selectedVehicle != null) {
                    ServiceToggleCard(
                        isInService = isInService,
                        onToggle = onToggleInService,
                        vehiclePlaca = selectedVehicle.placa
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Opciones del panel
                Text(
                    text = "Opciones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Opción: Registrar/Ver vehículo
                DashboardMenuItem(
                    icon = Icons.Default.DirectionsCar,
                    title = if (vehicles.isEmpty()) "Registrar Vehículo" else "Gestionar Vehículos",
                    subtitle = if (vehicles.isEmpty())
                        "Ingresa los datos de tu vehículo"
                    else
                        "Ver, editar o agregar más vehículos",
                    onClick = onNavigateToRegisterVehicle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Opción: Ir al mapa
                DashboardMenuItem(
                    icon = Icons.Default.Map,
                    title = "Ver Mapa",
                    subtitle = "Ver vehículos en tiempo real",
                    onClick = onNavigateToMap
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de cerrar sesión
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Información adicional
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (selectedVehicle != null)
                                "Activa 'En servicio' para enviar tu ubicación en tiempo real"
                            else
                                "Selecciona un vehículo y activa 'En servicio' para comenzar a operar",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Diálogo de selección de vehículo
        if (showVehicleSelector) {
            VehicleSelectorDialog(
                vehicles = vehicles,
                selectedVehicle = selectedVehicle,
                onSelectVehicle = { vehicle ->
                    onSelectVehicle(vehicle)
                    showVehicleSelector = false
                },
                onDismiss = { showVehicleSelector = false }
            )
        }
    }
}

/**
 * Tarjeta para seleccionar el vehículo activo
 */
@Composable
private fun VehicleSelectorCard(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    onShowSelector: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShowSelector),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vehículo en Servicio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedVehicle?.placa ?: "No seleccionado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedVehicle?.linea ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Seleccionar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Diálogo para seleccionar el vehículo activo
 */
@Composable
private fun VehicleSelectorDialog(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    onSelectVehicle: (Auto) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar Vehículo")
        },
        text = {
            LazyColumn {
                items(vehicles) { vehicle ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectVehicle(vehicle) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (vehicle.id == selectedVehicle?.id)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (vehicle.id == selectedVehicle?.id)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = vehicle.placa,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = vehicle.linea,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

/**
 * Tarjeta con el toggle de "En servicio"
 */
@Composable
private fun ServiceToggleCard(
    isInService: Boolean,
    onToggle: (Boolean) -> Unit,
    vehiclePlaca: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInService)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modo Conductor",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isInService) "En servicio ($vehiclePlaca)" else "Fuera de servicio",
                    fontSize = 12.sp,
                    color = if (isInService)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = isInService,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50), // Verde
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DashboardMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
