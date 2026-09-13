package oleginvoke.com.composium.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameterBase
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ComposiumContentPaddingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve()
                if (!node.isScenePaddingHostCall(context, method)) return
                val contentLambda = node.findContentLambda(method) ?: return
                if (contentLambda.usesItsPaddingParameter()) return
                context.report(
                    issue = UnusedComposiumContentPaddingParameter,
                    scope = contentLambda,
                    location = context.getLocation(contentLambda),
                    message = "${node.paddingHostName(method)} contentPadding is not used",
                )
            }
        }

    private fun UCallExpression.isScenePaddingHostCall(
        context: JavaContext,
        method: PsiMethod?,
    ): Boolean {
        if (method != null) {
            val contentType = method.parameterList.parameters
                .firstOrNull { it.name == ContentParameterName }
                ?.type?.canonicalText ?: return false
            return SceneScopeClassName in contentType && PaddingValuesClassName in contentType
        }

        val calledMethodName = methodName ?: methodIdentifier?.name ?: return false
        if (calledMethodName !in PublicHostNames) return false
        val qualifiedCall = (uastParent as? UQualifiedReferenceExpression)
            ?.takeIf { it.selector === this }
        val callReceiver = qualifiedCall?.receiver ?: receiver
        if (callReceiver != null) {
            return (callReceiver.sourcePsi?.text ?: callReceiver.asSourceString()).trim() ==
                ComposiumPackage
        }

        val containingFile = (sourcePsi?.containingFile ?: context.psiFile) as? KtFile
        return containingFile?.packageFqName?.asString() == ComposiumPackage ||
            containingFile?.importDirectives.orEmpty().any {
                it.importedFqName?.asString() == "$ComposiumPackage.$calledMethodName"
            }
    }

    private fun UCallExpression.paddingHostName(method: PsiMethod?): String =
        if (method?.isConstructor == true) {
            method.containingClass?.name ?: "Scene"
        } else {
            method?.name ?: methodName ?: methodIdentifier?.name ?: "scene"
        }

    private fun UCallExpression.findContentLambda(method: PsiMethod?): ULambdaExpression? {
        if (method == null) {
            return valueArguments.asReversed().firstNotNullOfOrNull { it.unwrapLambda() }
        }
        val contentParameterIndex = method.parameterList.parameters.indexOfFirst {
            it.name == ContentParameterName
        }
        if (contentParameterIndex < 0) return null
        return getArgumentForParameter(contentParameterIndex).unwrapLambda()
    }

    private fun ULambdaExpression.usesItsPaddingParameter(): Boolean {
        var usesPadding = false
        val parameter = contentPaddingParameter()
        accept(object : AbstractUastVisitor() {
            override fun visitElement(node: UElement): Boolean {
                if (usesPadding) return true
                if (
                    node is UReferenceExpression &&
                    node.isContentPaddingReference(parameter, this@usesItsPaddingParameter)
                ) {
                    usesPadding = true
                    return true
                }
                return super.visitElement(node)
            }
        })
        return usesPadding
    }

    private fun ULambdaExpression.contentPaddingParameter(): ContentPaddingParameter {
        val elements = valueParameters.flatMap {
            listOfNotNull(it.sourcePsi, it.javaPsi).map { element -> element.parameterDeclaration() }
        }.toSet()
        return ContentPaddingParameter(
            names = valueParameters.mapNotNull { it.name }.toSet(),
            elements = elements,
            textRanges = elements.mapNotNullTo(mutableSetOf()) { it.textRange },
        )
    }

    private fun PsiElement.parameterDeclaration(): PsiElement =
        (this as? UastKotlinPsiParameterBase<*>)?.ktOrigin ?: navigationElement

    private fun UReferenceExpression.isContentPaddingReference(
        parameter: ContentPaddingParameter,
        contentLambda: ULambdaExpression,
    ): Boolean {
        val referenceName = resolvedName ?: return false
        if (referenceName !in parameter.names) return false
        if (isSelectorOfQualifiedReference()) return false
        val isSimpleReference = (sourcePsi?.text ?: asSourceString()).trim() == referenceName
        val resolved = resolve()
        if (resolved != null) {
            val declaration = resolved.parameterDeclaration()
            return declaration in parameter.elements ||
                (declaration.containingFile == contentLambda.sourcePsi?.containingFile &&
                    declaration.textRange in parameter.textRanges)
        }
        return isSimpleReference &&
            !isShadowedByNestedLambdaParameter(referenceName, contentLambda)
    }

    private fun UReferenceExpression.isSelectorOfQualifiedReference(): Boolean {
        val qualifiedParent = uastParent as? UQualifiedReferenceExpression ?: return false
        if (qualifiedParent.selector === this) return true
        val referenceRange = sourcePsi?.textRange ?: return false
        return qualifiedParent.selector.sourcePsi?.textRange == referenceRange
    }

    private fun UElement.isShadowedByNestedLambdaParameter(
        referenceName: String,
        contentLambda: ULambdaExpression,
    ): Boolean {
        var parent = uastParent
        while (parent != null && parent !== contentLambda) {
            if (
                parent is ULambdaExpression &&
                parent.valueParameters.any { it.name == referenceName }
            ) return true
            parent = parent.uastParent
        }
        return false
    }

    private data class ContentPaddingParameter(
        val names: Set<String>,
        val elements: Set<PsiElement>,
        val textRanges: Set<TextRange>,
    )

    private fun UElement?.unwrapLambda(): ULambdaExpression? = when (this) {
        is ULambdaExpression -> this
        is UParenthesizedExpression -> expression.unwrapLambda()
        is UQualifiedReferenceExpression -> selector.unwrapLambda()
        else -> null
    }

    companion object {
        val UnusedComposiumContentPaddingParameter: Issue = Issue.create(
            id = "UnusedComposiumContentPaddingParameter",
            briefDescription = "Scene contentPadding is not used",
            explanation = """
                Composium scenes receive contentPadding for system bars and scene tools.
                Apply or forward this parameter. For intentional full-bleed content,
                explicitly suppress UnusedComposiumContentPaddingParameter.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.ERROR,
            implementation = Implementation(
                ComposiumContentPaddingDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )

        private const val ComposiumPackage = "oleginvoke.com.composium"
        private const val SceneScopeClassName = "$ComposiumPackage.SceneScope"
        private const val PaddingValuesClassName = "androidx.compose.foundation.layout.PaddingValues"
        private const val ContentParameterName = "content"
        private val PublicHostNames = setOf("scene", "Scene")
    }
}
