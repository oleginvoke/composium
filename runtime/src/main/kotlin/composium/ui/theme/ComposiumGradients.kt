package oleginvoke.com.composium.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal object ComposiumGradients {

    val accent: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(
            colors = listOf(
                Tokens.colors.primary,
                Tokens.colors.secondary,
            ),
        )

    val accentSoft: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(
            colors = listOf(
                Tokens.colors.primary.copy(alpha = 0.14f),
                Tokens.colors.secondary.copy(alpha = 0.08f),
            ),
        )

    val hairline: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.horizontalGradient(
            colors = listOf(
                Tokens.colors.primary.copy(alpha = 0.45f),
                Tokens.colors.secondary.copy(alpha = 0.28f),
                Tokens.colors.tertiary.copy(alpha = 0.2f),
            ),
        )

    val ambient: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.verticalGradient(
            colors = listOf(
                Tokens.colors.primary.copy(alpha = 0.1f),
                Tokens.colors.background.copy(alpha = 0f),
            ),
        )

    val previewGridTint: Color
        @Composable
        @ReadOnlyComposable
        get() = Tokens.colors.outlineVariant.copy(alpha = 0.65f)
}
