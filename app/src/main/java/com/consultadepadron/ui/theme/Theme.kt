package com.consultadepadron.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Azul,
    onPrimary = Blanco,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = AzulNoche,
    secondary = Slate700,
    onSecondary = Blanco,
    background = Hielo,
    onBackground = Slate900,
    surface = Blanco,
    onSurface = Slate900,
    surfaceVariant = Hielo,
    onSurfaceVariant = Slate500,
    outline = BordeSuave,
    outlineVariant = BordeSuave,
    error = RojoCoral,
    onError = Blanco,
    errorContainer = RojoFondo,
    onErrorContainer = RojoCoral
)

@Composable
fun ConsultaPadronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = PadronTypography,
        content = content
    )
}