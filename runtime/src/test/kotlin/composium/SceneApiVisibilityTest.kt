package oleginvoke.com.composium

import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneApiVisibilityTest {
    @Test
    fun sceneDelegateIsPublicButItsConstructorIsInternal() {
        assertEquals(KVisibility.PUBLIC, SceneDelegate::class.visibility)
        assertEquals(KVisibility.INTERNAL, SceneDelegate::class.primaryConstructor?.visibility)
    }

    @Test
    fun paramPropertyIsPublicButItsConstructorIsInternal() {
        assertEquals(KVisibility.PUBLIC, ParamProperty::class.visibility)
        assertEquals(KVisibility.INTERNAL, ParamProperty::class.primaryConstructor?.visibility)
    }
}
