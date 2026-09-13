package oleginvoke.com.composium

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35])
class ComposiumScreenInsetsTest {
    @get:Rule val composeRule = createComposeRule()
    @get:Rule val testName = TestName()
    private val sceneName get() = "Insets: ${testName.methodName}"
    private lateinit var ownerView: View
    private lateinit var systemInsets: WindowInsets
    private lateinit var imeInsets: WindowInsets
    private var platformInsets: WindowInsetsCompat? = null
    private var scenePadding: PaddingValues? = null
    private val providedInsets = mutableStateOf<WindowInsets>(WindowInsets(7, 24, 20, 30))
    private val parentPadding = mutableStateOf(PaddingValues(0.dp))

    @After
    fun removeFixtureScenes() {
        // The runtime registry survives Robolectric test methods. Remove only this fixture's
        // entries so they cannot affect other catalog/navigation tests.
        val entries = ReflectionHelpers.getField<MutableList<SceneEntry>>(ComposiumRuntime, "_scenes")
        val registry = ReflectionHelpers.getField<MutableMap<SceneKey, Scene>>(ComposiumRuntime, "registeredScenesById")
        val fixtureKeys = entries.filter { it.scene.name.startsWith(sceneName) }.map { it.id }.toSet()
        entries.removeAll { it.id in fixtureKeys }
        fixtureKeys.forEach(registry::remove)
    }

    @Test
    fun catalogBottomPaddingDoesNotDuplicateConsumedNavigationInset() {
        repeat(8) { index ->
            ComposiumRuntime.register(Scene(null, "${sceneName} filler $index", thumbnail = null) { })
        }
        render()
        val list = composeRule.onNode(hasScrollToIndexAction())
        // One metadata row precedes the flat scene entries.
        list.performScrollToIndex(ComposiumRuntime.scenes.size)
        val before = composeRule.onNodeWithText(sceneName, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle { parentPadding.value = PaddingValues(bottom = 12.dp) }
        list.performScrollToIndex(ComposiumRuntime.scenes.size)
        val after = composeRule.onNodeWithText(sceneName, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("Consuming part of the bottom inset must not push the last card farther up", before.bottom, after.bottom, 1f)
    }

    @Test
    fun defaultInsetsProtectStandaloneCatalogAndFloatingScene() {
        render(useDefault = true)
        val titleBefore = composeRule.onNodeWithText("Composium", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        dispatchInsets()
        val titleAfter = composeRule.onNodeWithText("Composium", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(titleBefore.top + 24f, titleAfter.top, 1f)
        openScene()
        assertScenePadding(24, 30)
        assertFloatingAnchor(leftInset = 7, topInset = 24, rightInset = 20)
    }

    @Test
    @Config(sdk = [35])
    fun defaultInsetsAlsoProtectAgainstDisplayCutout() {
        render(useDefault = true)
        dispatchInsets(cutoutLeft = 40)
        openScene()
        assertScenePadding(24, 30)
        assertFloatingAnchor(leftInset = 40, topInset = 24, rightInset = 20)
    }

    @Test
    fun deprecatedOverloadUsesTheSameDefaultInsets() {
        render(useDefault = true, legacy = true)
        dispatchInsets()
        openScene()
        assertScenePadding(24, 30)
    }

    @Test
    fun explicitZeroInsetsOverridePlatformInsets() {
        providedInsets.value = WindowInsets(0, 0, 0, 0)
        render()
        dispatchInsets()
        openScene()
        assertScenePadding(0, 0)
        assertFloatingAnchor(leftInset = 0, topInset = 0, rightInset = 0)
    }

    @Test
    fun defaultInsetsUpdateWhenSystemBarsChangeAndDoNotIncludeIme() {
        render(useDefault = true)
        dispatchInsets()
        openScene()
        assertScenePadding(24, 30)
        dispatchInsets(top = 36, bottom = 48, ime = 200)
        assertScenePadding(36, 48)
        dispatchInsets(top = 0, bottom = 0)
        assertScenePadding(0, 0)
    }

    @Test
    fun zeroInsetsKeepExternalPaddingOwnership() {
        providedInsets.value = WindowInsets(0)
        parentPadding.value = PaddingValues(start = 7.dp, top = 24.dp, end = 20.dp, bottom = 30.dp)
        render(consumeParent = false)
        dispatchInsets()
        openScene()
        assertScenePadding(0, 0)
        assertFloatingAnchor(leftInset = 7, topInset = 24, rightInset = 20)
    }

    @Test
    fun scaffoldConsumedPaddingIsNotAddedAgainByDefault() {
        render(useDefault = true, inScaffold = true)
        dispatchInsets()
        openScene()
        assertScenePadding(0, 0)
        assertFloatingAnchor(leftInset = 7, topInset = 24, rightInset = 20)
    }

    @Test
    fun topBarInsideScaffoldAddsOnlyItsOwnHeight() {
        render(useDefault = true, inScaffold = true, tools = SceneTools.TopBar)
        dispatchInsets()
        openScene()
        assertScenePadding(72, 0)
    }

    @Test
    fun partialConsumptionIsSubtractedOnlyOnceOnEverySideInRtl() {
        parentPadding.value = PaddingValues(start = 8.dp, top = 10.dp, end = 3.dp, bottom = 12.dp)
        render(direction = LayoutDirection.Rtl)
        openScene()
        assertScenePadding(14, 18)
        assertFloatingAnchor(leftInset = 7, topInset = 24, rightInset = 20)
        val scene = composeRule.onNodeWithTag("scene").fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.left + 7f, scene.left, 1f)
        assertEquals(screen.right - 20f, scene.right, 1f)
    }

    @Test
    fun changesToConsumedAndProvidedInsetsReachOpenScene() {
        render()
        openScene()
        assertScenePadding(24, 30)
        composeRule.runOnIdle { parentPadding.value = PaddingValues(top = 10.dp, bottom = 12.dp) }
        assertScenePadding(14, 18)
        composeRule.runOnIdle { providedInsets.value = WindowInsets(7, 36, 20, 40) }
        assertScenePadding(26, 28)
        composeRule.runOnIdle { providedInsets.value = WindowInsets(0) }
        assertScenePadding(0, 0)
    }

    @Test
    fun partialConsumptionKeepsTopBarAndContentAligned() {
        parentPadding.value = PaddingValues(top = 10.dp, bottom = 12.dp)
        render(tools = SceneTools.TopBar)
        openScene()
        assertScenePadding(86, 18)
        val back = composeRule.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.top + 24f + 10f, back.top, 1f)
    }

    private fun assertScenePadding(top: Int, bottom: Int) {
        composeRule.waitForIdle()
        assertEquals(top.dp, checkNotNull(scenePadding).calculateTopPadding())
        assertEquals(bottom.dp, checkNotNull(scenePadding).calculateBottomPadding())
    }

    private fun assertFloatingAnchor(leftInset: Int, topInset: Int, rightInset: Int) {
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val back = composeRule.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.right - rightInset - 12f - 64f + 8f, back.left, 1f)
        assertEquals(screen.top + topInset + 12f + 8f, back.top, 1f)
        val scene = composeRule.onNodeWithTag("scene").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.left + leftInset, scene.left, 1f)
    }

