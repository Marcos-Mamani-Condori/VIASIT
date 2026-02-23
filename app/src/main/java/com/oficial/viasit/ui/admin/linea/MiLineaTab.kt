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
import com.oficial.viasit.ui.admin.AdminTextField
import com.oficial.viasit.ui.theme.*

/**
 * Tab "Mi Línea" — solo para ADMIN_LINEA.
 *
 * Muestra la información de la línea asignada al admin
 * y permite crear rutas:
 *  - Con mapa interactivo (modo preferido, si está disponible)
 *  - Con el diálogo manual de texto (fallback)
 */
@Composable
fun MiLineaTabContent(
    lineaId: String,
    lineas: List<Linea>,
    isLoading: Boolean,
    onCreateRuta: (name: String, description: String, start: String, end: String, lineaId: String) -> Unit,
    onNavigateToRoutePicker: ((lineaId: String, lineaName: String) -> Unit)? = null
) {
    var showCreateRutaDialog by remember { mutableStateOf(false) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
        return
    }

    val miLinea = lineas.find { it.id == lineaId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Info de la línea asignada ─────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tu Línea Asignada",
                    style = MaterialTheme.typography.labelSmall, color = Slate400)
                if (miLinea != null) {
                    Text(miLinea.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Código: ${miLinea.code}",
                        style = MaterialTheme.typography.bodyMedium, color = Brand400)
                } else {
                    Text("No tienes línea asignada",
                        style = MaterialTheme.typography.bodyMedium, color = Slate500)
                }
            }
        }

        // ── Botón de crear ruta ───────────────────────────────────────────
        item {
            if (onNavigateToRoutePicker != null && miLinea != null) {
                // Preferido: selector en mapa
                Button(
                    onClick  = { onNavigateToRoutePicker(lineaId, miLinea.name) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Ruta en Mapa", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            } else {
                // Fallback: diálogo de texto
                Button(
                    onClick  = { showCreateRutaDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Route, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Ruta", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── Diálogo fallback: crear ruta con texto ────────────────────────────
    if (showCreateRutaDialog) {
        var name        by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var startPoint  by remember { mutableStateOf("") }
        var endPoint    by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateRutaDialog = false },
            containerColor   = Slate800,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Nueva Ruta", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminTextField(value = name,        onValueChange = { name = it },        label = "Nombre")
                    AdminTextField(value = description, onValueChange = { description = it }, label = "Descripción")
                    AdminTextField(value = startPoint,  onValueChange = { startPoint = it },  label = "Punto inicio")
                    AdminTextField(value = endPoint,    onValueChange = { endPoint = it },    label = "Punto final")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateRuta(name, description, startPoint, endPoint, lineaId)
                            showCreateRutaDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand500)
                ) { Text("Crear", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRutaDialog = false }) { Text("Cancelar", color = Slate400) }
            }
        )
    }
}
