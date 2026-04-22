package oleginvoke.com.composium

import kotlin.test.Test
import kotlin.test.assertSame

class SceneScopeStateTest {

    @Test
    fun `paramsState returns stable instance`() {
        val sceneScope = SceneScope()

        assertSame(sceneScope.paramsState, sceneScope.paramsState)
    }
}
