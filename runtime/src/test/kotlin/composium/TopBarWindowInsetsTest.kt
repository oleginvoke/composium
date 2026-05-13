package oleginvoke.com.composium

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TopBarWindowInsetsTest {

    private val density = Density(1f)

    @Test
    fun topBarWindowInsetsKeepOnlyTopAndHorizontalSides() {
        val source = WindowInsets(
            left = 10,
            top = 20,
            right = 30,
            bottom = 40,
        )

        val result = source.onlyTopAndHorizontal()

        assertEquals(10, result.getLeft(density, LayoutDirection.Ltr))
        assertEquals(20, result.getTop(density))
        assertEquals(30, result.getRight(density, LayoutDirection.Ltr))
        assertEquals(0, result.getBottom(density))
    }

    @Test
    fun nullableTopBarWindowInsetsStayNull() {
        assertNull((null as WindowInsets?).onlyTopAndHorizontalOrNull())
    }
}
