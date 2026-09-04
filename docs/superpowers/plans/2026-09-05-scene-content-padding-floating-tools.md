# Scene Content Padding and Floating Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `enableEdgeToEdge`/`SceneScope.innerPadding` with an explicit scene `contentPadding` argument, add selectable top-bar or floating tools, and ship a strict unused-padding lint check inside the runtime AAR.

**Architecture:** Every scene is measured over the full preview pane and receives safe rectangular padding as `SceneScope.(PaddingValues) -> Unit`. `SceneTools` selects either the existing top bar or a persistent top-right 2×2 floating surface with a minimized handle; the existing scene reducer remains the source of UI state. A dedicated JVM lint module adapts the proven STG UAST detector and is embedded into the runtime AAR with `lintPublish`.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose BOM 2026.05.01, Android Gradle Plugin 9.1.1, Android Lint 32.1.1, Kotlin/JVM tests, Compose UI tests on Robolectric, KSP.

**Spec:** `docs/superpowers/specs/2026-09-05-scene-content-padding-floating-tools-design.md`

## Global Constraints

- `SceneTools.TopBar` is the default public behavior; `SceneTools.Floating` is opt-in per scene.
- Scene content always receives the full current preview-pane bounds; Composium does not physically offset scene content around its own UI.
- `contentPadding` includes system insets and, only for `SceneTools.TopBar`, the regular top-bar area.
- Floating tools are overlays and never contribute to `contentPadding` or eyedropper sampling.
- Floating tools are fixed below the status bar at the physical top-right; drag is out of scope.
- Floating tools start expanded on each scene-screen entry and do not persist minimized state after leaving the scene.
- `UnusedComposiumContentPaddingParameter` has `Severity.ERROR`; `_` is not an opt-out, and intentional omission requires explicit suppression.
- Do not port STG-specific `TopSpacer`, `BottomSpacer`, `consumeParentScaffoldPadding`, nested-Scaffold, or `ScaffoldSheet` exceptions.
- The lint JAR must be packaged into the runtime AAR; consumers must not add a separate lint dependency.

## File Structure

- `runtime/src/main/kotlin/composium/SceneTools.kt` — public scene tools presentation enum.
- `runtime/src/main/kotlin/composium/Scene.kt` — public scene metadata and the new content function type.
- `runtime/src/main/kotlin/composium/SceneDelegate.kt` — scene DSL wiring for `tools` and `contentPadding`.
- `runtime/src/main/kotlin/composium/SceneScope.kt` — parameter APIs only; remove inset ownership.
- `runtime/src/main/kotlin/composium/ScenePreview.kt` — invoke scene content with zero padding.
- `runtime/src/main/kotlin/composium/scene_screen/SceneContentPadding.kt` — pure safe-padding calculation.
- `runtime/src/main/kotlin/composium/scene_screen/SceneScreenContract.kt` — minimized state, intents, and callbacks.
- `runtime/src/main/kotlin/composium/scene_screen/SceneScreenLogic.kt` — reducer and visibility models.
- `runtime/src/main/kotlin/composium/scene_screen/SceneToolActionButton.kt` — shared top-bar/floating action button.
- `runtime/src/main/kotlin/composium/scene_screen/SceneFloatingTools.kt` — 2×2 floating surface and minimized handle.
- `runtime/src/main/kotlin/composium/scene_screen/SceneScreen.kt` — host layout and selection between top-bar/floating presentations.
- `runtime/src/main/kotlin/composium/scene_thumbnail/SceneThumbnailRenderSurface.kt` — zero-padding thumbnail invocation.
- `runtime/src/test/kotlin/composium/SceneApiTest.kt` — public metadata/content contract.
- `runtime/src/test/kotlin/composium/scene_screen/SceneContentPaddingTest.kt` — padding calculations.
- `runtime/src/test/kotlin/composium/scene_screen/SceneFloatingToolsStateTest.kt` — reducer and visibility behavior.
- `runtime/src/test/kotlin/composium/scene_screen/SceneFloatingToolsTest.kt` — Robolectric Compose UI behavior.
- `lint/build.gradle.kts` — standalone lint detector module.
- `lint/src/main/kotlin/oleginvoke/com/composium/lint/ComposiumContentPaddingDetector.kt` — UAST detector.
- `lint/src/main/kotlin/oleginvoke/com/composium/lint/ComposiumIssueRegistry.kt` — lint registry and vendor metadata.
- `lint/src/test/kotlin/oleginvoke/com/composium/lint/ComposiumContentPaddingDetectorTest.kt` — detector regression suite.
- `sample/src/main/kotlin/com/example/app/sceneWithDecorator.kt` — wrapper consumes scene padding around its own frame.
- `sample/src/main/kotlin/com/example/app/SampleScenes.kt` — migrated declarations and a floating-tools example.
- `sample/src/benchmark/kotlin/com/example/app/BenchmarkScenes.kt` — migrated benchmark scene declaration.
- `README.md` — public migration, padding, tools, suppression, and Preview documentation.

