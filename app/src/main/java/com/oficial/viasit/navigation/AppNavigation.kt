@file:OptIn(ExperimentalMaterial3Api::class)

package com.oficial.viasit.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.ui.admin.AdminDashboardScreen
import com.oficial.viasit.ui.admin.RegisterAdminScreen
import com.oficial.viasit.ui.auth.AuthScreen
import com.oficial.viasit.ui.map.MainMapScreen
import com.oficial.viasit.ui.routes.RouteMapPickerScreen
import com.oficial.viasit.ui.vehicle.DriverDashboardHandler
import com.oficial.viasit.ui.vehicle.DriverVehicleRegistrationScreen
import com.oficial.viasit.viewmodels.AdminViewModel
import com.oficial.viasit.viewmodels.AuthViewModel

// ── Destinos de navegación ─────────────────────────────────────────────────
sealed class Screen {
    data object Auth                    : Screen()
    data object Map                     : Screen()
    data object DriverDashboard         : Screen()
    data object DriverVehicleRegistration: Screen()
    data object AdminDashboard          : Screen()
    data object RegisterAdmin           : Screen()
    data class  RouteMapPicker(val lineaId: String, val lineaName: String) : Screen()
}

// ── Composable raíz ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VIASITApp() {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authState by authViewModel.authState.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }

    // Un solo LaunchedEffect: authState ya cambia al restaurar sesión
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                currentScreen = when (state.user.userRole) {
                    UserRole.conductor                       -> Screen.DriverDashboard
                    UserRole.ADMIN_PRINCIPAL, UserRole.ADMIN_LINEA -> Screen.AdminDashboard
                    else                                    -> Screen.Map
                }
            }
            is AuthState.Unauthenticated -> currentScreen = Screen.Auth
            else -> {}
        }
    }

    when (currentScreen) {
        Screen.Auth -> {
            AuthScreen(
                onAuthSuccess = { /* navegación manejada por LaunchedEffect(authState) */ },
                onRegisterAdmin = { currentScreen = Screen.RegisterAdmin }
            )
        }

        Screen.Map -> {
            MainMapScreen(
                authViewModel = authViewModel,
                onLogout = { authViewModel.logout(); currentScreen = Screen.Auth },
                onBackToDashboard = if (authState is AuthState.Authenticated &&
                    (authState as AuthState.Authenticated).user.userRole == UserRole.conductor) {
                    { currentScreen = Screen.DriverDashboard }
                } else null
            )
        }

        Screen.DriverDashboard -> {
            DriverDashboardHandler(
                authViewModel = authViewModel,
                onNavigateToMap               = { currentScreen = Screen.Map },
                onNavigateToRegisterVehicle   = { currentScreen = Screen.DriverVehicleRegistration },
                onLogout                      = { authViewModel.logout(); currentScreen = Screen.Auth },
                onVehicleNotFound             = { currentScreen = Screen.DriverVehicleRegistration }
            )
        }

        Screen.DriverVehicleRegistration -> {
            DriverVehicleRegistrationScreen(
                authViewModel      = authViewModel,
                onVehicleRegistered = { currentScreen = Screen.DriverDashboard },
                onBackToDashboard   = { currentScreen = Screen.DriverDashboard }
            )
        }

        Screen.AdminDashboard -> {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            if (currentUser != null && currentUser.isAdmin) {
                AdminDashboardScreen(
                    currentUser = currentUser,
                    onLogout    = { authViewModel.logout(); currentScreen = Screen.Auth },
                    onNavigateToRoutePicker = { lineaId, lineaName ->
                        currentScreen = Screen.RouteMapPicker(lineaId, lineaName)
                    }
                )
            } else {
                currentScreen = Screen.Map
            }
        }

        Screen.RegisterAdmin -> {
            RegisterAdminScreen(
                onRegisterSuccess = { currentScreen = Screen.AdminDashboard },
                onNavigateBack    = { currentScreen = Screen.Auth },
                authState         = authState
            )
        }

        is Screen.RouteMapPicker -> {
            val adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory)
            val screen      = currentScreen as Screen.RouteMapPicker
            val currentUser = (authState as? AuthState.Authenticated)?.user

            RouteMapPickerScreen(
                lineaId   = screen.lineaId,
                lineaName = screen.lineaName,
                onSaveRoute = { routeData ->
                    adminViewModel.createRutaWithCoords(
                        n      = routeData.name,
                        d      = routeData.description,
                        sLat   = routeData.startPoint?.lat ?: 0.0,
                        sLng   = routeData.startPoint?.lng ?: 0.0,
                        eLat   = routeData.endPoint?.lat  ?: 0.0,
                        eLng   = routeData.endPoint?.lng  ?: 0.0,
                        lineaId = screen.lineaId,
                        uid    = currentUser?.id ?: ""
                    )
                    currentScreen = Screen.AdminDashboard
                },
                onBack = { currentScreen = Screen.AdminDashboard }
            )
        }
    }
}
