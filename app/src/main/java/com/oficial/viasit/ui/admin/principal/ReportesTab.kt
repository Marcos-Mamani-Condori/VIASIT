package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onResponder: (String, String) -> Unit = { _, _ -> }
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
    } else if (reportes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay reportes registrados", color = Slate500)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reportes) { reporte ->
                ReporteItem(reporte = reporte, onResponder = onResponder)
            }
        }
    }
}

@Composable
fun ReporteItem(reporte: Reporte, onResponder: (String, String) -> Unit = { _, _ -> }) {
    var showDialog by remember { mutableStateOf(false) }
    var respuesta by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = Amber400, modifier = Modifier.size(20.dp))
                Text(
                    "Reporte de Pasajero",
                    style = MaterialTheme.typography.labelMedium,
                    color = Amber400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    reporte.created.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }

            Text(
                reporte.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            // Status Indicator
            Surface(
                color = when(reporte.status) {
                    "resuelto" -> Emerald400.copy(alpha = 0.1f)
                    "en_revision" -> Amber400.copy(alpha = 0.1f)
                    else -> Rose500.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = reporte.status.replace("_", " ").uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when(reporte.status) {
                        "resuelto" -> Emerald400
                        "en_revision" -> Amber400
                        else -> Rose500
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    val reporter = if (reporte.reporterName.isNotBlank()) reporte.reporterName else "Pasajero Anónimo"
                    Text("De: $reporter", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    
                    val contra = reporte.driverName
                    Text("Contra: $contra", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (contra != "Conductor") Brand400 else Rose500)
                }
            }

            if (reporte.adminResponse.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Slate800).padding(12.dp)) {
                    Column {
                        Text("Respuesta de Administración:", style = MaterialTheme.typography.labelSmall, color = Brand400, fontWeight = FontWeight.Bold)
                        Text(reporte.adminResponse, style = MaterialTheme.typography.bodySmall, color = Slate300)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Slate900,
            title = { Text("Detalle del Reporte", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(reporte.description, color = Color.White)
                    if (reporte.status != "resuelto") {
                        OutlinedTextField(
                            value = respuesta,
                            onValueChange = { respuesta = it },
                            label = { Text("Respuesta") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Brand500,
                                unfocusedBorderColor = Slate700
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (reporte.status != "resuelto") {
                    Button(
                        onClick = {
                            onResponder(reporte.id, respuesta)
                            showDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Brand500)
                    ) {
                        Text("Responder y Resolver")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar", color = Slate400)
                }
            }
        )
    }
}
