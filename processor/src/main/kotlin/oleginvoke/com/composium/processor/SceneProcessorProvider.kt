package oleginvoke.com.composium.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP provider for Composium scene processor.
 */
class SceneProcessorProvider : SymbolProcessorProvider {
    /**
     * Creates a new scene processor instance.
     *
     * @param environment KSP processing environment.
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SceneProcessor(environment)
    }
}
