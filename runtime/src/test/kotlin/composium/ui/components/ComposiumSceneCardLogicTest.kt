package oleginvoke.com.composium.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailState

class ComposiumSceneCardLogicTest {

    @Test
    fun nonReadyThumbnailsUseStaticPlaceholderPresentation() {
        listOf(
            null,
            SceneThumbnailState.Pending,
            SceneThumbnailState.Capturing,
            SceneThumbnailState.Failed("timeout"),
        ).forEach { state ->
            assertEquals(
                SceneThumbnailPreviewPresentation.StaticPlaceholder,
                sceneThumbnailPreviewPresentation(state),
            )
        }
    }

}
