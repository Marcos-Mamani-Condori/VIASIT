package com.oficial.viasit.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oficial.viasit.AutosAplicacion
import com.oficial.viasit.data.remote.PocketBaseAuthClient
import com.oficial.viasit.data.repository.AdminRepository
import com.oficial.viasit.data.repository.AuthRepository
import com.oficial.viasit.domain.model.AdminRegisterRequest
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.ui.theme.*
import kotlinx.coroutines.launch

private val Slate500 = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAdminScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    authState: AuthState
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var invitationCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("ADMIN_LINEA") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var codeValidated by remember { mutableStateOf(false) }
    var validatedCodeRole by remember { mutableStateOf("") }
    var validatedCodeLineaId by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val authClient = remember { PocketBaseAuthClient() }
    val adminRepository = remember { AdminRepository() }
    val authRepository = remember { AutosAplicacion.instance.authRepository }

    // Auto-validate cuando el código tiene longitud suficiente
    LaunchedEffect(invitationCode) {
        if (invitationCode.length >= 12) {
            val result = adminRepository.validateInvitationCode(invitationCode)
            result.fold(
                onSuccess = { code ->
                    if (code != null) {
                        android.util.Log.d("RegisterAdmin", "Código validado: role=${code.role}, lineaId=${code.lineaId}")
                        codeValidated = true
                        validatedCodeRole = code.role
                        validatedCodeLineaId = code.lineaId
                        selectedRole = code.role
                    } else {
                        codeValidated = false
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("RegisterAdmin", "Error validando código: ${error.message}")
                    codeValidated = false
                }
            )
        } else {
            codeValidated = false
        }
    }

    fun validateCode() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = adminRepository.validateInvitationCode(invitationCode)
            result.fold(
                onSuccess = { code ->
                    if (code != null) {
                        codeValidated = true
                        validatedCodeRole = code.role
                        validatedCodeLineaId = code.lineaId
                        selectedRole = code.role
                        successMessage = "Código válido: ${code.role}"
                    } else {
                        codeValidated = false
                        errorMessage = "Código no válido o ya utilizado"
                    }
                },
                onFailure = { error ->
                    codeValidated = false
                    errorMessage = error.message ?: "Error al validar código"
                }
            )
            isLoading = false
        }
    }

    fun register() {
        if (!codeValidated) { errorMessage = "Valida primero el código de invitación"; return }
        if (password != confirmPassword) { errorMessage = "Las contraseñas no coinciden"; return }
        if (password.length < 8) { errorMessage = "La contraseña debe tener al menos 8 caracteres"; return }

        scope.launch {
            isLoading = true
            errorMessage = null

            android.util.Log.d("RegisterAdmin", "Iniciando registro con role=$validatedCodeRole, lineaId=$validatedCodeLineaId")

            val request = AdminRegisterRequest(
                email = email,
                password = password,
                passwordConfirm = confirmPassword,
                name = name,
                phone = phone,
                role = validatedCodeRole,
                invitationCode = invitationCode
            )

            val result = authClient.registerAdmin(
                email = request.email,
                password = request.password,
                passwordConfirm = request.passwordConfirm,
                name = request.name,
                phone = request.phone,
                role = request.role,
                invitationCode = request.invitationCode,
                lineaId = validatedCodeLineaId
            )

            result.fold(
                onSuccess = { user ->
                    // Marcar el código como usado
                    val codeValidation = adminRepository.validateInvitationCode(invitationCode)
                    codeValidation.fold(
                        onSuccess = { code ->
                            if (code != null) adminRepository.useInvitationCode(code.id, email)
                        },
                        onFailure = { }
                    )
                    
                    // Hacer login automáticamente a través de AuthRepository para actualizar el estado
                    val loginResult = authRepository.login(email, password)
                    loginResult.fold(
                        onSuccess = { loggedInUser ->
                            android.util.Log.d("RegisterAdmin", "Login automático exitoso: ${loggedInUser.email}, lineaId: ${loggedInUser.lineaId}")
                            successMessage = "Registro exitoso"
                            onRegisterSuccess()
                        },
                        onFailure = { loginError ->
                            android.util.Log.e("RegisterAdmin", "Error en login automático: ${loginError.message}")
                            // Aún así navegamos, el usuario puede hacer login manualmente
                            onRegisterSuccess()
                        }
                    )
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Error al registrar"
                }
            )
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Registrar administrador",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Slate400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Brand500, Brand400))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null,
                        tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text("Cuenta de administrador",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Requiere código de invitación",
                        style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
            }

            // ── Error / Success ───────────────────────────────────────────────
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2D0A12))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Rose500,
                            modifier = Modifier.size(18.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB3B3))
                    }
                }
            }

            // ── Código de invitación ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Key, null, tint = Amber400,
                        modifier = Modifier.size(18.dp))
                    Text("Código de invitación",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = invitationCode,
                    onValueChange = { invitationCode = it.uppercase() },
                    label = { Text("Ingresa el código") },
                    placeholder = { Text("XXXXXXXXXXXX", color = Slate600) },
                    singleLine = true,
                    isError = invitationCode.isNotEmpty() && !codeValidated,
                    supportingText = when {
                        codeValidated -> {{ Text("✓ Código válido para: $validatedCodeRole",
                            color = Emerald400, style = MaterialTheme.typography.labelSmall) }}
                        invitationCode.isNotEmpty() && !codeValidated ->
                            {{ Text("Código no válido o expirado",
                                style = MaterialTheme.typography.labelSmall) }}
                        else -> null
                    },
                    trailingIcon = {
                        when {
                            codeValidated -> Icon(Icons.Default.CheckCircle, null,
                                tint = Emerald400)
                            invitationCode.isNotEmpty() -> TextButton(onClick = { validateCode() }) {
                                Text("Validar", color = Brand400,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (codeValidated) Emerald400 else Amber400,
                        unfocusedBorderColor = Slate700,
                        errorBorderColor = Rose500,
                        focusedLabelColor = Amber400,
                        unfocusedLabelColor = Slate400,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        cursorColor = Amber400,
                        focusedContainerColor = SurfaceTinted,
                        unfocusedContainerColor = SurfaceTinted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Badge de rol
                AnimatedVisibility(visible = codeValidated) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brand500.copy(0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, null,
                            tint = Brand400, modifier = Modifier.size(16.dp))
                        Text(
                            if (validatedCodeRole == "ADMIN_PRINCIPAL")
                                "Administrador Principal" else "Administrador de Línea",
                            style = MaterialTheme.typography.labelMedium,
                            color = Brand300, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Información personal ──────────────────────────────────────────
            Text("Información personal",
                style = MaterialTheme.typography.labelMedium, color = Slate600)

            AdminRegTextField(
                value = name, onValueChange = { name = it },
                label = "Nombre completo",
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Slate400,
                    modifier = Modifier.size(20.dp)) }
            )

            AdminRegTextField(
                value = email, onValueChange = { email = it },
                label = "Correo electrónico",
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Slate400,
                    modifier = Modifier.size(20.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            AdminRegTextField(
                value = phone, onValueChange = { phone = it },
                label = "Teléfono (opcional)",
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = Slate400,
                    modifier = Modifier.size(20.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // ── Contraseña ────────────────────────────────────────────────────
            Text("Contraseña",
                style = MaterialTheme.typography.labelMedium, color = Slate600)

            AdminRegTextField(
                value = password, onValueChange = { password = it },
                label = "Contraseña",
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Slate400,
                    modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = Slate400, modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = "Mínimo 8 caracteres"
            )

            AdminRegTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = "Confirmar contraseña",
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Slate400,
                    modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = Slate400, modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword)
                    "Las contraseñas no coinciden" else null
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Botón registrar ───────────────────────────────────────────────
            Button(
                onClick = { register() },
                enabled = !isLoading && codeValidated && name.isNotBlank()
                        && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand500, contentColor = Color.White,
                    disabledContainerColor = Slate700, disabledContentColor = Slate500
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp),
                        color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear cuenta de administrador",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Text("Volver", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Campo de texto para RegisterAdmin ───────────────────────────────────────
@Composable
private fun AdminRegTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand500,
            unfocusedBorderColor = Slate700,
            errorBorderColor = Rose500,
            focusedLabelColor = Brand400,
            unfocusedLabelColor = Slate400,
            focusedTextColor = Color.White,
            unfocusedTextColor = Slate200,
            cursorColor = Brand500,
            focusedContainerColor = SurfaceTinted,
            unfocusedContainerColor = SurfaceTinted,
            errorContainerColor = Color(0xFF1A0A0E)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
