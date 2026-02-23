package com.oficial.viasit.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oficial.viasit.ui.theme.*

/**
 * Componentes compartidos por todas las tabs del panel de administración.
 *
 *  - [EmptyState]    → mensaje vacío genérico con ícono
 *  - [AdminTextField] → campo de texto con el estilo oscuro del panel
 *  - [GeneratedCodeBanner] → banner verde que muestra el código recién generado con botón de copiar
 */

/** Estado vacío genérico: ícono + mensaje centrado */
@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Inbox, null, tint = Slate700, modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = Slate600, textAlign = TextAlign.Center)
        }
    }
}

/** TextField con el esquema de colores oscuro del panel admin */
@Composable
fun AdminTextField(
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
            focusedBorderColor    = Brand500,
            unfocusedBorderColor  = Slate700,
            focusedLabelColor     = Brand400,
            unfocusedLabelColor   = Slate400,
            focusedTextColor      = Color.White,
            unfocusedTextColor    = Slate200,
            cursorColor           = Brand500,
            focusedContainerColor    = SurfaceTinted,
            unfocusedContainerColor  = SurfaceTinted
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Banner que muestra el código generado con botón de copiar al portapapeles */
@Composable
fun GeneratedCodeBanner(
    code: String,
    copiedCode: String?,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Emerald500.copy(0.12f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Código generado:", style = MaterialTheme.typography.labelSmall, color = Emerald400)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                code,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(
                    2f, androidx.compose.ui.unit.TextUnitType.Sp
                )
            )
            IconButton(
                onClick = { onCopy(code) },
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
            Text("¡Copiado al portapapeles!", style = MaterialTheme.typography.labelSmall, color = Emerald400)
        }
    }
}

/** Diálogo de confirmación para eliminar con fondo oscuro */
@Composable
fun DeleteConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        },
        text = {
            Text("¿Estás seguro? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium, color = Slate400)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500)
            ) { Text("Eliminar", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Slate400) }
        }
    )
}
