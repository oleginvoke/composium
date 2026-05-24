package oleginvoke.com.composium.main_screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal data class MainScreenHierarchyLayout(
    val contentStartDp: Float,
    val parentConnectorCentersDp: List<Float>,
    val childConnectorCenterDp: Float,
    val currentConnectorCenterDp: Float?,
    val elbowEndDp: Float,
    val childStemStartInsetFromBottomDp: Float,
)

internal data class MainScreenGroupCountBadgeLayout(
    val sizeDp: Float,
    val cornerRadiusDp: Float,
)

internal data class MainScreenGroupHeaderLayout(
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
)

internal data class MainScreenHierarchyConnectorStyle(
    val hasArrow: Boolean,
    val cornerRadiusDp: Float,
)

internal data class MainScreenHierarchyConnectorElbow(
    val radiusPx: Float,
    val verticalEndYPx: Float,
    val horizontalStartXPx: Float,
)

internal data class MainScreenSearchFieldLayout(
    val heightDp: Float,
    val horizontalPaddingDp: Float,
    val clearButtonSizeDp: Float,
)

internal data class MainScreenListViewportLayout(
    val topOffsetDp: Float,
    val extraTopPaddingDp: Float,
)

internal fun calculateMainScreenHierarchyLayout(depth: Int): MainScreenHierarchyLayout {
    require(depth >= 0) { "depth must be >= 0" }

    val contentStartDp = depth * MainScreenHierarchyDepthStepDp
    val parentConnectorCenters = (0 until depth).map { level ->
        MainScreenHierarchyConnectorCenterDp + level * MainScreenHierarchyDepthStepDp
    }

    return MainScreenHierarchyLayout(
        contentStartDp = contentStartDp,
        parentConnectorCentersDp = parentConnectorCenters,
        childConnectorCenterDp = MainScreenHierarchyConnectorCenterDp + depth * MainScreenHierarchyDepthStepDp,
        currentConnectorCenterDp = parentConnectorCenters.lastOrNull(),
        elbowEndDp = contentStartDp,
        childStemStartInsetFromBottomDp = MainScreenHierarchyItemBottomGapDp,
    )
}

internal fun mainScreenConnectorContinuationsForChild(
    parentConnectorContinuations: List<Boolean>,
    hasNextSibling: Boolean,
): List<Boolean> = parentConnectorContinuations + hasNextSibling

internal fun mainScreenGroupCountBadgeLayout(): MainScreenGroupCountBadgeLayout =
    MainScreenGroupCountBadgeLayout(
        sizeDp = MainScreenGroupCountBadgeSizeDp,
        cornerRadiusDp = MainScreenGroupCountBadgeCornerRadiusDp,
    )

internal fun mainScreenGroupHeaderLayout(depth: Int): MainScreenGroupHeaderLayout {
    require(depth >= 0) { "depth must be >= 0" }
    return MainScreenGroupHeaderLayout(
        horizontalPaddingDp = MainScreenGroupHeaderHorizontalPaddingDp,
        verticalPaddingDp = if (depth == 0) {
            MainScreenGroupHeaderRootVerticalPaddingDp
        } else {
            MainScreenGroupHeaderNestedVerticalPaddingDp
        },
    )
}

internal fun mainScreenHierarchyConnectorStyle(): MainScreenHierarchyConnectorStyle =
    MainScreenHierarchyConnectorStyle(
        hasArrow = false,
        cornerRadiusDp = MainScreenHierarchyConnectorCornerRadiusDp,
    )

internal fun calculateMainScreenHierarchyConnectorElbow(
    connectorX: Float,
    elbowEndX: Float,
    targetYPx: Float,
    requestedRadiusPx: Float,
): MainScreenHierarchyConnectorElbow {
    require(targetYPx >= 0f) { "targetYPx must be >= 0" }
    require(requestedRadiusPx >= 0f) { "requestedRadiusPx must be >= 0" }

    val horizontalLengthPx = (elbowEndX - connectorX).coerceAtLeast(0f)
    val radiusPx = minOf(
        requestedRadiusPx,
        targetYPx,
        horizontalLengthPx,
    )
    return MainScreenHierarchyConnectorElbow(
        radiusPx = radiusPx,
        verticalEndYPx = targetYPx - radiusPx,
        horizontalStartXPx = connectorX + radiusPx,
    )
}

