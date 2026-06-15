package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailPreviewHorizontalAlignment
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailState
import oleginvoke.com.composium.scene_thumbnail.calculateSceneThumbnailReadyPreviewSize
import oleginvoke.com.composium.scene_thumbnail.sceneThumbnailCardLayout
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.pressScale
import oleginvoke.com.composium.ui.theme.rememberMinPressedState

@Composable
internal fun ComposiumSceneCard(
    name: String,
    group: String?,
    thumbnailState: SceneThumbnailState?,
    badge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = rememberMinPressedState(
        interactionSource = interactionSource,
        minPressedStateMillis = SceneCardMinPressedStateMillis,
    )
    val layout = remember { sceneThumbnailCardLayout() }
    val previewHeight = layout.previewHeightDp.dp
    val previewPadding = SceneThumbnailPreviewPadding

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressed = pressed)
            .shadow(
                elevation = if (pressed) 3.dp else 1.dp,
                shape = Tokens.shapes.medium,
                ambientColor = Tokens.colors.scrim.copy(alpha = 0.08f),
                spotColor = Tokens.colors.scrim.copy(alpha = 0.10f),
            )
            .clip(Tokens.shapes.medium)
            .background(Tokens.colors.surface.copy(alpha = if (pressed) 0.94f else 0.98f))
            .border(
                width = 1.dp,
                color = Tokens.colors.outlineVariant.copy(alpha = 0.78f),
                shape = Tokens.shapes.medium,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .background(Tokens.colors.surfaceVariant.copy(alpha = 0.42f))
                    .drawThumbnailBottomBorder(
                        color = Tokens.colors.outlineVariant.copy(alpha = 0.52f),
                    ),
            ) {
                SceneThumbnailPreview(
                    state = thumbnailState,
                    horizontalAlignment = layout.previewHorizontalAlignment,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(previewPadding),
                )

                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                    ) {
                        badge()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 12.dp,
                    ),
            ) {
                ComposiumText(
                    text = name,
                    style = Tokens.typography.titleMedium,
                    color = Tokens.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!group.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    ComposiumText(
                        text = formatSceneGroupPath(group),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(if (group.isNullOrBlank()) 4.dp else 5.dp))
                ComposiumText(
                    text = "Scene",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal enum class SceneThumbnailPreviewPresentation {
    Ready,
    StaticPlaceholder,
}

internal fun sceneThumbnailPreviewPresentation(
    state: SceneThumbnailState?,
): SceneThumbnailPreviewPresentation =
    when (state) {
        is SceneThumbnailState.Ready -> SceneThumbnailPreviewPresentation.Ready
        is SceneThumbnailState.Failed,
        SceneThumbnailState.Capturing,
        SceneThumbnailState.Pending,
        null,
        -> SceneThumbnailPreviewPresentation.StaticPlaceholder
    }

@Composable
private fun SceneThumbnailPreview(
    state: SceneThumbnailState?,
    horizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    modifier: Modifier = Modifier,
) {
    when (sceneThumbnailPreviewPresentation(state)) {
        SceneThumbnailPreviewPresentation.Ready -> {
            val readyState = state as SceneThumbnailState.Ready
            BoxWithConstraints(
                modifier = modifier,
                contentAlignment = horizontalAlignment.toComposeAlignment(),
            ) {
                val maxPreviewHeight = SceneThumbnailPreviewMaxHeight
                val previewSize = calculateSceneThumbnailReadyPreviewSize(
                    imageWidthPx = readyState.image.width,
                    imageHeightPx = readyState.image.height,
                    maxWidthDp = maxWidth.value,
                    maxHeightDp = minOf(maxHeight.value, maxPreviewHeight.value),
                    captureScale = readyState.captureScale,
                )
                Box(
                    modifier = Modifier
                        .size(
                            width = previewSize.widthDp.dp,
                            height = previewSize.heightDp.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = readyState.image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        SceneThumbnailPreviewPresentation.StaticPlaceholder -> {
            SceneThumbnailStaticPlaceholder(
                horizontalAlignment = horizontalAlignment,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SceneThumbnailStaticPlaceholder(
    horizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawStaticThumbnailPlaceholder(
                horizontalAlignment = horizontalAlignment,
                containerColor = Tokens.colors.surface.copy(alpha = 0.38f),
                primaryShapeColor = Tokens.colors.onSurfaceVariant.copy(alpha = 0.10f),
                secondaryShapeColor = Tokens.colors.onSurfaceVariant.copy(alpha = 0.07f),
                accentShapeColor = Tokens.colors.primary.copy(alpha = 0.10f),
            ),
    )
}

private val SceneThumbnailPreviewMaxHeight = 124.dp
private val SceneThumbnailPreviewPadding = 7.dp
private const val SceneCardMinPressedStateMillis = 140

private fun Modifier.drawThumbnailBottomBorder(color: Color): Modifier =
    drawBehind {
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }

private fun Modifier.drawStaticThumbnailPlaceholder(
    horizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    containerColor: Color,
    primaryShapeColor: Color,
    secondaryShapeColor: Color,
    accentShapeColor: Color,
): Modifier =
    drawBehind {
        val containerWidth = minOf(size.width * 0.62f, 168.dp.toPx())
            .coerceAtLeast(minOf(size.width, 72.dp.toPx()))
        val containerHeight = minOf(size.height * 0.62f, 58.dp.toPx())
            .coerceAtLeast(minOf(size.height, 34.dp.toPx()))
        val left = when (horizontalAlignment) {
            SceneThumbnailPreviewHorizontalAlignment.Start -> 0f
            SceneThumbnailPreviewHorizontalAlignment.Center -> (size.width - containerWidth) / 2f
        }.coerceAtLeast(0f)
        val top = ((size.height - containerHeight) / 2f).coerceAtLeast(0f)
        val radius = 9.dp.toPx()
        val smallRadius = 4.dp.toPx()
        val inset = 10.dp.toPx().coerceAtMost(containerWidth * 0.14f)
        val lineHeight = 6.dp.toPx().coerceAtMost(containerHeight * 0.16f)
        val chipSize = 17.dp.toPx().coerceAtMost(containerHeight * 0.34f)

        drawRoundRect(
            color = containerColor,
            topLeft = Offset(left, top),
            size = Size(containerWidth, containerHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = primaryShapeColor,
            topLeft = Offset(left + inset, top + inset),
            size = Size(containerWidth * 0.42f, lineHeight),
            cornerRadius = CornerRadius(smallRadius, smallRadius),
        )
        drawRoundRect(
            color = accentShapeColor,
            topLeft = Offset(left + containerWidth - inset - chipSize, top + inset),
            size = Size(chipSize, chipSize),
            cornerRadius = CornerRadius(chipSize / 2f, chipSize / 2f),
        )
        drawRoundRect(
            color = secondaryShapeColor,
            topLeft = Offset(left + inset, top + containerHeight - inset - lineHeight),
            size = Size(containerWidth * 0.70f, lineHeight),
            cornerRadius = CornerRadius(smallRadius, smallRadius),
        )
    }

private fun SceneThumbnailPreviewHorizontalAlignment.toComposeAlignment(): Alignment =
    when (this) {
        SceneThumbnailPreviewHorizontalAlignment.Start -> Alignment.CenterStart
        SceneThumbnailPreviewHorizontalAlignment.Center -> Alignment.Center
    }

private fun formatSceneGroupPath(group: String): String =
    group.split("/")
        .map { segment -> segment.trim() }
        .filter { segment -> segment.isNotEmpty() }
        .joinToString(" / ")
        .ifEmpty { group }
