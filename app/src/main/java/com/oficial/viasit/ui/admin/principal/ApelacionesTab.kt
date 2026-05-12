package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.ui.theme.*

@Composable
fun ApelacionesTabContent(
    appeals: List<LogEntry>,
    isLoading: Boolean,
    onResolve: (String, String, Boolean) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
    } else if (appeals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay apelaciones pendientes", color = Slate500)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appeals) { appeal ->
                AppealItem(appeal = appeal, onResolve = onResolve)
            }
        }
    }
}

@Composable
fun AppealItem(appeal: LogEntry, onResolve: (String, String, Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Gavel, null, tint = Brand400, modifier = Modifier.size(20.dp))
                Text(
                    "Solicitud de Apelación",
                    style = MaterialTheme.typography.labelMedium,
                    color = Brand400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    appeal.created.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }

            Text(
                appeal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ID Usuario: ${appeal.userId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { onResolve(appeal.id, appeal.userId, false) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Rose500.copy(0.1f),
                            contentColor = Rose500
                        )
                    ) {
                        Icon(Icons.Default.Close, "Rechazar")
                    }
                    FilledTonalIconButton(
                        onClick = { onResolve(appeal.id, appeal.userId, true) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Emerald400.copy(0.1f),
                            contentColor = Emerald400
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aceptar")
                    }
                }
            }
        }
    }
}
