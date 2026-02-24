package com.oficial.viasit.ui.passenger

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.ui.map.AutosViewModel
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.PassengerViewModel

/**
 * Tab que lista buses activos con:
 * - Barra de búsqueda por placa
 * - Distancia en km desde la ubicación del usuario
 * - Tap → selecciona el bus y cambia al tab Mapa centrando la cámara en él
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAutosTab(
    autosViewModel: AutosViewModel,
    passengerViewModel: PassengerViewModel,
    onNavigateToMap: () -> Unit        // callback para cambiar al tab Mapa
) {
    val context        = LocalContext.current
    val todosLosAutos  by autosViewModel.autosUiState.collectAsState()
    val autosActivos   = todosLosAutos.filter { it.isActive() }

    // ── Búsqueda ─────────────────────────────────────────────────────────────
    var query by remember { mutableStateOf("") }
    val filtrados = remember(autosActivos, query) {
        if (query.isBlank()) autosActivos
        else autosActivos.filter { it.placa.contains(query.trim(), ignoreCase = true) }
    }

    // ── Ubicación del usuario (para calcular distancia) ───────────────────────
    var userLocation by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                        as android.location.LocationManager
                userLocation = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Buses en servicio",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color      = Color.White
                )
                Text(
                    "${autosActivos.size} activo(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
            IconButton(onClick = { autosViewModel.refresh() }) {
                Icon(Icons.Default.Refresh, "Actualizar", tint = Slate400)
            }
        }

        // ── Barra de búsqueda ───────────────────────────────────────────────
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder   = { Text("Buscar por placa…", color = Slate500) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Slate500) },
            trailingIcon  = if (query.isNotBlank()) {
                { IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, null, tint = Slate500)
                }}
            } else null,
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Brand500,
                unfocusedBorderColor = Slate700,
                cursorColor          = Brand500,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White
            ),
            shape         = RoundedCornerShape(12.dp)
        )

        // ── Lista ──────────────────────────────────────────────────────────
        if (filtrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.DirectionsBus, null,
                        tint = Slate700, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (query.isBlank()) "Sin buses activos" else "Sin resultados para \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate400, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (query.isBlank()) "No hay conductores en servicio ahora."
                        else "Prueba con otra placa.",
                        style = MaterialTheme.typography.bodySmall, color = Slate600
                    )
                    if (query.isBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { autosViewModel.refresh() },
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = Brand500)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Actualizar")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtrados, key = { it.id }) { auto ->
                    AutoBusCard(
                        auto         = auto,
                        userLocation = userLocation,
                        onClick      = {
                            passengerViewModel.selectAuto(auto)
                            onNavigateToMap()
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AutoBusCard(
    auto: Auto,
    userLocation: Location?,
    onClick: () -> Unit
) {
    // Calcular distancia si tenemos lat/lng válidos y ubicación del usuario
    val distanciaKm: Double? = remember(auto.lat, auto.lng, userLocation) {
        if (userLocation != null && (auto.lat != 0.0 || auto.lng != 0.0)) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                auto.lat, auto.lng, results
            )
            results[0] / 1000.0  // metros → km
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceTinted),
        shape  = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono bus activo
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brand500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsBus, null,
                    tint = Brand500, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    auto.placa.ifBlank { "Sin placa" },
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null,
                        tint = Slate500, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(auto.getLastUpdateText(),
                        style = MaterialTheme.typography.bodySmall, color = Slate400)

                    if (distanciaKm != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.NearMe, null,
                            tint = Slate500, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        val texto = if (distanciaKm < 1.0)
                            "${(distanciaKm * 1000).toInt()} m"
                        else
                            "${"%.1f".format(distanciaKm)} km"
                        Text(texto, style = MaterialTheme.typography.bodySmall, color = Brand400)
                    }
                }
            }

            // Flecha → indica que se puede tocar para ir al mapa
            Icon(Icons.Default.MyLocation, "Ver en mapa",
                tint = Slate500, modifier = Modifier.size(20.dp))
        }
    }
}
