package oleginvoke.com.composium.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailState
import oleginvoke.com.composium.test_fixtures.fakeImageBitmap

class ComposiumSceneCardLogicTest {

    @Test
    fun onlyReadyThumbnailsUseImagePresentation() {
        val ready = SceneThumbnailState.Ready(fakeImageBitmap(), byteSizeBytes = 400)
        assertEquals(SceneThumbnailPreviewPresentation.Ready, sceneThumbnailPreviewPresentation(ready))
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
