package oleginvoke.com.composium.processor

/**
 * Collected scene metadata used during code generation.
 *
 * @param packageName Package of the source declaration.
 * @param propertyName Scene property name.
 * @param referenceExpr Fully qualified access expression for generated code.
 * @param fqn Stable unique key for scene de-duplication.
 */
internal data class SceneInfo(
    val packageName: String,
    val propertyName: String,
    val referenceExpr: String,
    val fqn: String,
)
