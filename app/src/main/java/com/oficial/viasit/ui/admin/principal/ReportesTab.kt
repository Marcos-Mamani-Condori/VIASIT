package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.ui.theme.*

@Composable
fun ReportesTabContent(
    reportes: List<Reporte>,
    isLoading: Boolean,
    onResponder: (String, String) -> Unit = { _, _ -> },
    onSuspendUser: (String, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val reportCounts = remember(reportes) {
        reportes.groupBy { it.targetUserId }.mapValues { it.value.size }
    }

    val filteredReportes = remember(reportes, searchQuery) {
        reportes.filter { 
            it.driverName.contains(searchQuery, ignoreCase = true) || 
            it.reporterName.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }.sortedByDescending { it.created }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Buscar por conductor, pasajero o motivo...", color = Slate500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate500) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = Brand500,
                unfocusedBorderColor = Slate800
            ),
            singleLine = true
        )

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand500)
            }
        } else if (filteredReportes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if(searchQuery.isEmpty()) "No hay reportes" else "Sin resultados", color = Slate500)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReportes) { reporte ->
                    val totalReports = reportCounts[reporte.targetUserId] ?: 1
                    ReporteItem(
                        reporte = reporte, 
                        totalUserReports = totalReports,
                        onResponder = onResponder,
                        onSuspendUser = onSuspendUser
                    )
                }
            }
        }
    }
}

@Composable
fun ReporteItem(
    reporte: Reporte, 
    totalUserReports: Int,
    onResponder: (String, String) -> Unit,
    onSuspendUser: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var respuesta by remember { mutableStateOf("") }
    val isResolved = reporte.status == "resuelto"

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = if(isResolved) Emerald400 else Amber400, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REPORTE #${reporte.id.takeLast(4).uppercase()}", 
                    style = MaterialTheme.typography.labelSmall, color = if(isResolved) Emerald400 else Amber400, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(reporte.created.take(10), style = MaterialTheme.typography.labelSmall, color = Slate500)
            }

            if (totalUserReports > 1 && !isResolved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose500.copy(0.15f))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Rose500, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "REINCIDENTE: Este usuario tiene $totalUserReports reportes en total",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(reporte.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Conductor reportado:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text(reporte.driverName, style = MaterialTheme.typography.labelMedium, color = Brand400, fontWeight = FontWeight.Bold)
                }
                
                if (!isResolved) {
                    IconButton(
                        onClick = { onSuspendUser(reporte.targetUserId, reporte.driverName) },
                        modifier = Modifier.size(32.dp).background(Rose500.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Block, "Suspender", tint = Rose500, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier.size(32.dp).background(Emerald400.copy(0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("De: ${reporte.reporterName}", style = MaterialTheme.typography.labelSmall, color = Slate400, modifier = Modifier.weight(1f))
                
                Surface(
                    color = if(isResolved) Emerald400.copy(0.1f) else Amber400.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        reporte.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if(isResolved) Emerald400 else Amber400,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Slate950,
            title = { Text(if(isResolved) "Reporte Resuelto" else "Gestionar Reporte", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(reporte.description, color = Slate300)
                    if (!isResolved) {
                        OutlinedTextField(
                            value = respuesta,
                            onValueChange = { respuesta = it },
                            label = { Text("Escribe una respuesta para el pasajero...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Brand500
                            )
                        )
                    } else {
                        Column {
                            Text("Respuesta del Admin:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(reporte.adminResponse, color = Brand400, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                if (!isResolved) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onResponder(reporte.id, "Reporte rechazado por falta de pruebas."); showDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Rose500)
                        ) { Text("Ignorar") }
                        
                        Button(
                            onClick = { onResponder(reporte.id, respuesta); showDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Brand500)
                        ) { Text("Resolver") }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cerrar", color = Slate500) }
            }
        )
    }
}
