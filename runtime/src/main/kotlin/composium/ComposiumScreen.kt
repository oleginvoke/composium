package oleginvoke.com.composium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oleginvoke.com.composium.host_screen.ComposiumHostScreen
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import oleginvoke.com.composium.ui.theme.ComposiumThemeController
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Tokens

/**
 * Composium screen with auto-registration of generated scenes.
 *
 * If KSP is not used, scenes can be registered manually through [Composium.registerAll]
 * before rendering this screen.
 *
 * Uses [DefaultScenePreviewContainer] for scene preview rendering.
 *
 * @param modifier Modifier for the screen.
 * @param isDarkTheme Optional dark theme override. If not provided, the internal state is used.
 * @param contentWindowInsets Optional window insets for the content area. Use `WindowInsets.systemBars` for edge-to-edge
 * or apply padding manually if you are not using edge-to-edge.
 * @param onThemeChange Callback for theme change.
 */
@Composable
fun ComposiumScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean? = null,
    contentWindowInsets: WindowInsets? = null,
    onThemeChange: (isDarkTheme: Boolean) -> Unit = {}
) {
    ComposiumScreen(
        modifier = modifier,
        isDarkTheme = isDarkTheme,
        contentWindowInsets = contentWindowInsets,
        onThemeChange = onThemeChange,
        scenePreviewDecorator = DefaultScenePreviewContainerSlot,
    )
}

/**
 * Composium screen with auto-registration of generated scenes.
 *
 * If KSP is not used, scenes can be registered manually through [Composium.registerAll]
 * before rendering this screen.
 *
 * @param modifier Modifier for the screen.
 * @param isDarkTheme Optional dark theme override. If not provided, the internal state is used.
 * @param contentWindowInsets Optional window insets for the content area. Use `WindowInsets.systemBars` for edge-to-edge
 * or apply padding manually if you are not using edge-to-edge.
 * @param onThemeChange Callback for theme change.
 * @param scenePreviewDecorator Slot container used for scene content rendering across all scenes.
 */
@Composable
fun ComposiumScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean? = null,
    contentWindowInsets: WindowInsets? = null,
    onThemeChange: (isDarkTheme: Boolean) -> Unit = {},
    scenePreviewDecorator: ScenePreviewDecorator,
) {
    val internalIsDark by oleginvoke.com.composium.ui.theme.isDarkTheme
        .collectAsStateWithLifecycle()
    val effectiveIsDark = isDarkTheme ?: internalIsDark

    val themeController = remember(effectiveIsDark, isDarkTheme, onThemeChange) {
        ComposiumThemeController(
            isDarkTheme = effectiveIsDark,
            onThemeChange = { newValue ->
                if (isDarkTheme == null) {
                    oleginvoke.com.composium.ui.theme.isDarkTheme.value = newValue
                }
                onThemeChange(newValue)
            },
        )
    }

    ComposiumTheme(
        darkTheme = effectiveIsDark,
    ) {
        LaunchedEffect(Unit) {
            ComposiumAutoRegistration.registerGeneratedScenes()
        }
        CompositionLocalProvider(
            LocalComposiumThemeController provides themeController,
            LocalScenePreviewContainer provides scenePreviewDecorator,
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Tokens.colors.background),
            ) {
                ComposiumHostScreen(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = contentWindowInsets,
                )
            }
        }
    }
}
