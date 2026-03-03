package oleginvoke.com.composium

@PublishedApi
internal fun <T> SceneScope.buildOptionList(values: List<T>, label: (T) -> String): List<ParamOption<T>> {
    return values.map { v -> ParamOption(value = v, label = label(v)) }
}

@PublishedApi
internal fun <T> SceneScope.buildNullableOptionList(
    values: List<T>,
    nullLabel: String,
    label: (T) -> String,
): List<ParamOption<T?>> {
    return buildList {
        add(ParamOption(value = null, label = nullLabel))
        values.forEach { v -> add(ParamOption(value = v, label = label(v))) }
    }
}

/**
 * Builds parameter options from vararg [values] using default labels.
 *
 * @param values Values used as selectable options.
 */
fun <T> SceneScope.optionList(vararg values: T): List<ParamOption<T>> {
    return values.map { value -> ParamOption(value = value, label = defaultOptionLabel(value as Any?)) }
}

/**
 * Builds parameter options from [values] using default labels.
 *
 * @param values Values used as selectable options.
 */
fun <T> SceneScope.optionList(values: List<T>): List<ParamOption<T>> {
    return values.map { value -> ParamOption(value = value, label = defaultOptionLabel(value as Any?)) }
}

/**
 * Builds parameter options from [values] using custom [label].
 *
 * @param values Values used as selectable options.
 * @param label Label mapper for each value.
 */
fun <T> SceneScope.optionList(values: List<T>, label: (T) -> String): List<ParamOption<T>> {
    return values.map { value -> ParamOption(value = value, label = label(value)) }
}

/**
 * Builds parameter options from labeled values.
 *
 * @param options Value-label options.
 */
fun <T> SceneScope.optionList(vararg options: ParamOption<T>): List<ParamOption<T>> {
    return options.toList()
}

@PublishedApi
internal fun defaultOptionLabel(value: Any?): String = when (value) {
    null -> "null"
    is String, is Number, is Boolean, is Char -> value.toString()
    else -> value.javaClass.simpleName
}
