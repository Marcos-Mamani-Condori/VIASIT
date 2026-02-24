package com.oficial.viasit.ui.admin.linea

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.ui.theme.*

/**
 * Diálogo para que ADMIN_LINEA seleccione una ruta existente y la asigne a su línea.
 * Muestra la lista de rutas disponibles con nombre, descripción e inicio/fin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePickerDialog(
    rutas: List<Ruta>,
    currentRutaId: String,
    onSelect: (Ruta) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Slate800,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Título
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Route, null, tint = Brand400, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Seleccionar ruta existente",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Slate700)

            if (rutas.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No hay rutas disponibles", color = Slate500,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rutas) { ruta ->
                        val isSelected = ruta.id == currentRutaId
                        ListItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            headlineContent = {
                                Text(ruta.name, color = Color.White, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Column {
                                    if (ruta.description.isNotBlank())
                                        Text(ruta.description, color = Slate400,
                                            style = MaterialTheme.typography.bodySmall)
                                    if (ruta.startPoint.isNotBlank() && ruta.endPoint.isNotBlank()) {
                                        Text("${ruta.startPoint.take(12)} → ${ruta.endPoint.take(12)}",
                                            color = Slate500, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.Route,
                                    null,
                                    tint = if (isSelected) Emerald400 else Slate500,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                if (!isSelected) {
                                    TextButton(onClick = { onSelect(ruta) }) {
                                        Text("Asignar", color = Brand400,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Text("Actual", color = Emerald400,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) Emerald500.copy(alpha = 0.08f)
                                else Slate900
                            ),
                            tonalElevation = 0.dp
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = Slate700.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
