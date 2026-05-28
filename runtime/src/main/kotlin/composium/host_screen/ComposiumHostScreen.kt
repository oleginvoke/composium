package oleginvoke.com.composium.host_screen

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.main_screen.MainScreen
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailCaptureHost
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailCaptureRequest
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailFailureDecision
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailFailureRetryTracker
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailKey
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailQueue
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailStore
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailUnavailableDecision
import oleginvoke.com.composium.scene_thumbnail.buildSceneThumbnailFailureLogMessage
import oleginvoke.com.composium.scene_thumbnail.resolveSceneThumbnailUnavailableDecision
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumHostScreen(
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val scenes = ComposiumRuntime.scenes
    val themeController = LocalComposiumThemeController.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val scenesById by remember {
        derivedStateOf { scenes.associateBy { entry -> entry.id } }
    }
    val sceneIds by remember {
        derivedStateOf { scenes.map { entry -> entry.id } }
    }
    val mainScreenInsets = remember(contentWindowInsets) {
        contentWindowInsets?.only(WindowInsetsSides.Horizontal)
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
    val thumbnailStore = remember { SceneThumbnailStore() }
    val thumbnailQueue = remember { SceneThumbnailQueue() }
    val thumbnailRetryTracker = remember { SceneThumbnailFailureRetryTracker() }
    var visibleSceneIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isMainListScrollInProgress by remember { mutableStateOf(false) }
    var currentCaptureKey by remember { mutableStateOf<SceneThumbnailKey?>(null) }

    val renderedSceneId = state.renderedSceneId
    val renderedSceneEntry = renderedSceneId?.let(scenesById::get)
    val blocksMainScreenInput = shouldBlockMainScreenInput(state)
    val thumbnailKeys = remember(sceneIds, themeController.isDarkTheme) {
        scenes.map { entry ->
            SceneThumbnailKey(
                sceneId = entry.id,
                isDarkTheme = themeController.isDarkTheme,
            )
        }
    }
    val thumbnailKeySet = remember(thumbnailKeys) { thumbnailKeys.toSet() }
    val shouldPauseThumbnailCapture = isMainListScrollInProgress || blocksMainScreenInput
    val currentCaptureEntry = currentCaptureKey?.sceneId?.let(scenesById::get)
    val currentCaptureRequest = currentCaptureKey
        ?.takeUnless { shouldPauseThumbnailCapture }
        ?.let { key ->
            currentCaptureEntry?.let { entry ->
                SceneThumbnailCaptureRequest(
                    key = key,
                    sceneEntry = entry,
                )
            }
        }
    val thumbnailStatesBySceneId = thumbnailStore.statesBySceneId()

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

    LaunchedEffect(thumbnailKeys) {
        thumbnailStore.retain(thumbnailKeySet)
        thumbnailQueue.retain(thumbnailKeySet)
        thumbnailRetryTracker.retain(thumbnailKeySet)
        if (currentCaptureKey != null && currentCaptureKey !in thumbnailKeySet) {
            currentCaptureKey = null
        }
        thumbnailKeys.forEach { key ->
            if (thumbnailStore.needsCapture(key)) {
                thumbnailStore.putPending(key)
                thumbnailQueue.sync(listOf(key))
            }
        }
    }

    LaunchedEffect(visibleSceneIds, thumbnailKeys) {
        val visibleSet = visibleSceneIds.toSet()
        val visibleKeys = thumbnailKeys.filter { key ->
            key.sceneId in visibleSet && thumbnailStore.needsCapture(key)
        }
        visibleKeys.forEach(thumbnailStore::putPending)
        thumbnailQueue.prioritize(visibleKeys)
    }

    LaunchedEffect(shouldPauseThumbnailCapture, currentCaptureKey) {
        val captureKey = currentCaptureKey
        if (shouldPauseThumbnailCapture && captureKey != null) {
            thumbnailStore.putPending(captureKey)
            thumbnailQueue.prioritize(listOf(captureKey))
            currentCaptureKey = null
        }
    }

    LaunchedEffect(thumbnailKeys, shouldPauseThumbnailCapture) {
        while (isActive) {
            if (shouldPauseThumbnailCapture || currentCaptureKey != null) {
                delay(50)
                continue
            }

            var nextKey = thumbnailQueue.next()
            while (nextKey != null && !thumbnailStore.needsCapture(nextKey)) {
                nextKey = thumbnailQueue.next()
            }

            if (nextKey == null) {
                delay(120)
            } else {
                thumbnailStore.putCapturing(nextKey)
                currentCaptureKey = nextKey
            }
        }
    }

    LaunchedEffect(currentCaptureKey, currentCaptureEntry, thumbnailKeySet) {
        val captureKey = currentCaptureKey ?: return@LaunchedEffect
        if (currentCaptureEntry == null) {
            when (
                resolveSceneThumbnailUnavailableDecision(
                    key = captureKey,
                    currentKeys = thumbnailKeySet,
                )
            ) {
                SceneThumbnailUnavailableDecision.Retry -> {
                    thumbnailStore.putPending(captureKey)
                    thumbnailQueue.sync(listOf(captureKey))
                }

                SceneThumbnailUnavailableDecision.Drop -> {
                    thumbnailStore.remove(captureKey)
                    thumbnailQueue.remove(captureKey)
                    thumbnailRetryTracker.clear(captureKey)
                }
            }
            currentCaptureKey = null
        }
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
        SceneThumbnailCaptureHost(
            request = currentCaptureRequest,
            onCaptured = { key, result ->
                thumbnailRetryTracker.clear(key)
                thumbnailStore.putReady(
                    key = key,
                    image = result.image,
                    byteSizeBytes = result.byteSizeBytes,
                    captureScale = result.captureScale,
                )
                if (currentCaptureKey == key) {
                    currentCaptureKey = null
                }
            },
            onFailed = { key, failure ->
                val decision = thumbnailRetryTracker.recordFailure(key)
                val attemptNumber = thumbnailRetryTracker.failureCountFor(key)
                val sceneEntry = scenesById[key.sceneId]
                Log.w(
                    SceneThumbnailLogTag,
                    buildSceneThumbnailFailureLogMessage(
                        key = key,
                        sceneName = sceneEntry?.scene?.name,
                        sceneGroup = sceneEntry?.scene?.group,
                        reason = failure.reason,
                        attemptNumber = attemptNumber,
                        maxAttemptCount = thumbnailRetryTracker.maxAttemptCount,
                        decision = decision,
                    ),
                    failure.throwable,
                )
                when (decision) {
                    SceneThumbnailFailureDecision.Retry -> {
                        thumbnailStore.putPending(key)
                        thumbnailQueue.prioritize(listOf(key))
                    }

                    SceneThumbnailFailureDecision.Fail -> {
                        thumbnailStore.putFailed(key, failure.reason)
                    }
                }
                if (currentCaptureKey == key) {
                    currentCaptureKey = null
                }
            },
        )

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
                thumbnailStates = thumbnailStatesBySceneId,
                onVisibleSceneIdsChanged = { ids -> visibleSceneIds = ids },
                onListScrollInProgressChanged = { inProgress ->
                    isMainListScrollInProgress = inProgress
                },
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

private const val SceneThumbnailLogTag = "ComposiumThumbnail"

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
