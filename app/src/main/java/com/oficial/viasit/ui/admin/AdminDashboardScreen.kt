package com.oficial.viasit.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.domain.model.Log
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

private val Slate500 = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User,
    onLogout: () -> Unit,
    onNavigateToRoutePicker: ((String, String) -> Unit)? = null,
    viewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Tabs diferentes según el rol
    val tabs = if (currentUser.isAdminPrincipal) {
        listOf("Líneas", "Invitaciones", "Logs")
    } else {
        listOf("Mi Línea", "Códigos Vehículo", "Logs")
    }

    LaunchedEffect(Unit) {
        if (currentUser.isAdminPrincipal) {
            viewModel.loadLineas()
            viewModel.loadInvitationCodes()
        } else if (currentUser.lineaId.isNotEmpty()) {
            viewModel.loadVehicleInvitationCodes(currentUser.lineaId)
        }
        viewModel.loadLogs()
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Brand500, Brand400))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, null,
                                tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("VIASIT Admin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black, color = Color.White)
                            Text(
                                if (currentUser.isAdminPrincipal) "Admin Principal" else "Admin de Línea",
                                style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = Slate400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── User info banner ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brand500.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentUser.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                            style = MaterialTheme.typography.titleMedium,
                            color = Brand300, fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUser.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(currentUser.email,
                            style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    if (currentUser.isAdminPrincipal) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brand500.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Principal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Brand300, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = Brand400,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Brand500,
                        height = 2.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) Brand400 else Slate500)
                        },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Default.Route
                                    1 -> Icons.Default.Key
                                    else -> Icons.Default.History
                                },
                                null,
                                tint = if (selectedTab == index) Brand400 else Slate600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            if (currentUser.isAdminPrincipal) {
                // ADMIN_PRINCIPAL: Tabs originales
                when (selectedTab) {
                    0 -> LineasTabContent(
                        lineas = uiState.lineas,
                        isLoading = uiState.isLoading,
                        isAdminPrincipal = true,
                        onCreateLinea = { name, code, rutaId -> viewModel.createLinea(name, code, rutaId, currentUser.id) },
                        onDeleteLinea = { viewModel.deleteLinea(it, currentUser.id) }
                    )
                    1 -> InvitacionesTabContent(
                        invitationCodes = uiState.invitationCodes,
                        lineas = uiState.lineas,
                        isLoading = uiState.isLoading,
                        generatedCode = uiState.generatedCode,
                        onGenerateCode = { role, lineaId -> viewModel.generateInvitationCode(role, lineaId, 24, currentUser.id) },
                        onDeleteCode = { viewModel.deleteInvitationCode(it, currentUser.id) }
                    )
                    2 -> AuditoriaTabContent(
                        logs = uiState.logs,
                        isLoading = uiState.isLoading
                    )
                }
            } else {
                // ADMIN_LINEA: Tabs específicos
                when (selectedTab) {
                    0 -> MiLineaTabContent(
                        lineaId = currentUser.lineaId,
                        lineas = uiState.lineas,
                        isLoading = uiState.isLoading,
                        onCreateRuta = { name, desc, start, end, lineaId -> 
                            viewModel.createRuta(name, desc, start, end, lineaId, currentUser.id)
                        },
                        onNavigateToRoutePicker = onNavigateToRoutePicker
                    )
                    1 -> VehicleCodesTabContent(
                        lineaId = currentUser.lineaId,
                        vehicleCodes = uiState.vehicleCodes,
                        isLoading = uiState.isLoading,
                        generatedCode = uiState.generatedVehicleCode,
                        onGenerateCode = { 
                            viewModel.generateVehicleInvitationCode(currentUser.lineaId, currentUser.id, 72, currentUser.id)
                        },
                        onDeleteCode = { viewModel.deleteVehicleInvitationCode(it, currentUser.lineaId, currentUser.id) }
                    )
                    2 -> AuditoriaTabContent(
                        logs = uiState.logs,
                        isLoading = uiState.isLoading
                    )
                }
            }

            // ── Snackbars ─────────────────────────────────────────────────────
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF2D0A12),
                    contentColor = Color(0xFFFFB3B3),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK", color = Rose500)
                        }
                    }
                ) { Text(error, style = MaterialTheme.typography.bodySmall) }
            }

            uiState.successMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF064E3B),
                    contentColor = Emerald400,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK", color = Emerald400)
                        }
                    }
                ) { Text(msg, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Inbox, null, tint = Slate700,
                modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = Slate600, textAlign = TextAlign.Center)
        }
    }
}

