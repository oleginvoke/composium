package oleginvoke.com.composium

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

internal data class SceneEntry(
    val scene: Scene,
) {
    val id: SceneKey = SceneKey(group = scene.group.orEmpty(), name = scene.name)
}

internal object ComposiumRuntime {

    private val _scenes: SnapshotStateList<SceneEntry> = mutableStateListOf()
    private val registeredScenesById: MutableMap<SceneKey, Scene> = hashMapOf()
    private val warnedSceneIds: MutableSet<SceneKey> = hashSetOf()

    val scenes: List<SceneEntry> get() = _scenes

    fun register(scene: Scene) {
        registerAll(scene)
    }

    fun registerAll(vararg scenes: Scene) {
        registerAll(scenes.asIterable())
    }

    fun registerAll(scenes: Iterable<Scene>) {
        val entriesToAdd = mutableListOf<SceneEntry>()
        scenes.forEach { scene ->
            val entry = SceneEntry(scene)
            val existingScene = registeredScenesById[entry.id]
            if (existingScene == null) {
                registeredScenesById[entry.id] = scene
                entriesToAdd += entry
            } else if (existingScene !== scene && warnedSceneIds.add(entry.id)) {
                val group = entry.id.group.ifEmpty { "<root>" }
                Log.w(
                    "Composium",
                    "Scene \"${scene.name}\" in group \"$group\" is already registered. " +
                        "Another scene with the same group and name was ignored. " +
                        "Use a different name or group.",
                )
            }
        }
        if (entriesToAdd.isNotEmpty()) {
            _scenes.addAll(entriesToAdd)
        }
    }
}