internal fun mainScreenHierarchyConnectorTargetYPx(
    itemHeightPx: Float,
    bottomGapPx: Float,
): Float {
    require(itemHeightPx >= 0f) { "itemHeightPx must be >= 0" }
    require(bottomGapPx >= 0f) { "bottomGapPx must be >= 0" }

    return (itemHeightPx - bottomGapPx)
        .coerceAtLeast(0f) / 2f
}

internal fun mainScreenSearchFieldLayout(): MainScreenSearchFieldLayout =
    MainScreenSearchFieldLayout(
        heightDp = MainScreenSearchFieldHeightDp,
        horizontalPaddingDp = MainScreenSearchFieldHorizontalPaddingDp,
        clearButtonSizeDp = MainScreenSearchFieldClearButtonSizeDp,
    )

internal fun mainScreenListViewportLayout(
    topBarHeightDp: Float,
    searchFieldHeightDp: Float = MainScreenSearchFieldHeightDp,
    topBarBottomPaddingDp: Float = MainScreenTopBarBottomPaddingDp,
): MainScreenListViewportLayout {
    require(topBarHeightDp >= 0f) { "topBarHeightDp must be >= 0" }
    require(searchFieldHeightDp >= 0f) { "searchFieldHeightDp must be >= 0" }
    require(topBarBottomPaddingDp >= 0f) { "topBarBottomPaddingDp must be >= 0" }

    val overlapDp = searchFieldHeightDp / 2f
    return MainScreenListViewportLayout(
        topOffsetDp = (topBarHeightDp - topBarBottomPaddingDp - overlapDp).coerceAtLeast(0f),
        extraTopPaddingDp = topBarBottomPaddingDp + overlapDp,
    )
}

internal fun mainScreenListContentPadding(
    contentWindowInsets: WindowInsets?,
    density: Density,
    extraTopPaddingDp: Float = 0f,
): PaddingValues {
    require(extraTopPaddingDp >= 0f) { "extraTopPaddingDp must be >= 0" }

    val bottomInset = contentWindowInsets
        ?.getBottom(density)
        ?.let { insetPx -> with(density) { insetPx.toDp() } }
        ?: 0.dp

    return PaddingValues(
        start = MainScreenListHorizontalPaddingDp.dp,
        top = (MainScreenListBaseTopPaddingDp + extraTopPaddingDp).dp,
        end = MainScreenListHorizontalPaddingDp.dp,
        bottom = MainScreenListVerticalPaddingDp.dp + bottomInset,
    )
}

internal const val MainScreenListBaseTopPaddingDp = 8f
internal const val MainScreenTopBarBottomPaddingDp = 12f

private const val MainScreenListHorizontalPaddingDp = 18f
private const val MainScreenListVerticalPaddingDp = MainScreenListBaseTopPaddingDp
private const val MainScreenHierarchyDepthStepDp = 32f
private const val MainScreenHierarchyConnectorCenterDp = 16f
private const val MainScreenHierarchyItemBottomGapDp = 8f
private const val MainScreenHierarchyConnectorCornerRadiusDp = 6f
private const val MainScreenGroupCountBadgeSizeDp = 22f
private const val MainScreenGroupCountBadgeCornerRadiusDp = 6f
private const val MainScreenGroupHeaderHorizontalPaddingDp = 8f
private const val MainScreenGroupHeaderRootVerticalPaddingDp = 8f
private const val MainScreenGroupHeaderNestedVerticalPaddingDp = 6f
private const val MainScreenSearchFieldHeightDp = 40f
private const val MainScreenSearchFieldHorizontalPaddingDp = 15f
private const val MainScreenSearchFieldClearButtonSizeDp = 28f
