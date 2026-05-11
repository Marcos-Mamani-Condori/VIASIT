package com.oficial.viasit.ui.admin.linea

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
import com.oficial.viasit.domain.model.VehicleInvitationCode
import com.oficial.viasit.ui.admin.DeleteConfirmDialog
import com.oficial.viasit.ui.admin.EmptyState
import com.oficial.viasit.ui.admin.GeneratedCodeBanner
import com.oficial.viasit.ui.theme.*


@Composable
fun VehicleCodesTabContent(
    lineaId: String,
    vehicleCodes: List<VehicleInvitationCode>,
    isLoading: Boolean,
    generatedCode: String?,
    onGenerateCode: () -> Unit,
    onDeleteCode: (codeId: String) -> Unit
) {
    val context     = LocalContext.current
    val clipboard   = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var showDeleteDialogFor by remember { mutableStateOf<String?>(null) }
    var copiedCode          by remember { mutableStateOf<String?>(null) }

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
                Text("Los conductores usan este código para unir su vehículo a tu línea.",
                    style = MaterialTheme.typography.bodySmall, color = Slate400)
                Button(
                    onClick  = onGenerateCode,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Brand500)
                ) {
                    Icon(Icons.Default.DirectionsBus, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar código", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }


        generatedCode?.let { code ->
            item {
                GeneratedCodeBanner(
                    code       = code,
                    copiedCode = copiedCode,
                    onCopy     = { c ->
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código vehículo", c))
                        copiedCode = c
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item { Text("Códigos generados", style = MaterialTheme.typography.labelMedium, color = Slate600) }

        if (vehicleCodes.isEmpty()) {
            item { EmptyState("No hay códigos generados") }
        } else {
            items(vehicleCodes) { code ->
                VehicleCodeItem(
                    code     = code,
                    onCopy   = { clipboard.setPrimaryClip(ClipData.newPlainText("Código vehículo", code.code)) },
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


@Composable
fun VehicleCodeItem(code: VehicleInvitationCode, onCopy: () -> Unit, onDelete: () -> Unit) {
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
                .background(if (code.usedBy.isNotEmpty()) Slate700.copy(0.4f) else Cyan500.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (code.usedBy.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.DirectionsBus,
                null,
                tint = if (code.usedBy.isNotEmpty()) Slate500 else Cyan400,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(code.code, style = MaterialTheme.typography.titleSmall,
                color = if (code.usedBy.isNotEmpty()) Slate500 else Color.White,
                fontWeight = FontWeight.SemiBold)
            Text(if (code.usedBy.isNotEmpty()) "Usado por: ${code.usedBy}" else "Disponible",
                style = MaterialTheme.typography.labelSmall, color = Slate600)
        }
        IconButton(onClick = onCopy,   modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, "Copiar",   tint = Slate400, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete,      "Eliminar", tint = Rose500,  modifier = Modifier.size(18.dp))
        }
    }
}
