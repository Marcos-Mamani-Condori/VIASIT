package com.oficial.viasit.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oficial.viasit.domain.model.AuthState
import com.oficial.viasit.ui.theme.*
import com.oficial.viasit.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onRegisterAdmin: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val authState by viewModel.authState.collectAsState()
    val loginFormState by viewModel.loginForm.collectAsState()
    val registerFormState by viewModel.registerForm.collectAsState()

    var showRegister by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Slate950, Slate900),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Decoración de fondo — círculo de acento sutil
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 120.dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Brand500.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Brand500, Brand400)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "VIASIT",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Transporte público en tiempo real",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Formulario ────────────────────────────────────────────────────
            AnimatedContent(
                targetState = showRegister,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "auth_form"
            ) { isRegister ->
                if (isRegister) {
                    RegisterForm(
                        email = registerFormState.email,
                        password = registerFormState.password,
                        confirmPassword = registerFormState.confirmPassword,
                        name = registerFormState.name,
                        phone = registerFormState.phone,
                        role = registerFormState.role,
                        invitationCode = registerFormState.invitationCode,
                        emailError = registerFormState.emailError,
                        passwordError = registerFormState.passwordError,
                        confirmPasswordError = registerFormState.confirmPasswordError,
                        nameError = registerFormState.nameError,
                        isLoading = registerFormState.isLoading,
                        error = registerFormState.error,
                        onEmailChange = viewModel::updateRegisterEmail,
                        onPasswordChange = viewModel::updateRegisterPassword,
                        onConfirmPasswordChange = viewModel::updateRegisterConfirmPassword,
                        onNameChange = viewModel::updateRegisterName,
                        onPhoneChange = viewModel::updateRegisterPhone,
                        onRoleChange = viewModel::updateRegisterRole,
                        onInvitationCodeChange = viewModel::updateRegisterInvitationCode,
                        onRegisterClick = viewModel::register,
                        onShowLogin = { showRegister = false }
                    )
                } else {
                    LoginForm(
                        email = loginFormState.email,
                        password = loginFormState.password,
                        emailError = loginFormState.emailError,
                        passwordError = loginFormState.passwordError,
                        isLoading = loginFormState.isLoading,
                        error = loginFormState.error,
                        onEmailChange = viewModel::updateLoginEmail,
                        onPasswordChange = viewModel::updateLoginPassword,
                        onLoginClick = viewModel::login,
                        onShowRegister = { showRegister = true },
                        onGuestClick = viewModel::enterAsGuest,
                        onRegisterAdmin = onRegisterAdmin
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Campo de texto unificado ─────────────────────────────────────────────────
@Composable
private fun ViasitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
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
        modifier = modifier.fillMaxWidth()
    )
}

// ─── Botón primario ───────────────────────────────────────────────────────────
@Composable
private fun ViasitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand500,
            contentColor = Color.White,
            disabledContainerColor = Slate700,
            disabledContentColor = Slate400
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Card de error ────────────────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D0A12))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Rose500,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFFB3B3)
        )
    }
}

// ─── Login Form ───────────────────────────────────────────────────────────────
@Composable
private fun LoginForm(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onShowRegister: () -> Unit,
    onGuestClick: () -> Unit,
    onRegisterAdmin: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título del formulario
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(4.dp))

        error?.let { ErrorCard(it) }

        ViasitTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null,
                    tint = if (emailError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            isError = emailError != null,
            supportingText = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ViasitTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = if (passwordError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            isError = passwordError != null,
            supportingText = passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(4.dp))

        ViasitPrimaryButton(
            text = "Iniciar sesión",
            onClick = onLoginClick,
            isLoading = isLoading
        )

        // Divider con texto
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate700)
            Text("o", style = MaterialTheme.typography.bodySmall, color = Slate600)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate700)
        }

        // Botón invitado
        OutlinedButton(
            onClick = onGuestClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver mapa como invitado", style = MaterialTheme.typography.labelLarge)
        }

        // Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "¿No tienes cuenta? ",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
            Text(
                "Regístrate",
                style = MaterialTheme.typography.bodySmall,
                color = Brand400,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onShowRegister() }
            )
        }

        Text(
            text = "Registrar administrador",
            style = MaterialTheme.typography.labelMedium,
            color = Brand400,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRegisterAdmin() }
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Register Form ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterForm(
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
    var roleExpanded by remember { mutableStateOf(false) }
    val roleOptions = listOf("usuario" to "Pasajero", "conductor" to "Conductor")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = "Completa los datos para registrarte",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(2.dp))

        error?.let { ErrorCard(it) }

        ViasitTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nombre completo",
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null,
                    tint = if (nameError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            isError = nameError != null,
            supportingText = nameError
        )

        ViasitTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null,
                    tint = if (emailError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            isError = emailError != null,
            supportingText = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ViasitTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Teléfono (opcional)",
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null,
                    tint = Slate400, modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        // Role selector — chips en lugar de dropdown
        Column {
            Text(
                "Tipo de usuario",
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                roleOptions.forEach { (value, label) ->
                    val selected = role == value
                    FilterChip(
                        selected = selected,
                        onClick = { onRoleChange(value) },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand500,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = SurfaceTinted,
                            labelColor = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = Brand500,
                            borderColor = Slate700
                        )
                    )
                }
            }
        }

        // Código de invitación — solo para conductores
        AnimatedVisibility(
            visible = role == "conductor",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ViasitTextField(
                value = invitationCode,
                onValueChange = onInvitationCodeChange,
                label = "Código de invitación",
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = null,
                        tint = Amber400, modifier = Modifier.size(20.dp))
                },
                supportingText = "Proporcionado por el administrador de línea"
            )
        }

        ViasitTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = if (passwordError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            isError = passwordError != null,
            supportingText = passwordError ?: "Mínimo 8 caracteres",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        ViasitTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirmar contraseña",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = if (confirmPasswordError != null) Rose500 else Slate400,
                    modifier = Modifier.size(20.dp))
            },
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(4.dp))

        ViasitPrimaryButton(
            text = "Crear cuenta",
            onClick = onRegisterClick,
            isLoading = isLoading
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "¿Ya tienes cuenta? ",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
            Text(
                "Inicia sesión",
                style = MaterialTheme.typography.bodySmall,
                color = Brand400,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onShowLogin() }
            )
        }
    }
}
