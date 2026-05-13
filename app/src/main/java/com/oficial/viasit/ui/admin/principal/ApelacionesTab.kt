package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.ui.theme.*

@Composable
fun ApelacionesTabContent(
    appeals: List<LogEntry>,
    isLoading: Boolean,
    onResolve: (String, String, String, Boolean) -> Unit
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
                AppealItem(appeal = appeal, onResolve = { id, userId, accept -> 
                    onResolve(id, userId, appeal.userName, accept) 
                })
            }
        }
    }
}

@Composable
fun AppealItem(appeal: LogEntry, onResolve: (String, String, Boolean) -> Unit) {
    // Sincronizar el estado inicial con lo que viene de la DB (mapeado en type)
    var resolutionState by remember(appeal.id, appeal.type) { 
        mutableStateOf(
            when(appeal.type) {
                "success" -> true
                "error" -> false
                else -> null
            }
        ) 
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Gavel, 
                    null, 
                    tint = when(appeal.type) {
                        "success" -> Emerald400
                        "error" -> Rose500
                        else -> Brand400
                    }, 
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Solicitud de Apelación",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (resolutionState != null) (if(resolutionState!!) Emerald400 else Rose500) else Brand400,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Usuario: ${appeal.userName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ID: ${appeal.userId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }

                if (resolutionState == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = { 
                                resolutionState = false
                                onResolve(appeal.id, appeal.userId, false) 
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Rose500.copy(0.1f),
                                contentColor = Rose500
                            )
                        ) {
                            Icon(Icons.Default.Close, "Rechazar")
                        }
                        FilledTonalIconButton(
                            onClick = { 
                                resolutionState = true
                                onResolve(appeal.id, appeal.userId, true) 
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Emerald400.copy(0.1f),
                                contentColor = Emerald400
                            )
                        ) {
                            Icon(Icons.Default.Check, "Aceptar")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (resolutionState == true) Emerald400.copy(0.1f) else Rose500.copy(0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (resolutionState == true) "ACEPTADA" else "RECHAZADA",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (resolutionState == true) Emerald400 else Rose500,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
