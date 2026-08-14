package com.group1.dartbud.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

// Material3-fargeskjema for lyst tema, satt sammen av fargene i Color.kt
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    error = LightError,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Material3-fargeskjema for mørkt tema
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// App-temaet. Følger systemets lys/mørk-innstilling automatisk (kan overstyres
// via darkTheme-parameteren, f.eks. i previews). Wrapper alt innhold i
// MaterialTheme slik at composables lenger nede i treet kan bruke
// MaterialTheme.colorScheme / .typography.
@Composable
fun DartBudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // NB: bruker Material3s standard-typografi her, ikke AppTypography
        // som er definert i Type.kt.
        typography = Typography(),
        content = content
    )
}
