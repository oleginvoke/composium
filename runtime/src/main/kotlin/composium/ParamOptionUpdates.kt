package oleginvoke.com.composium

/** Resolve a valid assignment while retaining the existing preference between equal values. */
internal fun assignedParamOptionName(
    options: List<ParamOption<Any>>,
    preferredName: String?,
    value: Any?,
): String? {
    if (value == null) return null
    var firstMatch: String? = null
    var index = 0
    while (index < options.size) {
        val option = options[index++]
        if (option.value == value) {
            if (option.name == preferredName) return option.name
            if (firstMatch == null) firstMatch = option.name
        }
    }
    return firstMatch
}

/** A single scan on changed options; ordinary delegate reads never call this function. */
internal fun updatedParamOption(
    options: List<ParamOption<Any>>,
    intendedName: String?,
    preferredValueName: String?,
    value: Any,
    preferAssignedValue: Boolean,
    defaultName: String?,
): ParamOption<Any>? {
    var namedOption: ParamOption<Any>? = null
    var firstValueMatch: ParamOption<Any>? = null
    var preferredValueMatch: ParamOption<Any>? = null
    var defaultOption: ParamOption<Any>? = null
    var index = 0
    while (index < options.size) {
        val option = options[index++]
        if (option.name == intendedName) {
            if (!preferAssignedValue) return option
            namedOption = option
        }
        if (option.value == value) {
            if (firstValueMatch == null) firstValueMatch = option
            if (option.name == preferredValueName) {
                if (preferAssignedValue) return option
                preferredValueMatch = option
            }
        }
        if (option.name == defaultName) defaultOption = option
    }
    return if (preferAssignedValue) {
        preferredValueMatch ?: firstValueMatch ?: namedOption ?: defaultOption ?: options.firstOrNull()
    } else {
        namedOption ?: preferredValueMatch ?: firstValueMatch ?: defaultOption ?: options.firstOrNull()
    }
}
