package oleginvoke.com.composium

/**
 * Public entry point for scene registration.
 *
 * Recommended mode is automatic registration through KSP-generated registry.
 * If a consumer does not want to use KSP, scenes can be registered manually
 * by passing direct [Scene] references to [register] or [registerAll].
 *
 * Registrations are deduplicated by scene id (`group + name`).
 * Registering the same instance again is silent. A different instance with the same id
 * is ignored with a Logcat warning, once per conflicting id per process.
 */
object Composium {

    /**
     * Registers a single [scene].
     *
     * Safe to call multiple times for the same instance. Conflicting instances are ignored
     * with a warning; the first registered scene is kept.
     */
    fun register(scene: Scene) {
        ComposiumRuntime.register(scene)
    }

    /**
     * Registers all [scenes].
     *
     * Safe to call multiple times for the same instances. Conflicting instances are ignored
     * with a warning; the first registered scene for each id is kept.
     */
    fun registerAll(vararg scenes: Scene) {
        ComposiumRuntime.registerAll(*scenes)
    }

    /**
     * Registers all [scenes].
     *
     * Safe to call multiple times for the same instances. Conflicting instances are ignored
     * with a warning; the first registered scene for each id is kept.
     */
    fun registerAll(scenes: Iterable<Scene>) {
        ComposiumRuntime.registerAll(scenes)
    }
}
