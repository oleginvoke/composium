package com.example.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Color(0xFF003828),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldContainerLight,
    secondary = EmeraldGrey80,
    onSecondary = Color(0xFF1E352B),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFCDE9D9),
    tertiary = EmeraldBlue80,
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5A),
    onTertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF002116),
    secondary = EmeraldGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE9D9),
    onSecondaryContainer = Color(0xFF082016),
    tertiary = EmeraldBlue40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C1),
)

enum class SampleColorTheme {
    Default,
    Emerald,
}

internal fun sampleColorScheme(
    colorTheme: SampleColorTheme,
    darkTheme: Boolean,
    currentScheme: ColorScheme,
): ColorScheme {
    return when (colorTheme) {
        SampleColorTheme.Default -> currentScheme
        SampleColorTheme.Emerald -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
    }
}

@Composable
fun ComposiumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorTheme: SampleColorTheme = SampleColorTheme.Default,
    content: @Composable () -> Unit
) {
    val currentScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)
        }

        darkTheme ->
            DarkColorScheme
        else ->
            LightColorScheme
    }

    val colorScheme = sampleColorScheme(
        colorTheme = colorTheme,
        darkTheme = darkTheme,
        currentScheme = currentScheme,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
