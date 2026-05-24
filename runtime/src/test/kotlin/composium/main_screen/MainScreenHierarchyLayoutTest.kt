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
    fun hierarchyConnectorStyleUsesStraightLinesWithoutArrows() {
        val style = mainScreenHierarchyConnectorStyle()

        assertEquals(false, style.hasArrow)
        assertEquals(6f, style.cornerRadiusDp)
    }

    @Test
    fun hierarchyConnectorElbowRoundsVerticalToHorizontalTurn() {
        val elbow = calculateMainScreenHierarchyConnectorElbow(
            connectorX = 16f,
            elbowEndX = 32f,
            targetYPx = 30f,
            requestedRadiusPx = 6f,
        )

        assertEquals(6f, elbow.radiusPx)
        assertEquals(24f, elbow.verticalEndYPx)
        assertEquals(22f, elbow.horizontalStartXPx)
    }

    @Test
    fun hierarchyConnectorElbowClampsRadiusToAvailableSpace() {
        val elbow = calculateMainScreenHierarchyConnectorElbow(
            connectorX = 16f,
            elbowEndX = 20f,
            targetYPx = 3f,
            requestedRadiusPx = 6f,
        )

        assertEquals(3f, elbow.radiusPx)
        assertEquals(0f, elbow.verticalEndYPx)
        assertEquals(19f, elbow.horizontalStartXPx)
    }

    @Test
    fun hierarchyConnectorTargetUsesItemCenter() {
        assertEquals(
            106f,
            mainScreenHierarchyConnectorTargetYPx(
                itemHeightPx = 220f,
                bottomGapPx = 8f,
            ),
        )
        assertEquals(
            30f,
            mainScreenHierarchyConnectorTargetYPx(
                itemHeightPx = 68f,
                bottomGapPx = 8f,
            ),
        )
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
    fun listViewportStartsAtSearchFieldMidlineAndPreservesInitialContentPosition() {
        val viewport = mainScreenListViewportLayout(
            topBarHeightDp = 150f,
            searchFieldHeightDp = 40f,
        )

        assertEquals(118f, viewport.topOffsetDp)
        assertEquals(32f, viewport.extraTopPaddingDp)
        assertEquals(
            158f,
            viewport.topOffsetDp + MainScreenListBaseTopPaddingDp + viewport.extraTopPaddingDp,
        )
        assertEquals(158f, 150f + MainScreenListBaseTopPaddingDp)
    }

    @Test
    fun listContentPaddingIncludesViewportOverlapPadding() {
        val padding = mainScreenListContentPadding(
            contentWindowInsets = null,
            density = density,
            extraTopPaddingDp = 32f,
        )

        assertEquals(40.dp, padding.calculateTopPadding())
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
