package oleginvoke.com.composium.host_screen

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.main_screen.MainScreen
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.ui.theme.Tokens

private sealed interface HostScreenRoute {
    data object Main : HostScreenRoute
    data class Scene(val sceneId: String) : HostScreenRoute
}

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

    var selectedSceneId by remember { mutableStateOf<String?>(null) }

    val selectedEntry = selectedSceneId?.let(scenesById::get)

    val route: HostScreenRoute = if (selectedEntry == null) {
        HostScreenRoute.Main
    } else {
        HostScreenRoute.Scene(selectedEntry.id)
    }

    fun openScene(sceneId: String) {
        selectedSceneId = sceneId
    }

    fun closeScene() {
        selectedSceneId = null
    }

    SystemBackHandler(
        enabled = route is HostScreenRoute.Scene,
        onBack = ::closeScene,
    )

    LaunchedEffect(sceneIds) {
        val currentId = selectedSceneId
        if (currentId != null && !scenesById.containsKey(currentId)) {
            selectedSceneId = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.background)
            .then(
                if (contentWindowInsets != null) {
                    val hostInsets = when (route) {
                        HostScreenRoute.Main ->
                            contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

                        is HostScreenRoute.Scene ->
                            contentWindowInsets.only(WindowInsetsSides.Horizontal)
                    }
                    Modifier.windowInsetsPadding(
                        hostInsets,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                val scaleSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                )
                when (initialState) {
                    is HostScreenRoute.Main -> {
                        if (targetState is HostScreenRoute.Scene) {
                            (
                                fadeIn(tween(260)) +
                                    scaleIn(initialScale = 0.97f, animationSpec = scaleSpec) +
                                    slideInHorizontally(
                                        initialOffsetX = { width -> width / 8 },
                                        animationSpec = tween(280),
                                    )
                                )
                                .togetherWith(
                                    fadeOut(tween(180)) +
                                        scaleOut(targetScale = 1.02f, animationSpec = tween(260)),
                                )
                        } else {
                            fadeIn(tween(120)).togetherWith(fadeOut(tween(120)))
                        }
                    }

                    is HostScreenRoute.Scene -> {
                        if (targetState is HostScreenRoute.Main) {
                            (
                                fadeIn(tween(260)) +
                                    scaleIn(initialScale = 1.02f, animationSpec = scaleSpec) +
                                    slideInHorizontally(
                                        initialOffsetX = { width -> -width / 10 },
                                        animationSpec = tween(280),
                                    )
                                )
                                .togetherWith(
                                    fadeOut(tween(180)) +
                                        scaleOut(targetScale = 0.97f, animationSpec = tween(260)),
                                )
                        } else {
                            fadeIn(tween(120)).togetherWith(fadeOut(tween(120)))
                        }
                    }

                    else -> {
                        fadeIn(tween(120)).togetherWith(fadeOut(tween(120)))
                    }
                }
            },
            label = "list_detail",
        ) { routeState ->
            when (routeState) {
                HostScreenRoute.Main -> {
                    saveableStateHolder.SaveableStateProvider("route_main") {
                        MainScreen(
                            scenes = scenes,
                            onSceneSelected = ::openScene,
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = contentWindowInsets,
                        )
                    }
                }

                is HostScreenRoute.Scene -> {
                    val sceneEntry = scenesById[routeState.sceneId]

                    if (sceneEntry != null) {
                        saveableStateHolder.SaveableStateProvider("route_scene_${routeState.sceneId}") {
                            key(routeState.sceneId) {
                                SceneScreen(
                                    sceneEntry = sceneEntry,
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
    }
}

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
