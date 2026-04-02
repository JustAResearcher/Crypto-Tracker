package com.cryptotracker.ui.theme

import android.app.Activity
import android.os.Build
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

private val DarkColorScheme = darkColorScheme(
    primary = MeowGoldLight,
    onPrimary = MeowDarkBrown,
    primaryContainer = MeowGoldDark,
    onPrimaryContainer = MeowAmber80,
    secondary = MeowAmber80,
    onSecondary = MeowAmber40,
    secondaryContainer = MeowWarmGrey,
    onSecondaryContainer = MeowAmber80,
    tertiary = MeowGoldLight,
    background = MeowDarkBrown,
    onBackground = Color(0xFFEDE0CF),
    surface = MeowDarkSurface,
    onSurface = Color(0xFFEDE0CF),
    surfaceVariant = MeowDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1C4B0)
)

private val LightColorScheme = lightColorScheme(
    primary = MeowGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0A0),
    onPrimaryContainer = MeowGoldDark,
    secondary = MeowGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECC8),
    onSecondaryContainer = MeowAmber40,
    tertiary = MeowGoldDark,
    background = MeowCream,
    onBackground = MeowDarkBrown,
    surface = LightSurface,
    onSurface = MeowDarkBrown,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MeowWarmGrey
)

@Composable
fun CryptoPriceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use the Meowcoin brand scheme — no dynamic color override
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
