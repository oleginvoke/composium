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

/**
 * Минимальная типографика, чтобы не тянуть Material.
 * labelSmall / bodySmall — для компактных блоков (badge, params).
 */
internal data class ComposiumTypography(
    val headlineMedium: TextStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    val headlineSmall: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    val titleLarge: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    val titleMedium: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    val bodyMedium: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val bodySmall: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    val labelSmall: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

internal data class ComposiumShapes(
    val large: Shape = RoundedCornerShape(20.dp),
    val medium: Shape = RoundedCornerShape(14.dp),
    val small: Shape = RoundedCornerShape(10.dp),
    val extraSmall: Shape = RoundedCornerShape(6.dp),
    val pill: Shape = RoundedCornerShape(999.dp),
)

internal interface ComposiumColors {
    /** Brand / акцент */
    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color

    /** Вторичный акцент */
    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color

    /** Доп. акцент (опционально для редких кейсов) */
    val tertiary: Color
    val onTertiary: Color
    val tertiaryContainer: Color
    val onTertiaryContainer: Color

    /** Ошибки */
    val error: Color
    val onError: Color
    val errorContainer: Color
    val onErrorContainer: Color

    /** Основные поверхности */
    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color
    val surfaceVariant: Color
    val onSurfaceVariant: Color

    /** Обводки/разделители */
    val outline: Color
    val outlineVariant: Color

    /** Инверсные поверхности (например, для snackbar/overlay) */
    val inverseSurface: Color
    val inverseOnSurface: Color
    val inversePrimary: Color

    /** Служебные */
    val scrim: Color
    val surfaceTint: Color
}

internal class ComposiumLightColors : ComposiumColors {
    // Современная палитра: teal/emerald акцент, мягкий фон
    override val primary: Color = Color(0xFF0D7377)
    override val onPrimary: Color = Color(0xFFFFFFFF)
    // Чуть более насыщенный контейнер, чтобы badge/иконки не "сливались" с фоном.
    override val primaryContainer: Color = Color(0xFF7DE7E1)
    override val onPrimaryContainer: Color = Color(0xFF052E30)

    override val secondary: Color = Color(0xFF4A5568)
    override val onSecondary: Color = Color(0xFFFFFFFF)
    override val secondaryContainer: Color = Color(0xFFE2E8F0)
    override val onSecondaryContainer: Color = Color(0xFF1A202C)

    override val tertiary: Color = Color(0xFFB56576)
    override val onTertiary: Color = Color(0xFFFFFFFF)
    override val tertiaryContainer: Color = Color(0xFFFFE4E9)
    override val onTertiaryContainer: Color = Color(0xFF3D1F26)

    override val error: Color = Color(0xFFDC2626)
    override val onError: Color = Color(0xFFFFFFFF)
    override val errorContainer: Color = Color(0xFFFEE2E2)
    override val onErrorContainer: Color = Color(0xFF450A0A)

    override val background: Color = Color(0xFFF8FAFC)
    override val onBackground: Color = Color(0xFF0F172A)
    override val surface: Color = Color(0xFFFFFFFF)
    override val onSurface: Color = Color(0xFF0F172A)
    override val surfaceVariant: Color = Color(0xFFF1F5F9)
    override val onSurfaceVariant: Color = Color(0xFF475569)

    override val outline: Color = Color(0xFF94A3B8)
    override val outlineVariant: Color = Color(0xFFE2E8F0)

    override val inverseSurface: Color = Color(0xFF1E293B)
    override val inverseOnSurface: Color = Color(0xFFF1F5F9)
    override val inversePrimary: Color = Color(0xFF5EEAD4)

    override val scrim: Color = Color(0xFF0F172A)
    override val surfaceTint: Color = primary
}

internal class ComposiumDarkColors : ComposiumColors {
    // Тёмная тема: cyan/teal акцент, глубокий фон
    override val primary: Color = Color(0xFF5EEAD4)
    override val onPrimary: Color = Color(0xFF052E30)
    override val primaryContainer: Color = Color(0xFF0D7377)
    override val onPrimaryContainer: Color = Color(0xFFA7F0ED)

    override val secondary: Color = Color(0xFF94A3B8)
    override val onSecondary: Color = Color(0xFF1E293B)
    override val secondaryContainer: Color = Color(0xFF334155)
    override val onSecondaryContainer: Color = Color(0xFFE2E8F0)

    override val tertiary: Color = Color(0xFFF9A8D4)
    override val onTertiary: Color = Color(0xFF3D1F26)
    override val tertiaryContainer: Color = Color(0xFF7267A8)
    override val onTertiaryContainer: Color = Color(0xFFFFE4E9)

    override val error: Color = Color(0xFFFCA5A5)
    override val onError: Color = Color(0xFF450A0A)
    override val errorContainer: Color = Color(0xFF7F1D1D)
    override val onErrorContainer: Color = Color(0xFFFEE2E2)

    override val background: Color = Color(0xFF0F172A)
    override val onBackground: Color = Color(0xFFF1F5F9)
    override val surface: Color = Color(0xFF1E293B)
    override val onSurface: Color = Color(0xFFF1F5F9)
    override val surfaceVariant: Color = Color(0xFF334155)
    override val onSurfaceVariant: Color = Color(0xFFCBD5E1)

    override val outline: Color = Color(0xFF64748B)
    override val outlineVariant: Color = Color(0xFF47536A)

    override val inverseSurface: Color = Color(0xFFF1F5F9)
    override val inverseOnSurface: Color = Color(0xFF1E293B)
    override val inversePrimary: Color = Color(0xFF0D7377)

    override val scrim: Color = Color(0xFF000000)
    override val surfaceTint: Color = primary
}

internal object Tokens {

    /** Цвета применяемые в дизайн системе.*/
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
