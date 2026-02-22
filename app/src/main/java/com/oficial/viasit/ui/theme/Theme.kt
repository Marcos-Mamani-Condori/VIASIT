package com.oficial.viasit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark Color Scheme (modo principal de la app) ────────────────────────────
private val DarkColorScheme = darkColorScheme(
    // Primario — azul índigo eléctrico
    primary          = Brand500,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF1A237E),   // fondo de chips/badges primarios
    onPrimaryContainer = Brand300,

    // Secundario — ámbar cálido
    secondary        = Amber400,
    onSecondary      = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = Amber200,

    // Terciario — esmeralda (éxito / en servicio)
    tertiary         = Emerald400,
    onTertiary       = Color(0xFF003828),
    tertiaryContainer = Emerald900,
    onTertiaryContainer = Emerald400,

    // Fondos
    background       = Slate950,
    onBackground     = Slate200,

    // Superficies
    surface          = Slate900,
    onSurface        = Slate200,
    surfaceVariant   = Slate800,
    onSurfaceVariant = Slate400,

    // Bordes
    outline          = Slate700,
    outlineVariant   = Slate800,

    // Error
    error            = Rose500,
    onError          = Color.White,
    errorContainer   = Rose900,
    onErrorContainer = Color(0xFFFFDAD6),

    // Inversión (para snackbars, tooltips)
    inverseSurface   = Slate200,
    inverseOnSurface = Slate900,
    inversePrimary   = Brand700,

    // Scrim
    scrim            = Color(0x99000000)
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Brand500,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFE8EAFF),
    onPrimaryContainer = Brand700,

    secondary        = Amber500,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = Color(0xFF4A3800),

    tertiary         = Emerald500,
    onTertiary       = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),

    background       = Gray50,
    onBackground     = Gray900,

    surface          = Color.White,
    onSurface        = Gray900,
    surfaceVariant   = Gray100,
    onSurfaceVariant = Gray700,

    outline          = Gray200,
    outlineVariant   = Gray100,

    error            = Rose500,
    onError          = Color.White,
    errorContainer   = Color(0xFFFFE4E6),
    onErrorContainer = Rose900
)

@Composable
fun VIASITTheme(
    darkTheme: Boolean = true,   // dark mode por defecto
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Barra de estado transparente con iconos claros/oscuros según tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}