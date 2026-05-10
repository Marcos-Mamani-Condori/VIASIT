package com.oficial.viasit.ui.passenger

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.viewmodels.AutosViewModel
import com.oficial.viasit.ui.routes.GeocodingService
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.PassengerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val autosActivos  = todosLosAutos.filter { it.isActive() }
    val destinoLineas by passengerViewModel.destinoLineas.collectAsState()
    val destinoNombre by passengerViewModel.destinoNombre.collectAsState()
    val destinoQuery  by passengerViewModel.destinoQuery.collectAsState()
    val destinoIds    = destinoLineas.map { it.id }.toSet()

    val puedeReportar = currentUser.userRole == UserRole.usuario

    var query by remember { mutableStateOf("") }
    val filtrados = remember(autosActivos, query) {
        if (query.isBlank()) autosActivos
        else autosActivos.filter { it.placa.contains(query.trim(), ignoreCase = true) }
    }
    val filtradosOrdenados = remember(filtrados, userLocation) {
        val loc = userLocation ?: return@remember filtrados
        filtrados.sortedBy { auto ->
            if (auto.lat == 0.0 && auto.lng == 0.0) Float.MAX_VALUE.toDouble()
            else { val r = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, auto.lat, auto.lng, r); r[0].toDouble() }
        }
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
    val geocodingService = remember { GeocodingService() }

    var selectedAuto    by remember { mutableStateOf<Auto?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportesDialog by remember { mutableStateOf(false) }
    var reportesList   by remember { mutableStateOf<List<String>>(emptyList()) }
    var reportesLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Slate950)) {

        Row(
            modifier = Modifier.fillMaxWidth().background(Slate900).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Buses en servicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text("${autosActivos.size} activo(s)", style = MaterialTheme.typography.bodySmall, color = Slate400)
            }
            IconButton(onClick = { autosViewModel.refresh() }) {
                Icon(Icons.Default.Refresh, "Actualizar", tint = Slate400)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(Slate900).padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            OutlinedTextField(
                value         = destinoQuery,
                onValueChange = { v ->
                    passengerViewModel.setDestinoQuery(v)
                    if (v.isBlank()) {
                        destinoResults = emptyList()
                        passengerViewModel.limpiarDestino()
                    } else {
                        destinoJob?.cancel()
                        destinoJob = scope.launch {
                            delay(400)
                            destinoBuscando = true
                            geocodingService.search(v).onSuccess { destinoResults = it }
                            destinoBuscando = false
                        }
                    }
                },
                modifier    = Modifier.fillMaxWidth(),
                placeholder = { Text("¿A dónde vas? Busca tu destino…", color = Slate500) },
                leadingIcon = { Icon(Icons.Default.TravelExplore, null, tint = if (destinoNombre != null) Brand500 else Slate500) },
                trailingIcon = when {
                    destinoBuscando -> { { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Brand500) } }
                    destinoQuery.isNotBlank() -> { { IconButton(onClick = { passengerViewModel.limpiarDestino(); destinoResults = emptyList() }) { Icon(Icons.Default.Close, null, tint = Slate500) } } }
                    else -> null
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Brand500,
                    unfocusedBorderColor = Slate700,
                    cursorColor          = Brand500,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (destinoResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors   = CardDefaults.cardColors(containerColor = Slate800),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    destinoResults.take(4).forEach { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    passengerViewModel.buscarLineasPorDestino(result.lat, result.lon, result.name)
                                    destinoQuery   = result.name
                                    destinoResults = emptyList()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Brand500, modifier = Modifier.size(16.dp))
                            Column {
                                Text(result.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1)
                                Text(result.displayName.split(",").take(2).joinToString(","), style = MaterialTheme.typography.labelSmall, color = Slate500, maxLines = 1)
                            }
                        }
                        HorizontalDivider(color = Slate700, thickness = 0.5.dp)
                    }
                }
            }

            if (destinoNombre != null) {
                if (destinoLineas.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp)).background(Slate800).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Slate500, modifier = Modifier.size(16.dp))
                        Text("No encontramos líneas que pasen por \"$destinoNombre\"", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Líneas que pasan por \"$destinoNombre\"", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            destinoLineas.forEach { linea ->
                                AssistChip(
                                    onClick = { passengerViewModel.selectLinea(linea.id); onNavigateToMap() },
                                    label   = { Text(linea.name, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.DirectionsBus, null, modifier = Modifier.size(14.dp)) },
                                    colors  = AssistChipDefaults.assistChipColors(
                                        containerColor  = Brand500.copy(alpha = 0.15f),
                                        labelColor      = Brand400,
                                        leadingIconContentColor = Brand500
                                    ),
                                    border  = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Brand500.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder   = { Text("Buscar por placa…", color = Slate500) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Slate500) },
            trailingIcon  = if (query.isNotBlank()) { { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null, tint = Slate500) } } } else null,
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Brand500, unfocusedBorderColor = Slate700,
                cursorColor = Brand500, focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (filtrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.DirectionsBus, null, tint = Slate700, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (query.isBlank()) "Sin buses activos" else "Sin resultados para \"$query\"",
                        style = MaterialTheme.typography.titleMedium, color = Slate400, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (query.isBlank()) "No hay conductores en servicio ahora." else "Prueba con otra placa.",
                        style = MaterialTheme.typography.bodySmall, color = Slate600)
                    if (query.isBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(onClick = { autosViewModel.refresh() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand500)) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Actualizar")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (destinoIds.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.TravelExplore, null, tint = Brand500, modifier = Modifier.size(14.dp))
                            Text("Buses ordenados por cercanía — toca un chip para ver la ruta", style = MaterialTheme.typography.labelSmall, color = Brand400)
                        }
                    }
                }
                items(filtradosOrdenados, key = { it.id }) { auto ->
                    AutoBusCard(auto = auto, userLocation = userLocation, onClick = { selectedAuto = auto })
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
                        Text(auto.placa.ifBlank { "Sin placa" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(auto.getLastUpdateText(), style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Slate800)
                Spacer(modifier = Modifier.height(4.dp))

                BottomSheetOpcion(icon = Icons.Default.MyLocation, tint = Brand500, titulo = "Ver en mapa", subtitulo = "Centrar el mapa en este bus") {
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
                    BottomSheetOpcion(icon = Icons.Default.Flag, tint = Rose500, titulo = "Reportar conductor", subtitulo = "Anónimo — nadie verá tu nombre") {
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
                            Text("Bus: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                    when {
                        reportesLoading -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Brand500, strokeWidth = 2.dp)
                        }
                        reportesList.isEmpty() -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ChatBubbleOutline, null, tint = Slate500, modifier = Modifier.size(16.dp))
                            Text("Aún no hay comentarios para este bus.", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            reportesList.forEach { rep ->
                                val texto = rep.replace(Regex("""^\[Bus: [^\]]+\]\s*"""), "")
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate800).padding(12.dp),
                                    verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, tint = Slate500, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Text(texto, style = MaterialTheme.typography.bodySmall, color = Slate300)
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
                            Text("Bus: ${auto.placa.ifBlank { "Sin placa" }}", style = MaterialTheme.typography.bodySmall, color = Slate400)
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
                if (enviado) {
                    Button(onClick = { showReportDialog = false; selectedAuto = null; enviado = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Brand500), shape = RoundedCornerShape(10.dp)) { Text("Cerrar") }
                } else {
                    Button(
                        onClick = {
                            if (reportText.isNotBlank()) {
                                isLoading = true
                                val desc = if (auto != null && auto.placa.isNotBlank()) "[Bus: ${auto.placa}] $reportText" else reportText
                                autosViewModel.submitReporte(desc) { ok -> isLoading = false; if (ok) enviado = true }
                            }
                        },
                        enabled = reportText.isNotBlank() && !isLoading,
                        colors  = ButtonDefaults.buttonColors(containerColor = Brand500),
                        shape   = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Enviar")
                    }
                }
            },
            dismissButton = {
                if (!enviado) TextButton(onClick = { showReportDialog = false; reportText = "" }) { Text("Cancelar", color = Slate400) }
            }
        )
    }
}

