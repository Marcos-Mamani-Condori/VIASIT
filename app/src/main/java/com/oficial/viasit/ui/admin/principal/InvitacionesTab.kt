package com.oficial.viasit.ui.admin.principal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.InvitationCode
import com.oficial.viasit.domain.model.Linea
import com.oficial.viasit.ui.admin.DeleteConfirmDialog
import com.oficial.viasit.ui.admin.EmptyState
import com.oficial.viasit.ui.admin.GeneratedCodeBanner
import com.oficial.viasit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitacionesTabContent(
    invitationCodes: List<InvitationCode>,
    lineas: List<Linea> = emptyList(),
    isLoading: Boolean,
    generatedCode: String?,
    onGenerateCode: (role: String, lineaId: String) -> Unit,
    onDeleteCode: (codeId: String) -> Unit
) {
    val context          = LocalContext.current
    val clipboard        = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var selectedRole         by remember { mutableStateOf("ADMIN_LINEA") }
    var selectedLineaId      by remember { mutableStateOf("") }
    var showDeleteDialogFor  by remember { mutableStateOf<String?>(null) }
    var copiedCode           by remember { mutableStateOf<String?>(null) }
    var expandedLineaDrop    by remember { mutableStateOf(false) }

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
        // ── Panel de generación ────────────────────────────────────────────
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
                                selected    = selected,
                                onClick     = { selectedRole = value },
                                label       = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor   = Brand500,
                                    selectedLabelColor       = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = SurfaceTinted,
                                    labelColor     = Slate400
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selected,
                                    selectedBorderColor = Brand500,
                                    borderColor         = Slate700
                                )
                            )
                        }
                }

                // Selector de línea (solo para ADMIN_LINEA)
                if (selectedRole == "ADMIN_LINEA") {
                    Text("Asignar a línea:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                    ExposedDropdownMenuBox(
                        expanded         = expandedLineaDrop,
                        onExpandedChange = { expandedLineaDrop = !expandedLineaDrop }
                    ) {
                        OutlinedTextField(
                            value         = lineas.find { it.id == selectedLineaId }?.name ?: "Seleccionar línea",
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLineaDrop) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = Brand500,
                                unfocusedBorderColor    = Slate700,
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Slate200,
                                focusedContainerColor   = SurfaceTinted,
                                unfocusedContainerColor = SurfaceTinted
                            )
                        )
                        ExposedDropdownMenu(
                            expanded         = expandedLineaDrop,
                            onDismissRequest = { expandedLineaDrop = false },
                            containerColor   = Slate800
                        ) {
                            lineas.forEach { linea ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(linea.name, color = Color.White)
                                            Text("Código: ${linea.code}",
                                                style = MaterialTheme.typography.labelSmall, color = Slate400)
                                        }
                                    },
                                    onClick = { selectedLineaId = linea.id; expandedLineaDrop = false },
                                    leadingIcon = {
                                        if (selectedLineaId == linea.id) Icon(Icons.Default.Check, null, tint = Brand400)
                                    }
                                )
                            }
                        }
                    }
                    if (selectedLineaId.isEmpty()) {
                        Text("⚠️ Debes seleccionar una línea",
                            style = MaterialTheme.typography.labelSmall, color = Amber400)
                    }
                }

                Button(
                    onClick  = { onGenerateCode(selectedRole, selectedLineaId) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500),
                    enabled  = selectedRole != "ADMIN_LINEA" || selectedLineaId.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar código", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Código recién generado ─────────────────────────────────────────
        generatedCode?.let { code ->
            item {
                GeneratedCodeBanner(
                    code       = code,
                    copiedCode = copiedCode,
                    onCopy     = { c ->
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código invitación", c))
                        copiedCode = c
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item { Text("Códigos generados", style = MaterialTheme.typography.labelMedium, color = Slate600) }

        if (invitationCodes.isEmpty()) {
            item { EmptyState("No hay códigos generados") }
        } else {
            items(invitationCodes) { code ->
                InvitationCodeItem(
                    code     = code,
                    onCopy   = { clipboard.setPrimaryClip(ClipData.newPlainText("Código", code.code)) },
                    onDelete = { showDeleteDialogFor = code.id }
                )
            }
        }
    }

    showDeleteDialogFor?.let { codeId ->
        DeleteConfirmDialog(
            title     = "Eliminar código",
            onConfirm = { onDeleteCode(codeId); showDeleteDialogFor = null },
            onDismiss = { showDeleteDialogFor = null }
        )
    }
}

// ── Item de código de invitación ──────────────────────────────────────────────
@Composable
fun InvitationCodeItem(code: InvitationCode, onCopy: () -> Unit, onDelete: () -> Unit) {
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
                .background(if (code.isUsed) Slate700.copy(0.4f) else Amber500.copy(0.12f)),
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
            Text(code.code, style = MaterialTheme.typography.titleSmall,
                color = if (code.isUsed) Slate500 else Color.White, fontWeight = FontWeight.SemiBold)
            Text(code.role, style = MaterialTheme.typography.labelSmall, color = Slate600)
        }
        IconButton(onClick = onCopy,   modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, "Copiar",   tint = Slate400, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete,      "Eliminar", tint = Rose500,  modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (code.isUsed) Slate800 else Emerald500.copy(0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(if (code.isUsed) "Usado" else "Disponible",
                style = MaterialTheme.typography.labelSmall,
                color = if (code.isUsed) Slate500 else Emerald400,
                fontWeight = FontWeight.SemiBold)
        }
    }
}