// ─── Líneas Tab ───────────────────────────────────────────────────────────────
@Composable
fun LineasTabContent(
    lineas: List<Linea>,
    isLoading: Boolean,
    isAdminPrincipal: Boolean = false,
    onCreateLinea: (String, String, String) -> Unit,
    onDeleteLinea: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

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
        if (isAdminPrincipal) {
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva línea", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (lineas.isEmpty()) {
            item { EmptyState("No hay líneas creadas") }
        } else {
            items(lineas) { linea ->
                LineaCard(linea = linea, onDelete = { showDeleteDialog = linea.id })
            }
        }
    }

    // Diálogo crear
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var rutaId by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Nueva línea", style = MaterialTheme.typography.headlineSmall,
                color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                    AdminTextField(value = code, onValueChange = { code = it }, label = "Código")
                    AdminTextField(value = rutaId, onValueChange = { rutaId = it },
                        label = "ID de Ruta (opcional)")
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
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = Slate400)
                }
            }
        )
    }

    // Diálogo eliminar
    showDeleteDialog?.let { lineaId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar línea", style = MaterialTheme.typography.headlineSmall,
                color = Color.White) },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium, color = Slate400) },
            confirmButton = {
                Button(
                    onClick = { onDeleteLinea(lineaId); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar", color = Slate400)
                }
            }
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
            Icon(Icons.Default.Route, null, tint = Brand400,
                modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(linea.name, style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Código: ${linea.code}",
                style = MaterialTheme.typography.bodySmall, color = Slate400)
            if (linea.rutaId.isNotBlank()) {
                Text("Ruta: ${linea.rutaId}",
                    style = MaterialTheme.typography.labelSmall, color = Brand400)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Eliminar", tint = Rose500,
                modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Invitaciones Tab ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitacionesTabContent(
    invitationCodes: List<InvitationCode>,
    lineas: List<Linea> = emptyList(),
    isLoading: Boolean,
    generatedCode: String?,
    onGenerateCode: (String, String) -> Unit, // (role, lineaId)
    onDeleteCode: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var selectedRole by remember { mutableStateOf("ADMIN_LINEA") }
    var selectedLineaId by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var copiedCode by remember { mutableStateOf<String?>(null) }
    var expandedLineaSelector by remember { mutableStateOf(false) }

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
        // Generador
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Generar código de invitación",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)

                // Selector de rol
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ADMIN_LINEA" to "Admin Línea", "ADMIN_PRINCIPAL" to "Admin Principal")
                        .forEach { (value, label) ->
                            val selected = selectedRole == value
                            FilterChip(
                                selected = selected,
                                onClick = { selectedRole = value },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Brand500,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = SurfaceTinted,
                                    labelColor = Slate400
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selected,
                                    selectedBorderColor = Brand500, borderColor = Slate700
                                )
                            )
                        }
                }

                // Selector de línea (solo visible si es ADMIN_LINEA)
                if (selectedRole == "ADMIN_LINEA") {
                    Text("Asignar a línea:",
                        style = MaterialTheme.typography.labelMedium, color = Slate400)
                    
                    // Dropdown para seleccionar línea
                    ExposedDropdownMenuBox(
                        expanded = expandedLineaSelector,
                        onExpandedChange = { expandedLineaSelector = !expandedLineaSelector }
                    ) {
                        OutlinedTextField(
                            value = lineas.find { it.id == selectedLineaId }?.name ?: "Seleccionar línea",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLineaSelector)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Slate200,
                                focusedContainerColor = SurfaceTinted,
                                unfocusedContainerColor = SurfaceTinted
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLineaSelector,
                            onDismissRequest = { expandedLineaSelector = false },
                            containerColor = Slate800
                        ) {
                            lineas.forEach { linea ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(linea.name, color = Color.White)
                                            Text("Código: ${linea.code}", 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = Slate400)
                                        }
                                    },
                                    onClick = {
                                        selectedLineaId = linea.id
                                        expandedLineaSelector = false
                                    },
                                    leadingIcon = {
                                        if (selectedLineaId == linea.id) {
                                            Icon(Icons.Default.Check, null, tint = Brand400)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    if (selectedLineaId.isEmpty()) {
                        Text("⚠️ Debes seleccionar una línea para el admin",
                            style = MaterialTheme.typography.labelSmall, color = Amber400)
                    }
                }

                Button(
                    onClick = { onGenerateCode(selectedRole, selectedLineaId) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled = selectedRole != "ADMIN_LINEA" || selectedLineaId.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar código", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Show generated code with copy button
        generatedCode?.let { code ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Emerald500.copy(0.12f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Código generado:",
                        style = MaterialTheme.typography.labelSmall, color = Emerald400)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(code,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp))
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Código de invitación", code)
                                clipboardManager.setPrimaryClip(clip)
                                copiedCode = code
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Emerald500.copy(0.2f))
                        ) {
                            Icon(
                                if (copiedCode == code) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copiar código",
                                tint = if (copiedCode == code) Emerald400 else Emerald300,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (copiedCode == code) {
                        Text("¡Copiado al portapapeles!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald400)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Text("Códigos generados",
                style = MaterialTheme.typography.labelMedium, color = Slate600)
        }

        if (invitationCodes.isEmpty()) {
            item { EmptyState("No hay códigos generados") }
        } else {
            items(invitationCodes) { code -> 
                InvitationCodeItem(
                    code = code,
                    onCopy = {
                        val clip = ClipData.newPlainText("Código de invitación", code.code)
                        clipboardManager.setPrimaryClip(clip)
                    },
                    onDelete = { showDeleteDialog = code.id }
                )
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { codeId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar código", style = MaterialTheme.typography.headlineSmall,
                color = Color.White) },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium, color = Slate400) },
            confirmButton = {
                Button(
                    onClick = { onDeleteCode(codeId); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun InvitationCodeItem(
    code: InvitationCode,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (code.isUsed) Slate700.copy(0.4f) else Amber500.copy(0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (code.isUsed) Icons.Default.LockOpen else Icons.Default.Key,
                null,
                tint = if (code.isUsed) Slate500 else Amber400,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(code.code,
                style = MaterialTheme.typography.titleSmall,
                color = if (code.isUsed) Slate500 else Color.White,
                fontWeight = FontWeight.SemiBold)
            Text(code.role,
                style = MaterialTheme.typography.labelSmall, color = Slate600)
        }
        // Copy button
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.ContentCopy, "Copiar código",
                tint = Slate400, modifier = Modifier.size(18.dp))
        }
        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Delete, "Eliminar",
                tint = Rose500, modifier = Modifier.size(18.dp))
        }
        // Status badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (code.isUsed) Slate800 else Emerald500.copy(0.15f)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                if (code.isUsed) "Usado" else "Disponible",
                style = MaterialTheme.typography.labelSmall,
                color = if (code.isUsed) Slate500 else Emerald400,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Auditoría Tab ────────────────────────────────────────────────────────────
@Composable
fun AuditoriaTabContent(logs: List<LogEntry>, isLoading: Boolean) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (logs.isEmpty()) {
            item { EmptyState("No hay registros de actividad") }
        } else {
            items(logs) { log -> LogEntryItem(log = log) }
        }
    }
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Brand500)
                .padding(top = 6.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(log.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate200, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ID: ${log.userId.take(8)}…",
                    style = MaterialTheme.typography.labelSmall, color = Slate600)
                Text(
                    try {
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val d = fmt.parse(log.created)
                        if (d != null) dateFormat.format(d) else log.created
                    } catch (e: Exception) { log.created },
                    style = MaterialTheme.typography.labelSmall, color = Slate600
                )
            }
        }
    }
}