---

### Task 1: Replace scene inset ownership with explicit content padding

**Files:**
- Create: `runtime/src/main/kotlin/composium/SceneTools.kt`
- Create: `runtime/src/main/kotlin/composium/scene_screen/SceneContentPadding.kt`
- Create: `runtime/src/test/kotlin/composium/SceneApiTest.kt`
- Create: `runtime/src/test/kotlin/composium/scene_screen/SceneContentPaddingTest.kt`
- Modify: `runtime/src/main/kotlin/composium/Scene.kt`
- Modify: `runtime/src/main/kotlin/composium/SceneDelegate.kt`
- Modify: `runtime/src/main/kotlin/composium/SceneScope.kt`
- Modify: `runtime/src/main/kotlin/composium/ScenePreview.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_screen/SceneScreen.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_thumbnail/SceneThumbnailRenderSurface.kt`
- Modify: `runtime/src/test/kotlin/composium/SceneThumbnailApiTest.kt`
- Modify: `sample/src/main/kotlin/com/example/app/sceneWithDecorator.kt`
- Modify: `sample/src/main/kotlin/com/example/app/SampleScenes.kt`
- Modify: `sample/src/benchmark/kotlin/com/example/app/BenchmarkScenes.kt`

**Interfaces:**
- Produces: `enum class SceneTools { TopBar, Floating }`.
- Produces: `Scene.content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit`.
- Produces: `scene(..., tools: SceneTools = SceneTools.TopBar, ..., content: @Composable SceneScope.(PaddingValues) -> Unit)`.
- Produces: `calculateSceneContentPadding(SceneTools, Dp, Dp, Dp, SceneInspectorLayoutMode): PaddingValues`.
- Removes: `Scene.enableEdgeToEdge`, `scene(enableEdgeToEdge = ...)`, `SceneScope.innerPadding`, and `SceneScope.internalInnerPadding`.

- [ ] **Step 1: Write failing public API and padding tests**

Create `SceneApiTest.kt` with tests that name the metadata contract:

```kotlin
package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneApiTest {
    @Test
    fun sceneUsesTopBarToolsByDefault() {
        val content: @Composable SceneScope.(PaddingValues) -> Unit = { padding ->
            padding.calculateTopPadding()
        }
        val scene = Scene(group = null, name = "Default", content = content)

        assertEquals(SceneTools.TopBar, scene.tools)
        assertEquals(content, scene.content)
    }

    @Test
    fun sceneStoresFloatingToolsPresentation() {
        val scene = Scene(
            group = null,
            name = "Fullscreen",
            tools = SceneTools.Floating,
            content = { padding -> padding.calculateBottomPadding() },
        )

        assertEquals(SceneTools.Floating, scene.tools)
    }
}
```

Create `SceneContentPaddingTest.kt`:

```kotlin
package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneTools
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneContentPaddingTest {
    @Test
    fun topBarPaddingIncludesSystemInsetAndTopBar() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.TopBar,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Closed,
        )

        assertEquals(96.dp, padding.calculateTopPadding())
        assertEquals(32.dp, padding.calculateBottomPadding())
    }

    @Test
    fun floatingPaddingExcludesFloatingSurfaceAndTopBar() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.Floating,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Closed,
        )

        assertEquals(24.dp, padding.calculateTopPadding())
        assertEquals(32.dp, padding.calculateBottomPadding())
    }

    @Test
    fun splitInspectorAlreadyBoundsThePreviewBottom() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.TopBar,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Split,
        )

        assertEquals(0.dp, padding.calculateBottomPadding())
    }
}
```

- [ ] **Step 2: Run the new tests and verify RED**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest --tests 'oleginvoke.com.composium.SceneApiTest' --tests 'oleginvoke.com.composium.scene_screen.SceneContentPaddingTest'
```

Expected: compilation fails because `SceneTools`, `Scene.tools`, and `calculateSceneContentPadding` do not exist and `Scene.content` still has arity zero.

- [ ] **Step 3: Add the public enum and migrate the scene model**

Create `SceneTools.kt`:

```kotlin
package oleginvoke.com.composium

