package oleginvoke.com.composium

import androidx.compose.runtime.MutableState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/**
 * Property delegate returned by [SceneScope.param].
 *
 * @param state Backing Compose state.
 */
class ParamProperty<T>(
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
     *
     * @param thisRef Owner object of delegated property.
     * @param property Delegated property metadata.
     * @param value New value.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
    }
}
