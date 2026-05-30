package ch.heuscher.gentlemessaging.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    secondary = ButtonBlue,
    onSecondary = TextOnPrimary,
    tertiary = ButtonOrange,
    onTertiary = TextOnPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = TextOnPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    secondary = ButtonBlue,
    onSecondary = TextOnPrimary,
    tertiary = ButtonOrange,
    onTertiary = TextOnPrimary,
    background = BackgroundDark,
    onBackground = TextOnPrimary,
    surface = SurfaceDark,
    onSurface = TextOnPrimary,
    error = ErrorRed,
    onError = TextOnPrimary
)

@Composable
fun TreemaLightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontSizeLevel: Int = 1,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(fontSizeLevel),
        content = content
    )
}
