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
    val hasNode: Boolean,
    val arrowLengthDp: Float,
    val arrowHalfHeightDp: Float,
)

internal data class MainScreenSearchFieldLayout(
    val heightDp: Float,
    val horizontalPaddingDp: Float,
    val clearButtonSizeDp: Float,
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
        hasNode = false,
        arrowLengthDp = MainScreenHierarchyArrowLengthDp,
        arrowHalfHeightDp = MainScreenHierarchyArrowHalfHeightDp,
    )

internal fun mainScreenSearchFieldLayout(): MainScreenSearchFieldLayout =
    MainScreenSearchFieldLayout(
        heightDp = MainScreenSearchFieldHeightDp,
        horizontalPaddingDp = MainScreenSearchFieldHorizontalPaddingDp,
        clearButtonSizeDp = MainScreenSearchFieldClearButtonSizeDp,
    )

internal fun mainScreenListContentPadding(
    contentWindowInsets: WindowInsets?,
    density: Density,
): PaddingValues {
    val bottomInset = contentWindowInsets
        ?.getBottom(density)
        ?.let { insetPx -> with(density) { insetPx.toDp() } }
        ?: 0.dp

    return PaddingValues(
        start = MainScreenListHorizontalPaddingDp.dp,
        top = MainScreenListVerticalPaddingDp.dp,
        end = MainScreenListHorizontalPaddingDp.dp,
        bottom = MainScreenListVerticalPaddingDp.dp + bottomInset,
    )
}

private const val MainScreenListHorizontalPaddingDp = 18f
private const val MainScreenListVerticalPaddingDp = 8f
private const val MainScreenHierarchyDepthStepDp = 32f
private const val MainScreenHierarchyConnectorCenterDp = 16f
private const val MainScreenHierarchyItemBottomGapDp = 8f
private const val MainScreenGroupCountBadgeSizeDp = 22f
private const val MainScreenGroupCountBadgeCornerRadiusDp = 6f
private const val MainScreenGroupHeaderHorizontalPaddingDp = 8f
private const val MainScreenGroupHeaderRootVerticalPaddingDp = 8f
private const val MainScreenGroupHeaderNestedVerticalPaddingDp = 6f
private const val MainScreenHierarchyArrowLengthDp = 5f
private const val MainScreenHierarchyArrowHalfHeightDp = 2.5f
private const val MainScreenSearchFieldHeightDp = 40f
private const val MainScreenSearchFieldHorizontalPaddingDp = 15f
private const val MainScreenSearchFieldClearButtonSizeDp = 28f
