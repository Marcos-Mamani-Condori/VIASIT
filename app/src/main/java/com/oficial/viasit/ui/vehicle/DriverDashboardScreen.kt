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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AuthViewModel

import com.oficial.viasit.ui.admin.AuditoriaTabContent
import com.oficial.viasit.viewmodels.LogsViewModel

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
    onLogout: () -> Unit,
    autosViewModel: AutosViewModel = viewModel(factory = AutosViewModel.Factory),
    logsViewModel: LogsViewModel = viewModel(factory = LogsViewModel.Factory)
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? com.oficial.viasit.domain.model.AuthState.Authenticated)?.user
    var showVehicleSelector by remember { mutableStateOf(false) }
    var showReportesDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var reportesList by remember { mutableStateOf<List<com.oficial.viasit.domain.model.Reporte>>(emptyList()) }
    var reportesLoading by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    LaunchedEffect(showLogsDialog) {
        if (showLogsDialog) {
            logsViewModel.loadLogs()
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", tint = Slate400)
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

            GreetingCard(
                name = currentUser?.name ?: "Conductor",
                vehicleCount = vehicles.size
            )

            if (vehicles.isNotEmpty()) {
                VehicleSelectorCard(
                    vehicles = vehicles,
                    selectedVehicle = selectedVehicle,
                    lineaId = currentUser?.lineaId,
                    onShowSelector = { showVehicleSelector = true }
                )
            }

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

            DashboardMenuItem(
                icon = Icons.Default.History,
                title = "Muro de la Verdad",
                subtitle = "Historial de acciones del sistema",
                iconColor = Rose500,
                onClick = { showLogsDialog = true }
            )

            if (selectedVehicle != null) {
                DashboardMenuItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Mis reportes",
                    subtitle = "Opiniones de los pasajeros",
                    iconColor = Cyan400,
                    onClick = {
                        reportesList = emptyList()
                        reportesLoading = true
                        showReportesDialog = true
                        autosViewModel.fetchReportes(selectedVehicle.placa.ifBlank { selectedVehicle.id }) { lista ->
                            reportesList = lista
                            reportesLoading = false
                        }
                    }
                )
            }

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

        if (showLogsDialog) {
            val logs by logsViewModel.logs.collectAsState()
            val isLoadingLogs by logsViewModel.isLoading.collectAsState()
            AlertDialog(
                onDismissRequest = { showLogsDialog = false },
                containerColor = Slate950,
                modifier = Modifier.fillMaxWidth(0.95f),
                title = { Text("Muro de la Verdad", color = Color.White) },
                text = {
                    Box(modifier = Modifier.height(500.dp)) {
                        AuditoriaTabContent(logs = logs, isLoading = isLoadingLogs)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLogsDialog = false }) { Text("Cerrar", color = Brand500) }
                }
            )
        }

        if (showReportesDialog) {
            AlertDialog(
                onDismissRequest = { showReportesDialog = false },
                containerColor = Slate900,
                title = { Text("Mis reportes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            reportesLoading -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Brand500, strokeWidth = 2.dp)
                            }
                            reportesList.isEmpty() -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                                Text("No tienes reportes. ¡Buen trabajo!", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                            else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(reportesList) { rep ->
                                    val texto = rep.descripcion.replace(Regex("""^\[Bus: [^\]]+\]\s*"""), "")
                                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.ChatBubbleOutline, null, tint = Slate500, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                            Text(texto, style = MaterialTheme.typography.bodySmall, color = Slate300)
                                        }
                                        if (rep.respuesta.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 16.dp)) {
                                                Icon(Icons.Default.SubdirectoryArrowRight, null, tint = Brand500, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                                Text(rep.respuesta, style = MaterialTheme.typography.bodySmall, color = Brand400)
                                            }
                                        } else {
                                            var replyText by remember { mutableStateOf("") }
                                            var isReplying by remember { mutableStateOf(false) }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = replyText,
                                                    onValueChange = { replyText = it },
                                                    modifier = Modifier.weight(1f).height(46.dp),
                                                    placeholder = { Text("Responder...", color = Slate600, fontSize = 12.sp) },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Brand500, unfocusedBorderColor = Slate700,
                                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                                    ),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        if (replyText.isNotBlank()) {
                                                            isReplying = true
                                                            autosViewModel.responderReporte(rep.id, replyText) { ok ->
                                                                isReplying = false
                                                                if (ok) {
                                                                    autosViewModel.fetchReportes(selectedVehicle?.placa ?: "") { lista -> reportesList = lista }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = replyText.isNotBlank() && !isReplying,
                                                    modifier = Modifier.size(36.dp).background(Brand500, CircleShape)
                                                ) {
                                                    if (isReplying) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                                    else Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReportesDialog = false }) { Text("Cerrar", color = Brand400) }
                }
            )
        }
    }
}