// ─── Admin TextField ──────────────────────────────────────────────────────────
@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand500,
            unfocusedBorderColor = Slate700,
            focusedLabelColor = Brand400,
            unfocusedLabelColor = Slate400,
            focusedTextColor = Color.White,
            unfocusedTextColor = Slate200,
            cursorColor = Brand500,
            focusedContainerColor = SurfaceTinted,
            unfocusedContainerColor = SurfaceTinted
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ─── Mi Línea Tab (ADMIN_LINEA) ───────────────────────────────────────────────
@Composable
fun MiLineaTabContent(
    lineaId: String,
    lineas: List<Linea>,
    isLoading: Boolean,
    onCreateRuta: (String, String, String, String, String) -> Unit,
    onNavigateToRoutePicker: ((String, String) -> Unit)? = null
) {
    var showCreateRutaDialog by remember { mutableStateOf(false) }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
        return
    }

    // Encontrar la línea asignada
    val miLinea = lineas.find { it.id == lineaId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Info de la línea
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

        // Crear ruta con mapa
        item {
            if (onNavigateToRoutePicker != null && miLinea != null) {
                // New map-based route creation
                Button(
                    onClick = { onNavigateToRoutePicker(lineaId, miLinea.name) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Ruta en Mapa", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                }
            } else {
                // Fallback to old dialog method
                Button(
                    onClick = { showCreateRutaDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand500, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Route, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Ruta", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Diálogo crear ruta (fallback)
    if (showCreateRutaDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var startPoint by remember { mutableStateOf("") }
        var endPoint by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateRutaDialog = false },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Nueva Ruta", style = MaterialTheme.typography.headlineSmall,
                color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                    AdminTextField(value = description, onValueChange = { description = it }, label = "Descripción")
                    AdminTextField(value = startPoint, onValueChange = { startPoint = it }, label = "Punto inicio")
                    AdminTextField(value = endPoint, onValueChange = { endPoint = it }, label = "Punto final")
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
                TextButton(onClick = { showCreateRutaDialog = false }) {
                    Text("Cancelar", color = Slate400)
                }
            }
        )
    }
}

// ─── Vehicle Codes Tab (ADMIN_LINEA) ───────────────────────────────────────────
@Composable
fun VehicleCodesTabContent(
    lineaId: String,
    vehicleCodes: List<VehicleInvitationCode>,
    isLoading: Boolean,
    generatedCode: String?,
    onGenerateCode: () -> Unit,
    onDeleteCode: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var copiedCode by remember { mutableStateOf<String?>(null) }

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
        // Generador
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Generar código para vehículos",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)

                Text("Los conductores pueden usar este código para unir su vehículo a tu línea.",
                    style = MaterialTheme.typography.bodySmall, color = Slate400)

                Button(
                    onClick = onGenerateCode,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand500)
                ) {
                    Icon(Icons.Default.DirectionsBus, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar código", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Show generated code
        generatedCode?.let { code ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Emerald500.copy(0.12f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Código generado:",
                        style = MaterialTheme.typography.labelSmall, color = Emerald400)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(code,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Código de vehículo", code)
                                clipboardManager.setPrimaryClip(clip)
                                copiedCode = code
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Emerald500.copy(0.2f))
                        ) {
                            Icon(
                                if (copiedCode == code) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copiar código",
                                tint = if (copiedCode == code) Emerald400 else Emerald300,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Text("Códigos generados",
                style = MaterialTheme.typography.labelMedium, color = Slate600)
        }

        if (vehicleCodes.isEmpty()) {
            item { EmptyState("No hay códigos generados") }
        } else {
            items(vehicleCodes) { code ->
                VehicleCodeItem(
                    code = code,
                    onCopy = {
                        val clip = ClipData.newPlainText("Código de vehículo", code.code)
                        clipboardManager.setPrimaryClip(clip)
                    },
                    onDelete = { showDeleteDialog = code.id }
                )
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { codeId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Slate800,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar código", style = MaterialTheme.typography.headlineSmall,
                color = Color.White) },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium, color = Slate400) },
            confirmButton = {
                Button(
                    onClick = { onDeleteCode(codeId); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun VehicleCodeItem(
    code: VehicleInvitationCode,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (code.usadoPor.isNotEmpty()) Slate700.copy(0.4f) else Cyan500.copy(0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (code.usadoPor.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.DirectionsBus,
                null,
                tint = if (code.usadoPor.isNotEmpty()) Slate500 else Cyan400,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(code.code,
                style = MaterialTheme.typography.titleSmall,
                color = if (code.usadoPor.isNotEmpty()) Slate500 else Color.White,
                fontWeight = FontWeight.SemiBold)
            Text(if (code.usadoPor.isNotEmpty()) "Usado por: ${code.usadoPor}" else "Disponible",
                style = MaterialTheme.typography.labelSmall, color = Slate600)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, "Copiar", tint = Slate400, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, "Eliminar", tint = Rose500, modifier = Modifier.size(18.dp))
        }
    }
}
