package oleginvoke.com.composium.host_screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposiumHostScreenLogicTest {

    @Test
    fun `main screen input is blocked while scene overlay is rendered`() {
        assertTrue(
            shouldBlockMainScreenInput(
                ComposiumHostScreenState(
                    selectedSceneId = "buttons::primary",
                    renderedSceneId = "buttons::primary",
                    isSceneVisible = true,
                ),
            ),
        )
        assertTrue(
            shouldBlockMainScreenInput(
                ComposiumHostScreenState(
                    selectedSceneId = null,
                    renderedSceneId = "buttons::primary",
                    isSceneVisible = false,
                ),
            ),
        )
        assertFalse(shouldBlockMainScreenInput(ComposiumHostScreenState()))
    }

    @Test
    fun `select scene starts visible overlay for selected scene`() {
        val result = reduceComposiumHostScreen(
            state = ComposiumHostScreenState(),
            intent = ComposiumHostScreenIntent.SceneSelected("buttons::primary"),
        )

        assertEquals("buttons::primary", result.selectedSceneId)
        assertEquals("buttons::primary", result.renderedSceneId)
        assertTrue(result.isSceneVisible)
    }

    @Test
    fun `close scene keeps rendered overlay until exit animation settles`() {
        val result = reduceComposiumHostScreen(
            state = ComposiumHostScreenState(
                selectedSceneId = "buttons::primary",
                renderedSceneId = "buttons::primary",
                isSceneVisible = true,
            ),
            intent = ComposiumHostScreenIntent.SceneClosed,
        )

        assertNull(result.selectedSceneId)
        assertEquals("buttons::primary", result.renderedSceneId)
        assertFalse(result.isSceneVisible)
    }

    @Test
    fun `transition settled clears rendered overlay after close`() {
        val result = reduceComposiumHostScreen(
            state = ComposiumHostScreenState(
                selectedSceneId = null,
                renderedSceneId = "buttons::primary",
                isSceneVisible = false,
            ),
            intent = ComposiumHostScreenIntent.TransitionSettled,
        )

        assertNull(result.selectedSceneId)
        assertNull(result.renderedSceneId)
        assertFalse(result.isSceneVisible)
    }

    @Test
    fun `sync available scenes closes overlay when selected scene disappears`() {
        val result = reduceComposiumHostScreen(
            state = ComposiumHostScreenState(
                selectedSceneId = "buttons::primary",
                renderedSceneId = "buttons::primary",
                isSceneVisible = true,
            ),
            intent = ComposiumHostScreenIntent.AvailableScenesChanged(
                sceneIds = setOf("buttons::secondary"),
            ),
        )

        assertNull(result.selectedSceneId)
        assertEquals("buttons::primary", result.renderedSceneId)
        assertFalse(result.isSceneVisible)
    }
}
