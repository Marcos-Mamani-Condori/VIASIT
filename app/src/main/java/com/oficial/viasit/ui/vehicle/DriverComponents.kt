package com.oficial.viasit.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.ui.theme.*

/**
 * Componentes reutilizables del panel del conductor.
 *
 * Separados de [DriverDashboardScreen] para mantener el archivo
 * principal corto y cada componente legible por separado.
 *
 *  - [GreetingCard]         → saludo con nombre del usuario y conteo de vehículos
 *  - [VehicleSelectorCard]  → tarjeta que muestra el vehículo seleccionado
 *  - [ServiceToggleCard]    → switch para activar/desactivar el servicio
 *  - [StatusInfoCard]       → tarjeta informativa de estado (icono + título + mensaje)
 *  - [DashboardMenuItem]    → ítem del menú de acciones rápidas
 *  - [VehicleSelectorDialog]→ diálogo para elegir entre varios vehículos
 */

// ── Tarjeta de saludo ─────────────────────────────────────────────────────────
@Composable
fun GreetingCard(name: String, vehicleCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Hola, $name 👋",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            if (vehicleCount == 0) "No tienes vehículos registrados"
            else "$vehicleCount vehículo${if (vehicleCount != 1) "s" else ""} disponible${if (vehicleCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium, color = Slate400
        )
    }
}

// ── Tarjeta de selección de vehículo ─────────────────────────────────────────
@Composable
fun VehicleSelectorCard(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    lineaId: String?,
    onShowSelector: () -> Unit
) {
    Card(
        onClick  = onShowSelector,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brand500.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsBus, null, tint = Brand400, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Vehículo activo", style = MaterialTheme.typography.labelSmall, color = Slate400)
                Text(
                    selectedVehicle?.placa ?: "Sin vehículo seleccionado",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedVehicle != null) Color.White else Slate500,
                    fontWeight = FontWeight.SemiBold
                )
                lineaId?.let { Text("Línea: $it", style = MaterialTheme.typography.bodySmall, color = Brand400) }
            }
            if (vehicles.size > 1) {
                Icon(Icons.Default.ExpandMore, null, tint = Slate400)
            }
        }
    }
}

// ── Tarjeta de toggle de servicio ─────────────────────────────────────────────
@Composable
fun ServiceToggleCard(isInService: Boolean, onToggle: (Boolean) -> Unit, vehiclePlaca: String = "") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isInService) Emerald500.copy(0.12f) else SurfaceElevated
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isInService) Emerald500.copy(0.2f) else Slate700.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isInService) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    null, tint = if (isInService) Emerald400 else Slate500, modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isInService) "En servicio" else "Fuera de servicio",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isInService) Emerald300 else Slate400,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    if (isInService) "Tu ubicación es visible en el mapa" else "Activa para aparecer en el mapa",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInService) Emerald500.copy(0.7f) else Slate500
                )
            }
            Switch(
                checked   = isInService,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor       = Color.White,
                    checkedTrackColor       = Emerald500,
                    uncheckedThumbColor     = Slate400,
                    uncheckedTrackColor     = Slate700
                )
            )
        }
    }
}

// ── Tarjeta de estado informativo ─────────────────────────────────────────────
@Composable
fun StatusInfoCard(icon: ImageVector, title: String, message: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceElevated).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Slate400)
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Ítem del menú de acciones ─────────────────────────────────────────────────
@Composable
fun DashboardMenuItem(icon: ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(iconColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Slate600, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Diálogo de selección de vehículo ─────────────────────────────────────────
@Composable
fun VehicleSelectorDialog(
    vehicles: List<Auto>,
    selectedVehicle: Auto?,
    lineaId: String?,
    onSelectVehicle: (Auto) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Slate800,
        shape            = RoundedCornerShape(24.dp),
        title  = { Text("Seleccionar vehículo", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicles.forEach { auto ->
                    val isSelected = auto.id == selectedVehicle?.id
                    Card(
                        onClick = { onSelectVehicle(auto); onDismiss() },
                        colors  = CardDefaults.cardColors(
                            containerColor = if (isSelected) Brand500.copy(0.15f) else SurfaceTinted
                        ),
                        shape   = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.DirectionsBus, null,
                                tint = if (isSelected) Brand400 else Slate400, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(auto.placa, style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) Color.White else Slate200, fontWeight = FontWeight.SemiBold)
                                lineaId?.let { Text("Línea: $it", style = MaterialTheme.typography.labelSmall, color = Slate500) }
                            }
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Brand400, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Slate400) }
        }
    )
}

// Nota: color auxiliar solo necesario en este archivo
private val Slate500 = Color(0xFF64748B)
