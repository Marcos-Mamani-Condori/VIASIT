package com.oficial.viasit.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.oficial.viasit.ui.admin.linea.MiLineaTabContent
import com.oficial.viasit.ui.admin.principal.InvitacionesTabContent
import com.oficial.viasit.ui.admin.principal.LineasTabContent
import com.oficial.viasit.ui.admin.principal.RutasTabContent
import com.oficial.viasit.ui.passenger.PassengerMapScreen
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AdminViewModel
import com.oficial.viasit.viewmodels.PassengerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User,
    onLogout: () -> Unit,
    onNavigateToRoutePicker: ((String, String) -> Unit)? = null,
    viewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory),
    passengerViewModel: PassengerViewModel = viewModel(factory = PassengerViewModel.Factory)
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs: List<Pair<String, ImageVector>> = if (currentUser.isAdminPrincipal) {
        listOf(
            "Mapa" to Icons.Default.Map,
            "Líneas" to Icons.Default.DirectionsBus,
            "Rutas" to Icons.Default.Route,
            "Invitaciones" to Icons.Default.Key,
            "Logs" to Icons.Default.History
        )
    } else {
        listOf(
            "Mapa" to Icons.Default.Map,
            "Mi Línea" to Icons.Default.Route,
            "Logs" to Icons.Default.History
        )
    }

    LaunchedEffect(Unit) {
        if (currentUser.isAdminPrincipal) {
            viewModel.loadLineas()
            viewModel.loadRutas()
            viewModel.loadInvitationCodes()
        } else {
            viewModel.loadLineas()
            viewModel.loadRutas()
        }
        viewModel.loadLogs()
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar         = { AdminTopBar(currentUser = currentUser, onLogout = onLogout) },
        containerColor = Slate950
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            AdminUserBanner(currentUser = currentUser)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Slate900, contentColor = Brand400,
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

            if (currentUser.isAdminPrincipal) {
                when (selectedTab) {
                    0 -> PassengerMapScreen(passengerViewModel = passengerViewModel)
                    1 -> LineasTabContent(
                        lineas        = uiState.lineas, isLoading = uiState.isLoading,
                        onCreateLinea = { n, c, r -> viewModel.createLinea(n, c, r, currentUser.id) },
                        onDeleteLinea = { viewModel.deleteLinea(it, currentUser.id) }
                    )
                    2 -> RutasTabContent(
                        rutas     = uiState.rutas,
                        lineas    = uiState.lineas,
                        isLoading = uiState.isLoading,
                        onDeleteRuta = { rutaId, rutaName, lineaId ->
                            viewModel.deleteRuta(rutaId, rutaName, lineaId, currentUser.id)
                        }
                    )
                    3 -> InvitacionesTabContent(
                        invitationCodes = uiState.invitationCodes, lineas = uiState.lineas,
                        isLoading       = uiState.isLoading, generatedCode = uiState.generatedCode,
                        onGenerateCode  = { role, lineaId ->
                            viewModel.generateInvitationCode(role, lineaId, 24, currentUser.id)
                        },
                        onDeleteCode = { viewModel.deleteInvitationCode(it, currentUser.id) }
                    )
                    4 -> AuditoriaTabContent(logs = uiState.logs, isLoading = uiState.isLoading)
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        LaunchedEffect(Unit) {
                            passengerViewModel.selectLinea(currentUser.lineaId)
                        }
                        PassengerMapScreen(passengerViewModel = passengerViewModel)
                    }
                    1 -> MiLineaTabContent(
                        lineaId   = currentUser.lineaId,
                        lineas    = uiState.lineas,
                        rutas     = uiState.rutas,
                        isLoading = uiState.isLoading,
                        userEmail = currentUser.email,
                        onCreateRuta = { name, desc, start, end, lineaId ->
                            viewModel.createRuta(name, desc, start, end, lineaId, currentUser.id)
                        },
                        onAssignRuta = { rutaId, rutaName ->
                            viewModel.assignRutaToLinea(currentUser.lineaId, rutaId, rutaName, currentUser.id)
                        },
                        onNavigateToRoutePicker = onNavigateToRoutePicker
                    )
                    2 -> AuditoriaTabContent(logs = uiState.logs, isLoading = uiState.isLoading)
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
                Icon(Icons.Default.Logout, "Cerrar sesión", tint = Slate400)
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
