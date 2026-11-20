package oleginvoke.com.composium.host_screen

import androidx.compose.runtime.saveable.listSaver
import oleginvoke.com.composium.SceneKey

internal data class ComposiumHostScreenState(
    val selectedSceneId: SceneKey? = null,
    val renderedSceneId: SceneKey? = null,
    val isSceneVisible: Boolean = false,
)

// Save the destination, not an in-flight entrance/exit animation.
internal val ComposiumHostScreenStateSaver = listSaver<ComposiumHostScreenState, String>(
    save = { state -> state.selectedSceneId?.let { listOf(it.group, it.name) } ?: emptyList() },
    restore = { saved ->
        val sceneId = saved.takeIf { it.size == 2 }?.let { SceneKey(it[0], it[1]) }
        ComposiumHostScreenState(sceneId, sceneId, isSceneVisible = sceneId != null)
    },
)

internal sealed interface ComposiumHostScreenIntent {
    data class SceneSelected(val sceneId: SceneKey) : ComposiumHostScreenIntent
    data object SceneClosed : ComposiumHostScreenIntent
    data object TransitionSettled : ComposiumHostScreenIntent
    data class AvailableScenesChanged(val sceneIds: Set<SceneKey>) : ComposiumHostScreenIntent
}

internal fun shouldBlockMainScreenInput(state: ComposiumHostScreenState): Boolean {
    return state.renderedSceneId != null
}

internal fun reduceComposiumHostScreen(
    state: ComposiumHostScreenState,
    intent: ComposiumHostScreenIntent,
): ComposiumHostScreenState {
    return when (intent) {
        is ComposiumHostScreenIntent.SceneSelected -> {
            if (
                state.selectedSceneId == intent.sceneId &&
                state.renderedSceneId == intent.sceneId &&
                state.isSceneVisible
            ) {
                state
            } else {
                state.copy(
                    selectedSceneId = intent.sceneId,
                    renderedSceneId = intent.sceneId,
                    isSceneVisible = true,
                )
            }
        }

        ComposiumHostScreenIntent.SceneClosed -> {
            if (state.selectedSceneId == null && !state.isSceneVisible) {
                state
            } else {
                state.copy(
                    selectedSceneId = null,
                    isSceneVisible = false,
                )
            }
        }

        ComposiumHostScreenIntent.TransitionSettled -> {
            if (state.isSceneVisible || state.renderedSceneId == null) {
                state
            } else {
                state.copy(renderedSceneId = state.selectedSceneId)
            }
        }

        is ComposiumHostScreenIntent.AvailableScenesChanged -> {
            val selectedSceneId = state.selectedSceneId
            val renderedSceneId = state.renderedSceneId
            val selectedExists = selectedSceneId != null && selectedSceneId in intent.sceneIds
            val renderedExists = renderedSceneId != null && renderedSceneId in intent.sceneIds

            when {
                selectedExists -> state
                !state.isSceneVisible && !renderedExists && renderedSceneId == null -> state
                else -> state.copy(
                    selectedSceneId = null,
                    renderedSceneId = if (state.isSceneVisible || renderedExists) renderedSceneId else null,
                    isSceneVisible = false,
                )
            }
        }
    }
}
