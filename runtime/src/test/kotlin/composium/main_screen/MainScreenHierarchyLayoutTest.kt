package oleginvoke.com.composium.main_screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class MainScreenHierarchyLayoutTest {

    private val density = Density(1f)

    @Test
    fun rootItemHasNoParentConnectors() {
        val layout = calculateMainScreenHierarchyLayout(depth = 0)

        assertEquals(0f, layout.contentStartDp)
        assertEquals(emptyList(), layout.parentConnectorCentersDp)
        assertEquals(16f, layout.childConnectorCenterDp)
        assertEquals(8f, layout.childStemStartInsetFromBottomDp)
    }

    @Test
    fun nestedItemOffsetsContentAndParentConnectorsByDepth() {
        val layout = calculateMainScreenHierarchyLayout(depth = 2)

        assertEquals(64f, layout.contentStartDp)
        assertEquals(listOf(16f, 48f), layout.parentConnectorCentersDp)
        assertEquals(80f, layout.childConnectorCenterDp)
        assertEquals(48f, layout.currentConnectorCenterDp)
        assertEquals(64f, layout.elbowEndDp)
        assertEquals(8f, layout.childStemStartInsetFromBottomDp)
    }

    @Test
    fun childConnectorContinuationsAppendSiblingStateToInheritedLevels() {
        assertEquals(
            listOf(true, false, true),
            mainScreenConnectorContinuationsForChild(
                parentConnectorContinuations = listOf(true, false),
                hasNextSibling = true,
            ),
        )
        assertEquals(
            listOf(true, false, false),
            mainScreenConnectorContinuationsForChild(
                parentConnectorContinuations = listOf(true, false),
                hasNextSibling = false,
            ),
        )
    }

    @Test
    fun groupCountBadgeUsesCompactSquareMetrics() {
        val layout = mainScreenGroupCountBadgeLayout()

        assertEquals(22f, layout.sizeDp)
        assertEquals(6f, layout.cornerRadiusDp)
    }

    @Test
    fun groupHeaderLayoutUsesCompactInternalPadding() {
        val rootLayout = mainScreenGroupHeaderLayout(depth = 0)
        val nestedLayout = mainScreenGroupHeaderLayout(depth = 1)

        assertEquals(8f, rootLayout.horizontalPaddingDp)
        assertEquals(8f, rootLayout.verticalPaddingDp)
        assertEquals(8f, nestedLayout.horizontalPaddingDp)
        assertEquals(6f, nestedLayout.verticalPaddingDp)
    }

    @Test
    fun hierarchyConnectorStyleUsesArrowsWithoutNodes() {
        val style = mainScreenHierarchyConnectorStyle()

        assertEquals(false, style.hasNode)
        assertEquals(5f, style.arrowLengthDp)
        assertEquals(2.5f, style.arrowHalfHeightDp)
    }

    @Test
    fun listContentPaddingAddsBottomWindowInsetToBasePadding() {
        val padding = mainScreenListContentPadding(
            contentWindowInsets = WindowInsets(bottom = 42),
            density = density,
        )

        assertEquals(18.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(18.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(8.dp, padding.calculateTopPadding())
        assertEquals(50.dp, padding.calculateBottomPadding())
    }

    @Test
    fun listContentPaddingUsesBasePaddingWithoutWindowInsets() {
        val padding = mainScreenListContentPadding(
            contentWindowInsets = null,
            density = density,
        )

        assertEquals(8.dp, padding.calculateBottomPadding())
    }

    @Test
    fun searchFieldLayoutKeepsFixedHeightAcrossStates() {
        val layout = mainScreenSearchFieldLayout()

        assertEquals(40f, layout.heightDp)
        assertEquals(28f, layout.clearButtonSizeDp)
        assertEquals(15f, layout.horizontalPaddingDp)
        assertEquals(true, layout.clearButtonSizeDp < layout.heightDp)
    }
}
