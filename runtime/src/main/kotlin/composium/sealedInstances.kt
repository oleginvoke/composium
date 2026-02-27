package oleginvoke.com.composium

import kotlin.reflect.KClass

internal fun <T : Any> sealedObjectInstancesForClass(root: KClass<T>): List<T> {

    fun objectInstanceOrNull(kClass: KClass<*>): Any? {
        // Kotlin path (if available)
        runCatching { kClass.objectInstance }.getOrNull()?.let { return it }

        // JVM fallback for Kotlin object
        return runCatching { kClass.java.getField("INSTANCE").get(null) }.getOrNull()
    }

    fun subclassesOf(kClass: KClass<out T>): List<KClass<out T>> {
        val direct = runCatching { kClass.sealedSubclasses }.getOrDefault(emptyList())

        @Suppress("UNCHECKED_CAST")
        val nested =
            (kClass.java.declaredClasses.asSequence() + kClass.java.classes.asSequence())
                .distinct()
                .map { it.kotlin }
                .filter { kClass.java.isAssignableFrom(it.java) }
                .map { it as KClass<out T> }
                .toList()

        return (direct + nested).distinct()
    }

    fun collect(kClass: KClass<out T>): List<T> {
        val subs = subclassesOf(kClass)
        if (subs.isNotEmpty()) return subs.flatMap { collect(it) }.distinct()

        @Suppress("UNCHECKED_CAST")
        return listOfNotNull(objectInstanceOrNull(kClass) as T?).distinct()
    }

    return collect(root).distinct()
}
