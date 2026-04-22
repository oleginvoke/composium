package oleginvoke.com.composium.scene_screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

internal const val DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION = 0.60f
internal const val MIN_SCENE_INSPECTOR_CLOSE_SNAP_FRACTION = 0.18f
internal const val MAX_SCENE_INSPECTOR_EXPAND_SNAP_FRACTION = 0.84f

private const val MIN_SCENE_INSPECTOR_DRAG_FRACTION = 0.08f
private const val MAX_SCENE_INSPECTOR_DRAG_FRACTION = 0.92f

internal data class SceneLayoutMetrics(
    val previewVisibleHeightPx: Int,
    val previewComposedHeightPx: Int,
    val inspectorHeightPx: Int,
)

internal fun calculateSceneLayoutMetrics(
    availableHeightPx: Int,
    mode: SceneInspectorLayoutMode,
    splitFraction: Float,
): SceneLayoutMetrics {
    if (availableHeightPx <= 0) {
        return SceneLayoutMetrics(
            previewVisibleHeightPx = 0,
            previewComposedHeightPx = 0,
            inspectorHeightPx = 0,
        )
    }

    val resolvedSplitFraction = splitFraction.coerceIn(0f, 1f)

    return when (mode) {
        SceneInspectorLayoutMode.Closed -> SceneLayoutMetrics(
            previewVisibleHeightPx = availableHeightPx,
            previewComposedHeightPx = availableHeightPx,
            inspectorHeightPx = 0,
        )

        SceneInspectorLayoutMode.Split -> {
            val inspectorHeightPx = (availableHeightPx * resolvedSplitFraction).roundToInt()
                .coerceIn(0, availableHeightPx)
            val previewHeightPx = availableHeightPx - inspectorHeightPx
            SceneLayoutMetrics(
                previewVisibleHeightPx = previewHeightPx,
                previewComposedHeightPx = previewHeightPx,
                inspectorHeightPx = inspectorHeightPx,
            )
        }

        SceneInspectorLayoutMode.Expanded -> SceneLayoutMetrics(
            previewVisibleHeightPx = 0,
            previewComposedHeightPx = 1,
            inspectorHeightPx = availableHeightPx,
        )
    }
}

internal fun shouldShowSceneSplitDivider(
    mode: SceneInspectorLayoutMode,
    hasScrollableOverflow: Boolean,
): Boolean {
    return mode == SceneInspectorLayoutMode.Split && hasScrollableOverflow
}

internal fun reduceSceneScreen(
    state: SceneScreenState,
    intent: SceneScreenIntent,
): SceneScreenState {
    return when (intent) {
        SceneScreenIntent.ShowControls -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Closed) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Split,
                        splitFraction = state.controlsSheet.splitFraction.restoreVisibleSplitFraction(),
                    ),
                )
            }
        }

        SceneScreenIntent.HideControls -> {
            if (state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Closed) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Closed,
                    ),
                )
            }
        }

        SceneScreenIntent.ExpandControls -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Split) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Expanded,
                    ),
                )
            }
        }

        SceneScreenIntent.RestoreSplitControls -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Expanded) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Split,
                        splitFraction = state.controlsSheet.splitFraction.restoreVisibleSplitFraction(),
                    ),
                )
            }
        }

        SceneScreenIntent.NavigateBackFromExpandedControls -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Expanded) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Split,
                        splitFraction = state.controlsSheet.splitFraction.restoreVisibleSplitFraction(),
                    ),
                )
            }
        }

        is SceneScreenIntent.UpdateSplitFraction -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Split) {
                state
            } else {
                val resolvedFraction = intent.fraction.clampDraggedSplitFraction()
                if (resolvedFraction == state.controlsSheet.splitFraction) {
                    state
                } else {
                    state.copy(
                        controlsSheet = state.controlsSheet.copy(
                            splitFraction = resolvedFraction,
                        ),
                    )
                }
            }
        }

        SceneScreenIntent.SettleSplitFraction -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Split) {
                state
            } else {
                when {
                    state.controlsSheet.splitFraction <= MIN_SCENE_INSPECTOR_CLOSE_SNAP_FRACTION -> {
                        state.copy(
                            controlsSheet = state.controlsSheet.copy(
                                layoutMode = SceneInspectorLayoutMode.Closed,
                            ),
                        )
                    }

                    state.controlsSheet.splitFraction >= MAX_SCENE_INSPECTOR_EXPAND_SNAP_FRACTION -> {
                        state.copy(
                            controlsSheet = state.controlsSheet.copy(
                                layoutMode = SceneInspectorLayoutMode.Expanded,
                            ),
                        )
                    }

                    else -> state
                }
            }
        }

        SceneScreenIntent.PreviewPaneTapped -> {
            if (state.controlsSheet.layoutMode != SceneInspectorLayoutMode.Split) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        layoutMode = SceneInspectorLayoutMode.Closed,
                    ),
                )
            }
        }

        is SceneScreenIntent.SelectTab -> {
            if (state.controlsSheet.selectedTab == intent.tab) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(
                        selectedTab = intent.tab,
                    ),
                )
            }
        }

    }
}

@Stable
internal class SceneScreenStore(
    initialState: SceneScreenState = SceneScreenState(),
) {
    var state by mutableStateOf(initialState)
        private set

    fun dispatch(intent: SceneScreenIntent) {
        val reducedState = reduceSceneScreen(
            state = state,
            intent = intent,
        )
        if (reducedState != state) {
            state = reducedState
        }
    }
}

@Composable
internal fun rememberSceneScreenStore(key: Any?): SceneScreenStore {
    return remember(key) { SceneScreenStore() }
}

private fun Float.clampDraggedSplitFraction(): Float {
    return coerceIn(
        minimumValue = MIN_SCENE_INSPECTOR_DRAG_FRACTION,
        maximumValue = MAX_SCENE_INSPECTOR_DRAG_FRACTION,
    )
}

private fun Float.restoreVisibleSplitFraction(): Float {
    return if (this in MIN_SCENE_INSPECTOR_CLOSE_SNAP_FRACTION..MAX_SCENE_INSPECTOR_EXPAND_SNAP_FRACTION) {
        this
    } else {
        DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION
    }
}
