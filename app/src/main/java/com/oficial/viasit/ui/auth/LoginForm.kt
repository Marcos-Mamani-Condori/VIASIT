package com.oficial.viasit.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oficial.viasit.ui.theme.*

@Composable
fun LoginForm(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    error: String?,
    successMessage: String? = null,
    isSuspended: Boolean = false,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onShowRegister: () -> Unit,
    onGuestClick: () -> Unit,
    onRegisterAdmin: () -> Unit = {},
    onSubmitAppeal: (String) -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var appealReason by remember { mutableStateOf("") }

    if (showAppealDialog) {
        AlertDialog(
            onDismissRequest = { showAppealDialog = false },
            title = { Text("Enviar Apelación") },
            text = {
                Column {
                    Text("Explica por qué deberíamos reactivar tu cuenta:", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    ViasitTextField(
                        value = appealReason,
                        onValueChange = { appealReason = it },
                        label = "Motivo",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitAppeal(appealReason)
                        showAppealDialog = false
                    },
                    enabled = appealReason.isNotBlank()
                ) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showAppealDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bienvenido", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("Inicia sesión para continuar", style = MaterialTheme.typography.bodyMedium, color = Slate400)

        Spacer(modifier = Modifier.height(4.dp))

        error?.let { ErrorCard(it) }
        successMessage?.let { 
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Brand500.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Brand500.copy(alpha = 0.5f))
            ) {
                Text(it, color = Brand400, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (isSuspended) {
            Button(
                onClick = { showAppealDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)) // Rose 600
            ) {
                Icon(Icons.Default.Gavel, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apelar Suspensión")
            }
        }

        ViasitTextField(
            value = email, onValueChange = onEmailChange, label = "Correo electrónico",
            leadingIcon = { Icon(Icons.Default.Email, null, tint = if (emailError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            isError = emailError != null, supportingText = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ViasitTextField(
            value = password, onValueChange = onPasswordChange, label = "Contraseña",
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (passwordError != null) Rose500 else Slate400, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate400, modifier = Modifier.size(20.dp))
                }
            },
            isError = passwordError != null, supportingText = passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(4.dp))

        ViasitPrimaryButton(text = "Iniciar sesión", onClick = onLoginClick, isLoading = isLoading)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate700)
            Text("o", style = MaterialTheme.typography.bodySmall, color = Slate600)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate700)
        }

        OutlinedButton(
            onClick  = onGuestClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
            border   = BorderStroke(1.dp, Slate700)
        ) {
            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver mapa como invitado", style = MaterialTheme.typography.labelLarge)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta? ", style = MaterialTheme.typography.bodySmall, color = Slate400)
            Text("Regístrate", style = MaterialTheme.typography.bodySmall, color = Brand400, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onShowRegister() })
        }

        Text(
            text = "Registrar administrador",
            style = MaterialTheme.typography.labelMedium, color = Brand400, fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().clickable { onRegisterAdmin() }.padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}
