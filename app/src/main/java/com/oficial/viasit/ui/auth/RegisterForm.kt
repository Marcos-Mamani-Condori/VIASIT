package com.oficial.viasit.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.oficial.viasit.ui.theme.*

/**
 * Formulario de registro de nuevos usuarios.
 *
 * Campos:
 *  - Nombre completo
 *  - Correo electrónico
 *  - Teléfono (opcional)
 *  - Tipo de usuario (Pasajero / Conductor) → chips
 *  - Código de invitación (solo para conductores, animated)
 *  - Contraseña + confirmar contraseña
 *
 * Los conductores necesitan un código de invitación provisto por
 * el ADMIN_LINEA de su línea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterForm(
    email: String,
    password: String,
    confirmPassword: String,
    name: String,
    phone: String,
    role: String,
    invitationCode: String,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    nameError: String?,
    isLoading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onInvitationCodeChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onShowLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val roleOptions = listOf("usuario" to "Pasajero", "conductor" to "Conductor")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("Completa los datos para registrarte", style = MaterialTheme.typography.bodyMedium, color = Slate400)

        Spacer(modifier = Modifier.height(2.dp))

        error?.let { ErrorCard(it) }

        ViasitTextField(value = name, onValueChange = onNameChange, label = "Nombre completo",
            leadingIcon = { Icon(Icons.Default.Person, null, tint = if (nameError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            isError = nameError != null, supportingText = nameError
        )

        ViasitTextField(value = email, onValueChange = onEmailChange, label = "Correo electrónico",
            leadingIcon = { Icon(Icons.Default.Email, null, tint = if (emailError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            isError = emailError != null, supportingText = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ViasitTextField(value = phone, onValueChange = onPhoneChange, label = "Teléfono (opcional)",
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Slate400, modifier = Modifier.size(20.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        // ── Selector de rol (chips) ────────────────────────────────────────
        Column {
            Text("Tipo de usuario", style = MaterialTheme.typography.labelMedium, color = Slate400,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                roleOptions.forEach { (value, label) ->
                    val selected = role == value
                    FilterChip(
                        selected    = selected,
                        onClick     = { onRoleChange(value) },
                        label       = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor   = Brand500,
                            selectedLabelColor       = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = SurfaceTinted,
                            labelColor     = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = selected,
                            selectedBorderColor = Brand500, borderColor = Slate700
                        )
                    )
                }
            }
        }

        // ── Código de invitación (solo conductores) ────────────────────────
        AnimatedVisibility(
            visible = role == "conductor",
            enter = expandVertically() + fadeIn(),
            exit  = shrinkVertically() + fadeOut()
        ) {
            ViasitTextField(
                value = invitationCode, onValueChange = onInvitationCodeChange,
                label = "Código de invitación",
                leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Amber400, modifier = Modifier.size(20.dp)) },
                supportingText = "Proporcionado por el administrador de línea"
            )
        }

        ViasitTextField(value = password, onValueChange = onPasswordChange, label = "Contraseña",
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (passwordError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate400, modifier = Modifier.size(20.dp))
                }
            },
            isError = passwordError != null,
            supportingText = passwordError ?: "Mínimo 8 caracteres",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        ViasitTextField(value = confirmPassword, onValueChange = onConfirmPasswordChange, label = "Confirmar contraseña",
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (confirmPasswordError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            isError = confirmPasswordError != null, supportingText = confirmPasswordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(4.dp))

        ViasitPrimaryButton(text = "Crear cuenta", onClick = onRegisterClick, isLoading = isLoading)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿Ya tienes cuenta? ", style = MaterialTheme.typography.bodySmall, color = Slate400)
            Text("Inicia sesión", style = MaterialTheme.typography.bodySmall, color = Brand400,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onShowLogin() })
        }
    }
}
