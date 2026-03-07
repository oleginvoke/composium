package oleginvoke.com.composium

@PublishedApi
internal fun defaultOptionName(value: Any?): String = when (value) {
    null -> "null"
    is String, is Number, is Boolean, is Char -> value.toString()
    else -> value.javaClass.simpleName
}

@PublishedApi
internal fun naturalStringCompare(left: String, right: String): Int {
    if (left == right) return 0

    var leftIndex = 0
    var rightIndex = 0

    while (leftIndex < left.length && rightIndex < right.length) {
        val leftChar = left[leftIndex]
        val rightChar = right[rightIndex]
        val leftIsDigit = leftChar.isDigit()
        val rightIsDigit = rightChar.isDigit()

        if (leftIsDigit && rightIsDigit) {
            val leftStart = leftIndex
            val rightStart = rightIndex

            while (leftIndex < left.length && left[leftIndex].isDigit()) {
                leftIndex++
            }
            while (rightIndex < right.length && right[rightIndex].isDigit()) {
                rightIndex++
            }

            val leftDigits = left.substring(leftStart, leftIndex)
            val rightDigits = right.substring(rightStart, rightIndex)

            val leftTrimmed = leftDigits.trimStart('0')
            val rightTrimmed = rightDigits.trimStart('0')

            val leftNormalized = leftTrimmed.ifEmpty { "0" }
            val rightNormalized = rightTrimmed.ifEmpty { "0" }

            val byLength = leftNormalized.length.compareTo(rightNormalized.length)
            if (byLength != 0) return byLength

            val byValue = leftNormalized.compareTo(rightNormalized)
            if (byValue != 0) return byValue

            val byOriginalLength = leftDigits.length.compareTo(rightDigits.length)
            if (byOriginalLength != 0) return byOriginalLength

            continue
        }

        val byLowercase = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
        if (byLowercase != 0) return byLowercase

        val byExact = leftChar.compareTo(rightChar)
        if (byExact != 0) return byExact

        leftIndex++
        rightIndex++
    }

    return left.length.compareTo(right.length)
}

@PublishedApi
internal fun compareAutoInferredValues(left: Any?, right: Any?): Int {
    if (left === right) return 0
    if (left == null) return -1
    if (right == null) return 1

    val leftName = defaultOptionName(left)
    val rightName = defaultOptionName(right)
    val byName = naturalStringCompare(leftName, rightName)
    if (byName != 0) return byName

    val byClassName = left.javaClass.name.compareTo(right.javaClass.name)
    if (byClassName != 0) return byClassName

    return left.toString().compareTo(right.toString())
}
