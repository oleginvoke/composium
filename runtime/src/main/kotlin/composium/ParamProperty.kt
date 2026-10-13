package oleginvoke.com.composium

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/** Identity distinguishes consecutive writes, including writes back to an earlier value. */
internal class ParamValueAssignment(
    val optionName: String?,
    val lastNonNullValue: Any?,
    val hasNonNullAssignment: Boolean,
)

/**
 * Property delegate returned by [SceneScope.param].
 * Obtain instances through [SceneScope.param] so controls registration is configured.
 *
 * @param state Backing Compose state.
 */
class ParamProperty<T> internal constructor(
    internal val state: MutableState<T>,
) : ReadWriteProperty<Any?, T> {

    internal var explicitName: String? = null
    internal var delegatedName: String? = null

    internal var delegatedType: KType? = null
    internal var delegatedKClass: KClass<*>? = null
    internal var delegatedNullable: Boolean? = null

    internal var declaredKClass: KClass<*>? = null
    internal var declaredNullable: Boolean? = null

    internal var bindingSignature: Any? = null
    internal var lastRegisteredName: String? = null

    internal var objectOptions: List<ParamOption<Any>>? = null
    internal var unregisteredOptions: List<ParamOption<Any>>? = null
    internal var intendedOptionName: String? = null
    internal var activationState: MutableState<Boolean>? = null
    internal var onValueAssigned: ((Any?) -> Unit)? = null
    internal val valueAssignmentState = mutableStateOf<ParamValueAssignment?>(null)
    internal var acknowledgedValueAssignment: ParamValueAssignment? = null
    internal val hasPendingValueAssignment: Boolean
        get() = valueAssignmentState.value !== acknowledgedValueAssignment

    internal fun acknowledgeValueAssignment() {
        acknowledgedValueAssignment = valueAssignmentState.value
    }

    internal val resolvedName: String?
        get() = explicitName ?: delegatedName

    /**
     * Captures delegated property metadata.
     *
     * @param thisRef Owner object of delegated property.
     * @param property Delegated property metadata.
     */
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadWriteProperty<Any?, T> {
        delegatedName = delegatedName ?: property.name

        val rt = delegatedType ?: runCatching { property.returnType }.getOrNull()
        delegatedType = rt

        if (rt != null) {
            delegatedKClass = rt.classifier as? KClass<*>
            delegatedNullable = rt.isMarkedNullable
        }

        return this
    }

    /**
     * Returns current parameter value.
     *
     * @param thisRef Owner object of delegated property.
     * @param property Delegated property metadata.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    /**
     * Updates parameter value.
     * Nullable assignments synchronize activity and preserve the last non-null selection.
     *
     * @param thisRef Owner object of delegated property.
     * @param property Delegated property metadata.
     * @param value New value.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Preserve Compose's no-op policy for equal writes, including SideEffect writes.
        val requestedActivity = value != null
        val activityChanged = activationState?.value?.let { it != requestedActivity } == true
        if (state.value == value && !activityChanged) return
        state.value = value
        if (activityChanged) activationState?.value = requestedActivity
        onValueAssigned?.invoke(value)
    }
}
