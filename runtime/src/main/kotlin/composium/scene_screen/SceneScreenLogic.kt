package oleginvoke.com.composium.scene_screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal fun reduceSceneScreen(
    state: SceneScreenState,
    intent: SceneScreenIntent,
): SceneScreenState {
    return when (intent) {
        SceneScreenIntent.ShowControls -> {
            if (state.controlsSheet.isVisible) {
                state
            } else {
                state.copy(controlsSheet = state.controlsSheet.copy(isVisible = true))
            }
        }

        SceneScreenIntent.HideControls -> {
            if (!state.controlsSheet.isVisible) {
                state
            } else {
                state.copy(controlsSheet = state.controlsSheet.copy(isVisible = false))
            }
        }

        is SceneScreenIntent.ControlsTabChanged -> {
            if (state.controlsSheet.selectedTab == intent.tab) {
                state
            } else {
                state.copy(
                    controlsSheet = state.controlsSheet.copy(selectedTab = intent.tab),
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
internal fun rememberSceneScreenStore(): SceneScreenStore {
    return remember { SceneScreenStore() }
}
