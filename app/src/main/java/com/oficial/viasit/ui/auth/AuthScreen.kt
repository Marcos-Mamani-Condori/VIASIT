package com.oficial.viasit.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val authState       by viewModel.authState.collectAsState()
    val loginFormState  by viewModel.loginForm.collectAsState()
    val registerFormState by viewModel.registerForm.collectAsState()

    var showRegister by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Slate950, Slate900)))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 120.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(listOf(Brand500.copy(alpha = 0.12f), Color.Transparent)),
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

            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Brand500, Brand400))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsBus, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("VIASIT", style = MaterialTheme.typography.displaySmall,
                color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Transporte público en tiempo real", style = MaterialTheme.typography.bodyMedium,
                color = Slate400, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = showRegister,
                transitionSpec = {
                    if (targetState) slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else             slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "auth_form"
            ) { isRegister ->
                if (isRegister) {
                    RegisterForm(
                        email = registerFormState.email, password = registerFormState.password,
                        confirmPassword = registerFormState.confirmPassword, name = registerFormState.name,
                        phone = registerFormState.phone, role = registerFormState.role,
                        invitationCode = registerFormState.invitationCode,
                        emailError = registerFormState.emailError, passwordError = registerFormState.passwordError,
                        confirmPasswordError = registerFormState.confirmPasswordError, nameError = registerFormState.nameError,
                        isLoading = registerFormState.isLoading, error = registerFormState.error,
                        onEmailChange           = viewModel::updateRegisterEmail,
                        onPasswordChange        = viewModel::updateRegisterPassword,
                        onConfirmPasswordChange = viewModel::updateRegisterConfirmPassword,
                        onNameChange            = viewModel::updateRegisterName,
                        onPhoneChange           = viewModel::updateRegisterPhone,
                        onRoleChange            = viewModel::updateRegisterRole,
                        onInvitationCodeChange  = viewModel::updateRegisterInvitationCode,
                        onRegisterClick         = viewModel::register,
                        onShowLogin             = { showRegister = false }
                    )
                } else {
                    LoginForm(
                        email = loginFormState.email, password = loginFormState.password,
                        emailError = loginFormState.emailError, passwordError = loginFormState.passwordError,
                        isLoading = loginFormState.isLoading, error = loginFormState.error,
                        successMessage = loginFormState.successMessage,
                        isSuspended = authState is AuthState.Suspended,
                        onEmailChange    = viewModel::updateLoginEmail,
                        onPasswordChange = viewModel::updateLoginPassword,
                        onLoginClick     = viewModel::login,
                        onShowRegister   = { showRegister = true },
                        onGuestClick     = viewModel::enterAsGuest,
                        onRegisterAdmin  = onRegisterAdmin,
                        onSubmitAppeal   = viewModel::submitAppeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
