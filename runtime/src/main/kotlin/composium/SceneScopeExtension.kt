package oleginvoke.com.composium

@PublishedApi
internal fun defaultOptionName(value: Any?): String = when (value) {
    null -> "null"
    is String, is Number, is Boolean, is Char -> value.toString()
    else -> value.javaClass.simpleName
}
