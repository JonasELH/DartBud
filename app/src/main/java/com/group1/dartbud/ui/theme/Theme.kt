import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.group1.dartbud.ui.theme.CustomBackground
import com.group1.dartbud.ui.theme.CustomError
import com.group1.dartbud.ui.theme.CustomPrimary
import com.group1.dartbud.ui.theme.CustomSecondary
import com.group1.dartbud.ui.theme.CustomSurface

import com.group1.dartbud.ui.theme.CustomDarkBackground
import com.group1.dartbud.ui.theme.CustomDarkError
import com.group1.dartbud.ui.theme.CustomDarkPrimary
import com.group1.dartbud.ui.theme.CustomDarkSecondary
import com.group1.dartbud.ui.theme.CustomDarkSurface

private val LightColorScheme = lightColorScheme(
    primary = CustomPrimary,
    secondary = CustomSecondary,
    background = CustomBackground,
    surface = CustomSurface,
    error = CustomError,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = CustomDarkPrimary,
    secondary = CustomDarkSecondary,
    background = CustomDarkBackground,
    surface = CustomDarkSurface,
    error = CustomDarkError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun DartBudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
