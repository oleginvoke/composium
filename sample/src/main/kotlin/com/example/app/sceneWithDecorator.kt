package com.example.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.theme.ComposiumTheme
import com.example.app.theme.SampleColorTheme
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.SceneDelegate
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.scene

enum class SamplePreviewTheme {
    Default,
    Emerald,
}

private val LocalPreviewIsDarkTheme = compositionLocalOf { false }

@Composable
fun ComposiumPreviewScreen(
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
    isDarkTheme: Boolean? = null,
    onThemeChange: (Boolean) -> Unit = {},
) {
    val darkTheme = isDarkTheme ?: isSystemInDarkTheme()

    CompositionLocalProvider(LocalPreviewIsDarkTheme provides darkTheme) {
        SamplePreviewTheme {
            ComposiumScreen(
                contentWindowInsets = contentWindowInsets,
                modifier = modifier,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
            )
        }
    }
}

private var previewThemeCache = SamplePreviewTheme.Emerald

internal fun sceneWithDecorator(
    group: String? = null,
    name: String? = null,
    enableEdgeToEdge: Boolean = true,
    scrollable: Boolean = true,
    backgroundColorLight: Color = Color.White,
    backgroundColorDark: Color = Color(0xFF4A4949),
    content: @Composable (SceneScope.() -> Unit),
): SceneDelegate {
    return scene(
        group = group,
        name = name,
        enableEdgeToEdge = enableEdgeToEdge,
        content = {
            var showContentBounds by remember { mutableStateOf(false) }
            var previewTheme by remember { mutableStateOf(previewThemeCache) }
            SamplePreviewTheme(
                previewTheme = previewTheme,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .then(
                            if (scrollable) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier.fillMaxSize()
                            },
                        ),
                ) {
                    val isDarkTheme = LocalPreviewIsDarkTheme.current
                    Spacer(Modifier.height(innerPadding.calculateTopPadding() + 16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isDarkTheme) {
                                    backgroundColorDark
                                } else {
                                    backgroundColorLight
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Tokens.colors.information.light
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .then(
                                    if (scrollable)
                                        Modifier.fillMaxWidth()
                                    else
                                        Modifier.fillMaxSize()
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Tokens.colors.information.lightest,
                                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Show bounds",
                                    style = Tokens.typography.subtitleM,
                                    color = Tokens.colors.onSurface.extraDark,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Checkbox(
                                    checked = showContentBounds,
                                    onClick = { showContentBounds = it },
                                )
                                Spacer(Modifier.width(12.dp))
                                ThemeToggle(
                                    selectedTheme = previewTheme,
                                    onThemeSelected = {
                                        previewTheme = it
                                        previewThemeCache = it
                                    },
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(horizontal = 16.dp)
                                    .then(
                                        if (showContentBounds) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Tokens.colors.brand.default,
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                content()
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
                }
            }
        }
    )
}

@Composable
internal fun ThemeToggle(
    selectedTheme: SamplePreviewTheme,
    onThemeSelected: (SamplePreviewTheme) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isEmeraldTheme = selectedTheme == SamplePreviewTheme.Emerald
    val interactionSource = remember { MutableInteractionSource() }
    val width = 60.dp
    val height = 28.dp
    val thumbSize = 22.dp
    val edgePadding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (isEmeraldTheme) width - thumbSize - edgePadding else edgePadding,
        animationSpec = Motion.springBouncy(),
        label = "theme_thumb_offset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (isEmeraldTheme) {
            Tokens.colors.brand.lightest
        } else {
            Tokens.colors.onSurface.highLight
        },
        animationSpec = Motion.tweenStandard(),
        label = "theme_track_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isEmeraldTheme) {
            Tokens.colors.brand.default.copy(alpha = 0.55f)
        } else {
            Tokens.colors.surface.default
        },
        animationSpec = Motion.tweenStandard(),
        label = "theme_border_color",
    )
    val sunAlpha by animateFloatAsState(
        targetValue = if (isEmeraldTheme) 0.38f else 0f,
        animationSpec = Motion.tweenStandard(),
        label = "theme_sun_alpha",
    )
    val moonAlpha by animateFloatAsState(
        targetValue = if (isEmeraldTheme) 0f else 0.38f,
        animationSpec = Motion.tweenStandard(),
        label = "theme_moon_alpha",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isEmeraldTheme) 360f else 0f,
        animationSpec = Motion.springGentle(),
        label = "theme_thumb_rotation",
    )
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .pressScale(interactionSource)
            .clip(shape)
            .background(trackColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onThemeSelected(if (isEmeraldTheme) SamplePreviewTheme.Default else SamplePreviewTheme.Emerald) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .graphicsLayer { alpha = sunAlpha },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.default_theme_logo),
                contentDescription = null,
                tint = Tokens.colors.onSurface.on,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .graphicsLayer { alpha = moonAlpha },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.emerald_theme_logo),
                contentDescription = null,
                tint = Tokens.colors.onSurface.on,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(shape)
                .background(if (isEmeraldTheme) Gradients.accent else Gradients.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (isEmeraldTheme) {
                Icon(
                    painter = painterResource(id = R.drawable.emerald_theme_logo),
                    contentDescription = "Switch to default theme",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.default_theme_logo),
                    contentDescription = "Switch to emerald theme",
                    tint = Tokens.colors.onSurface.on,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation),
                )
            }
        }
    }
}

