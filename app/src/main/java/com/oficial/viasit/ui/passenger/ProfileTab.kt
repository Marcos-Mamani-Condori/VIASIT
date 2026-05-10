package com.oficial.viasit.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oficial.viasit.domain.model.User
import com.oficial.viasit.domain.model.UserRole
import com.oficial.viasit.ui.theme.*

@Composable
fun ProfileTab(user: User, onLogout: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header con gradiente ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Brand700, Slate950)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar con inicial
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Brand400, Brand700))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user.name.ifBlank { "Pasajero" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Badge de rol
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Brand500.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = when (user.userRole) {
                            UserRole.invitado -> "Invitado"
                            UserRole.usuario  -> "Pasajero"
                            else              -> user.userRole.name
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand300,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── Información de la cuenta ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Cuenta",
                style = MaterialTheme.typography.labelMedium,
                color = Slate500,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )

            ProfileInfoRow(
                icon     = Icons.Default.Person,
                label    = "Nombre",
                value    = user.name.ifBlank { "—" },
                iconColor = Brand500
            )

            ProfileInfoRow(
                icon     = Icons.Default.Email,
                label    = "Correo",
                value    = user.email.ifBlank { "—" },
                iconColor = Cyan400
            )

            if (user.phone.isNotBlank()) {
                ProfileInfoRow(
                    icon     = Icons.Default.Phone,
                    label    = "Teléfono",
                    value    = user.phone,
                    iconColor = Emerald400
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Aplicación",
                style = MaterialTheme.typography.labelMedium,
                color = Slate500,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            ProfileInfoRow(
                icon      = Icons.Default.Verified,
                label     = "Versión",
                value     = "VIASIT 1.0",
                iconColor = Slate400
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cerrar sesión ────────────────────────────────────────────────
            Button(
                onClick    = onLogout,
                modifier   = Modifier.fillMaxWidth().height(52.dp),
                shape      = RoundedCornerShape(14.dp),
                colors     = ButtonDefaults.buttonColors(
                    containerColor = Rose900,
                    contentColor   = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cerrar sesión",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceTinted)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Slate200)
        }
    }
}
