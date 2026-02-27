package oleginvoke.com.composium

/**
 * Marks a [Scene] property for KSP discovery.
 *
 * Only annotated properties are collected into generated scene registry.
 * This avoids full source scanning and keeps KSP processing predictable for large projects.
 *
 * Can be used together with [ComposiumSceneCatalog] for object-level discovery.
 */
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ComposiumScene
