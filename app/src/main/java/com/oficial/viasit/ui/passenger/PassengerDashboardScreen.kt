package com.oficial.viasit.ui.passenger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.ui.map.AutosViewModel
import com.oficial.viasit.ui.theme.Brand500
import com.oficial.viasit.ui.theme.Slate500
import com.oficial.viasit.ui.theme.Slate950
import com.oficial.viasit.viewmodels.AuthViewModel
import com.oficial.viasit.viewmodels.PassengerViewModel

private enum class PassengerTab(val label: String, val icon: ImageVector) {
    MAPA("Mapa",  Icons.Default.Map),
    AUTOS("Buses", Icons.Default.DirectionsBus),
    RUTAS("Rutas", Icons.Default.DirectionsBus),
    PERFIL("Perfil", Icons.Default.Person)
}

/**
 * Dashboard principal para usuarios de rol 'usuario' e 'invitado'.
 * El tab state está levantado aquí para que RoutesTab pueda cambiar
 * al tab Mapa cuando el pasajero toca "Ver en mapa".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDashboardScreen(
    authViewModel: AuthViewModel,
    currentUser: User,
    onLogout: () -> Unit
) {
    val passengerViewModel: PassengerViewModel = viewModel(factory = PassengerViewModel.Factory)
    val autosViewModel: AutosViewModel         = viewModel(factory = AutosViewModel.Factory)
    var selectedTab by remember { mutableStateOf(PassengerTab.MAPA) }

    Scaffold(
        // ✅ Respeta barra de estado del sistema en todos los dispositivos
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
                .padding(paddingValues)  // ✅ usa todo el padding (top + bottom)
        ) {
            when (selectedTab) {
                PassengerTab.MAPA   -> PassengerMapScreen(
                    passengerViewModel = passengerViewModel
                )
                PassengerTab.AUTOS  -> ActiveAutosTab(
                    autosViewModel     = autosViewModel,
                    passengerViewModel = passengerViewModel,
                    onNavigateToMap    = { selectedTab = PassengerTab.MAPA }
                )
                PassengerTab.RUTAS  -> RoutesTab(
                    viewModel   = passengerViewModel,
                    onViewOnMap = { selectedTab = PassengerTab.MAPA }
                )
                PassengerTab.PERFIL -> ProfileTab(
                    user     = currentUser,
                    onLogout = onLogout
                )
            }
        }
    }
}
