package oleginvoke.com.composium.host_screen

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.main_screen.MainScreen
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumHostScreen(
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val scenes = ComposiumRuntime.scenes
    val saveableStateHolder = rememberSaveableStateHolder()
    val scenesById by remember {
        derivedStateOf { scenes.associateBy { entry -> entry.id } }
    }
    val sceneIds by remember {
        derivedStateOf { scenes.map { entry -> entry.id } }
    }
    val mainScreenInsets = remember(contentWindowInsets) {
        contentWindowInsets?.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    }
    val sceneOverlayInsets = remember(contentWindowInsets) {
        contentWindowInsets?.only(WindowInsetsSides.Horizontal)
    }
    val transitionScaleSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )
    }
    val sceneVisibilityState = remember { MutableTransitionState(false) }

    var state by remember { mutableStateOf(ComposiumHostScreenState()) }

    val renderedSceneId = state.renderedSceneId
    val renderedSceneEntry = renderedSceneId?.let(scenesById::get)
    val blocksMainScreenInput = shouldBlockMainScreenInput(state)

    fun openScene(sceneId: String) {
        state = reduceComposiumHostScreen(
            state = state,
            intent = ComposiumHostScreenIntent.SceneSelected(sceneId),
        )
    }

    fun closeScene() {
        state = reduceComposiumHostScreen(
            state = state,
            intent = ComposiumHostScreenIntent.SceneClosed,
        )
    }

    SystemBackHandler(
        enabled = state.isSceneVisible,
        onBack = ::closeScene,
    )

    LaunchedEffect(sceneIds) {
        state = reduceComposiumHostScreen(
            state = state,
            intent = ComposiumHostScreenIntent.AvailableScenesChanged(sceneIds.toSet()),
        )
    }

    SideEffect {
        sceneVisibilityState.targetState = state.isSceneVisible
    }

    LaunchedEffect(
        sceneVisibilityState.currentState,
        sceneVisibilityState.targetState,
        state.isSceneVisible,
        renderedSceneId,
    ) {
        if (
            !sceneVisibilityState.currentState &&
            !sceneVisibilityState.targetState &&
            !state.isSceneVisible &&
            renderedSceneId != null
        ) {
            state = reduceComposiumHostScreen(
                state = state,
                intent = ComposiumHostScreenIntent.TransitionSettled,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.background),
    ) {
        saveableStateHolder.SaveableStateProvider("route_main") {
            MainScreen(
                scenes = scenes,
                onSceneSelected = ::openScene,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (mainScreenInsets != null) {
                            Modifier.windowInsetsPadding(mainScreenInsets)
                        } else {
                            Modifier
                        },
                    ),
                contentWindowInsets = contentWindowInsets,
            )
        }

        if (blocksMainScreenInput) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeAllPointerInput(),
            )
        }

        if (renderedSceneEntry != null) {
            AnimatedVisibility(
                visibleState = sceneVisibilityState,
                enter = fadeIn(tween(260)) +
                    scaleIn(initialScale = 0.97f, animationSpec = transitionScaleSpec) +
                    slideInHorizontally(
                        initialOffsetX = { width -> width / 8 },
                        animationSpec = tween(280),
                    ),
                exit = fadeOut(tween(180)) +
                    scaleOut(targetScale = 0.97f, animationSpec = tween(260)) +
                    slideOutHorizontally(
                        targetOffsetX = { width -> width / 10 },
                        animationSpec = tween(240),
                    ),
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (sceneOverlayInsets != null) {
                            Modifier.windowInsetsPadding(sceneOverlayInsets)
                        } else {
                            Modifier
                        },
                    ),
                label = "scene_overlay",
            ) {
                saveableStateHolder.SaveableStateProvider("route_scene_$renderedSceneId") {
                    key(renderedSceneId) {
                        SceneScreen(
                            sceneEntry = renderedSceneEntry,
                            onBack = ::closeScene,
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = contentWindowInsets,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.consumeAllPointerInput(): Modifier = pointerInteropFilter { true }

@Composable
private fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack = rememberUpdatedState(onBack)
    val callback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack.value.invoke()
            }
        }
    }
    val dispatcher = remember(context) {
        context.findOnBackPressedDispatcherOwner()?.onBackPressedDispatcher
    }

    SideEffect {
        callback.isEnabled = enabled
    }

    DisposableEffect(dispatcher, lifecycleOwner, callback) {
        if (dispatcher == null) {
            onDispose {}
        } else {
            dispatcher.addCallback(lifecycleOwner, callback)
            onDispose {
                callback.remove()
            }
        }
    }
}

private fun Context.findOnBackPressedDispatcherOwner(): OnBackPressedDispatcherOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is OnBackPressedDispatcherOwner) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
