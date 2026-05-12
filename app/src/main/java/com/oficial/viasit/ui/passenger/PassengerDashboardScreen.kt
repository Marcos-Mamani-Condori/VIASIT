package com.oficial.viasit.ui.passenger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.ui.theme.Brand500
import com.oficial.viasit.ui.theme.Slate500
import com.oficial.viasit.ui.theme.Slate950
import com.oficial.viasit.viewmodels.AuthViewModel
import com.oficial.viasit.viewmodels.PassengerViewModel

import com.oficial.viasit.ui.admin.AuditoriaTabContent
import com.oficial.viasit.viewmodels.LogsViewModel

private enum class PassengerTab(val label: String, val icon: ImageVector) {
    MAPA("Mapa",  Icons.Default.Map),
    UNIDADES("Unidades", Icons.Default.DirectionsBus),
    LOGS("Muro de Verdad", Icons.Default.History),
    PERFIL("Perfil", Icons.Default.Person)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDashboardScreen(
    authViewModel: AuthViewModel,
    currentUser: User,
    onLogout: () -> Unit
) {
    val passengerViewModel: PassengerViewModel = viewModel(factory = PassengerViewModel.Factory)
    val autosViewModel: AutosViewModel         = viewModel(factory = AutosViewModel.Factory)
    val logsViewModel: LogsViewModel           = viewModel(factory = LogsViewModel.Factory)
    var selectedTab by remember { mutableStateOf(PassengerTab.MAPA) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == PassengerTab.LOGS) {
            logsViewModel.loadLogs()
        }
    }

    Scaffold(

        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Slate950,
        bottomBar = {
            NavigationBar(
                containerColor = Slate950,
                contentColor   = Brand500
            ) {
                PassengerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        icon     = { Icon(tab.icon, contentDescription = tab.label) },
                        label    = { Text(tab.label) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Brand500,
                            selectedTextColor   = Brand500,
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor      = Slate950
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                PassengerTab.MAPA   -> PassengerMapScreen(
                    passengerViewModel = passengerViewModel
                )
                PassengerTab.UNIDADES  -> ActiveAutosTab(
                    autosViewModel     = autosViewModel,
                    passengerViewModel = passengerViewModel,
                    currentUser        = currentUser,
                    onNavigateToMap    = { selectedTab = PassengerTab.MAPA }
                )
                PassengerTab.LOGS -> {
                    val logs by logsViewModel.logs.collectAsState()
                    val isLoading by logsViewModel.isLoading.collectAsState()
                    val searchQuery by logsViewModel.searchQuery.collectAsState()
                    val timeFilter by logsViewModel.timeFilter.collectAsState()
                    
                    AuditoriaTabContent(
                        logs = logs,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        onSearchChange = { logsViewModel.setSearchQuery(it) },
                        selectedFilter = timeFilter,
                        onFilterChange = { logsViewModel.setTimeFilter(it) }
                    )
                }
                PassengerTab.PERFIL -> ProfileTab(
                    user     = currentUser,
                    onLogout = onLogout
                )
            }
        }
    }
}
