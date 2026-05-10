package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.ui.admin.DeleteConfirmDialog
import com.oficial.viasit.ui.admin.EmptyState
import com.oficial.viasit.ui.theme.*

@Composable
fun RutasTabContent(
    rutas: List<Ruta>,
    lineas: List<Linea>,
    isLoading: Boolean,
    onDeleteRuta: (rutaId: String, rutaName: String, lineaId: String) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<Ruta?>(null) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (rutas.isEmpty()) {
            item { EmptyState("No hay rutas creadas") }
        } else {
            items(rutas) { ruta ->
                // Busca qué línea tiene asignada esta ruta
                val lineaAsignada = lineas.find { it.rutaId == ruta.id }
                RutaAdminCard(
                    ruta         = ruta,
                    lineaAsignada = lineaAsignada,
                    onDelete     = { deleteTarget = ruta }
                )
            }
        }
    }

    // Confirmación de borrado
    deleteTarget?.let { ruta ->
        val lineaId = lineas.find { it.rutaId == ruta.id }?.id ?: ""
        DeleteConfirmDialog(
            title     = "Eliminar ruta \"${ruta.name}\"",
            onConfirm = {
                onDeleteRuta(ruta.id, ruta.name, lineaId)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Tarjeta de ruta con detalles y linea asignada ────────────────────────────
@Composable
private fun RutaAdminCard(
    ruta: Ruta,
    lineaAsignada: Linea?,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape  = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Cabecera: nombre + botón borrar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brand500.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Route, null, tint = Brand400, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ruta.name, style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (ruta.description.isNotBlank())
                        Text(ruta.description, style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = Rose500, modifier = Modifier.size(18.dp))
                }
            }

            // Línea asignada
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DirectionsBus, null, tint = if (lineaAsignada != null) Emerald400 else Slate600,
                    modifier = Modifier.size(14.dp))
                Text(
                    if (lineaAsignada != null) "Línea: ${lineaAsignada.name} (${lineaAsignada.code})"
                    else "Sin línea asignada",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lineaAsignada != null) Emerald400 else Slate600
                )
            }

            // Puntos de la ruta
            if (ruta.startPoint.isNotBlank() || ruta.endPoint.isNotBlank()) {
                HorizontalDivider(color = Slate700)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (ruta.startPoint.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brand500))
                            Text(ruta.startPoint.take(18), style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                    if (ruta.endPoint.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50))
                                .background(Emerald500))
                            Text(ruta.endPoint.take(18), style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                    if (ruta.waypoints.isNotBlank()) {
                        val count = ruta.waypoints.split(";").size
                        Text("$count pts OSRM", style = MaterialTheme.typography.labelSmall, color = Slate600)
                    }
                }
            }
        }
    }
}
