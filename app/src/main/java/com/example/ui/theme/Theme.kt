package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SapphireContainer,
    onPrimary = SapphireOnContainer,
    primaryContainer = SapphireBrandDark,
    onPrimaryContainer = SapphireContainer,
    secondary = Color(0xFFBAC8DB),
    onSecondary = Color(0xFF243140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD6E4F7),
    tertiary = Color(0xFF70DB98),
    onTertiary = Color(0xFF00391A),
    background = Color(0xFF111418),
    surface = Color(0xFF191C20),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E)
)

private val LightColorScheme = lightColorScheme(
    primary = SapphireBrand,
    onPrimary = Color.White,
    primaryContainer = SapphireContainer,
    onPrimaryContainer = SapphireOnContainer,
    secondary = HighDensityTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = HighDensityPillBg,
    onSecondaryContainer = HighDensityTextPrimary,
    tertiary = DealGreen,
    onTertiary = Color.White,
    tertiaryContainer = DealGreenBg,
    onTertiaryContainer = DealGreen,
    background = HighDensityCanvas,
    surface = Color.White,
    onBackground = HighDensityTextPrimary,
    onSurface = HighDensityTextPrimary,
    surfaceVariant = HighDensityPillBg,
    onSurfaceVariant = HighDensityTextSecondary,
    surfaceContainer = HighDensityInputBg,
    outline = HighDensityBorder,
    outlineVariant = HighDensityBorder.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure branded High Density theme is consistently applied
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

