package com.oficial.viasit.ui.admin.linea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.oficial.viasit.ui.admin.AdminTextField
import com.oficial.viasit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiLineaTabContent(
    lineaId: String,
    lineas: List<Linea>,
    rutas: List<Ruta> = emptyList(),
    isLoading: Boolean,
    userEmail: String = "",
    onCreateRuta: (name: String, description: String, start: String, end: String, lineaId: String) -> Unit,
    onAssignRuta: (rutaId: String, rutaName: String) -> Unit = { _, _ -> },
    onNavigateToRoutePicker: ((lineaId: String, lineaName: String) -> Unit)? = null
) {
    var showAssignOptions  by remember { mutableStateOf(false) }  // modal de 2 opciones
    var showRoutePicker    by remember { mutableStateOf(false) }  // listado de rutas existentes
    var showCreateDialog   by remember { mutableStateOf(false) }  // fallback sin mapa

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
        return
    }

    val miLinea: Linea? = lineas.find { it.id == lineaId }
    val miRuta: Ruta?   = miLinea?.let { l -> rutas.find { it.id == l.rutaId } }
    val tieneRuta       = miRuta != null

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu Línea Asignada", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    if (miLinea != null) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brand500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsBus, null, tint = Brand400, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(miLinea.name, style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Código: ${miLinea.code}",
                                    style = MaterialTheme.typography.bodyMedium, color = Brand400)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Text("No tienes línea asignada",
                                style = MaterialTheme.typography.bodyMedium, color = Slate500)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(
                containerColor = if (tieneRuta) Slate800 else Slate900.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Route, null,
                            tint = if (tieneRuta) Brand400 else Slate600, modifier = Modifier.size(20.dp))
                        Text("Ruta Asignada", style = MaterialTheme.typography.labelMedium,
                            color = if (tieneRuta) Slate200 else Slate500, fontWeight = FontWeight.SemiBold)
                    }
                    if (miRuta != null) {
                        Text(miRuta.name, style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.Bold)
                        if (miRuta.description.isNotBlank())
                            Text(miRuta.description, style = MaterialTheme.typography.bodySmall, color = Slate400)
                        if (miRuta.startPoint.isNotBlank() || miRuta.endPoint.isNotBlank()) {
                            HorizontalDivider(color = Slate700, modifier = Modifier.padding(vertical = 4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (miRuta.startPoint.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Brand500))
                                        Text("Inicio: ${miRuta.startPoint}",
                                            style = MaterialTheme.typography.bodySmall, color = Slate200)
                                    }
                                }
                                if (miRuta.waypoints.isNotBlank()) {
                                    val wptCount = miRuta.waypoints.split(";").size
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.MoreVert, null, tint = Slate500, modifier = Modifier.size(10.dp))
                                        Text("$wptCount puntos de recorrido (OSRM)",
                                            style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    }
                                }
                                if (miRuta.endPoint.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Emerald500))
                                        Text("Fin: ${miRuta.endPoint}",
                                            style = MaterialTheme.typography.bodySmall, color = Slate200)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Esta línea no tiene ruta asignada.",
                            style = MaterialTheme.typography.bodyMedium, color = Slate500)
                    }
                }
            }
        }

        item {
            if (miLinea != null) {
                Button(
                    onClick = { showAssignOptions = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tieneRuta) Slate700 else Brand500,
                        contentColor   = Color.White
                    )
                ) {
                    Icon(if (tieneRuta) Icons.Default.EditLocationAlt else Icons.Default.AddLocation,
                        null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (tieneRuta) "Modificar ruta" else "Asignar ruta",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAssignOptions && miLinea != null) {
        ModalBottomSheet(
            onDismissRequest = { showAssignOptions = false },
            containerColor   = Slate800,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (tieneRuta) "Modificar ruta" else "Asignar ruta",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Text("¿Cómo deseas asignar la ruta?",
                    style = MaterialTheme.typography.bodySmall, color = Slate400)
                HorizontalDivider(color = Slate700)
                if (onNavigateToRoutePicker != null) {
                    Button(
                        onClick  = { showAssignOptions = false; onNavigateToRoutePicker(lineaId, miLinea.name) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Brand500)
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear nueva ruta en mapa", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick  = { showAssignOptions = false; showRoutePicker = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Brand400),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Brand500)
                ) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar ruta existente")
                }
                if (onNavigateToRoutePicker == null) {
                    OutlinedButton(
                        onClick  = { showAssignOptions = false; showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Slate600)
                    ) { Text("Crear ruta manualmente") }
                }
            }
        }
    }

    if (showRoutePicker) {
        RoutePickerDialog(
            rutas         = rutas,
            currentRutaId = miLinea?.rutaId ?: "",
            onSelect      = { ruta ->
                onAssignRuta(ruta.id, ruta.name)
                showRoutePicker = false
            },
            onDismiss = { showRoutePicker = false }
        )
    }

    if (showCreateDialog && miLinea != null) {
        var name        by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var startPoint  by remember { mutableStateOf("") }
        var endPoint    by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor   = Slate800, shape = RoundedCornerShape(20.dp),
            title  = { Text(if (tieneRuta) "Modificar Ruta" else "Asignar Ruta",
                style = MaterialTheme.typography.headlineSmall, color = Color.White) },
            text   = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminTextField(value = name,        onValueChange = { name = it },        label = "Nombre")
                AdminTextField(value = description, onValueChange = { description = it }, label = "Descripción")
                AdminTextField(value = startPoint,  onValueChange = { startPoint = it },  label = "Punto inicio")
                AdminTextField(value = endPoint,    onValueChange = { endPoint = it },    label = "Punto final")
            }},
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) { onCreateRuta(name, description, startPoint, endPoint, lineaId); showCreateDialog = false }
                }, colors = ButtonDefaults.buttonColors(containerColor = Brand500)) {
                    Text(if (tieneRuta) "Actualizar" else "Crear", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar", color = Slate400) } }
        )
    }
}
