package oleginvoke.com.composium

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver

/** Only values accepted by the host's saved-state registry, or names of current choices. */
internal data class SceneParamSavedState(
    val typeName: String,
    val isActive: Boolean?,
    val isNull: Boolean,
    val value: Any?,
    val lastNonNullValue: Any?,
    val selectedName: String? = null,
    val lastNonNullName: String? = null,
    val intendedName: String? = null,
) {
    fun toSaveableList(): List<Any?> = listOf(
        typeName, isActive, isNull, value, lastNonNullValue,
        selectedName, lastNonNullName, intendedName,
    )

    companion object {
        fun restore(value: Any?): SceneParamSavedState? {
            val fields = value as? List<*> ?: return null
            if (fields.size != 8) return null
            return SceneParamSavedState(
                typeName = fields[0] as? String ?: return null,
                isActive = fields[1] as? Boolean,
                isNull = fields[2] as? Boolean ?: return null,
                value = fields[3],
                lastNonNullValue = fields[4],
                selectedName = fields[5] as? String,
                lastNonNullName = fields[6] as? String,
                intendedName = fields[7] as? String,
            )
        }
    }
}

internal fun sceneScopeSaver(preview: SceneSystemSettings): Saver<SceneScope, Any> = mapSaver(
    save = { scope -> scope.saveParameters(this) },
    restore = { saved ->
        SceneScope(preview, saved.mapNotNull { (name, value) ->
            SceneParamSavedState.restore(value)?.let { name to it }
        }.toMap())
    },
)
