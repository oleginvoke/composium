package oleginvoke.com.composium

/**
 * Public entry point for scene registration.
 *
 * Recommended mode is automatic registration through KSP-generated registry.
 * If a consumer does not want to use KSP, scenes can be registered manually
 * by passing direct [Scene] references to [register] or [registerAll].
 *
 * Registrations are deduplicated by scene id (`group + name`).
 */
object Composium {

    /**
     * Registers a single [scene].
     *
     * Safe to call multiple times for the same scene. Duplicate scene ids are ignored.
     */
    fun register(scene: Scene) {
        ComposiumRuntime.register(scene)
    }

    /**
     * Registers all [scenes].
     *
     * Safe to call multiple times for the same scene set. Duplicate scene ids are ignored.
     */
    fun registerAll(vararg scenes: Scene) {
        ComposiumRuntime.registerAll(*scenes)
    }

    /**
     * Registers all [scenes].
     *
     * Safe to call multiple times for the same scene set. Duplicate scene ids are ignored.
     */
    fun registerAll(scenes: Iterable<Scene>) {
        ComposiumRuntime.registerAll(scenes)
    }
}
