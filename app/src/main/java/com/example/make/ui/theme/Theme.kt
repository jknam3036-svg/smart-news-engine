package com.example.make.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = SleekTeal,
    tertiary = DynamicViolet,
    background = DeepGray900,
    surface = Gray800,
    surfaceVariant = Gray700,
    onPrimary = DeepGray900,
    onSecondary = DeepGray900,
    onTertiary = DeepGray900,
    onBackground = WhitePure,
    onSurface = WhitePure,
    error = SoftRose
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    secondary = SleekTeal,
    tertiary = DynamicViolet,
    background = Gray100,
    surface = WhitePure,
    surfaceVariant = Gray200,
    onPrimary = WhitePure,
    onSecondary = WhitePure,
    onTertiary = WhitePure,
    onBackground = DeepGray900,
    onSurface = DeepGray900
)

@Composable
fun MakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
        typography = Typography,
        content = content
    )
}