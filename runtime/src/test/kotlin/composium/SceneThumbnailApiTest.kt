package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class SceneThumbnailApiTest {

    @Test
    fun explicitNullDisablesThumbnailForDirectScene() {
        val scene = Scene(group = null, name = "Disabled", thumbnail = null, content = {})

        assertNull(scene.thumbnail)
        assertNull(scene.thumbnailContent())
    }

    @Test
    fun explicitNullDisablesThumbnailForDelegatedScene() {
        val disabled by scene(thumbnail = null, content = {})

        assertNull(disabled.thumbnail)
        assertNull(disabled.thumbnailContent())
    }

    @Test
    fun thumbnailContentUsesThumbnailWhenProvided() {
        val content: @Composable SceneScope.(PaddingValues) -> Unit = {}
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
        val content: @Composable SceneScope.(PaddingValues) -> Unit = {}
        val thumbnail: @Composable SceneScope.() -> Unit = {}
        val builtScene by scene(thumbnail = thumbnail, content = content)

        assertSame(content, builtScene.content)
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
        val content: @Composable SceneScope.(PaddingValues) -> Unit = {}
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
        assertSame(content, scene.content)
        assertSame(thumbnail, scene.thumbnailContent())
    }

    @Test
    fun sceneDelegateStoresBadge() {
        val badge: @Composable () -> Unit = {}
        val builtScene by scene(badge = badge, content = {})

        assertSame(badge, builtScene.badge)
    }
}
