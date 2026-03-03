package oleginvoke.com.composium

@PublishedApi
internal fun <T> SceneScope.buildOptionList(values: List<T>, name: (T) -> String): List<ParamOption<T>> {
    return values.map { value -> value named name(value) }
}

@PublishedApi
internal fun <T> SceneScope.buildNullableOptionList(
    values: List<T>,
    nullName: String,
    name: (T) -> String,
): List<ParamOption<T?>> {
    return buildList {
        add((null as T?) named nullName)
        values.forEach { value -> add(value named name(value)) }
    }
}

/**
 * Builds parameter options from vararg [values] using default names.
 *
 * @param values Values used as selectable options.
 */
fun <T> SceneScope.optionList(vararg values: T): List<ParamOption<T>> {
    return values.map { value -> value named defaultOptionName(value as Any?) }
}

/**
 * Builds parameter options from [values] using default names.
 *
 * @param values Values used as selectable options.
 */
fun <T> SceneScope.optionList(values: List<T>): List<ParamOption<T>> {
    return values.map { value -> value named defaultOptionName(value as Any?) }
}

/**
 * Builds parameter options from [values] using custom [name].
 *
 * @param values Values used as selectable options.
 * @param name Name mapper for each value.
 */
fun <T> SceneScope.optionList(values: List<T>, name: (T) -> String): List<ParamOption<T>> {
    return values.map { value -> value named name(value) }
}

/**
 * Builds parameter options from named values.
 *
 * @param options Value-name options.
 */
fun <T> SceneScope.optionList(vararg options: ParamOption<T>): List<ParamOption<T>> {
    return options.toList()
}

@PublishedApi
internal fun defaultOptionName(value: Any?): String = when (value) {
    null -> "null"
    is String, is Number, is Boolean, is Char -> value.toString()
    else -> value.javaClass.simpleName
}
