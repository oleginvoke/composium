package oleginvoke.com.composium

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun sceneBadgeIsOptionalByDefault() {
        val scene = Scene(
            group = null,
            name = "Default",
            content = {},
        )

        assertNull(scene.badge)
    }

    @Test
    fun sceneStoresBadgeSeparatelyFromThumbnailAndContent() {
        val content: @Composable SceneScope.() -> Unit = {}
        val thumbnail: @Composable SceneScope.() -> Unit = {}
        val badge: @Composable () -> Unit = {}
        val scene = Scene(
            group = null,
            name = "Custom badge",
            content = content,
            thumbnail = thumbnail,
            badge = badge,
        )

        assertSame(badge, scene.badge)
        assertSame(thumbnail, scene.thumbnailContent())
    }

    @Test
    fun sceneDelegateStoresBadge() {
        val builtScene = DelegatedScenes.customBadge

        assertSame(assertNotNull(builtScene.badge), builtScene.badge)
    }

    private object DelegatedScenes {
        val customThumbnail by scene(
            thumbnail = {},
            content = {},
        )

        val customBadge by scene(
            badge = {},
            content = {},
        )
    }
}