/** Selects how Composium exposes scene tools. */
enum class SceneTools {
    TopBar,
    Floating,
}
```

Change the primary `Scene` content property and delegate fields to:

```kotlin
val tools: SceneTools = SceneTools.TopBar
val content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit
```

Change `scene` to accept `tools` in the former `enableEdgeToEdge` position by name, pass it through `SceneDelegate`, and remove every `enableEdgeToEdge` property, parameter, and KDoc statement. Remove `internalInnerPadding` and `innerPadding` from `SceneScope`, including their Compose imports and old inset comments.

- [ ] **Step 4: Implement the pure padding contract**

Create `SceneContentPadding.kt`:

```kotlin
package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import oleginvoke.com.composium.SceneTools

internal fun calculateSceneContentPadding(
    tools: SceneTools,
    statusBarInset: Dp,
    navigationBarInset: Dp,
    topBarHeight: Dp,
    inspectorLayoutMode: SceneInspectorLayoutMode,
): PaddingValues = PaddingValues(
    top = statusBarInset + if (tools == SceneTools.TopBar) topBarHeight else Dp.Zero,
    bottom = if (inspectorLayoutMode == SceneInspectorLayoutMode.Closed) {
        navigationBarInset
    } else {
        Dp.Zero
    },
)
```

Keep the current 72 dp top-bar content area as a named constant in `SceneScreen.kt` and pass it as `topBarHeight`.

- [ ] **Step 5: Pass padding through all rendering surfaces**

In `SceneScreenContent`, calculate padding from the scene metadata and pass it to `ScenePreviewPane`, then to `ScenePreviewContent`:

```kotlin
val sceneContentPadding = calculateSceneContentPadding(
    tools = sceneEntry.scene.tools,
    statusBarInset = topInset,
    navigationBarInset = bottomInset,
    topBarHeight = SceneTopBarContentHeight,
    inspectorLayoutMode = controlsSheet.layoutMode,
)

sceneEntry.scene.content(sceneScope, sceneContentPadding)
```

Simplify `ScenePreviewPane` placement so `sceneTopPx` and `sceneBottomPx` are always zero. Keep the existing bounded-height measurement, inspector fraction, clipping, and pane-height computation; only remove the branch that physically offsets or shortens scene content.

Invoke zero-padding content in the other surfaces:

```kotlin
// ScenePreview.kt
content(sceneScope, PaddingValues(0.dp))

// fallback thumbnail lambda in Scene.thumbnailContent()
val scene = this
thumbnail ?: { scene.content(this, PaddingValues(0.dp)) }
```

Keep a custom `thumbnail: @Composable SceneScope.() -> Unit` unchanged. Update `SceneThumbnailApiTest`: retain identity assertions for explicit thumbnails, but replace the fallback `assertSame(content, ...)` assertion because fallback now requires a zero-padding wrapper.

- [ ] **Step 6: Migrate the sample and benchmark without introducing invalid padding usage**

Let the sample decorator own the host padding, so its callers retain a focused content lambda:

```kotlin
internal fun sceneWithDecorator(
    group: String? = null,
    name: String? = null,
    tools: SceneTools = SceneTools.TopBar,
    // existing visual arguments
    content: @Composable SceneScope.() -> Unit,
): SceneDelegate = scene(
    group = group,
    name = name,
    tools = tools,
    // thumbnail and badge
) { contentPadding ->
    // existing decorator
    Spacer(Modifier.height(contentPadding.calculateTopPadding() + 16.dp))
    content()
    Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 16.dp))
}
```

For direct `scene` calls, name `contentPadding` and apply it to the existing root container or list. Use an explicit `@Suppress("UnusedComposiumContentPaddingParameter")` only for a genuinely full-bleed declaration; do not use `_`. Migrate `BenchmarkScenes.kt` with the same rule.

- [ ] **Step 7: Run API, runtime, sample, and benchmark compilation checks**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest :sample:compileDebugKotlin :sample:compileBenchmarkKotlin
```

Expected: all tasks pass; repository search returns no production references to `enableEdgeToEdge`, `innerPadding`, or `internalInnerPadding` except migration documentation that will be updated in Task 5.

- [ ] **Step 8: Commit the scene contract migration**

