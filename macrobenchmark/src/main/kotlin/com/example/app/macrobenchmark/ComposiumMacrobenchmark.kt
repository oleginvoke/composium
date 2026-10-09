package com.example.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposiumMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
        waitForMainScreen()
    }

    @Test
    fun scrollCatalog() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 7,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForMainScreen()
        },
    ) {
        repeat(3) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).roundToInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.24f).roundToInt(),
                24,
            )
            device.waitForIdle()
        }
        repeat(2) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.24f).roundToInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).roundToInt(),
                24,
            )
            device.waitForIdle()
        }
    }

    @Test
    fun searchScenes() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForMainScreen()
        },
    ) {
        focusSearchField()
        device.executeShellCommand("input text Benchmark")
        waitForObject(By.text(BENCHMARK_SCENE_NAME), "benchmark scene result")
    }

    @Test
    fun openSceneAndReturn() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForMainScreen()
        },
    ) {
        showBenchmarkSceneInCatalog()
        clickObject(By.text(BENCHMARK_SCENE_NAME), "benchmark scene")
        waitForObject(By.desc("Open properties"), "open properties button")
        device.pressBack()
        waitForMainScreen()
    }

    @Test
    fun switchInspectorTabs() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForMainScreen()
            openBenchmarkScene()
            clickObject(By.desc("Open properties"), "open properties button")
            waitForObject(By.text("Environment"), "environment tab")
        },
    ) {
        clickObject(By.text("Environment"), "environment tab")
        clickObject(By.text("Properties"), "properties tab")
    }

    @Test
    fun dragInspectorPane() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForMainScreen()
            openBenchmarkScene()
            clickObject(By.desc("Open properties"), "open properties button")
            waitForObject(By.text("Properties"), "properties tab")
        },
    ) {
        val centerX = device.displayWidth / 2
        val collapsedY = (device.displayHeight * 0.72f).roundToInt()
        val expandedY = (device.displayHeight * 0.34f).roundToInt()

        device.swipe(centerX, collapsedY, centerX, expandedY, 24)
        device.waitForIdle()
        device.swipe(centerX, expandedY, centerX, collapsedY, 24)
        device.waitForIdle()
    }

    @Test
    fun reactiveParameterThemeSwitching() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            openReactiveParametersScene()
        },
    ) {
        var status = reactiveParameterStatus()
        repeat(8) {
            clickObject(By.desc("Switch product theme"), "product theme button")
            status = status.copy(
                isProductB = !status.isProductB,
                themeSwitches = status.themeSwitches + 1,
            )
            waitForReactiveParameterStatus(status)
        }
    }

    @Test
    fun reactiveParameterUnrelatedRecomposition() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            openReactiveParametersScene()
        },
    ) {
        var status = reactiveParameterStatus()
        repeat(10) {
            clickObject(By.desc("Recompose scene"), "unrelated recomposition button")
            status = status.copy(unrelatedRecompositions = status.unrelatedRecompositions + 1)
            waitForReactiveParameterStatus(status)
        }
    }

    private fun openReactiveParametersScene() {
        // A warm activity can retain the previous iteration's scene. Reopen it to reset counters.
        device.findObject(By.desc("Return to catalog"))?.click()
        waitForMainScreen()
        focusSearchField()
        device.findObject(By.desc("Clear search"))?.click()
        device.executeShellCommand("input text Reactive")
        waitForObject(By.text(REACTIVE_PARAMETERS_SCENE_NAME), "reactive parameters scene result")
        device.pressBack() // Dismiss the search keyboard before entering the scene.
        clickObject(By.text(REACTIVE_PARAMETERS_SCENE_NAME), "reactive parameters scene")
        val status = reactiveParameterStatus()
        check(status.themeSwitches == 0 && status.unrelatedRecompositions == 0) {
            "Reactive parameter scene counters did not reset: $status"
        }
        device.waitForIdle()
    }

    private fun reactiveParameterStatus(): ReactiveParameterStatus {
        val text = checkNotNull(
            waitForObject(
                By.desc("Reactive parameter status"),
                "reactive parameter status",
            ).text,
        ) { "Reactive parameter status has no text" }
        val match = checkNotNull(REACTIVE_PARAMETER_STATUS_PATTERN.matchEntire(text)) {
            "Unexpected reactive parameter status: $text"
        }
        return ReactiveParameterStatus(
            isProductB = match.groupValues[1] == "B",
            themeSwitches = match.groupValues[2].toInt(),
            unrelatedRecompositions = match.groupValues[3].toInt(),
        )
    }

    private fun waitForReactiveParameterStatus(status: ReactiveParameterStatus) {
        // Wait for the product theme and event counters, never for reactive color updates.
        // This permits measurements against the baseline runtime where selected colors stay stale.
        waitForObject(By.text(status.text), "reactive parameter status ${status.text}")
        device.waitForIdle()
    }

    private data class ReactiveParameterStatus(
        val isProductB: Boolean,
        val themeSwitches: Int,
        val unrelatedRecompositions: Int,
    ) {
        val text: String
            get() = "Theme: product ${if (isProductB) "B" else "A"}; " +
                "theme switches: $themeSwitches; " +
                "unrelated recompositions: $unrelatedRecompositions; params: 100"
    }

    private fun waitForMainScreen() {
        waitForObject(By.text("Composium"), "main screen title")
        waitForObject(By.textContains("Search scenes"), "search field")
    }

    private fun openBenchmarkScene() {
        showBenchmarkSceneInCatalog()
        clickObject(By.text(BENCHMARK_SCENE_NAME), "benchmark scene")
        waitForObject(By.desc("Open properties"), "open properties button")
    }

    private fun showBenchmarkSceneInCatalog() {
        focusSearchField()
        val clearSearchButton = device.findObject(By.desc("Clear search"))
        if (clearSearchButton != null) {
            clearSearchButton.click()
        }
        device.executeShellCommand("input text Benchmark")
        waitForObject(By.text(BENCHMARK_SCENE_NAME), "benchmark scene result")
    }

    private fun focusSearchField() {
        val field = device.wait(
            Until.findObject(By.textContains("Search scenes")),
            WAIT_TIMEOUT_MS,
        ) ?: device.wait(
            Until.findObject(By.clazz("android.widget.EditText")),
            WAIT_TIMEOUT_MS,
        )

        checkNotNull(field) {
            "Timed out waiting for search field"
        }.click()
    }

    private fun clickObject(
        selector: BySelector,
        description: String,
    ) {
        waitForObject(selector, description).click()
    }

    private fun waitForObject(
        selector: BySelector,
        description: String,
    ): UiObject2 {
        return checkNotNull(device.wait(Until.findObject(selector), WAIT_TIMEOUT_MS)) {
            "Timed out waiting for $description"
        }
    }

    companion object {
        private const val TARGET_PACKAGE = "com.example.ComposiumSample"
        private const val BENCHMARK_SCENE_NAME = "Benchmark 1"
        private const val REACTIVE_PARAMETERS_SCENE_NAME = "Reactive parameters"
        private const val WAIT_TIMEOUT_MS = 5_000L
        private val REACTIVE_PARAMETER_STATUS_PATTERN = Regex(
            "Theme: product (A|B); theme switches: (\\d+); unrelated recompositions: (\\d+); params: 100",
        )
    }
}
