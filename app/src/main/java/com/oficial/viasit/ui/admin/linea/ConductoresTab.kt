package com.oficial.viasit.ui.admin.linea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oficial.viasit.domain.model.Auto
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.ui.theme.*

@Composable
fun ConductoresTabContent(
    users: List<User>,
    autos: List<Auto> = emptyList(),
    isLoading: Boolean,
    onToggleActive: (String, Boolean) -> Unit,
    onShowLocation: (Double, Double) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Brand500)
        }
    } else if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay conductores registrados", color = Slate500)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                val userAuto = autos.find { it.userId == user.id }
                ConductorItem(
                    user = user,
                    auto = userAuto,
                    onToggleActive = onToggleActive,
                    onShowLocation = onShowLocation
                )
            }
        }
    }
}

@Composable
fun ConductorItem(
    user: User,
    auto: Auto?,
    onToggleActive: (String, Boolean) -> Unit,
    onShowLocation: (Double, Double) -> Unit
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
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Text(
                    if (user.active) "Estado: Activo" else "Estado: Suspendido",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.active) Emerald400 else Rose500
                )
                if (auto != null) {
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
                } else {
                    Text(
                        "Sin vehículo asignado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

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