```powershell
git add runtime/src sample/src
git commit -m "refactor: pass content padding to scenes"
```

---

### Task 2: Add floating-tools state and visibility rules

**Files:**
- Create: `runtime/src/test/kotlin/composium/scene_screen/SceneFloatingToolsStateTest.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_screen/SceneScreenContract.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_screen/SceneScreenLogic.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_screen/SceneScreen.kt`

**Interfaces:**
- Consumes: `SceneTools.TopBar` and `SceneTools.Floating` from Task 1.
- Produces: `SceneScreenState.isFloatingToolsMinimized: Boolean`.
- Produces: `SceneScreenIntent.MinimizeFloatingTools` and `SceneScreenIntent.ShowFloatingTools`.
- Produces: `shouldShowFloatingTools(SceneTools, SceneInspectorLayoutMode): Boolean`.
- Produces: `floatingToolsToggleContentDescription(Boolean): String`.

- [ ] **Step 1: Write failing reducer and visibility tests**

Create `SceneFloatingToolsStateTest.kt`:

```kotlin
package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.SceneTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneFloatingToolsStateTest {
    @Test
    fun floatingToolsStartExpandedAndCanBeMinimizedAndRestored() {
        val initial = SceneScreenState()
        val minimized = reduceSceneScreen(initial, SceneScreenIntent.MinimizeFloatingTools)
        val restored = reduceSceneScreen(minimized, SceneScreenIntent.ShowFloatingTools)

        assertFalse(initial.isFloatingToolsMinimized)
        assertTrue(minimized.isFloatingToolsMinimized)
        assertFalse(restored.isFloatingToolsMinimized)
    }

    @Test
    fun inspectorTransitionsPreserveMinimizedState() {
        val minimized = SceneScreenState(isFloatingToolsMinimized = true)
        val split = reduceSceneScreen(minimized, SceneScreenIntent.ShowControls)
        val expanded = reduceSceneScreen(split, SceneScreenIntent.ExpandControls)
        val restored = reduceSceneScreen(expanded, SceneScreenIntent.NavigateBackFromExpandedControls)

        assertTrue(split.isFloatingToolsMinimized)
        assertTrue(expanded.isFloatingToolsMinimized)
        assertTrue(restored.isFloatingToolsMinimized)
    }

    @Test
    fun floatingToolsAreHiddenOnlyForTopBarScenesAndExpandedInspector() {
        assertTrue(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Closed))
        assertTrue(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Split))
        assertFalse(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Expanded))
        assertFalse(shouldShowFloatingTools(SceneTools.TopBar, SceneInspectorLayoutMode.Closed))
    }

    @Test
    fun toggleDescriptionsDescribeTheResultingAction() {
        assertEquals("Minimize tools", floatingToolsToggleContentDescription(isMinimized = false))
        assertEquals("Show tools", floatingToolsToggleContentDescription(isMinimized = true))
    }
}
```

- [ ] **Step 2: Run the state tests and verify RED**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest --tests 'oleginvoke.com.composium.scene_screen.SceneFloatingToolsStateTest'
```

Expected: compilation fails because the state field, intents, and helper functions do not exist.

- [ ] **Step 3: Add deterministic state transitions**

Extend `SceneScreenState` and `SceneScreenIntent`:

```kotlin
internal data class SceneScreenState(
    val controlsSheet: ControlsSheetUiState = ControlsSheetUiState(),
    val isEyedropperVisible: Boolean = false,
    val isFloatingToolsMinimized: Boolean = false,
)

internal sealed interface SceneScreenIntent {
    data object MinimizeFloatingTools : SceneScreenIntent
    data object ShowFloatingTools : SceneScreenIntent
    // existing intents
}
```

Add reducer branches that use `copy(isFloatingToolsMinimized = true/false)` and return the existing state if it already has the requested value. Do not modify the field in inspector, eyedropper, or theme transitions.

- [ ] **Step 4: Add pure presentation helpers and callbacks**

Add to `SceneScreenLogic.kt`:

```kotlin
internal fun shouldShowFloatingTools(
    tools: SceneTools,
    inspectorLayoutMode: SceneInspectorLayoutMode,
): Boolean = tools == SceneTools.Floating &&
    inspectorLayoutMode != SceneInspectorLayoutMode.Expanded

internal fun floatingToolsToggleContentDescription(isMinimized: Boolean): String =
    if (isMinimized) "Show tools" else "Minimize tools"
