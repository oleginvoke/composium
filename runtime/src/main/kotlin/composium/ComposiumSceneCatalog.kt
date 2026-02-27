package oleginvoke.com.composium

/**
 * Marks an `object` as a catalog with scene properties for KSP discovery.
 *
 * All non-private properties of type [Scene] declared inside this object are collected
 * into the generated scene registry.
 *
 * Can be used together with [ComposiumScene].
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ComposiumSceneCatalog

/**
 * Deprecated alias for [ComposiumSceneCatalog].
 */
@Deprecated(
    message = "Renamed to ComposiumSceneCatalog",
    replaceWith = ReplaceWith("ComposiumSceneCatalog"),
)
typealias ComposiumScenes = ComposiumSceneCatalog
