package oleginvoke.com.composium.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class ComposiumContentPaddingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposiumContentPaddingDetector()

    override fun getIssues(): List<Issue> = listOf(
        ComposiumContentPaddingDetector.UnusedComposiumContentPaddingParameter,
    )

    fun testNamedPaddingUse_isClean() {
        check("scene { contentPadding -> consume(contentPadding) }").expectClean()
    }

    fun testImplicitPaddingUse_isClean() {
        check("scene { consume(it) }").expectClean()
    }

    fun testForwardedWrapperPadding_isClean() {
        check("sceneWrapper { padding -> consume(padding) }").expectClean()
    }

    fun testUnusedNamedPadding_reportsError() {
        check("scene { contentPadding -> Unit }").expectUnused()
    }

    fun testUnusedImplicitPadding_reportsError() {
        check("scene { Unit }").expectUnused()
    }

    fun testUnderscorePadding_reportsError() {
        check("scene { _ -> Unit }").expectUnused()
    }

    fun testSuppressedUnusedPadding_isClean() {
        check("""@Suppress("UnusedComposiumContentPaddingParameter") scene { Unit }""")
            .expectClean()
    }

    fun testShadowedPadding_reportsError() {
        check("scene { contentPadding -> listOf(1).forEach { contentPadding -> consume(contentPadding) } }")
            .expectUnused()
    }

    fun testUnrelatedContentPaddingProperty_reportsError() {
        check("scene { contentPadding -> consume(Holder().contentPadding) }").expectUnused()
    }

    fun testSameNamedFunctionWithWrongSignature_isIgnored() {
        lint().files(
            kotlin("""
                package oleginvoke.com.composium
                fun scene(content: (String) -> Unit) = content("")
                fun example() { scene { Unit } }
            """).indented(),
        ).allowMissingSdk().run().expectClean()
    }

    fun testDirectSceneConstructorIsChecked() {
        check("Scene { contentPadding -> Unit }").expectUnused("Scene")
    }

    fun testTopSpacerNameDoesNotCountAsUsage() {
        check("scene { TopSpacer() }").expectUnused()
    }

    fun testBottomSpacerNameDoesNotCountAsUsage() {
        check("scene { BottomSpacer() }").expectUnused()
    }

    fun testConsumeParentScaffoldPaddingNameDoesNotCountAsUsage() {
        check("scene { consumeParentScaffoldPadding() }").expectUnused()
    }

    fun testUnusedWrapperPadding_reportsError() {
        check("sceneWrapper { padding -> Unit }").expectUnused("sceneWrapper")
    }

    fun testImplicitNestedLambdaShadow_reportsError() {
        check("scene { listOf(1).forEach { consume(it) } }").expectUnused()
    }

    fun testCapturedPaddingInNestedLambda_isClean() {
        check("scene { padding -> listOf(1).forEach { consume(padding) } }").expectClean()
    }

    fun testNamedContentArgumentBeforeAnotherLambda_isChecked() {
        check("sceneWithAfter(content = { Unit }, after = { consume(1) })")
            .expectUnused("sceneWithAfter")
    }

    fun testNamedContentArgumentUsedBeforeAnotherLambda_isClean() {
        check("sceneWithAfter(content = { consume(it) }, after = { Unit })").expectClean()
    }

    fun testDirectSceneConstructorPaddingUse_isClean() {
        check("Scene { consume(it) }").expectClean()
    }

    fun testUnresolvedImportedScene_reportsError() {
        checkUnresolved("""
            package example
            import oleginvoke.com.composium.scene
            fun example() { scene { Unit } }
        """).expectUnused()
    }

    fun testUnresolvedImportedConstructor_reportsError() {
        checkUnresolved("""
            package example
            import oleginvoke.com.composium.Scene
            fun example() { Scene { Unit } }
        """).expectUnused("Scene")
    }

    fun testUnresolvedFullyQualifiedScene_reportsError() {
        checkUnresolved("""
            package example
            fun example() { oleginvoke.com.composium.scene { Unit } }
        """).expectUnused()
    }

    fun testUnresolvedSceneInPublicPackage_reportsError() {
        checkUnresolved("""
            package oleginvoke.com.composium
            fun example() { scene { Unit } }
        """).expectUnused()
    }

    fun testUnresolvedWrapper_isIgnored() {
        checkUnresolved("""
            package oleginvoke.com.composium
            fun example() { sceneWrapper { Unit } }
        """).expectClean()
    }

    fun testUnresolvedUnrelatedScene_isIgnored() {
        checkUnresolved("""
            package example
            fun example() { scene { Unit } }
        """).expectClean()
    }

    fun testPaddingValuesWithoutSceneScope_isIgnored() {
        lint().files(
            paddingValues,
            kotlin("""
                package oleginvoke.com.composium
                import androidx.compose.foundation.layout.PaddingValues
                fun scene(content: (PaddingValues) -> Unit) = Unit
                fun example() { scene { Unit } }
            """).indented(),
        ).allowMissingSdk().run().expectClean()
    }

    fun testSceneScopeWithoutPaddingValues_isIgnored() {
        lint().files(
            kotlin("""
                package oleginvoke.com.composium
                interface SceneScope
                fun scene(content: SceneScope.(String) -> Unit) = Unit
                fun example() { scene { Unit } }
            """).indented(),
        ).allowMissingSdk().run().expectClean()
    }

    // The API is deliberately absent, so these fixtures exercise unresolved calls as written.
    private fun checkUnresolved(source: String): TestLintResult = lint()
        .files(kotlin(source).indented())
        .allowMissingSdk()
        .allowCompilationErrors()
        .testModes(TestMode.DEFAULT)
        .run()

    private fun check(body: String): TestLintResult = lint().files(
        sceneApi,
        paddingValues,
        kotlin("""
            package example

            import androidx.compose.foundation.layout.PaddingValues
            import oleginvoke.com.composium.Scene
            import oleginvoke.com.composium.SceneScope
            import oleginvoke.com.composium.scene

            fun sceneWrapper(content: SceneScope.(contentPadding: PaddingValues) -> Unit) = scene(content)
            fun sceneWithAfter(content: SceneScope.(PaddingValues) -> Unit, after: () -> Unit) = scene(content)
            fun consume(value: Any?) = Unit
            class Holder { val contentPadding: Int = 0 }
            fun TopSpacer() = Unit
            fun BottomSpacer() = Unit
            fun consumeParentScaffoldPadding() = Unit

            fun example() {
                $body
            }
        """).indented(),
    ).allowMissingSdk().run()

    private fun TestLintResult.expectUnused(host: String = "scene") =
        expectErrorCount(1).expectContains(
            "$host contentPadding is not used [UnusedComposiumContentPaddingParameter]",
        )

    private val sceneApi = kotlin("""
        package oleginvoke.com.composium

        import androidx.compose.foundation.layout.PaddingValues

        interface SceneScope

        class Scene(
            val content: SceneScope.(contentPadding: PaddingValues) -> Unit,
        )

        fun scene(
            content: SceneScope.(contentPadding: PaddingValues) -> Unit,
        ) = Scene(content)
    """).indented()

    private val paddingValues = kotlin("""
        package androidx.compose.foundation.layout

        class PaddingValues
    """).indented()
}