```

Add `onMinimizeFloatingTools()` and `onShowFloatingTools()` to `SceneScreenCallbacks`, and dispatch the new intents from the callback implementation in `SceneScreen`.

- [ ] **Step 5: Run the focused and existing reducer tests**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest --tests 'oleginvoke.com.composium.scene_screen.SceneFloatingToolsStateTest' --tests 'oleginvoke.com.composium.scene_screen.SceneScreenEyedropperTest' --tests 'oleginvoke.com.composium.scene_screen.SceneInspectorLayoutTest'
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit the state model**

```powershell
git add runtime/src/main/kotlin/composium/scene_screen runtime/src/test/kotlin/composium/scene_screen
git commit -m "feat: add floating tools state"
```

---

### Task 3: Render and test the floating 2×2 tools surface

**Files:**
- Create: `runtime/src/main/kotlin/composium/scene_screen/SceneToolActionButton.kt`
- Create: `runtime/src/main/kotlin/composium/scene_screen/SceneFloatingTools.kt`
- Create: `runtime/src/test/kotlin/composium/scene_screen/SceneFloatingToolsTest.kt`
- Modify: `runtime/src/main/kotlin/composium/scene_screen/SceneScreen.kt`
- Modify: `runtime/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: state and visibility functions from Task 2.
- Produces: `SceneFloatingTools(...)`, a separate internal composable with callback-only dependencies.
- Produces: `SceneToolActionButton(...)`, shared by existing top-bar and floating actions.

- [ ] **Step 1: Add Robolectric Compose test dependencies**

Add version-catalog entries:

```toml
robolectric = "4.16"

compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
```

Add to `runtime/build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

- [ ] **Step 2: Write failing UI tests for actions and minimized state**

Create a Robolectric test using `createComposeRule()` and `ComposiumTheme`. The primary test must exercise real semantics and callbacks:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneFloatingToolsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedToolsExposeAllFourActions() {
        var backClicks = 0
        var propertiesClicks = 0
        var eyedropperClicks = 0
        var requestedDarkTheme: Boolean? = null

        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = { backClicks++ },
                    onToggleControls = { propertiesClicks++ },
                    onToggleEyedropper = { eyedropperClicks++ },
                    onThemeChange = { requestedDarkTheme = it },
                    onMinimize = {},
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithContentDescription("Switch to dark theme").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, propertiesClicks)
        assertEquals(1, eyedropperClicks)
        assertEquals(true, requestedDarkTheme)
    }

    @Test
    fun minimizedToolsExposeOnlyTheRestoreHandle() {
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = true,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = {},
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = {},
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Show tools").assertExists()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open properties").assertDoesNotExist()
    }

    @Test
    fun minimizeHandleDispatchesMinimize() {
        var minimizeClicks = 0
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = {},
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = { minimizeClicks++ },
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Minimize tools").performClick()
        assertEquals(1, minimizeClicks)
    }
}
```

Add a private renderer plus the two remaining state-dependent tests:

```kotlin
private fun renderTools(
    controlsLayout: SceneInspectorLayoutMode = SceneInspectorLayoutMode.Closed,
    isEyedropperVisible: Boolean = false,
    onToggleControls: () -> Unit = {},
    onToggleEyedropper: () -> Unit = {},
) {
    composeRule.setContent {
        ComposiumTheme(darkTheme = false) {
            SceneFloatingTools(
                controlsLayout = controlsLayout,
                isMinimized = false,
                isDarkTheme = false,
                isEyedropperVisible = isEyedropperVisible,
                onBack = {},
                onToggleControls = onToggleControls,
                onToggleEyedropper = onToggleEyedropper,
                onThemeChange = {},
                onMinimize = {},
                onShow = {},
            )
        }
    }
}

@Test
fun splitInspectorShowsExpandAction() {
    var clicks = 0
    renderTools(
        controlsLayout = SceneInspectorLayoutMode.Split,
        onToggleControls = { clicks++ },
    )

    composeRule.onNodeWithContentDescription("Expand settings").performClick()
    assertEquals(1, clicks)
}

@Test
fun activeEyedropperShowsCloseAction() {
    var clicks = 0
    renderTools(
        isEyedropperVisible = true,
        onToggleEyedropper = { clicks++ },
    )

    composeRule.onNodeWithContentDescription("Close eyedropper").performClick()
    assertEquals(1, clicks)
}
```

