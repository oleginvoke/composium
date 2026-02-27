package oleginvoke.com.composium.scene_screen

import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.LocalScenePreviewContainer
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumScaffold
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumTopBar
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun SceneScreen(
    sceneEntry: SceneEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val themeController = LocalComposiumThemeController.current
    val sceneScope = remember(sceneEntry.id) { SceneScope() }
    val store = rememberSceneScreenStore()
    val state = store.state
    val paramsCallbacks: SceneParamsCallbacks = sceneScope.paramsCallbacks

    val uiState = SceneScreenUiState(
        sceneEntry = sceneEntry,
        sceneScope = sceneScope,
        paramsState = sceneScope.paramsState,
        controlsSheet = state.controlsSheet,
        isDarkTheme = themeController.isDarkTheme,
    )

    val callbacks = remember(store, onBack, themeController.onThemeChange) {
        object : SceneScreenCallbacks {
            override fun onBack() {
                onBack.invoke()
            }

            override fun onShowParams() {
                store.dispatch(SceneScreenIntent.ShowControls)
            }

            override fun onDismissControls() {
                store.dispatch(SceneScreenIntent.HideControls)
            }

            override fun onThemeChange(isDarkTheme: Boolean) {
                themeController.onThemeChange.invoke(isDarkTheme)
            }

            override fun onControlsTabChanged(tab: ControlsSheetTab) {
                store.dispatch(SceneScreenIntent.ControlsTabChanged(tab))
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        ComposiumScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SceneScreenTopBar(
                    state = uiState,
                    callbacks = callbacks,
                    statusBarInsets = contentWindowInsets,
                )
            },
        ) { padding ->
            SceneScreenContent(
                state = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }

        SceneScreenControls(
            state = uiState,
            callbacks = callbacks,
            paramsCallbacks = paramsCallbacks,
            sheetWindowInsets = contentWindowInsets,
        )
    }
}

@Composable
private fun SceneScreenTopBar(
    state: SceneScreenUiState,
    callbacks: SceneScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
) {
    ComposiumTopBar(
        title = {
            ComposiumText(
                text = state.sceneEntry.scene.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Tokens.typography.titleLarge,
                color = Tokens.colors.onSurface,
            )
        },
        navigationIcon = {
            ComposiumIconButton(onClick = callbacks::onBack) {
                ComposiumIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Tokens.colors.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ComposiumIconButton(
                    onClick = callbacks::onShowParams,
                ) {
                    ComposiumIcon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = "Properties",
                        tint = Tokens.colors.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                AnimatedContent(targetState = state.isDarkTheme) { isDark ->
                    if (isDark) {
                        ComposiumIconButton(
                            onClick = { callbacks.onThemeChange(false) },
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Outlined.WbSunny,
                                contentDescription = "Change theme",
                                tint = Color.Yellow,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    } else {
                        ComposiumIconButton(
                            onClick = { callbacks.onThemeChange(true) },
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Outlined.NightsStay,
                                contentDescription = "Change theme",
                                tint = Color.Blue,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        },
        containerColor = Tokens.colors.surface,
        statusBarInsets = statusBarInsets,
    )
}

@Composable
private fun SceneScreenContent(
    state: SceneScreenUiState,
    modifier: Modifier = Modifier,
) {
    ScenePreviewContent(
        sceneEntry = state.sceneEntry,
        sceneScope = state.sceneScope,
        modifier = modifier,
    )
}

@Composable
private fun SceneScreenControls(
    state: SceneScreenUiState,
    callbacks: SceneScreenCallbacks,
    paramsCallbacks: SceneParamsCallbacks,
    sheetWindowInsets: WindowInsets? = null,
) {
    if (!state.controlsSheet.isVisible) return

    ControlsSheet(
        uiState = state.controlsSheet,
        paramsState = state.paramsState,
        paramsCallbacks = paramsCallbacks,
        preview = state.sceneScope.preview,
        isDarkThemeEnabled = state.isDarkTheme,
        onDismiss = callbacks::onDismissControls,
        onTabSelected = callbacks::onControlsTabChanged,
        onThemeChange = callbacks::onThemeChange,
        sheetWindowInsets = sheetWindowInsets,
    )
}

@Composable
private fun ScenePreviewContent(
    sceneEntry: SceneEntry,
    sceneScope: SceneScope,
    modifier: Modifier = Modifier,
) {
    val settings = sceneScope.preview
    val baseDensity = LocalDensity.current

    val stableDensity = DisplayMetrics.DENSITY_DEVICE_STABLE / 160f
    val resolvedDensityValue = settings.displayScaleOverride?.let { stableDensity * it } ?: baseDensity.density
    val resolvedFontScale = settings.fontScaleOverride ?: baseDensity.fontScale
    val layoutDirection = if (settings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides Density(density = resolvedDensityValue, fontScale = resolvedFontScale),
    ) {
        Box(modifier = modifier) {
            LocalScenePreviewContainer.current.Decoration {
                sceneEntry.scene.content(sceneScope)
            }
        }
    }
}