@Composable
private fun BottomSheetOpcion(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Slate500)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Slate600, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AutoBusCard(auto: Auto, userLocation: Location?, lineaNombre: String? = null, destacado: Boolean = false, onClick: () -> Unit) {
    val distanciaKm: Double? = remember(auto.lat, auto.lng, userLocation) {
        if (userLocation != null && (auto.lat != 0.0 || auto.lng != 0.0)) {
            val r = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, auto.lat, auto.lng, r)
            r[0] / 1000.0
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = if (destacado) Brand500.copy(alpha = 0.10f) else SurfaceTinted),
        shape    = RoundedCornerShape(16.dp),
        border   = if (destacado) androidx.compose.foundation.BorderStroke(1.dp, Brand500.copy(alpha = 0.45f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brand500.copy(alpha = if (destacado) 0.25f else 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsBus, null, tint = Brand500, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(auto.placa.ifBlank { "Sin placa" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    if (destacado) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Brand500.copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("Tu ruta", style = MaterialTheme.typography.labelSmall, color = Brand400, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (lineaNombre != null) {
                    Text(lineaNombre, style = MaterialTheme.typography.labelSmall, color = if (destacado) Brand400 else Slate500)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Slate500, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(auto.getLastUpdateText(), style = MaterialTheme.typography.bodySmall, color = Slate400)
                    if (distanciaKm != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.NearMe, null, tint = Slate500, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        val txt = if (distanciaKm < 1.0) "${(distanciaKm * 1000).toInt()} m" else "${"%.1f".format(distanciaKm)} km"
                        Text(txt, style = MaterialTheme.typography.bodySmall, color = Brand400)
                    }
                }
            }
            Icon(Icons.Default.MoreVert, null, tint = Slate500, modifier = Modifier.size(20.dp))
        }
    }
}




