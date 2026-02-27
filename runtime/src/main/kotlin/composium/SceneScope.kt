@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package oleginvoke.com.composium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import oleginvoke.com.composium.scene_screen.SceneParamsCallbacks
import oleginvoke.com.composium.scene_screen.SceneParamsState
import kotlin.internal.NoInfer
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Scope available inside [Scene.content].
 *
 * Provides parameter declaration helpers used by Composium controls UI.
 */
class SceneScope internal constructor() {

    internal val params: SnapshotStateList<ParamDescriptor> = mutableStateListOf()
    internal val preview: SceneSystemSettings = SceneSystemSettings()

    private val paramBindings: MutableMap<String, ParamBinding> = hashMapOf()
    private val paramDescriptorIndexes: MutableMap<String, Int> = hashMapOf()
    private val paramStateSnapshots: MutableMap<String, ParamStateSnapshot> = hashMapOf()

    internal val paramsState: SceneParamsState
        get() = SceneParamsState(params = params)

    internal val paramsCallbacks: SceneParamsCallbacks = object : SceneParamsCallbacks {
        override fun onParamActivationChange(paramName: String, isActive: Boolean) {
            dispatchParamIntent(SceneParamIntent.ParamActivationChanged(paramName = paramName, isActive = isActive))
        }

        override fun onBooleanParamChange(paramName: String, value: Boolean) {
            dispatchParamIntent(SceneParamIntent.BooleanParamChanged(paramName = paramName, value = value))
        }

        override fun onStringParamChange(paramName: String, value: String) {
            dispatchParamIntent(SceneParamIntent.StringParamChanged(paramName = paramName, value = value))
        }

        override fun onObjectParamChange(paramName: String, option: ParamOption<Any>) {
            dispatchParamIntent(SceneParamIntent.ObjectParamChanged(paramName = paramName, option = option))
        }
    }

    /**
     * Optional global ordering for automatically inferred options, keyed by the inferred sealed root.
     * Applied only when user did NOT pass explicit `options` and did NOT pass per-call `autoOptionsOrder`.
     */
    @PublishedApi
    internal val globalAutoOptionsOrder: MutableMap<KClass<*>, (List<Any?>) -> List<Any?>> = hashMapOf()
    @PublishedApi
    internal var globalAutoOptionsOrderVersion: Int = 0

    /**
     * Global cache to avoid repeating sealed reflection work on every recomposition.
     * Key: root KClass, Value: all discovered object instances for that root.
     */
    private object SealedInstancesCache {
        private val cache = HashMap<KClass<*>, List<Any>>()