    private fun openScene() {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(sceneName))
        composeRule.onNodeWithText(sceneName).performClick()
        composeRule.onNodeWithTag("scene").assertExists()
        // The scene changes the window's soft-input mode. Robolectric's resulting traversal
        // delivers zero insets; restore this fixture's platform values after navigation settles.
        platformInsets?.let(::applyPlatformInsets)
    }

    private fun dispatchInsets(top: Int = 24, bottom: Int = 30, ime: Int = 0, cutoutLeft: Int = 0) {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(7, 0, 20, bottom))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
            .setDisplayCutout(
                if (cutoutLeft == 0) null else
                    DisplayCutoutCompat(Rect(cutoutLeft, 0, 0, 0), listOf(Rect(0, 0, cutoutLeft, 100))),
            )
            .build()
        platformInsets = insets
        applyPlatformInsets(insets)
        assertEquals("The fixture must deliver actual platform insets", top, systemInsets.getTop(Density(1f)))
    }

    @Suppress("DEPRECATION")
    private fun applyPlatformInsets(insets: WindowInsetsCompat) {
        composeRule.runOnIdle {
            var platform = checkNotNull(insets.toWindowInsets())
            if (Build.VERSION.SDK_INT < 30) {
                // Before API 30, AndroidX separates navigation bars from the IME using the
                // window's stable insets. Robolectric has no system UI to populate those.
                val attachInfo = ReflectionHelpers.getField<Any>(ownerView, "mAttachInfo")
                val stable = ReflectionHelpers.getField<Rect>(attachInfo, "mStableInsets")
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                stable.set(bars.left, bars.top, bars.right, bars.bottom)
                val viewRoot = ReflectionHelpers.getField<Any>(attachInfo, "mViewRootImpl")
                ReflectionHelpers.callInstanceMethod<android.view.WindowInsets>(
                    viewRoot,
                    "getWindowInsets",
                    ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true),
                )
                // Legacy platform insets encode an adjustResize IME in the same bottom field.
                platform = platform.replaceSystemWindowInsets(
                    bars.left, bars.top, bars.right,
                    maxOf(bars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom),
                )
            }
            ownerView.dispatchApplyWindowInsets(platform)
        }
        composeRule.waitForIdle()
        assertEquals(insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, systemInsets.getTop(Density(1f)))
        assertEquals(insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom, systemInsets.getBottom(Density(1f)))
        assertEquals(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom, imeInsets.getBottom(Density(1f)))
    }

    @Suppress("DEPRECATION")
    private fun render(
        useDefault: Boolean = false,
        inScaffold: Boolean = false,
        consumeParent: Boolean = true,
        tools: SceneTools = SceneTools.Floating,
        direction: LayoutDirection = LayoutDirection.Ltr,
        legacy: Boolean = false,
    ) {
        ComposiumRuntime.register(
            Scene(group = null, name = sceneName, tools = tools, thumbnail = null) { padding ->
                scenePadding = padding
                Box(Modifier.fillMaxSize().testTag("scene"))
            },
        )
        composeRule.setContent {
            ownerView = LocalView.current
            systemInsets = WindowInsets.systemBars
            imeInsets = WindowInsets.ime
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLayoutDirection provides direction,
            ) {
                Box(Modifier.fillMaxSize().testTag("screen")) {
                    val screen: @Composable (Modifier) -> Unit = { modifier ->
                        if (legacy) {
                            ComposiumScreen(modifier = modifier, scenePreviewDecorator = defaultScenePreviewDecorator())
                        } else if (useDefault) {
                            ComposiumScreen(modifier = modifier)
                        } else {
                            ComposiumScreen(modifier = modifier, contentWindowInsets = providedInsets.value)
                        }
                    }
                    if (inScaffold) {
                        Scaffold { padding ->
                            screen(Modifier.padding(padding).consumeWindowInsets(padding))
                        }
                    } else {
                        val padding = parentPadding.value
                        screen(
                            Modifier.padding(padding).then(
                                if (consumeParent) Modifier.consumeWindowInsets(padding) else Modifier,
                            ),
                        )
                    }
                }
            }
        }
    }
}
