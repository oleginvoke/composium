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
    if(options.isEmpty()) {
        error("Param options list is empty. Provide at least one option.")
    } else {
        options.first().value
    }
}
