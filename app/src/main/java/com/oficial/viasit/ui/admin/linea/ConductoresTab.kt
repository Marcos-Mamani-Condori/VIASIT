package com.oficial.viasit.ui.admin.linea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.ui.theme.*

@Composable
fun ConductoresTabContent(
    users: List<User>,
    autos: List<Auto> = emptyList(),
    isLoading: Boolean,
    onToggleActive: (String, String, Boolean) -> Unit,
    onShowLocation: (Double, Double) -> Unit,
    showLocationOnlyForDrivers: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(null) }

    val roles = listOf("usuario", "conductor", "ADMIN_LINEA")

    val filteredUsers = remember(users, searchQuery, selectedRole) {
        users.filter { user ->
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) || 
                               user.email.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRole == null || user.role.equals(selectedRole, ignoreCase = true)
            matchesSearch && matchesRole
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar por nombre o correo...", color = Slate500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate500) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = Brand500,
                unfocusedBorderColor = Slate800
            ),
            singleLine = true
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedRole == null,
                    onClick = { selectedRole = null },
                    label = { Text("Todos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand500,
                        selectedLabelColor = Color.White,
                        labelColor = Slate400,
                        containerColor = Slate900
                    ),
                    border = null
                )
            }
            items(roles) { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(role.replace("_", " ").lowercase().capitalize()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Brand500,
                        selectedLabelColor = Color.White,
                        labelColor = Slate400,
                        containerColor = Slate900
                    ),
                    border = null
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand500)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if(searchQuery.isEmpty() && selectedRole == null) "No hay usuarios" else "Sin resultados", color = Slate500)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUsers) { user ->
                    val userAuto = autos.find { it.userId == user.id }
                    ConductorItem(
                        user = user,
                        auto = userAuto,
                        onToggleActive = { id, active -> onToggleActive(id, user.name, active) },
                        onShowLocation = onShowLocation,
                        showLocationButton = if (showLocationOnlyForDrivers) user.isDriver else true
                    )
                }
            }
        }
    }
}

@Composable
fun ConductorItem(
    user: User,
    auto: Auto?,
    onToggleActive: (String, Boolean) -> Unit,
    onShowLocation: (Double, Double) -> Unit,
    showLocationButton: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (user.active) Brand500.copy(0.1f) else Rose500.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = if (user.active) Brand400 else Rose500
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when(user.role.lowercase()) {
                            "conductor" -> Brand500.copy(0.15f)
                            "admin_linea" -> Amber400.copy(0.15f)
                            else -> Slate700.copy(0.15f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = user.role.replace("_", " ").uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when(user.role.lowercase()) {
                                "conductor" -> Brand400
                                "admin_linea" -> Amber400
                                else -> Slate400
                            },
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                if (user.lineName.isNotBlank()) {
                    Text(
                        "Línea: ${user.lineName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand400,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (user.active) "Estado: Activo" else "Estado: Suspendido",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.active) Emerald400 else Rose500
                )
                if (auto != null && user.isDriver) {
                    Text(
                        "Vehículo: ${auto.placa}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Última vez: ${auto.getLastUpdateText()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

            if (showLocationButton) {
                IconButton(
                    onClick = {
                        if (auto != null && (auto.lat != 0.0 || auto.lng != 0.0)) {
                            onShowLocation(auto.lat, auto.lng)
                        }
                    },
                    enabled = auto != null && (auto.lat != 0.0 || auto.lng != 0.0),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Brand400,
                        disabledContentColor = Slate700
                    )
                ) {
                    Icon(Icons.Default.LocationOn, "Ver en mapa")
                }
            }

            IconButton(
                onClick = { onToggleActive(user.id, !user.active) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (user.active) Rose500 else Emerald400
                )
            ) {
                Icon(
                    if (user.active) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = if (user.active) "Suspender" else "Reactivar"
                )
            }
        }
    }
}
