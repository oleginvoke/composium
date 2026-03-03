package oleginvoke.com.composium

/**
 * Selectable parameter option.
 *
 * @param value Option value.
 * @param label Option label shown in Composium controls.
 */
data class ParamOption<out T>(
    val value: T,
    val label: String,
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
internal fun <T> List<ParamOption<T>>.ensureUniqueLabels(): List<ParamOption<T>> {
    if (isEmpty()) return this

    val originalLabels = map { option -> option.label }.toSet()
    val usedLabels = hashSetOf<String>()

    return map { option ->
        val baseLabel = option.label
        val resolvedLabel = if (usedLabels.add(baseLabel)) {
            baseLabel
        } else {
            var suffix = 1
            var candidate = baseLabel + suffix
            while (candidate in usedLabels || candidate in originalLabels) {
                suffix++
                candidate = baseLabel + suffix
            }
            usedLabels.add(candidate)
            candidate
        }

        if (resolvedLabel == option.label) {
            option
        } else {
            ParamOption(
                value = option.value,
                label = resolvedLabel,
            )
        }
    }
}
