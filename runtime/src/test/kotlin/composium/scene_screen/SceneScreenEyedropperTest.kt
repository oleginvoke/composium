package oleginvoke.com.composium.scene_screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneScreenEyedropperTest {

    @Test
    fun toggleEyedropperIntentTogglesVisibility() {
        val opened = reduceSceneScreen(
            state = SceneScreenState(),
            intent = SceneScreenIntent.ToggleEyedropper,
        )
        val closed = reduceSceneScreen(
            state = opened,
            intent = SceneScreenIntent.ToggleEyedropper,
        )

        assertTrue(opened.isEyedropperVisible)
        assertFalse(closed.isEyedropperVisible)
    }

    @Test
    fun hideEyedropperIntentIsIdempotent() {
        val visible = SceneScreenState(isEyedropperVisible = true)
        val hidden = reduceSceneScreen(
            state = visible,
            intent = SceneScreenIntent.HideEyedropper,
        )
        val unchanged = reduceSceneScreen(
            state = hidden,
            intent = SceneScreenIntent.HideEyedropper,
        )

        assertFalse(hidden.isEyedropperVisible)
        assertEquals(hidden, unchanged)
    }

    @Test
    fun eyedropperButtonStateReflectsVisibility() {
        val inactive = calculateSceneEyedropperButtonState(isVisible = false)
        val active = calculateSceneEyedropperButtonState(isVisible = true)

        assertFalse(inactive.active)
        assertEquals("Open eyedropper", inactive.contentDescription)
        assertTrue(active.active)
        assertEquals("Close eyedropper", active.contentDescription)
    }
}
