package com.oficial.viasit.ui.passenger

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.viewmodels.PassengerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAutosTab(
    autosViewModel: AutosViewModel,
    passengerViewModel: PassengerViewModel,
    currentUser: User,
    onNavigateToMap: () -> Unit
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val todosLosAutos by autosViewModel.autosUiState.collectAsState()
    val todasLasLineas by passengerViewModel.lineas.collectAsState()
    val autosActivos  = todosLosAutos.filter { it.isActive() }
    val destinoLineas by passengerViewModel.destinoLineas.collectAsState()
    val destinoNombre by passengerViewModel.destinoNombre.collectAsState()
    val destinoQuery  by passengerViewModel.destinoQuery.collectAsState()
    val destinoIds    = destinoLineas.map { it.id }.toSet()

    // BLOQUEO ESTRICTO: Solo 'usuario' o 'pasajero' pueden reportar. Invitados NO.
    val userRoleStr = currentUser.role.firstOrNull() ?: ""
    val puedeReportar = (userRoleStr.equals("usuario", ignoreCase = true) || userRoleStr.equals("pasajero", ignoreCase = true)) && !currentUser.isGuest

    var query by remember { mutableStateOf("") }
    var selectedLineaFilter by remember { mutableStateOf<String?>(null) } // null = todas

    val filtrados = remember(autosActivos, query, selectedLineaFilter) {
        var list = if (query.isBlank()) autosActivos
                   else autosActivos.filter { it.placa.contains(query.trim(), ignoreCase = true) }
        
        if (selectedLineaFilter != null) {
            list = list.filter { it.lineaId == selectedLineaFilter }
        }
        list
    }

    var userLocation by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(Unit) {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                 ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (ok) {
            try {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                userLocation = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) {}
        }
    }

    var destinoResults  by remember { mutableStateOf<List<com.oficial.viasit.ui.routes.SearchResult>>(emptyList()) }
    var destinoBuscando by remember { mutableStateOf(false) }
    var destinoJob      by remember { mutableStateOf<Job?>(null) }
    val geocodingService = remember { com.oficial.viasit.ui.routes.GeocodingService() }

    var selectedAuto    by remember { mutableStateOf<Auto?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportesDialog by remember { mutableStateOf(false) }
    var reportesList   by remember { mutableStateOf<List<com.oficial.viasit.domain.model.Reporte>>(emptyList()) }
    var reportesLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Slate950)) {

        Row(
            modifier = Modifier.fillMaxWidth().background(Slate900).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Unidades Activas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${autosActivos.size} vehículos en ruta", style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Brand500.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Refresh, null, tint = Brand500, modifier = Modifier.size(20.dp).clickable { autosViewModel.refresh() })
            }
        }

        // --- BUSCADOR DE DESTINO (RESTURADO) ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (destinoNombre != null) Brand500 else Slate800)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = if (destinoNombre != null) Brand500 else Slate500)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (destinoNombre != null) "¿Cómo llegar a $destinoNombre?" else "¿A dónde quieres ir hoy?",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (destinoNombre != null) Brand400 else Slate400
                        )
                        OutlinedTextField(
                            value = destinoQuery,
                            onValueChange = { 
                                passengerViewModel.setDestinoQuery(it)
                                destinoJob?.cancel()
                                if (it.length > 3) {
                                    destinoJob = scope.launch {
                                        kotlinx.coroutines.delay(500)
                                        destinoBuscando = true
                                        geocodingService.search(it).onSuccess { results ->
                                            destinoResults = results
                                            destinoBuscando = false
                                        }
                                    }
                                } else {
                                    destinoResults = emptyList()
                                }
                            },
                            placeholder = { Text("Ej: Plaza Murillo, San Miguel...", color = Slate600, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                if (destinoNombre != null) {
                                    IconButton(onClick = { passengerViewModel.limpiarDestino() }) {
                                        Icon(Icons.Default.Close, null, tint = Rose500)
                                    }
                                }
                            }
                        )
                    }
                }

                if (destinoResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    destinoResults.forEach { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                passengerViewModel.buscarLineasPorDestino(res.lat, res.lon, res.name)
                                destinoResults = emptyList()
                                passengerViewModel.setDestinoQuery("")
                            }.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.History, null, tint = Slate600, modifier = Modifier.size(16.dp))
                            Text(res.displayName, style = MaterialTheme.typography.bodySmall, color = Slate300, maxLines = 1)
                        }
                    }
                }
            }
        }

        // --- LÍNEAS RECOMENDADAS (RESTAURADO) ---
        if (destinoLineas.isNotEmpty()) {
            Text(
                "Líneas que te llevan a tu destino",
                style = MaterialTheme.typography.labelLarge,
                color = Brand400,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(destinoLineas) { linea ->
                    Card(
                        modifier = Modifier.width(140.dp).clickable { passengerViewModel.selectLinea(linea.id); onNavigateToMap() },
                        colors = CardDefaults.cardColors(containerColor = Brand500.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Brand500.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.DirectionsBus, null, tint = Brand500)
                            Text(linea.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Ver ruta", style = MaterialTheme.typography.labelSmall, color = Brand400)
                        }
                    }
                }
            }
        }

    // --- EXPLORAR TODAS LAS LÍNEAS (NUEVO) ---
    if (destinoLineas.isEmpty() && query.isBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Filtrar por Línea",
                style = MaterialTheme.typography.labelLarge,
                color = Slate400,
                fontWeight = FontWeight.Bold
            )
            if (selectedLineaFilter != null) {
                Text(
                    "Ver Ruta Oficial",
                    modifier = Modifier.clickable { 
                        passengerViewModel.selectLinea(selectedLineaFilter!!)
                        onNavigateToMap()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Brand400,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
                item {
                    FilterChip(
                        selected = selectedLineaFilter == null,
                        onClick = { selectedLineaFilter = null },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand500,
                            selectedLabelColor = Color.White,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Slate800,
                            selectedBorderColor = Brand500,
                            enabled = true,
                            selected = selectedLineaFilter == null
                        )
                    )
                }
                items(todasLasLineas) { linea ->
                    val isSelected = selectedLineaFilter == linea.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedLineaFilter = if (isSelected) null else linea.id 
                        },
                        label = { Text(linea.name) },
                        leadingIcon = {
                            if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand500,
                            selectedLabelColor = Color.White,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Slate800,
                            selectedBorderColor = Brand500,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // Search Bar (Placa)
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar unidad por placa...", color = Slate500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate500) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900, unfocusedContainerColor = Slate900,
                    focusedBorderColor = Brand500, unfocusedBorderColor = Slate800,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }

        if (filtrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DirectionsBus, null, tint = Slate700, modifier = Modifier.size(64.dp))
                    Text(if (query.isEmpty()) "No hay unidades activas" else "No se encontró la placa", color = Slate500)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(filtrados) { auto ->
                    AutoUnidadCard(auto = auto, userLocation = userLocation, onClick = { selectedAuto = auto })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    selectedAuto?.let { auto ->
        ModalBottomSheet(
            onDismissRequest = { selectedAuto = null },
            containerColor   = Slate900,
            dragHandle = {
                Box(modifier = Modifier.padding(vertical = 12.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Slate700))
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brand500.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsBus, null, tint = Brand500, modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text("Placa: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Conductor: ${auto.driverName}", style = MaterialTheme.typography.bodySmall, color = Brand400, fontWeight = FontWeight.SemiBold)
                        Text(auto.getLastUpdateText(), style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Slate800)
                Spacer(modifier = Modifier.height(4.dp))

                BottomSheetOpcion(icon = Icons.Default.MyLocation, tint = Brand500, titulo = "Ver en mapa", subtitulo = "Centrar el mapa en esta unidad") {
                    passengerViewModel.selectAuto(auto); selectedAuto = null; onNavigateToMap()
                }
                HorizontalDivider(color = Slate800)

                BottomSheetOpcion(icon = Icons.Default.ChatBubbleOutline, tint = Cyan400, titulo = "Ver comentarios", subtitulo = "Opiniones de otros pasajeros") {
                    reportesList = emptyList()
                    reportesLoading = true
                    showReportesDialog = true
                    autosViewModel.fetchReportes(auto.placa.ifBlank { auto.id }) { lista ->
                        reportesList = lista
                        reportesLoading = false
                    }
                }

                if (puedeReportar) {
                    HorizontalDivider(color = Slate800)
                    BottomSheetOpcion(icon = Icons.Default.Flag, tint = Rose500, titulo = "Reportar conductor", subtitulo = "Vinculado a tu identidad para mayor transparencia") {
                        showReportDialog = true
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showReportesDialog) {
        val auto = selectedAuto
        AlertDialog(
            onDismissRequest = { showReportesDialog = false },
            containerColor   = Slate900,
            title = { Text("Comentarios del conductor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (auto != null) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DirectionsBus, null, tint = Slate400, modifier = Modifier.size(16.dp))
                            Text("Placa: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                    when {
                        reportesLoading -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Brand500, strokeWidth = 2.dp)
                        }
                        reportesList.isEmpty() -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ChatBubbleOutline, null, tint = Slate500, modifier = Modifier.size(16.dp))
                            Text("Aún no hay comentarios para esta unidad.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            reportesList.forEach { rep ->
                                val texto = rep.description.replace(Regex("""^\[Unidad: [^\]]+\]\s*"""), "")
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate800),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(rep.reporterName.ifBlank { "Pasajero" }, style = MaterialTheme.typography.labelMedium, color = Brand400, fontWeight = FontWeight.Bold)
                                            Text("•", color = Slate600)
                                            Text(rep.created.take(10), style = MaterialTheme.typography.labelSmall, color = Slate500)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(texto, style = MaterialTheme.typography.bodySmall, color = Slate300)
                                        
                                        if (rep.driverResponse.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Respuesta del Conductor:", style = MaterialTheme.typography.labelSmall, color = Emerald400, fontWeight = FontWeight.Bold)
                                            Text(rep.driverResponse, style = MaterialTheme.typography.bodySmall, color = Emerald400.copy(alpha = 0.8f))
                                        }

                                        if (rep.adminResponse.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Respuesta de Administración:", style = MaterialTheme.typography.labelSmall, color = Brand400, fontWeight = FontWeight.Bold)
                                            Text(rep.adminResponse, style = MaterialTheme.typography.bodySmall, color = Slate400)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportesDialog = false }) { Text("Cerrar", color = Brand400) }
            }
        )
    }

    if (showReportDialog) {
        val auto = selectedAuto
        var reportText  by remember { mutableStateOf("") }
        var isLoading   by remember { mutableStateOf(false) }
        var enviado     by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) { showReportDialog = false; reportText = ""; enviado = false } },
            containerColor   = Slate900,
            title = { Text("Reportar conductor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (auto != null) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DirectionsBus, null, tint = Slate400, modifier = Modifier.size(16.dp))
                            Text("Placa: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                    if (enviado) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Brand500.copy(alpha = 0.12f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Brand500, modifier = Modifier.size(18.dp))
                            Text("Reporte enviado. Gracias.", style = MaterialTheme.typography.bodyMedium, color = Brand400)
                        }
                    } else {
                        OutlinedTextField(
                            value = reportText, onValueChange = { if (it.length <= 300) reportText = it },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("Describe el problema…", color = Slate600) },
                            minLines = 3, maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand500, unfocusedBorderColor = Slate700,
                                cursorColor = Brand500, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ), shape = RoundedCornerShape(12.dp)
                        )
                        Text("${reportText.length}/300", style = MaterialTheme.typography.labelSmall, color = Slate600, modifier = Modifier.align(Alignment.End))
                    }
                }
            },
            confirmButton = {
                if (!enviado) {
                    Button(
                        onClick = {
                            if (reportText.isNotBlank() && auto != null) {
                                isLoading = true
                                val finalMsg = "[Unidad: ${auto.placa}] $reportText"
                                autosViewModel.submitReporte(finalMsg, currentUser.id, currentUser.name, auto.userId, auto.driverName) { success ->
                                    isLoading = false
                                    if (success) enviado = true
                                }
                            }
                        },
                        enabled = reportText.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Brand500, disabledContainerColor = Slate800)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Enviar Reporte")
                    }
                } else {
                    Button(onClick = { showReportDialog = false; reportText = ""; enviado = false }, colors = ButtonDefaults.buttonColors(containerColor = Brand500)) {
                        Text("Aceptar")
                    }
                }
            }
        )
    }
}

@Composable
fun BottomSheetOpcion(icon: ImageVector, tint: Color, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = Slate400)
        }
    }
}

@Composable
fun AutoUnidadCard(auto: Auto, userLocation: Location?, onClick: () -> Unit) {
    val distanceText = remember(auto.lat, auto.lng, userLocation) {
        if (userLocation == null || (auto.lat == 0.0 && auto.lng == 0.0)) "Distancia desconocida"
        else {
            val results = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, auto.lat, auto.lng, results)
            val dist = results[0]
            if (dist < 1000) "${dist.toInt()} m" else "%.1f km".format(dist / 1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brand500.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsBus, null, tint = Brand500, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Placa: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(auto.driverName, style = MaterialTheme.typography.labelSmall, color = Brand400)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(distanceText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(auto.getLastUpdateText(), style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Slate900)
    }
}
