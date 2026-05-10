package com.oficial.viasit.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditoriaTabContent(
    logs: List<LogEntry>,
    isLoading: Boolean
) {
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
            items(logs) { log ->
                LogEntryItem(log = log)
            }
        }
    }
}

// ── Entrada de log ────────────────────────────────────────────────────────────
@Composable
fun LogEntryItem(log: LogEntry) {
    // SimpleDateFormat solo para formatear la fecha al mostrar; no necesita ser thread-safe aquí
    val displayFormat = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Punto indicador
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
                color = Slate200,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ID: ${log.userId.take(8)}…",
                    style = MaterialTheme.typography.labelSmall, color = Slate600)
                Text(
                    // Intentar parsear la fecha de PocketBase (yyyy-MM-dd HH:mm:ss.SSSZ)
                    try {
                        val parseFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val d = parseFmt.parse(log.created)
                        if (d != null) displayFormat.format(d) else log.created
                    } catch (e: Exception) { log.created },
                    style = MaterialTheme.typography.labelSmall, color = Slate600
                )
            }
        }
    }
}