- [ ] **Step 3: Run the UI test and verify RED**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest --tests 'oleginvoke.com.composium.scene_screen.SceneFloatingToolsTest'
```

Expected: compilation fails because `SceneFloatingTools` does not exist.

- [ ] **Step 4: Extract the shared action button without changing behavior**

Move the existing `SceneTopBarActionButton` implementation and `SceneSettingsButtonIcon.imageVector()` from `SceneScreen.kt` to `SceneToolActionButton.kt`. Rename the composable to `SceneToolActionButton`, keep its existing 48 dp size, active/disabled colors, animation, and semantics, and update the existing top bar to call it. Run `:runtime:testDebugUnitTest` after the extraction before adding new UI.

- [ ] **Step 5: Implement the floating surface**

Use this callback-oriented signature:

```kotlin
@Composable
internal fun SceneFloatingTools(
    controlsLayout: SceneInspectorLayoutMode,
    isMinimized: Boolean,
    isDarkTheme: Boolean,
    isEyedropperVisible: Boolean,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleEyedropper: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onMinimize: () -> Unit,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Build one `ComposiumSurface` containing:

- an attached, visibly small chevron capsule with a minimum 48 dp semantic/touch target;
- a 2×2 grid of 48 dp action cells when expanded;
- Back at row 0/column 0;
- Properties/Expand at row 0/column 1 using `calculateSceneSettingsButtonState`;
- Eyedropper at row 1/column 0 using `calculateSceneEyedropperButtonState`;
- a light/dark icon action at row 1/column 1 calling `onThemeChange(!isDarkTheme)`.

Use `AnimatedVisibility` or `AnimatedContent` for the grid transition, but keep the handle at a stable top-right anchor. The visible handle may be smaller than 48 dp; its surrounding hit target must not overlap the four action cells.

- [ ] **Step 6: Select the correct scene tools presentation in `SceneScreen`**

Render the regular top bar only when:

```kotlin
sceneEntry.scene.tools == SceneTools.TopBar ||
    state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Expanded
```

Render floating tools only when `shouldShowFloatingTools(...)` is true. Place them outside `ColorEyedropperHost` with:

```kotlin
Modifier
    .align(Alignment.TopEnd)
    .then(
        contentWindowInsets
            .onlyTopAndHorizontalOrNull()
            ?.let(Modifier::windowInsetsPadding)
            ?: Modifier,
    )
    .padding(top = 12.dp, end = 12.dp)
```

Use the callbacks from Task 2 and the existing back, controls, eyedropper, and theme callbacks. Do not pass floating bounds into `calculateSceneContentPadding` or `ColorEyedropperHost.overlaySafePadding`.

- [ ] **Step 7: Run UI, reducer, and full runtime tests**

Run:

```powershell
.\gradlew.bat :runtime:testDebugUnitTest
```

Expected: all runtime unit and Robolectric Compose tests pass. Confirm the UI tests find four actions in expanded mode and only the restore handle in minimized mode.

- [ ] **Step 8: Add a floating-tools sample scene and compile it**

Set one existing full-screen sample scene to:

```kotlin
tools = SceneTools.Floating
```

Keep its background full-bleed while applying `contentPadding` only to the controls/list that must avoid system insets. Compile with:

```powershell
.\gradlew.bat :sample:compileDebugKotlin
```

- [ ] **Step 9: Commit the floating UI**

```powershell
git add gradle/libs.versions.toml runtime sample/src/main
git commit -m "feat: render floating scene tools"
```

---

### Task 4: Adapt and embed the strict content-padding lint check

**Files:**
- Create: `lint/build.gradle.kts`
- Create: `lint/src/main/kotlin/oleginvoke/com/composium/lint/ComposiumContentPaddingDetector.kt`
- Create: `lint/src/main/kotlin/oleginvoke/com/composium/lint/ComposiumIssueRegistry.kt`
- Create: `lint/src/test/kotlin/oleginvoke/com/composium/lint/ComposiumContentPaddingDetectorTest.kt`
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `runtime/build.gradle.kts`

**Interfaces:**
- Consumes: the `SceneScope.(PaddingValues) -> Unit` signature from Task 1.
- Produces: lint issue ID `UnusedComposiumContentPaddingParameter` with `Severity.ERROR`.
- Produces: an embedded `lint.jar` inside `runtime-release.aar`.

- [ ] **Step 1: Wire the lint test module and write the failing detector suite**

Add `include(":lint")`, Android Lint version `32.1.1`, and aliases for `lint-api`, `lint-checks`, and `lint-tests`. Create `lint/build.gradle.kts` following `F:\New-STG-design\design-lint\build.gradle.kts`, using JVM 17 for the lint tool and this registry manifest:

```kotlin
tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to
                "oleginvoke.com.composium.lint.ComposiumIssueRegistry",
        )
    }
}
```

Write `ComposiumContentPaddingDetectorTest` first. Its test API stub must include both public entry points and a project-local wrapper:

```kotlin
package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues

interface SceneScope

class Scene(
    val content: SceneScope.(contentPadding: PaddingValues) -> Unit,
)

fun scene(
    content: SceneScope.(contentPadding: PaddingValues) -> Unit,
) = Scene(content)
```

Provide the actual Compose type in a separate lint fixture:

```kotlin
package androidx.compose.foundation.layout

class PaddingValues
```

The initial tests must include these cases, using the exact source forms shown after each method name:

```kotlin
fun testNamedPaddingUse_isClean() // scene { contentPadding -> consume(contentPadding) }
fun testImplicitPaddingUse_isClean() // scene { consume(it) }
fun testForwardedWrapperPadding_isClean() // sceneWrapper { padding -> consume(padding) }
fun testUnusedNamedPadding_reportsError() // scene { contentPadding -> Unit }
fun testUnusedImplicitPadding_reportsError() // scene { Unit }
fun testUnderscorePadding_reportsError() // scene { _ -> Unit }
fun testSuppressedUnusedPadding_isClean() // @Suppress(issue id) scene { Unit }
fun testShadowedPadding_reportsError()
fun testUnrelatedContentPaddingProperty_reportsError()
fun testSameNamedFunctionWithWrongSignature_isIgnored()
fun testDirectSceneConstructorIsChecked()
fun testTopSpacerNameDoesNotCountAsUsage()
fun testBottomSpacerNameDoesNotCountAsUsage()
fun testConsumeParentScaffoldPaddingNameDoesNotCountAsUsage()
```

For each reporting test, require output containing:

```text
scene contentPadding is not used [UnusedComposiumContentPaddingParameter]
```

For the direct constructor, use `Scene contentPadding is not used`. Do not use source-text assertions; run each fixture through `LintDetectorTest.lint()`.

- [ ] **Step 2: Run lint tests and verify RED**

Run:

```powershell
.\gradlew.bat :lint:test
```

Expected: test compilation fails because `ComposiumContentPaddingDetector` and its issue do not exist.

- [ ] **Step 3: Adapt the STG detector core**

Start from `F:\New-STG-design\design-lint\src\main\kotlin\stg\design\lint\DesignContentPaddingDetector.kt`, retaining these mechanisms:

```kotlin
class ComposiumContentPaddingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
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
}
```

Identify a resolved host by a parameter named `content` whose canonical function type contains both:

```text
oleginvoke.com.composium.SceneScope
androidx.compose.foundation.layout.PaddingValues
```

This signature-based rule covers `scene`, direct `Scene`, and project-local wrappers. Keep the STG unresolved-call fallback only for the public package/imported `scene` and `Scene` names. Locate `content` via `getArgumentForParameter`; fall back to the last lambda only when resolution is unavailable.

Copy the STG parameter identity checks based on PSI elements and text ranges, simple-reference filtering, qualified-selector rejection, and nested-lambda shadow detection. Remove all branches and constants related to `Scaffold`, `ScaffoldSheet`, `ParentScaffoldBars`, `TopSpacer`, `BottomSpacer`, and `consumeParentScaffoldPadding`. A padding reference is the only normal successful path.

Create the issue exactly as:

```kotlin
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
```

- [ ] **Step 4: Register and publish the detector**

Create `ComposiumIssueRegistry`:

```kotlin
class ComposiumIssueRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val vendor = Vendor(
        vendorName = "Composium",
        identifier = "oleginvoke.com.composium",
    )
    override val issues = listOf(
        ComposiumContentPaddingDetector.UnusedComposiumContentPaddingParameter,
    )
}
```

Add to `runtime/build.gradle.kts`:

```kotlin
lintChecks(project(":lint")) {
    isTransitive = false
}
lintPublish(project(":lint")) {
    isTransitive = false
}
```

`lintChecks` exercises the rule while developing the runtime project; `lintPublish` embeds it for external consumers.

- [ ] **Step 5: Run the detector suite and verify GREEN**

Run:

```powershell
.\gradlew.bat :lint:test --rerun-tasks
```

Expected: every detector fixture passes, including `_` reporting an error and the three STG helper names not counting as usage.

- [ ] **Step 6: Verify the sample against the embedded rule**

Run:

```powershell
.\gradlew.bat :sample:lintDebug
```

Expected: no `UnusedComposiumContentPaddingParameter` errors. Fix call sites by applying/forwarding padding; use suppression only for deliberate full-bleed content.

- [ ] **Step 7: Commit the lint module**

```powershell
git add settings.gradle.kts gradle/libs.versions.toml runtime/build.gradle.kts lint
git commit -m "feat: add scene content padding lint"
```

---

### Task 5: Document migration and verify the published artifact

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-09-05-scene-content-padding-floating-tools-design.md` only if implementation reveals a factual correction; do not broaden scope.

**Interfaces:**
- Consumes: all public API and UI behavior from Tasks 1-4.
- Produces: complete consumer documentation and release-level verification evidence.

- [ ] **Step 1: Replace old edge-to-edge documentation**

Remove the `enableEdgeToEdge`/`innerPadding` section and document:

```kotlin
val RegularScene by scene { contentPadding ->
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        Content()
    }
}

