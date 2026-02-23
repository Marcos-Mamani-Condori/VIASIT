package com.oficial.viasit.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.oficial.viasit.ui.theme.*

/**
 * Componentes compartidos entre [LoginForm] y [RegisterForm].
 *
 *  - [ViasitTextField]     → campo de texto con el tema oscuro de VIASIT
 *  - [ViasitPrimaryButton] → botón principal con estado de carga
 *  - [ErrorCard]           → tarjeta roja para mostrar errores de autenticación
 */

/** Campo de texto unificado con el estilo oscuro de VIASIT */
@Composable
fun ViasitTextField(
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
        value            = value,
        onValueChange    = onValueChange,
        label            = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon      = leadingIcon,
        trailingIcon     = trailingIcon,
        isError          = isError,
        supportingText   = supportingText?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        visualTransformation = visualTransformation,
        keyboardOptions  = keyboardOptions,
        singleLine       = true,
        shape            = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Brand500,
            unfocusedBorderColor    = Slate700,
            errorBorderColor        = Rose500,
            focusedLabelColor       = Brand400,
            unfocusedLabelColor     = Slate400,
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Slate200,
            cursorColor             = Brand500,
            focusedContainerColor   = SurfaceTinted,
            unfocusedContainerColor = SurfaceTinted,
            errorContainerColor     = Color(0xFF1A0A0E)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Botón principal con estado de carga animado */
@Composable
fun ViasitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick   = onClick,
        enabled   = !isLoading,
        modifier  = modifier.fillMaxWidth().height(54.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor         = Brand500,
            contentColor           = Color.White,
            disabledContainerColor = Slate700,
            disabledContentColor   = Slate400
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Tarjeta de error para mensajes de fallo de autenticación */
@Composable
fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D0A12))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = Rose500, modifier = Modifier.size(18.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB3B3))
    }
}
