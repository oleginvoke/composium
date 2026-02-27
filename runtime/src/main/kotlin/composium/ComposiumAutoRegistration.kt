package oleginvoke.com.composium

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

internal object ComposiumAutoRegistration {

    private const val GENERATED_FQN =
        "oleginvoke.com.composium.generated.GeneratedSceneRegistry"

    private val registered = AtomicBoolean(false)
    private val registrationAttempted = AtomicBoolean(false)

    fun registerGeneratedScenes() {
        if (registered.get() || registrationAttempted.get()) return

        val ok = synchronized(this) {
            if (registered.get() || registrationAttempted.get()) {
                return@synchronized registered.get()
            }
            registrationAttempted.set(true)

            runCatching {
                val clazz = Class.forName(GENERATED_FQN)

                val instance = clazz.getDeclaredField("INSTANCE").get(null)
                val method = clazz.getDeclaredMethod("collectAll")

                val collected = method.invoke(instance)
                val scenes: List<Scene> = when (collected) {
                    is Iterable<*> -> collected.filterIsInstance<Scene>()
                    is Array<*> -> collected.filterIsInstance<Scene>()
                    else -> emptyList()
                }

                ComposiumRuntime.registerAll(scenes)
                true
            }.getOrElse { t ->
                Log.d("Composium", "GeneratedSceneRegistry not available / invoke failed: $t")
                false
            }
        }

        if (ok) {
            registered.set(true)
        }
    }
}
