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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Block
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oficial.viasit.domain.model.LogEntry
import com.oficial.viasit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditoriaTabContent(
    logs: List<LogEntry>,
    isLoading: Boolean,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    selectedFilter: String = "todos",
    onFilterChange: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por acción o ID...", color = Slate500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Brand400) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand500,
                    unfocusedBorderColor = Slate700,
                    cursorColor = Brand500,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "todos",
                    onClick = { onFilterChange("todos") },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = selectedFilter == "hoy",
                    onClick = { onFilterChange("hoy") },
                    label = { Text("Hoy") }
                )
                FilterChip(
                    selected = selectedFilter == "semana",
                    onClick = { onFilterChange("semana") },
                    label = { Text("Semana") }
                )
                FilterChip(
                    selected = selectedFilter == "mes",
                    onClick = { onFilterChange("mes") },
                    label = { Text("Mes") }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand500)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logs.isEmpty()) {
                    item { 
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = Slate700)
                                Spacer(Modifier.height(8.dp))
                                Text("No hay registros que coincidan", color = Slate500)
                            }
                        }
                    }
                } else {
                    items(logs) { log ->
                        LogEntryItem(log = log)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Brand500.copy(0.2f),
            selectedLabelColor = Brand400,
            labelColor = Slate400,
            containerColor = Slate950
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (selected) Brand500 else Slate700,
            borderWidth = 1.dp,
            selectedBorderColor = Brand500,
            selectedBorderWidth = 1.dp,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val displayFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when(log.type) {
                            "danger" -> Rose500.copy(0.15f)
                            "warning" -> Amber500.copy(0.15f)
                            else -> Brand500.copy(0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(log.type) {
                        "danger" -> Icons.Default.Block
                        "warning" -> Icons.Default.Event
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when(log.type) {
                        "danger" -> Rose500
                        "warning" -> Amber500
                        else -> Brand500
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Slate500)
                    Text(
                        "Responsable: ${log.userName.ifEmpty { log.userId.take(if (log.userId.length > 12) 12 else log.userId.length) + "..." }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        val d = inputFormat.parse(log.created)
                        if (d != null) displayFormat.format(d) else log.created
                    } catch (e: Exception) { log.created }
                    
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
            }
        }
    }
}
