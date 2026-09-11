package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

val FajrColorScheme = darkColorScheme(
    primary = DarkOlive,
    secondary = DarkOlive,
    tertiary = DarkGold,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkOlive,
    secondary = DarkGold,
    tertiary = GoldAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextLightPrimary,
    onSurface = TextLightPrimary,
    surfaceVariant = Color(0xFF232D2A),
    onSurfaceVariant = TextLightSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = OliveGreen,
    secondary = GoldAccent,
    tertiary = OliveGreenLight,
    background = CreamBackground,
    surface = PureWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary,
    surfaceVariant = PureWhite,
    onSurfaceVariant = TextDarkSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFajrMode: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        isFajrMode -> FajrColorScheme
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
