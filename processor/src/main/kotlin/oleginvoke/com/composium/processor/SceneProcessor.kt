package oleginvoke.com.composium.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

/**
 * KSP processor that discovers Composium scenes and generates scene registry.
 *
 * Supports:
 * 1. Property-level discovery via `@ComposiumScene`.
 * 2. Object-level discovery via `@ComposiumSceneCatalog`.
 *
 * @param env KSP processing environment.
 */
internal class SceneProcessor(
    env: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val logger = env.logger
    private val codeGenerator: CodeGenerator = env.codeGenerator
    private val options = env.options

    private val sceneFqn = "oleginvoke.com.composium.Scene"
    private val sceneAnnotationFqn = "oleginvoke.com.composium.ComposiumScene"
    private val scenesContainerAnnotationFqn = "oleginvoke.com.composium.ComposiumSceneCatalog"

    private val failIfNoScenes: Boolean =
        options["composium.failIfNoScenes"]?.toBooleanStrictOrNull() ?: false

    private val logSceneDiscovery: Boolean =
        options["composium.logSceneDiscovery"]?.toBooleanStrictOrNull() ?: false

    private val maxScenesPerChunk: Int =
        options["composium.maxScenesPerChunk"]?.toIntOrNull()?.coerceAtLeast(1) ?: 400

    private var generated = false
    private var sawAnyScene = false

    private data class SceneDiscovery(
        val scene: SceneInfo,
        val file: KSFile,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val deferred = mutableListOf<KSAnnotated>()
        val discovered = mutableListOf<SceneDiscovery>()

        collectPropertyAnnotatedScenes(
            resolver = resolver,
            deferred = deferred,
            discovered = discovered,
        )
        collectContainerAnnotatedScenes(
            resolver = resolver,
            deferred = deferred,
            discovered = discovered,
        )

        if (deferred.isNotEmpty()) {
            return deferred
        }

        if (discovered.isEmpty()) {
            if (logSceneDiscovery) {
                logger.info(
                    "[SceneProcessor] No scene properties found for @$sceneAnnotationFqn or @$scenesContainerAnnotationFqn"
                )
            }
            return emptyList()
        }

        sawAnyScene = true

        val scenes = discovered
            .map { it.scene }
            .distinctBy { it.fqn }
            .sortedBy { it.fqn }

        val sourceFiles = discovered
            .map { it.file }
            .distinct()

        generateRegistry(
            scenes = scenes,
            sourceFiles = sourceFiles,
        )
        generated = true
        return emptyList()
    }

    override fun finish() {
        if (failIfNoScenes && !sawAnyScene) {
            logger.error(
                "[SceneProcessor] No scene properties found and composium.failIfNoScenes=true"
            )
        }
    }

    private fun collectPropertyAnnotatedScenes(
        resolver: Resolver,
        deferred: MutableList<KSAnnotated>,
        discovered: MutableList<SceneDiscovery>,
    ) {
        resolver
            .getSymbolsWithAnnotation(sceneAnnotationFqn)
            .forEach { symbol ->
                val property = symbol as? KSPropertyDeclaration
                if (property == null) {
                    logger.error("[SceneProcessor] @$sceneAnnotationFqn can only annotate properties", symbol)
                    return@forEach
                }

                if (!property.validate()) {
                    deferred += property
                    return@forEach
                }

                val file = property.containingFile
                if (file == null) {
                    logger.error("[SceneProcessor] Annotated property must belong to a source file", property)
                    return@forEach
                }

                val ownerFqn = (property.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
                val info = toSceneInfoOrNull(
                    packageName = file.packageName.asString(),
                    prop = property,
                    ownerFqn = ownerFqn,
                )

                if (info == null) {
                    logger.error(
                        "[SceneProcessor] @ComposiumScene property must be non-private and have type $sceneFqn",
                        property,
                    )
                    return@forEach
                }

                discovered += SceneDiscovery(scene = info, file = file)
                if (logSceneDiscovery) {
                    logger.info("[SceneProcessor] Found Scene: ${info.fqn}")
                }
            }
    }

    private fun collectContainerAnnotatedScenes(
        resolver: Resolver,
        deferred: MutableList<KSAnnotated>,
        discovered: MutableList<SceneDiscovery>,
    ) {
        resolver
            .getSymbolsWithAnnotation(scenesContainerAnnotationFqn)
            .forEach { symbol ->
                val container = symbol as? KSClassDeclaration
                if (container == null) {
                    logger.error("[SceneProcessor] @$scenesContainerAnnotationFqn can only annotate objects", symbol)
                    return@forEach
                }

                if (container.classKind != ClassKind.OBJECT) {
                    logger.error("[SceneProcessor] @$scenesContainerAnnotationFqn can only annotate objects", container)
                    return@forEach
                }

                if (!container.validate()) {
                    deferred += container
                    return@forEach
                }

                val ownerFqn = container.qualifiedName?.asString()
                if (ownerFqn == null) {
                    logger.error("[SceneProcessor] Annotated object must have a qualified name", container)
                    return@forEach
                }

                val containerFile = container.containingFile
                if (containerFile == null) {
                    logger.error("[SceneProcessor] Annotated object must belong to a source file", container)
                    return@forEach
                }

                container.declarations
                    .filterIsInstance<KSPropertyDeclaration>()
                    .forEach { property ->
                        if (!property.validate()) {
                            deferred += property
                            return@forEach
                        }

                        if (!isSceneProperty(property)) {
                            return@forEach
                        }

                        if (Modifier.PRIVATE in property.modifiers) {
                            logger.error(
                                "[SceneProcessor] Scene property in @$scenesContainerAnnotationFqn object must be non-private",
                                property,
                            )
                            return@forEach
                        }

                        val file = property.containingFile ?: containerFile
                        val info = toSceneInfoOrNull(
                            packageName = file.packageName.asString(),
                            prop = property,
                            ownerFqn = ownerFqn,
                        )

                        if (info == null) {
                            logger.error(
                                "[SceneProcessor] Failed to collect scene property from @$scenesContainerAnnotationFqn object",
                                property,
                            )
                            return@forEach
                        }

                        discovered += SceneDiscovery(scene = info, file = file)
                        if (logSceneDiscovery) {
                            logger.info("[SceneProcessor] Found Scene: ${info.fqn}")
                        }
                    }
            }
    }

    private fun isSceneProperty(prop: KSPropertyDeclaration): Boolean {
        val getterTypeFqn = prop.getter
            ?.returnType
            ?.resolve()
            ?.declaration
            ?.qualifiedName
            ?.asString()

        val declaredTypeFqn = prop.type
            .resolve()
            .declaration
            .qualifiedName
            ?.asString()

        return getterTypeFqn == sceneFqn || declaredTypeFqn == sceneFqn
    }

    private fun toSceneInfoOrNull(
        packageName: String,
        prop: KSPropertyDeclaration,
        ownerFqn: String?,
    ): SceneInfo? {
        if (Modifier.PRIVATE in prop.modifiers) return null
        if (!isSceneProperty(prop)) return null

        val propertyName = prop.simpleName.asString()

        val referenceExpr = buildReferenceExpr(
            filePackage = packageName,
            ownerFqn = ownerFqn,
            propertyName = propertyName
        )

        return SceneInfo(
            packageName = packageName,
            propertyName = propertyName,
            referenceExpr = referenceExpr,
            fqn = referenceExpr
        )
    }

    private fun buildReferenceExpr(
        filePackage: String,
        ownerFqn: String?,
        propertyName: String
    ): String {
        val owner = (ownerFqn ?: filePackage).removeSuffix(".Companion")
        return "$owner.$propertyName"
    }

    private fun generateRegistry(
        scenes: List<SceneInfo>,
        sourceFiles: List<KSFile>,
    ) {
        val packageName = "oleginvoke.com.composium.generated"
        val className = "GeneratedSceneRegistry"

        val dependencies = Dependencies(aggregating = true, sources = sourceFiles.toTypedArray())

        val stream = try {
            codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName = packageName,
                fileName = className,
            )
        } catch (t: Throwable) {
            logger.error("[SceneProcessor] Failed to create output file: ${t::class.simpleName}: ${t.message}")
            throw t
        }

        stream.writer().use { out ->
            out.appendLine("package $packageName")
            out.appendLine()
            out.appendLine("import oleginvoke.com.composium.Scene")
            out.appendLine()
            out.appendLine("internal object $className {")
            out.appendLine("    fun collectAll(): List<Scene> {")
            out.appendLine("        val result = ArrayList<Scene>(${scenes.size})")
            val chunks = scenes.chunked(maxScenesPerChunk)
            chunks.indices.forEach { chunkIndex ->
                out.appendLine("        appendChunk$chunkIndex(result)")
            }
            out.appendLine("        return result")
            out.appendLine("    }")
            out.appendLine()
            chunks.forEachIndexed { chunkIndex, chunkScenes ->
                out.appendLine("    private fun appendChunk$chunkIndex(target: MutableList<Scene>) {")
                chunkScenes.forEach { scene ->
                    out.appendLine("        target += ${scene.referenceExpr}")
                }
                out.appendLine("    }")
                out.appendLine()
            }
            out.appendLine("}")
        }
    }
}
