package oleginvoke.com.composium

/**
 * Selectable parameter option.
 *
 * @param value Option value.
 * @param name Option name shown in Composium controls.
 */
data class ParamOption<out T>(
    val value: T,
    val name: String,
)

@PublishedApi
internal fun <T> List<ParamOption<T>>.checkFirstValue(): T = this.let { options ->
    if (options.isEmpty()) {
        error("Param options list is empty. Provide at least one option.")
    } else {
        options.first().value
    }
}

@PublishedApi
internal fun <T> List<ParamOption<T>>.ensureUniqueNames(): List<ParamOption<T>> {
    if (isEmpty()) return this

    val originalNames = map { option -> option.name }.toSet()
    val usedNames = hashSetOf<String>()

    return map { option ->
        val baseName = option.name
        val resolvedName = if (usedNames.add(baseName)) {
            baseName
        } else {
            var suffix = 1
            var candidate = baseName + suffix
            while (candidate in usedNames || candidate in originalNames) {
                suffix++
                candidate = baseName + suffix
            }
            usedNames.add(candidate)
            candidate
        }

        if (resolvedName == option.name) {
            option
        } else {
            option.value named resolvedName
        }
    }
}
