package com.oficial.viasit.ui.passenger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Ruta
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.PassengerViewModel

@Composable
fun RoutesTab(
    viewModel: PassengerViewModel,
    onViewOnMap: (lineaId: String) -> Unit
) {
    val lineas          by viewModel.lineas.collectAsState()
    val rutas           by viewModel.rutas.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val error           by viewModel.error.collectAsState()
    val selectedLineaId by viewModel.selectedLineaId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Rutas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "Líneas de La Paz",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
            IconButton(onClick = { viewModel.loadLineas() }) {
                Icon(Icons.Default.Refresh, "Recargar", tint = Slate400)
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Brand500)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando rutas...", color = Slate400,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.WifiOff, null,
                            tint = Slate600, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(error ?: "Error", color = Slate400,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadLineas() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand500)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }
            lineas.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.DirectionsBus, null,
                            tint = Slate700, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sin líneas registradas", color = Slate400,
                            style = MaterialTheme.typography.bodyMedium)
                        Text("El administrador aún no ha configurado líneas",
                            color = Slate600, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${lineas.size} línea(s) disponible(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(lineas, key = { it.id }) { linea ->
                        val ruta = rutas[linea.rutaId]
                        val isSelected = selectedLineaId == linea.id
                        LineaCard(
                            linea      = linea,
                            ruta       = ruta,
                            isSelected = isSelected,
                            onViewOnMap = {
                                if (isSelected) {
                                    viewModel.clearSelectedLinea()
                                } else {
                                    viewModel.selectLinea(linea.id)
                                    onViewOnMap(linea.id)
                                }
                            },
                            onReportRoute = { descripcion ->
                                viewModel.reportarProblemaRuta(linea.name, descripcion)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LineaCard(
    linea: Linea,
    ruta: Ruta?,
    isSelected: Boolean,
    onViewOnMap: () -> Unit,
    onReportRoute: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SurfaceTinted),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Brand700, Brand500))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = linea.name.ifBlank { "Línea sin nombre" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (linea.code.isNotBlank()) {
                        Text(
                            text = "Código: ${linea.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Emerald500.copy(alpha = 0.18f),
                        modifier = Modifier.clickable { onViewOnMap() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                tint = Emerald400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "En mapa",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald400
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = onViewOnMap,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Map, null,
                            tint = Brand400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Ver",
                            style = MaterialTheme.typography.labelSmall,
                            color = Brand400
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (ruta != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevated)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (ruta.name.isNotBlank()) {
                            Text(
                                text = ruta.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200,
                                fontSize = 13.sp
                            )
                        }
                        if (ruta.description.isNotBlank()) {
                            Text(
                                text = ruta.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        if (ruta.startPoint.isNotBlank() || ruta.endPoint.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate900)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Emerald400)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Inicio", style = MaterialTheme.typography.labelSmall,
                                            color = Emerald400)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ruta.startPoint.ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate200,
                                        modifier = Modifier.widthIn(max = 130.dp)
                                    )
                                }

                                Icon(Icons.Default.ArrowForward, null,
                                    tint = Slate600, modifier = Modifier.size(18.dp)
                                        .align(Alignment.CenterVertically))

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text("Fin", style = MaterialTheme.typography.labelSmall,
                                            color = Rose500)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Rose500)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ruta.endPoint.ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate200,
                                        modifier = Modifier.widthIn(max = 130.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Slate800, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onReportRoute("Falla reportada en la ruta de ${linea.name}") },
                                colors = ButtonDefaults.textButtonColors(contentColor = Rose500)
                            ) {
                                Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reportar falla en ruta", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevated)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Sin ruta asignada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}