private object Gradients {

    val accent: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(
            colors = listOf(
                Tokens.colors.brand.default,
                Tokens.colors.brand.light,
            ),
        )

    val accentSoft: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(
            colors = listOf(
                Tokens.colors.surface.default,
                Tokens.colors.onSurface.light,
            ),
        )
}

private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.985f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.tweenFast(),
        label = "press_scale",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private object Motion {
    fun <T> tweenFast(): TweenSpec<T> = tween(durationMillis = 140)

    fun <T> tweenStandard(): TweenSpec<T> = tween(durationMillis = 220)

    fun <T> springGentle(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

@Composable
private fun SamplePreviewTheme(
    previewTheme: SamplePreviewTheme = SamplePreviewTheme.Emerald,
    content: @Composable () -> Unit,
) {
    val colorTheme = when (previewTheme) {
        SamplePreviewTheme.Default -> SampleColorTheme.Default
        SamplePreviewTheme.Emerald -> SampleColorTheme.Emerald
    }

    ComposiumTheme(
        darkTheme = LocalPreviewIsDarkTheme.current,
        dynamicColor = false,
        colorTheme = colorTheme,
        content = content,
    )
}

@Composable
private fun Checkbox(
    checked: Boolean,
    onClick: (Boolean) -> Unit,
) {
    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = onClick,
    )
}

private object Tokens {
    val colors: SampleTokensColors
        @Composable
        @ReadOnlyComposable
        get() {
            val colorScheme = MaterialTheme.colorScheme
            return SampleTokensColors(
                information = SampleInformationColors(
                    light = colorScheme.primary.copy(alpha = 0.46f),
                    lightest = colorScheme.primaryContainer.copy(alpha = 0.42f),
                ),
                onSurface = SampleOnSurfaceColors(
                    extraDark = colorScheme.onSurfaceVariant,
                    highLight = colorScheme.surfaceVariant,
                    on = colorScheme.onSurface,
                    light = colorScheme.surfaceVariant,
                ),
                brand = SampleBrandColors(
                    default = colorScheme.primary,
                    light = colorScheme.tertiary,
                    lightest = colorScheme.primaryContainer.copy(alpha = 0.72f),
                ),
                surface = SampleSurfaceColors(
                    default = colorScheme.surface,
                ),
            )
        }

    val typography: SampleTokensTypography
        @Composable
        @ReadOnlyComposable
        get() = SampleTokensTypography(
            subtitleM = MaterialTheme.typography.labelLarge,
        )
}

private data class SampleTokensColors(
    val information: SampleInformationColors,
    val onSurface: SampleOnSurfaceColors,
    val brand: SampleBrandColors,
    val surface: SampleSurfaceColors,
)

private data class SampleInformationColors(
    val light: Color,
    val lightest: Color,
)

private data class SampleOnSurfaceColors(
    val extraDark: Color,
    val highLight: Color,
    val on: Color,
    val light: Color,
)

private data class SampleBrandColors(
    val default: Color,
    val light: Color,
    val lightest: Color,
)

private data class SampleSurfaceColors(
    val default: Color,
)

private data class SampleTokensTypography(
    val subtitleM: TextStyle,
)
