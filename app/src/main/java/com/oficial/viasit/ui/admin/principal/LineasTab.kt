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
import com.oficial.viasit.ui.admin.AdminTextField
import com.oficial.viasit.ui.admin.DeleteConfirmDialog
import com.oficial.viasit.ui.admin.EmptyState
import com.oficial.viasit.ui.theme.*

@Composable
fun LineasTabContent(
    lineas: List<Linea>,
    isLoading: Boolean,
    onCreateLinea: (name: String, code: String, rutaId: String) -> Unit,
    onDeleteLinea: (lineaId: String, name: String) -> Unit
) {
    var showCreateDialog   by remember { mutableStateOf(false) }
    var showDeleteDialogFor by remember { mutableStateOf<Linea?>(null) }

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
        item {
            Button(
                onClick  = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Brand500, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva línea", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (lineas.isEmpty()) {
            item { EmptyState("No hay líneas creadas") }
        } else {
            items(lineas) { linea ->
                LineaCard(linea = linea, onDelete = { showDeleteDialogFor = linea })
            }
        }
    }

    if (showCreateDialog) {
        var name   by remember { mutableStateOf("") }
        var code   by remember { mutableStateOf("") }
        var rutaId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor   = Slate800,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Nueva línea", style = MaterialTheme.typography.headlineSmall, color = Color.White) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminTextField(value = name,   onValueChange = { name = it },   label = "Nombre")
                    AdminTextField(value = code,   onValueChange = { code = it },   label = "Código")
                    AdminTextField(value = rutaId, onValueChange = { rutaId = it }, label = "ID de Ruta (opcional)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && code.isNotBlank()) {
                            onCreateLinea(name, code, rutaId.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand500)
                ) { Text("Crear", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar", color = Slate400) }
            }
        )
    }

    showDeleteDialogFor?.let { linea ->
        DeleteConfirmDialog(
            title     = "Eliminar línea: ${linea.name}",
            onConfirm = { onDeleteLinea(linea.id, linea.name); showDeleteDialogFor = null },
            onDismiss = { showDeleteDialogFor = null }
        )
    }
}

@Composable
fun LineaCard(linea: Linea, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brand500.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Route, null, tint = Brand400, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(linea.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Código: ${linea.code}",
                style = MaterialTheme.typography.bodySmall, color = Slate400)
            if (linea.rutaId.isNotBlank()) {
                Text("Ruta: ${linea.rutaId}",
                    style = MaterialTheme.typography.labelSmall, color = Brand400)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Eliminar", tint = Rose500, modifier = Modifier.size(18.dp))
        }
    }
}
