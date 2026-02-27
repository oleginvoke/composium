package oleginvoke.com.composium

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

internal data class SceneEntry(
    val scene: Scene,
) {
    val id: String get() = "${scene.group ?: ""}::${scene.name}"
}

internal object ComposiumRuntime {

    private val _scenes: SnapshotStateList<SceneEntry> = mutableStateListOf()
    private val registeredSceneIds: MutableSet<String> = hashSetOf()

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
            if (registeredSceneIds.add(entry.id)) {
                entriesToAdd += entry
            }
        }
        if (entriesToAdd.isNotEmpty()) {
            _scenes.addAll(entriesToAdd)
        }
    }
}
