package com.oficial.viasit.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.ui.admin.linea.ConductoresTabContent
import com.oficial.viasit.ui.admin.linea.MiLineaTabContent
import com.oficial.viasit.ui.admin.principal.*
import com.oficial.viasit.ui.passenger.PassengerMapScreen
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AdminViewModel
import com.oficial.viasit.viewmodels.PassengerViewModel
import com.oficial.viasit.viewmodels.LogsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User,
    onLogout: () -> Unit,
    onNavigateToRoutePicker: ((String, String) -> Unit)? = null,
    viewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory),
    passengerViewModel: PassengerViewModel = viewModel(factory = PassengerViewModel.Factory),
    logsViewModel: LogsViewModel = viewModel(factory = LogsViewModel.Factory)
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs: List<Pair<String, ImageVector>> = if (currentUser.isAdminPrincipal) {
        listOf(
            "Mapa" to Icons.Default.Map,
            "Usuarios" to Icons.Default.Group,
            "Líneas" to Icons.Default.DirectionsBus,
            "Rutas" to Icons.Default.Route,
            "Reportes" to Icons.Default.Warning,
            "Apelaciones" to Icons.Default.Gavel,
            "Invitaciones" to Icons.Default.Key,
            "Logs" to Icons.Default.History
        )
    } else {
        listOf(
            "Mapa" to Icons.Default.Map,
            "Mi Línea" to Icons.Default.Route,
            "Conductores" to Icons.Default.People,
            "Invitaciones" to Icons.Default.Key,
            "Reportes" to Icons.Default.Warning,
            "Logs" to Icons.Default.History
        )
    }

    LaunchedEffect(Unit) {
        if (currentUser.isAdminPrincipal) {
            viewModel.loadLineas()
            viewModel.loadRutas()
            viewModel.loadUsers("", true)
            viewModel.loadInvitationCodes()
            viewModel.loadAllReportes()
            viewModel.loadAppeals()
        } else {
            viewModel.loadLineas()
            viewModel.loadRutas()
            viewModel.loadUsers(currentUser.lineaId, false)
            viewModel.loadVehicleInvitationCodes(currentUser.lineaId)
            viewModel.loadAllReportes()
            logsViewModel.setLineId(currentUser.lineaId)
        }
        logsViewModel.loadLogs()
    }

    // Refresco automático al cambiar de pestaña
    LaunchedEffect(selectedTab) {
        if (currentUser.isAdminPrincipal) {
            when (selectedTab) {
                1 -> viewModel.loadUsers("", true)
                2 -> viewModel.loadLineas()
                3 -> viewModel.loadRutas()
                4 -> viewModel.loadAllReportes()
                5 -> viewModel.loadAppeals()
                6 -> viewModel.loadInvitationCodes()
                7 -> logsViewModel.loadLogs()
            }
        } else {
            when (selectedTab) {
                1 -> { viewModel.loadLineas(); viewModel.loadRutas() }
                2 -> viewModel.loadUsers(currentUser.lineaId, false)
                3 -> viewModel.loadVehicleInvitationCodes(currentUser.lineaId)
                4 -> viewModel.loadAllReportes()
                5 -> logsViewModel.loadLogs()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val autos by viewModel.autosUiState.collectAsState(initial = emptyList())

    Scaffold(
        topBar         = { AdminTopBar(currentUser = currentUser, onLogout = onLogout) },
        containerColor = Slate950
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            AdminUserBanner(currentUser = currentUser)

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Slate900, 
                contentColor     = Brand400,
                edgePadding      = 16.dp,
                divider          = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Brand500, height = 2.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text  = { Text(title, style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTab == index) Brand400 else Slate600) },
                        icon  = { Icon(icon, null,
                            tint = if (selectedTab == index) Brand400 else Slate600,
                            modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (currentUser.isAdminPrincipal) {
                    when (selectedTab) {
                        0 -> PassengerMapScreen(
                            passengerViewModel = passengerViewModel,
                            showTailsButton = true,
                            currentUser = currentUser
                        )
                        1 -> ConductoresTabContent(
                            users = uiState.users,
                            autos = autos,
                            isLoading = uiState.isLoading,
                            onToggleActive = { targetUserId, targetUserName, active ->
                                viewModel.setUserActiveStatus(targetUserId, targetUserName, active, currentUser.id, currentUser.name)
                            },
                            onShowLocation = { lat, lng ->
                                passengerViewModel.onShowLocation(lat, lng)
                                selectedTab = 0
                            },
                            showLocationOnlyForDrivers = true
                        )
                        2 -> LineasTabContent(
                            lineas        = uiState.lineas, isLoading = uiState.isLoading,
                            onCreateLinea = { n, c, r -> viewModel.createLinea(n, c, r, currentUser.id, currentUser.name) },
                            onDeleteLinea = { id, name -> viewModel.deleteLinea(id, name, currentUser.id, currentUser.name) }
                        )
                        3 -> RutasTabContent(
                            rutas     = uiState.rutas,
                            lineas    = uiState.lineas,
                            isLoading = uiState.isLoading,
                            onDeleteRuta = { rutaId, rutaName, lineaId ->
                                viewModel.deleteRuta(rutaId, rutaName, lineaId, currentUser.id, currentUser.name)
                            }
                        )
                        4 -> ReportesTabContent(
                            reportes = uiState.reportes, 
                            isLoading = uiState.isLoading,
                            onResponder = { id, resp -> viewModel.responderReporte(id, resp, currentUser.id, currentUser.name) },
                            onSuspendUser = { targetUserId, targetUserName ->
                                viewModel.setUserActiveStatus(targetUserId, targetUserName, false, currentUser.id, currentUser.name)
                            }
                        )
                        5 -> ApelacionesTabContent(
                            appeals = uiState.appeals,
                            isLoading = uiState.isLoading,
                            onResolve = { appealId, targetUserId, targetUserName, accept ->
                                viewModel.resolveAppeal(appealId, targetUserId, targetUserName, accept, currentUser.id, currentUser.name)
                            }
                        )
                        6 -> InvitacionesTabContent(
                            invitationCodes = uiState.invitationCodes, lineas = uiState.lineas,
                            isLoading       = uiState.isLoading, generatedCode = uiState.generatedCode,
                            onGenerateCode  = { role, lineaId ->
                                viewModel.generateInvitationCode(role, lineaId, 24, currentUser.id, currentUser.name)
                            },
                            onDeleteCode = { viewModel.deleteInvitationCode(it, currentUser.id, currentUser.name) }
                        )
                        7 -> {
                            val logs by logsViewModel.logs.collectAsState()
                            val isLoadingLogs by logsViewModel.isLoading.collectAsState()
                            val searchQuery by logsViewModel.searchQuery.collectAsState()
                            val timeFilter by logsViewModel.timeFilter.collectAsState()
                            
                            AuditoriaTabContent(
                                logs = logs,
                                isLoading = isLoadingLogs,
                                searchQuery = searchQuery,
                                onSearchChange = { logsViewModel.setSearchQuery(it) },
                                selectedFilter = timeFilter,
                                onFilterChange = { logsViewModel.setTimeFilter(it) }
                            )
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            LaunchedEffect(Unit) {
                                passengerViewModel.selectLinea(currentUser.lineaId)
                            }
                            PassengerMapScreen(
                                passengerViewModel = passengerViewModel,
                                showTailsButton = true,
                                currentUser = currentUser
                            )
                        }
                        1 -> MiLineaTabContent(
                            lineaId   = currentUser.lineaId,
                            lineas    = uiState.lineas,
                            rutas     = uiState.rutas,
                            isLoading = uiState.isLoading,
                            userEmail = currentUser.email,
                            onCreateRuta = { name, desc, start, end, lineaId ->
                                viewModel.createRuta(name, desc, start, end, lineaId, currentUser.id, currentUser.name)
                            },
                            onAssignRuta = { rutaId, rutaName ->
                                viewModel.assignRutaToLinea(currentUser.lineaId, rutaId, rutaName, currentUser.id, currentUser.name)
                            },
                            onNavigateToRoutePicker = onNavigateToRoutePicker
                        )
                        2 -> ConductoresTabContent(
                            users = uiState.users,
                            autos = autos,
                            isLoading = uiState.isLoading,
                            onToggleActive = { targetUserId, targetUserName, active ->
                                viewModel.setUserActiveStatus(targetUserId, targetUserName, active, currentUser.id, currentUser.name)
                            },
                            onShowLocation = { lat, lng ->
                                passengerViewModel.onShowLocation(lat, lng)
                                selectedTab = 0
                            }
                        )
                        3 -> {
                            val vCodes = uiState.invitationCodes.filter { it.role == "conductor" }
                            com.oficial.viasit.ui.admin.linea.VehicleCodesTabContent(
                                lineaId = currentUser.lineaId,
                                vehicleCodes = vCodes,
                                isLoading = uiState.isLoading,
                                generatedCode = uiState.generatedCode,
                                onGenerateCode = {
                                    viewModel.generateVehicleInvitationCode(currentUser.lineaId, currentUser.name, 72, currentUser.id)
                                },
                                onDeleteCode = { viewModel.deleteVehicleInvitationCode(it, currentUser.lineaId, currentUser.id, currentUser.name) }
                            )
                        }
                        4 -> {
                            val miLinea = uiState.lineas.find { it.id == currentUser.lineaId }
                            val lineName = miLinea?.name ?: ""
                            
                            // Filtrar reportes por lineaId si el conductor pertenece a la línea del admin
                            val lineReportes = uiState.reportes.filter { reporte ->
                                val autoOfDriver = autos.find { it.userId == reporte.userid }
                                autoOfDriver?.lineaId == currentUser.lineaId
                            }

                            ReportesTabContent(
                                reportes = lineReportes,
                                isLoading = uiState.isLoading,
                                onResponder = { id, resp -> viewModel.responderReporte(id, resp, currentUser.id, currentUser.name) },
                                onSuspendUser = { targetUserId, targetUserName ->
                                    viewModel.setUserActiveStatus(targetUserId, targetUserName, false, currentUser.id, currentUser.name)
                                }
                            )
                        }
                        5 -> {
                            val logs by logsViewModel.logs.collectAsState()
                            val isLoadingLogs by logsViewModel.isLoading.collectAsState()
                            val searchQuery by logsViewModel.searchQuery.collectAsState()
                            val timeFilter by logsViewModel.timeFilter.collectAsState()

                            AuditoriaTabContent(
                                logs = logs,
                                isLoading = isLoadingLogs,
                                searchQuery = searchQuery,
                                onSearchChange = { logsViewModel.setSearchQuery(it) },
                                selectedFilter = timeFilter,
                                onFilterChange = { logsViewModel.setTimeFilter(it) }
                            )
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF2D0A12), contentColor = Color(0xFFFFB3B3),
                    action = { TextButton(onClick = { viewModel.clearMessages() }) { Text("OK", color = Rose500) } }
                ) { Text(error, style = MaterialTheme.typography.bodySmall) }
            }
            uiState.successMessage?.let { msg ->
                Snackbar(modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF064E3B), contentColor = Emerald400,
                    action = { TextButton(onClick = { viewModel.clearMessages() }) { Text("OK", color = Emerald400) } }
                ) { Text(msg, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(currentUser: User, onLogout: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Brand500, Brand400))),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("VIASIT Admin", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black, color = Color.White)
                    Text(if (currentUser.isAdminPrincipal) "Admin Principal" else "Admin de Línea",
                        style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", tint = Slate400)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
    )
}

@Composable
private fun AdminUserBanner(currentUser: User) {
    Box(modifier = Modifier.fillMaxWidth().background(Slate900)
        .padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brand500.copy(0.18f)),
                contentAlignment = Alignment.Center) {
                Text(currentUser.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                    style = MaterialTheme.typography.titleMedium, color = Brand300, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(currentUser.name, style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(currentUser.email, style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            if (currentUser.isAdminPrincipal) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Brand500.copy(0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Principal", style = MaterialTheme.typography.labelSmall,
                        color = Brand300, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
