package oleginvoke.com.composium.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow

internal val LocalComposiumColors = compositionLocalOf<ComposiumColors> { error("No DesignSystemColors provided") }
internal val LocalComposiumTypography = compositionLocalOf<ComposiumTypography> { error("No ComposiumTypography provided") }
internal val LocalComposiumShapes = compositionLocalOf<ComposiumShapes> { error("No ComposiumShapes provided") }

internal val isDarkTheme: MutableStateFlow<Boolean> = MutableStateFlow(false)

internal data class ComposiumThemeController(
    val isDarkTheme: Boolean = false,
    val onThemeChange: (Boolean) -> Unit = {},
)

internal val LocalComposiumThemeController = compositionLocalOf { ComposiumThemeController() }

@Composable
internal fun ComposiumTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalComposiumColors provides if (darkTheme) ComposiumDarkColors() else ComposiumLightColors(),
        LocalComposiumTypography provides ComposiumTypography(),
        LocalComposiumShapes provides ComposiumShapes(),
        content = content,
    )
}

internal data class ComposiumTypography(
    val displayLarge: TextStyle = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.03).em,
        lineHeight = 44.sp,
    ),
    val displayMedium: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.028).em,
        lineHeight = 34.sp,
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.02).em,
        lineHeight = 30.sp,
    ),
    val headlineSmall: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.015).em,
        lineHeight = 24.sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em,
        lineHeight = 21.sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 19.sp,
    ),
    val titleSmall: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
    val labelSmall: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.03.em,
        lineHeight = 14.sp,
    ),
)

internal data class ComposiumShapes(
    val extraLarge: Shape = RoundedCornerShape(30.dp),
    val large: Shape = RoundedCornerShape(22.dp),
    val medium: Shape = RoundedCornerShape(16.dp),
    val small: Shape = RoundedCornerShape(12.dp),
    val extraSmall: Shape = RoundedCornerShape(8.dp),
    val pill: Shape = RoundedCornerShape(999.dp),
)

internal interface ComposiumColors {
    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color

    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color

    val tertiary: Color
    val onTertiary: Color
    val tertiaryContainer: Color
    val onTertiaryContainer: Color

    val error: Color
    val onError: Color
    val errorContainer: Color
    val onErrorContainer: Color

    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color
    val surfaceVariant: Color
    val onSurfaceVariant: Color

    val outline: Color
    val outlineVariant: Color

    val inverseSurface: Color
    val inverseOnSurface: Color
    val inversePrimary: Color

    val scrim: Color
    val surfaceTint: Color
}

internal class ComposiumLightColors : ComposiumColors {
    override val primary: Color = Color(0xFF689EC2)
    override val onPrimary: Color = Color(0xFFFFFFFF)
    override val primaryContainer: Color = Color(0xFFCDE5FF)
    override val onPrimaryContainer: Color = Color(0xFF0F3247)

    override val secondary: Color = Color(0xFF3A6B86)
    override val onSecondary: Color = Color(0xFFFFFFFF)
    override val secondaryContainer: Color = Color(0xFFE0E8EE)
    override val onSecondaryContainer: Color = Color(0xFF1D3947)

    override val tertiary: Color = Color(0xFF5A6B7A)
    override val onTertiary: Color = Color(0xFFFFFFFF)
    override val tertiaryContainer: Color = Color(0xFFE4E8EC)
    override val onTertiaryContainer: Color = Color(0xFF263140)

    override val error: Color = Color(0xFFB91C1C)
    override val onError: Color = Color(0xFFFFFFFF)
    override val errorContainer: Color = Color(0xFFFEE2E2)
    override val onErrorContainer: Color = Color(0xFF450A0A)

    override val background: Color = Color(0xFFF3F5F7)
    override val onBackground: Color = Color(0xFF0E1820)
    override val surface: Color = Color(0xFFFFFFFF)
    override val onSurface: Color = Color(0xFF0E1820)
    override val surfaceVariant: Color = Color(0xFFEAEEF1)
    override val onSurfaceVariant: Color = Color(0xFF5A6874)

    override val outline: Color = Color(0xFF94A2AF)
    override val outlineVariant: Color = Color(0xFFD4DCE2)

    override val inverseSurface: Color = Color(0xFF0E1820)
    override val inverseOnSurface: Color = Color(0xFFF3F5F7)
    override val inversePrimary: Color = Color(0xFF6FAACB)

    override val scrim: Color = Color(0xFF0B1218)
    override val surfaceTint: Color = primary
}

internal class ComposiumDarkColors : ComposiumColors {
    override val primary: Color = Color(0xFF7AACC3)
    override val onPrimary: Color = Color(0xFF062B3E)
    override val primaryContainer: Color = Color(0xFF21394E)
    override val onPrimaryContainer: Color = Color(0xFFD4E3EE)

    override val secondary: Color = Color(0xFF8EA8B8)
    override val onSecondary: Color = Color(0xFF14273A)
    override val secondaryContainer: Color = Color(0xFF2A3D4A)
    override val onSecondaryContainer: Color = Color(0xFFD7E2EA)

    override val tertiary: Color = Color(0xFF93A2B1)
    override val onTertiary: Color = Color(0xFF1A2733)
    override val tertiaryContainer: Color = Color(0xFF2C3945)
    override val onTertiaryContainer: Color = Color(0xFFE4EBF0)

    override val error: Color = Color(0xFFF5A3A3)
    override val onError: Color = Color(0xFF450A0A)
    override val errorContainer: Color = Color(0xFF7F1D1D)
    override val onErrorContainer: Color = Color(0xFFFEE2E2)

    override val background: Color = Color(0xFF0B1116)
    override val onBackground: Color = Color(0xFFE9EEF2)
    override val surface: Color = Color(0xFF111920)
    override val onSurface: Color = Color(0xFFE9EEF2)
    override val surfaceVariant: Color = Color(0xFF18222B)
    override val onSurfaceVariant: Color = Color(0xFF96A4B0)

    override val outline: Color = Color(0xFF5A6874)
    override val outlineVariant: Color = Color(0xFF22303A)

    override val inverseSurface: Color = Color(0xFFF1F5F7)
    override val inverseOnSurface: Color = Color(0xFF101820)
    override val inversePrimary: Color = Color(0xFF015382)

    override val scrim: Color = Color(0xFF000000)
    override val surfaceTint: Color = primary
}

internal object Tokens {
    val colors: ComposiumColors
        @Composable
        @ReadOnlyComposable
        get() = LocalComposiumColors.current

    val typography: ComposiumTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalComposiumTypography.current

    val shapes: ComposiumShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalComposiumShapes.current
}