        @Synchronized
        fun getOrPut(root: KClass<*>, loader: () -> List<Any>): List<Any> {
            return cache[root] ?: loader().also { cache[root] = it }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun sealedInstancesCached(root: KClass<*>): List<Any> =
        SealedInstancesCache.getOrPut(root) {
            sealedObjectInstancesForClass(root as KClass<Any>)
        }

    private sealed interface SceneParamIntent {
        data class ParamActivationChanged(val paramName: String, val isActive: Boolean) : SceneParamIntent
        data class BooleanParamChanged(val paramName: String, val value: Boolean) : SceneParamIntent
        data class StringParamChanged(val paramName: String, val value: String) : SceneParamIntent
        data class ObjectParamChanged(val paramName: String, val option: ParamOption<Any>) : SceneParamIntent
    }

    private sealed interface ParamBinding {
        val name: String
        val typeName: String
        val activationState: MutableState<Boolean>?

        fun toDescriptor(): ParamDescriptor
        fun snapshot(): ParamStateSnapshot

        fun onActivationChanged(isActive: Boolean) = Unit
    }

    private data class ParamStateSnapshot(
        val stateValue: Any?,
        val isActive: Boolean?,
    )

    private data class ParamOptionsSignature(
        val size: Int,
        val contentHash: Int,
    )

    private fun optionsSignature(options: List<ParamOption<*>>?): ParamOptionsSignature? {
        if (options == null) return null

        var hash = 17
        options.forEach { option ->
            hash = 31 * hash + (option.value?.hashCode() ?: 0)
            hash = 31 * hash + option.label.hashCode()
        }
        return ParamOptionsSignature(
            size = options.size,
            contentHash = hash,
        )
    }

    private data class ParamBindingSignature(
        val name: String,
        val defaultValue: Any?,
        val optionsSignature: ParamOptionsSignature?,
        val delegatedType: KType?,
        val delegatedKClass: KClass<*>?,
        val delegatedNullable: Boolean?,
        val declaredKClass: KClass<*>?,
        val declaredNullable: Boolean?,
        val autoOptionsOrderIdentity: Any?,
        val globalAutoOptionsOrderVersion: Int,
    )

    private data class BooleanParamBinding(
        override val name: String,
        override val typeName: String,
        override val activationState: MutableState<Boolean>?,
        val state: MutableState<Any?>,
        val lastNonNullState: MutableState<Boolean?>,
        val defaultRestoreValue: Boolean,
    ) : ParamBinding {
        override fun toDescriptor(): ParamDescriptor {
            val value = (state.value as? Boolean) ?: lastNonNullState.value ?: defaultRestoreValue
            return BooleanParamDescriptor(
                name = name,
                typeName = typeName,
                activation = activationState?.let { ParamActivation(isActive = it.value) },
                value = value,
            )
        }

        override fun snapshot(): ParamStateSnapshot = ParamStateSnapshot(
            stateValue = state.value,
            isActive = activationState?.value,
        )

        override fun onActivationChanged(isActive: Boolean) {
            if (isActive) {
                if (state.value !is Boolean) {
                    val restored = lastNonNullState.value ?: defaultRestoreValue
                    state.value = restored
                    lastNonNullState.value = restored
                }
            } else {
                val current = state.value as? Boolean
                if (current != null) {
                    lastNonNullState.value = current
                }
                state.value = null
            }
        }
    }

    private data class StringParamBinding(
        override val name: String,
        override val typeName: String,
        override val activationState: MutableState<Boolean>?,
        val state: MutableState<Any?>,
        val lastNonNullState: MutableState<String?>,
        val defaultRestoreValue: String,
    ) : ParamBinding {
        override fun toDescriptor(): ParamDescriptor {
            val value = (state.value as? String) ?: lastNonNullState.value ?: defaultRestoreValue
            return StringParamDescriptor(
                name = name,
                typeName = typeName,
                activation = activationState?.let { ParamActivation(isActive = it.value) },
                value = value,
            )
        }

        override fun snapshot(): ParamStateSnapshot = ParamStateSnapshot(
            stateValue = state.value,
            isActive = activationState?.value,
        )

        override fun onActivationChanged(isActive: Boolean) {
            if (isActive) {
                if (state.value !is String) {
                    val restored = lastNonNullState.value ?: defaultRestoreValue
                    state.value = restored
                    lastNonNullState.value = restored
                }
            } else {
                val current = state.value as? String
                if (current != null) {
                    lastNonNullState.value = current
                }
                state.value = null
            }
        }
    }

    private data class ObjectParamBinding(
        override val name: String,
        override val typeName: String,
        override val activationState: MutableState<Boolean>?,
        val state: MutableState<Any?>,
        val lastNonNullState: MutableState<Any?>,
        val defaultRestoreValue: Any?,
        val options: List<ParamOption<Any>>,
        val labelForValue: (Any?) -> String,
    ) : ParamBinding {
        override fun toDescriptor(): ParamDescriptor {
            val currentValue = state.value
            val selectedOption: ParamOption<Any?> = when {
                currentValue != null -> {
                    val matched = options.firstOrNull { option -> option.value == currentValue }
                    if (matched != null) {
                        ParamOption(value = matched.value, label = matched.label)
                    } else {
                        ParamOption(value = currentValue, label = labelForValue(currentValue))
                    }
                }

                else -> {
                    val firstOption = options.firstOrNull()
                    if (firstOption != null) {
                        ParamOption(value = firstOption.value, label = firstOption.label)
                    } else {
                        ParamOption(value = null, label = "Not selected")
                    }
                }
            }
            return ObjectParamDescriptor(
                name = name,
                typeName = typeName,
                activation = activationState?.let { ParamActivation(isActive = it.value) },
                selectedOption = selectedOption,
                options = options,
            )
        }

        override fun snapshot(): ParamStateSnapshot = ParamStateSnapshot(
            stateValue = state.value,
            isActive = activationState?.value,
        )

        override fun onActivationChanged(isActive: Boolean) {
            if (isActive) {
                if (state.value == null) {
                    val restored = lastNonNullState.value ?: defaultRestoreValue ?: options.firstOrNull()?.value
                    if (restored != null) {
                        state.value = restored
                        lastNonNullState.value = restored
                    }
                }
            } else {
                val current = state.value
                if (current != null) {
                    lastNonNullState.value = current
                }
                state.value = null
            }
        }
    }

    private fun dispatchParamIntent(intent: SceneParamIntent) {
        when (intent) {
            is SceneParamIntent.ParamActivationChanged -> {
                val binding = paramBindings[intent.paramName] ?: return
                val activationState = binding.activationState ?: return
                if (activationState.value != intent.isActive) {
                    activationState.value = intent.isActive
                    binding.onActivationChanged(intent.isActive)
                    refreshDescriptorIfStateChanged(intent.paramName)
                }
            }

            is SceneParamIntent.BooleanParamChanged -> {
                val binding = paramBindings[intent.paramName] as? BooleanParamBinding ?: return
                if ((binding.state.value as? Boolean) != intent.value) {
                    binding.state.value = intent.value
                    binding.lastNonNullState.value = intent.value
                    refreshDescriptorIfStateChanged(intent.paramName)
                }
            }

            is SceneParamIntent.StringParamChanged -> {
                val binding = paramBindings[intent.paramName] as? StringParamBinding ?: return
                if ((binding.state.value as? String) != intent.value) {
                    binding.state.value = intent.value
                    binding.lastNonNullState.value = intent.value
                    refreshDescriptorIfStateChanged(intent.paramName)
                }
            }

            is SceneParamIntent.ObjectParamChanged -> {
                val binding = paramBindings[intent.paramName] as? ObjectParamBinding ?: return
                if (binding.state.value != intent.option.value) {
                    binding.state.value = intent.option.value
                    binding.lastNonNullState.value = intent.option.value
                    refreshDescriptorIfStateChanged(intent.paramName)
                }
            }
        }
    }

    private fun registerBinding(binding: ParamBinding) {
        paramBindings[binding.name] = binding
        paramStateSnapshots[binding.name] = binding.snapshot()
        upsertDescriptor(binding.toDescriptor())
    }

    private fun unregisterBinding(paramName: String) {
        val removed = paramBindings.remove(paramName) ?: return
        paramStateSnapshots.remove(paramName)
        removeDescriptor(removed.name)
    }

    private fun refreshDescriptorIfStateChanged(paramName: String) {
        val binding = paramBindings[paramName] ?: return
        val snapshot = binding.snapshot()
        val previousSnapshot = paramStateSnapshots[paramName]
        if (previousSnapshot == snapshot) return

        paramStateSnapshots[paramName] = snapshot
        upsertDescriptor(binding.toDescriptor())
    }

    private fun removeDescriptor(paramName: String) {
        val index = paramDescriptorIndexes.remove(paramName) ?: return
        if (index !in params.indices) return
        params.removeAt(index)
        for (i in index until params.size) {
            paramDescriptorIndexes[params[i].name] = i
        }
    }

    private fun upsertDescriptor(descriptor: ParamDescriptor) {
        val existingIndex = paramDescriptorIndexes[descriptor.name]
        if (existingIndex == null) {
            paramDescriptorIndexes[descriptor.name] = params.size
            params.add(descriptor)
        } else if (params[existingIndex] != descriptor) {
            params[existingIndex] = descriptor
        }
    }

    @PublishedApi
    @Composable
    internal fun <T> paramImpl(
        name: String?,
        default: @NoInfer T,
        options: List<ParamOption<T>>?,
        declaredKClass: KClass<*>?,
        declaredNullable: Boolean?,
        autoOptionsOrder: ((List<T?>) -> List<T?>)? = null,
    ): ParamProperty<T> {

        val state: MutableState<T> = remember { mutableStateOf(default) }
        val lastNonNullBooleanState: MutableState<Boolean?> = remember { mutableStateOf(default as? Boolean) }
        val lastNonNullStringState: MutableState<String?> = remember { mutableStateOf(default as? String) }
        val lastNonNullObjectState: MutableState<Any?> = remember { mutableStateOf(default as Any?) }

        val isActive: MutableState<Boolean>? = if (declaredNullable == true) {
            remember { mutableStateOf(default != null) }
        } else {
            null
        }

        val property: ParamProperty<T> = remember {
            ParamProperty(state).also {
                it.explicitName = name
                it.declaredKClass = declaredKClass
                it.declaredNullable = declaredNullable
            }
        }

        DisposableEffect(property) {
            onDispose {
                property.lastRegisteredName?.let(::unregisterBinding)
                property.lastRegisteredName = null
                property.bindingSignature = null
            }
        }

        SideEffect { property.explicitName = name }

        val explicitOptionItems: List<ParamOption<T>>? = remember(options, default) {
            fun insertedLabel(v: Any?): String = when (v) {
                null -> "null"
                is Enum<*> -> v.name
                else -> defaultOptionLabel(v)
            }

            options
                ?.let { items ->
                    val nonNullItems = items.filter { option -> option.value != null }
                    val defaultValue = default as Any?
                    val hasDefault = defaultValue != null && nonNullItems.any { option -> option.value == default }
                    if (defaultValue != null && !hasDefault) {
                        buildList {
                            add(ParamOption(value = default, label = insertedLabel(defaultValue)))
                            addAll(nonNullItems)
                        }
                    } else {
                        nonNullItems
                    }
                }
        }
        val explicitOptionsSignature = remember(explicitOptionItems) {
            optionsSignature(explicitOptionItems)
        }

        SideEffect {
            val resolvedName = property.resolvedName ?: return@SideEffect
            val signature = ParamBindingSignature(
                name = resolvedName,
                defaultValue = default as Any?,
                optionsSignature = explicitOptionsSignature,
                delegatedType = property.delegatedType,
                delegatedKClass = property.delegatedKClass,
                delegatedNullable = property.delegatedNullable,
                declaredKClass = property.declaredKClass,
                declaredNullable = property.declaredNullable,
                autoOptionsOrderIdentity = autoOptionsOrder,
                globalAutoOptionsOrderVersion = globalAutoOptionsOrderVersion,
            )

            val previousName = property.lastRegisteredName
            if (property.bindingSignature == signature && previousName == resolvedName) {
                refreshDescriptorIfStateChanged(resolvedName)
                return@SideEffect
            }

            if (previousName != null && previousName != resolvedName) {
                unregisterBinding(previousName)
            }

            val optionItems: List<ParamOption<T>>? = explicitOptionItems

            val inferredSealedOptionValues: List<T>? = if (optionItems == null) {
                fun mostSpecificSealedRoot(candidates: List<KClass<*>>): KClass<*>? {
                    if (candidates.isEmpty()) return null
                    return candidates.firstOrNull { candidate ->
                        candidates.none { other -> other != candidate && candidate.java.isAssignableFrom(other.java) }
                    } ?: candidates.first()
                }

                fun inferredFromSealedRoot(): List<T>? {
                    val expectedType = property.delegatedType
                    val delegatedKClass =
                        (expectedType?.classifier as? KClass<*>) ?: property.delegatedKClass
                    val expectedKClass = delegatedKClass ?: property.declaredKClass

                    fun looksLikeSealedRoot(kClass: KClass<*>): Boolean {
                        val instances = sealedInstancesCached(kClass)
                        return instances.any { instance -> instance::class != kClass }
                    }

                    val root: KClass<*>? = expectedKClass?.takeIf { looksLikeSealedRoot(it) }
                        ?: run {
                            val leaf = (default as Any?)?.let { it::class } ?: return null

                            val leafSupertypes: List<KClass<*>> = runCatching {
                                leaf.supertypes.mapNotNull { superType -> superType.classifier as? KClass<*> }
                            }.getOrDefault(emptyList())

                            val sealedCandidates: List<KClass<*>> = buildList {
                                if (looksLikeSealedRoot(leaf)) add(leaf)
                                leafSupertypes.filter { looksLikeSealedRoot(it) }.forEach { add(it) }
                            }.distinct()

                            mostSpecificSealedRoot(sealedCandidates)
                        }

                    if (root == null) return null

                    val sealedInstances: List<Any> = sealedInstancesCached(root)
                    val filtered: List<Any> = sealedInstances.filter { instance -> root.java.isInstance(instance) }

                    val merged: List<Any> = buildList {
                        addAll(filtered)

                        val defaultValue: Any? = default as Any?
                        if (defaultValue != null && root.java.isInstance(defaultValue)) {
                            add(defaultValue)
                        }
                    }.distinct()

                    if (merged.isEmpty()) return null

                    val orderedAny: List<Any> = when {
                        autoOptionsOrder != null -> {
                            @Suppress("UNCHECKED_CAST")
                            val typed = merged.map { it as T? }
                            autoOptionsOrder(typed).mapNotNull { it as Any? }
                        }

                        else -> {
                            globalAutoOptionsOrder[root]?.invoke(merged.map { it as Any? })?.mapNotNull { it } ?: merged
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    return orderedAny.map { it as T }
                }

                inferredFromSealedRoot()
            } else {
                null
            }

            val optionValues: List<T>? = optionItems?.map { it.value } ?: inferredSealedOptionValues

            val labelByValue: Map<Any, String> =
                optionItems?.associate { option -> option.value as Any to option.label } ?: emptyMap()

            fun labelForValue(value: Any?): String {
                val nonNullValue = value ?: return defaultOptionLabel(value)
                return labelByValue[nonNullValue] ?: defaultOptionLabel(nonNullValue)
            }

            val nullableMode = (default as Any?) == null
            val resolvedNullableType =
                property.delegatedType?.isMarkedNullable
                    ?: property.delegatedNullable
                    ?: property.declaredNullable
                    ?: false
            fun resolvedTypeName(baseName: String): String =
                if (resolvedNullableType) "$baseName?" else baseName

            val resolvedTypeKClass =
                (property.delegatedType?.classifier as? KClass<*>) ?: property.delegatedKClass ?: property.declaredKClass
            val isBooleanType = resolvedTypeKClass == Boolean::class
            val isStringType = resolvedTypeKClass == String::class

            val binding: ParamBinding = when {
                optionValues == null && isBooleanType -> {
                    val defaultBoolean = (default as? Boolean) ?: false
                    @Suppress("UNCHECKED_CAST")
                    BooleanParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("Boolean"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullBooleanState,
                        defaultRestoreValue = defaultBoolean,
                    )
                }

                optionValues == null && !nullableMode && default is Enum<*> -> {
                    val enumConstants = default.javaClass.enumConstants as Array<Enum<*>>
                    val enumOptions: List<ParamOption<Any>> =
                        enumConstants.map { enumValue -> ParamOption(value = enumValue as Any, label = enumValue.name) }

                    @Suppress("UNCHECKED_CAST")
                    ObjectParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("Enum"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullObjectState,
                        defaultRestoreValue = default as Any?,
                        options = enumOptions,
                        labelForValue = ::labelForValue,
                    )
                }

                optionValues == null && isStringType -> {
                    val defaultString = (default as? String) ?: ""
                    @Suppress("UNCHECKED_CAST")
                    StringParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("String"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullStringState,
                        defaultRestoreValue = defaultString,
                    )
                }

                !nullableMode && default is Enum<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val enumOptions: List<ParamOption<Any>> =
                        requireNotNull(optionItems).map { option ->
                            ParamOption(option.value as Any, option.label)
                        }

                    @Suppress("UNCHECKED_CAST")
                    ObjectParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("Enum"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullObjectState,
                        defaultRestoreValue = default as Any?,
                        options = enumOptions,
                        labelForValue = ::labelForValue,
                    )
                }

                optionValues != null -> {
                    val anyOptions: List<ParamOption<Any>> = optionValues.mapNotNull { optionValue ->
                        val nonNullValue = optionValue as Any? ?: return@mapNotNull null
                        ParamOption(value = nonNullValue, label = labelForValue(nonNullValue))
                    }

                    @Suppress("UNCHECKED_CAST")
                    ObjectParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("Object"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullObjectState,
                        defaultRestoreValue = default as Any?,
                        options = anyOptions,
                        labelForValue = ::labelForValue,
                    )
                }

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    ObjectParamBinding(
                        name = resolvedName,
                        typeName = resolvedTypeName("Object"),
                        activationState = isActive,
                        state = state as MutableState<Any?>,
                        lastNonNullState = lastNonNullObjectState,
                        defaultRestoreValue = default as Any?,
                        options = emptyList(),
                        labelForValue = ::labelForValue,
                    )
                }
            }

            registerBinding(binding)
            property.bindingSignature = signature
            property.lastRegisteredName = resolvedName
        }

        return property
    }

    /**
     * Declares a scene parameter with a [default] value.
     *
     * @param default Initial parameter value.
     * @param name Optional explicit parameter name. If `null`, delegated property name is used.
     * @param orderTransform Optional per-parameter ordering for automatically inferred options.
     */
    @Composable
    inline fun <reified T> param(
        default: @NoInfer T,
        name: String? = null,
        noinline orderTransform: ((List<T?>) -> List<T?>)? = null,
    ): ParamProperty<T> =
        paramImpl(
            name = name,
            default = default,
            options = null,
            declaredKClass = T::class,
            declaredNullable = (null is T),
            autoOptionsOrder = orderTransform,
        )

    /**
     * Declares a scene parameter from explicit [options].
     *
     * @param options Allowed values with labels.
     * @param name Optional explicit parameter name. If `null`, delegated property name is used.
     */
    @Composable
    inline fun <reified T> param(
        options: List<ParamOption<T>>,
        name: String? = null,
    ): ParamProperty<T> {
        val declaredNullable = (null is T)
        return paramImpl(
            name = name,
            default = if (declaredNullable) null as T else options.checkFirstValue(),
            options = options,
            declaredKClass = T::class,
            declaredNullable = declaredNullable,
        )
    }

    /**
     * Declares a scene parameter with explicit [default] and [options].
     *
     * @param default Initial parameter value.
     * @param options Allowed values with labels.
     * @param name Optional explicit parameter name. If `null`, delegated property name is used.
     */
    @Composable
    inline fun <reified T> param(
        default: T,
        options: List<ParamOption<T>>,
        name: String? = null,
    ): ParamProperty<T> {
        return paramImpl(
            name = name,
            default = default,
            options = options,
            declaredKClass = T::class,
            declaredNullable = (null is T),
        )
    }
}
