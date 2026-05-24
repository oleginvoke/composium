package oleginvoke.com.composium

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class SceneThumbnailApiTest {

    @Test
    fun thumbnailContentFallsBackToSceneContentWhenThumbnailIsMissing() {
        val content: @Composable SceneScope.() -> Unit = {}
        val scene = Scene(
            group = null,
            name = "Fallback",
            content = content,
        )

        assertSame(content, scene.thumbnailContent())
    }

    @Test
    fun thumbnailContentUsesThumbnailWhenProvided() {
        val content: @Composable SceneScope.() -> Unit = {}
        val thumbnail: @Composable SceneScope.() -> Unit = {}
        val scene = Scene(
            group = null,
            name = "Custom",
            content = content,
            thumbnail = thumbnail,
        )

        assertSame(thumbnail, scene.thumbnailContent())
    }

    @Test
    fun sceneDelegateStoresThumbnailSeparatelyFromContent() {
        val builtScene = DelegatedScenes.customThumbnail

        val thumbnail = assertNotNull(builtScene.thumbnail)
        assertSame(thumbnail, builtScene.thumbnailContent())
    }

    private object DelegatedScenes {
        val customThumbnail by scene(
            thumbnail = {},
            content = {},
        )
    }
}
