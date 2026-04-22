package com.example.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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
        device.wait(Until.hasObject(By.textContains("Benchmark 1")), WAIT_TIMEOUT_MS)
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
        device.wait(Until.findObject(By.text("Playground")), WAIT_TIMEOUT_MS)?.click()
        device.wait(Until.hasObject(By.desc("Open properties")), WAIT_TIMEOUT_MS)
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
            device.wait(Until.findObject(By.text("Playground")), WAIT_TIMEOUT_MS)?.click()
            device.wait(Until.findObject(By.desc("Open properties")), WAIT_TIMEOUT_MS)?.click()
            device.wait(Until.hasObject(By.text("Environment")), WAIT_TIMEOUT_MS)
        },
    ) {
        device.wait(Until.findObject(By.text("Environment")), WAIT_TIMEOUT_MS)?.click()
        device.wait(Until.findObject(By.text("Properties")), WAIT_TIMEOUT_MS)?.click()
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
            device.wait(Until.findObject(By.text("Playground")), WAIT_TIMEOUT_MS)?.click()
            device.wait(Until.findObject(By.desc("Open properties")), WAIT_TIMEOUT_MS)?.click()
            device.wait(Until.hasObject(By.text("Properties")), WAIT_TIMEOUT_MS)
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

    private fun waitForMainScreen() {
        device.wait(Until.hasObject(By.text("Composium")), WAIT_TIMEOUT_MS)
        device.wait(Until.hasObject(By.textContains("Search scenes")), WAIT_TIMEOUT_MS)
    }

    private fun focusSearchField() {
        device.wait(Until.findObject(By.textContains("Search scenes")), WAIT_TIMEOUT_MS)?.click()
            ?: device.findObject(By.clazz("android.widget.EditText"))?.click()
    }

    companion object {
        private const val TARGET_PACKAGE = "com.example.ComposiumSample"
        private const val WAIT_TIMEOUT_MS = 5_000L
    }
}
