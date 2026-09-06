package oleginvoke.com.composium

internal data class SceneKey(
    val group: String,
    val name: String,
) {
    // Bundle-compatible key for lazy items and saved scene state. The group length
    // makes the boundary unambiguous even when either field contains delimiters.
    fun toSaveableKey(): String = "${group.length}:$group$name"
}