val FullScreenScene by scene(
    tools = SceneTools.Floating,
) { contentPadding ->
    Box(Modifier.fillMaxSize()) {
        FullScreenBackground()
        Actions(Modifier.padding(contentPadding))
    }
}
```

Document the 2×2 action positions, minimized handle, fixed top-right placement, expanded-inspector behavior, and the fact that floating tools never contribute to padding.

- [ ] **Step 2: Document strict lint and intentional suppression**

Use this exact full-bleed example:

```kotlin
@Suppress("UnusedComposiumContentPaddingParameter")
val BackgroundScene by scene {
    FullScreenBackground()
}
```

State that `_` still reports an error. Explain that wrappers may either consume padding themselves, as `sceneWithDecorator` does, or expose and forward `SceneScope.(PaddingValues) -> Unit`; signature-exposing wrappers are checked by the same detector.

- [ ] **Step 3: Migrate every README scene snippet**

Search all fenced examples and make each `scene` content lambda do one of the following:

1. apply `contentPadding` to a root;
2. forward it to a child/list;
3. use the explicit suppression for intentional full-bleed content.

Do not leave examples that compile but fail the packaged lint rule.

- [ ] **Step 4: Run the complete verification suite**

Run:

```powershell
.\gradlew.bat :lint:test :processor:test :runtime:testDebugUnitTest :sample:testDebugUnitTest :sample:compileBenchmarkKotlin :runtime:lintDebug :sample:lintDebug :runtime:assembleRelease
```

Expected: build succeeds with zero test or lint errors. Existing unrelated dependency/version warnings may remain but must be listed in the handoff.

- [ ] **Step 5: Verify the lint detector is inside the AAR**

Run:

```powershell
jar tf runtime\build\outputs\aar\runtime-release.aar | Select-String '^lint.jar$'
```

Expected output contains exactly `lint.jar`.

Extract only to a temporary directory and inspect the nested registry entry:

```powershell
$artifactCheckDir = Join-Path ([System.IO.Path]::GetTempPath()) 'composium-aar-check'
if (Test-Path -LiteralPath $artifactCheckDir) {
    Remove-Item -LiteralPath $artifactCheckDir -Recurse -Force
}
New-Item -ItemType Directory -Path $artifactCheckDir | Out-Null
Push-Location $artifactCheckDir
jar xf 'F:\composium\runtime\build\outputs\aar\runtime-release.aar' lint.jar
jar tf lint.jar | Select-String 'ComposiumIssueRegistry|ComposiumContentPaddingDetector'
Pop-Location
```

Expected: both detector and registry classes are present. The temporary directory is outside the workspace and may be removed after inspection.

- [ ] **Step 6: Run repository hygiene checks**

Run:

```powershell
rg -n "enableEdgeToEdge|innerPadding|internalInnerPadding|TopSpacer|BottomSpacer|consumeParentScaffoldPadding" runtime sample README.md lint
git diff --check
git status --short --branch
```

Expected: old scene inset names are absent except deliberate migration prose; STG-only helper names appear only in negative detector tests/documentation; `git diff --check` is clean.

- [ ] **Step 7: Commit documentation and final migration cleanup**

```powershell
git add README.md runtime sample lint gradle settings.gradle.kts docs
git commit -m "docs: document scene tools and content padding"
```
