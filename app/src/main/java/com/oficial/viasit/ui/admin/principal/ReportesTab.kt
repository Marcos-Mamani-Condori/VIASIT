package com.oficial.viasit.ui.admin.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Reporte
import com.oficial.viasit.ui.theme.*

@Composable
fun ReportesTabContent(
    reportes: List<Reporte>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
    } else if (reportes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay reportes registrados", color = Slate500)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reportes) { reporte ->
                ReporteItem(reporte = reporte)
            }
        }
    }
}

@Composable
fun ReporteItem(reporte: Reporte) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = Amber400, modifier = Modifier.size(20.dp))
                Text(
                    "Reporte de Pasajero",
                    style = MaterialTheme.typography.labelMedium,
                    color = Amber400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    reporte.created.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }

            Text(
                reporte.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("De: ${reporte.reporterName}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Text("Para ID: ${reporte.userid}", style = MaterialTheme.typography.labelSmall, color = Brand400)
                }
            }
        }
    }
}
