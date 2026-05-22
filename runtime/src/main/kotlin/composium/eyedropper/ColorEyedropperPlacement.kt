package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class ColorEyedropperLensAnchor(
    val horizontal: Int,
    val vertical: Int,
) {
    Top(horizontal = 0, vertical = -1),
    TopEnd(horizontal = 1, vertical = -1),
    TopStart(horizontal = -1, vertical = -1),
    End(horizontal = 1, vertical = 0),
    Start(horizontal = -1, vertical = 0),
    Bottom(horizontal = 0, vertical = 1),
    BottomEnd(horizontal = 1, vertical = 1),
    BottomStart(horizontal = -1, vertical = 1);
}

internal data class ColorEyedropperLensPlacement(
    val offset: IntOffset,
    val anchor: ColorEyedropperLensAnchor,
)

internal data class ColorEyedropperOverlayPlacement(
    val lensPlacement: ColorEyedropperLensPlacement,
    val islandOffset: IntOffset,
)

internal data class ColorEyedropperSafeInsets(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal fun calculateColorEyedropperOverlayPlacement(
    target: Offset,
    containerSize: IntSize,
    lensSize: IntSize,
    cursorSize: IntSize,
    islandSize: IntSize,
    lensSpacingPx: Float,
    islandSpacingPx: Float,
    safePaddingPx: Float,
): ColorEyedropperOverlayPlacement =
    calculateColorEyedropperOverlayPlacement(
        target = target,
        containerSize = containerSize,
        lensSize = lensSize,
        cursorSize = cursorSize,
        islandSize = islandSize,
        lensSpacingPx = lensSpacingPx,
        islandSpacingPx = islandSpacingPx,
        safeInsets = safePaddingPx.toColorEyedropperSafeInsets(),
    )

internal fun calculateColorEyedropperOverlayPlacement(
    target: Offset,
    containerSize: IntSize,
    lensSize: IntSize,
    cursorSize: IntSize,
    islandSize: IntSize,
    lensSpacingPx: Float,
    islandSpacingPx: Float,
    safeInsets: ColorEyedropperSafeInsets,
): ColorEyedropperOverlayPlacement {
    val lensPlacement = calculateColorEyedropperLensPlacement(
        target = target,
        containerSize = containerSize,
        lensSize = lensSize,
        cursorSize = cursorSize,
        spacingPx = lensSpacingPx,
        safeInsets = safeInsets,
    )
    if (islandSize.width <= 0 || islandSize.height <= 0) {
        return ColorEyedropperOverlayPlacement(
            lensPlacement = lensPlacement,
            islandOffset = lensPlacement.offset,
        )
    }

    val fitting = colorEyedropperAnchors()
        .asSequence()
        .mapNotNull { anchor ->
            val lensOffset = calculateLensCandidateOffset(
                target = target,
                lensSize = lensSize,
                cursorRadiusX = cursorSize.width / 2f,
                cursorRadiusY = cursorSize.height / 2f,
                spacingPx = lensSpacingPx,
                anchor = anchor,
            )
            if (
                !lensOffset.fits(
                    width = lensSize.width,
                    height = lensSize.height,
                    containerSize = containerSize,
                    safeInsets = safeInsets,
                )
            ) {
                return@mapNotNull null
            }

            islandOffsetsForAnchor(
                lensOffset = lensOffset,
                lensSize = lensSize,
                islandSize = islandSize,
                spacingPx = islandSpacingPx,
                anchor = anchor,
            ).firstOrNull { islandOffset ->
                islandOffset.fits(
                    width = islandSize.width,
                    height = islandSize.height,
                    containerSize = containerSize,
                    safeInsets = safeInsets,
                )
            }?.let { islandOffset ->
                ColorEyedropperOverlayPlacement(
                    lensPlacement = ColorEyedropperLensPlacement(
                        offset = lensOffset.round(),
                        anchor = anchor,
                    ),
                    islandOffset = islandOffset.round(),
                )
            }
        }
        .firstOrNull()

    if (fitting != null) return fitting

    return ColorEyedropperOverlayPlacement(
        lensPlacement = lensPlacement,
        islandOffset = calculateColorEyedropperIslandOffset(
            lensPlacement = lensPlacement,
            containerSize = containerSize,
            lensSize = lensSize,
            islandSize = islandSize,
            spacingPx = islandSpacingPx,
            safeInsets = safeInsets,
        ),
    )
}

internal fun calculateColorEyedropperLensPlacement(
    target: Offset,
    containerSize: IntSize,
    lensSize: IntSize,
    cursorSize: IntSize,
    spacingPx: Float,
    safePaddingPx: Float,
): ColorEyedropperLensPlacement =
    calculateColorEyedropperLensPlacement(
        target = target,
        containerSize = containerSize,
        lensSize = lensSize,
        cursorSize = cursorSize,
        spacingPx = spacingPx,
        safeInsets = safePaddingPx.toColorEyedropperSafeInsets(),
    )

internal fun calculateColorEyedropperLensPlacement(
    target: Offset,
    containerSize: IntSize,
    lensSize: IntSize,
    cursorSize: IntSize,
    spacingPx: Float,
    safeInsets: ColorEyedropperSafeInsets,
): ColorEyedropperLensPlacement {
    if (containerSize.width <= 0 || containerSize.height <= 0) {
        return ColorEyedropperLensPlacement(
            offset = IntOffset.Zero,
            anchor = ColorEyedropperLensAnchor.Top,
        )
    }
    if (lensSize.width <= 0 || lensSize.height <= 0) return ColorEyedropperLensPlacement(
        offset = IntOffset.Zero,
        anchor = ColorEyedropperLensAnchor.Top,
    )

    val minX = safeInsets.left
    val minY = safeInsets.top
    val maxX = (containerSize.width - lensSize.width - safeInsets.right).coerceAtLeast(minX)
    val maxY = (containerSize.height - lensSize.height - safeInsets.bottom).coerceAtLeast(minY)
    val cursorRadiusX = cursorSize.width / 2f
    val cursorRadiusY = cursorSize.height / 2f

    val candidates = colorEyedropperAnchors().map { anchor ->
        anchor to calculateLensCandidateOffset(
            target = target,
            lensSize = lensSize,
            cursorRadiusX = cursorRadiusX,
            cursorRadiusY = cursorRadiusY,
            spacingPx = spacingPx,
            anchor = anchor,
        )
    }
    val selected = candidates.firstOrNull { (_, candidate) ->
        candidate.fits(
            width = lensSize.width,
            height = lensSize.height,
            containerSize = containerSize,
            safeInsets = safeInsets,
        )
    } ?: candidates.first()

    return ColorEyedropperLensPlacement(
        offset = selected.second.clampTo(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        ),
        anchor = selected.first,
    )
}

internal fun calculateColorEyedropperIslandOffset(
    lensPlacement: ColorEyedropperLensPlacement,
    containerSize: IntSize,
    lensSize: IntSize,
    islandSize: IntSize,
    spacingPx: Float,
    safePaddingPx: Float,
): IntOffset =
    calculateColorEyedropperIslandOffset(
        lensPlacement = lensPlacement,
        containerSize = containerSize,
        lensSize = lensSize,
        islandSize = islandSize,
        spacingPx = spacingPx,
        safeInsets = safePaddingPx.toColorEyedropperSafeInsets(),
    )

internal fun calculateColorEyedropperIslandOffset(
    lensPlacement: ColorEyedropperLensPlacement,
    containerSize: IntSize,
    lensSize: IntSize,
    islandSize: IntSize,
    spacingPx: Float,
    safeInsets: ColorEyedropperSafeInsets,
): IntOffset {
    if (containerSize.width <= 0 || containerSize.height <= 0) return IntOffset.Zero
    if (lensSize.width <= 0 || lensSize.height <= 0) return lensPlacement.offset
    if (islandSize.width <= 0 || islandSize.height <= 0) return lensPlacement.offset

    val minX = safeInsets.left
    val minY = safeInsets.top
    val maxX = (containerSize.width - islandSize.width - safeInsets.right).coerceAtLeast(minX)
    val maxY = (containerSize.height - islandSize.height - safeInsets.bottom).coerceAtLeast(minY)
    val centeredX = lensPlacement.offset.x + lensSize.width / 2f - islandSize.width / 2f
    val candidates = islandOffsetsForAnchor(
        lensOffset = Offset(
            x = lensPlacement.offset.x.toFloat(),
            y = lensPlacement.offset.y.toFloat(),
        ),
        lensSize = lensSize,
        islandSize = islandSize,
        spacingPx = spacingPx,
        anchor = lensPlacement.anchor,
    ).ifEmpty {
        listOf(
            Offset(
                x = centeredX,
                y = lensPlacement.offset.y - spacingPx - islandSize.height,
            ),
        )
    }
    val selected = candidates.firstOrNull { candidate ->
        candidate.fits(
            width = islandSize.width,
            height = islandSize.height,
            containerSize = containerSize,
            safeInsets = safeInsets,
        )
    } ?: candidates.first()

    return selected.clampTo(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
    )
}

private fun colorEyedropperAnchors(): List<ColorEyedropperLensAnchor> =
    listOf(
        ColorEyedropperLensAnchor.Top,
        ColorEyedropperLensAnchor.TopEnd,
        ColorEyedropperLensAnchor.TopStart,
        ColorEyedropperLensAnchor.End,
        ColorEyedropperLensAnchor.Start,
        ColorEyedropperLensAnchor.Bottom,
        ColorEyedropperLensAnchor.BottomEnd,
        ColorEyedropperLensAnchor.BottomStart,
    )

private fun islandOffsetsForAnchor(
    lensOffset: Offset,
    lensSize: IntSize,
    islandSize: IntSize,
    spacingPx: Float,
    anchor: ColorEyedropperLensAnchor,
): List<Offset> {
    val centeredX = lensOffset.x + lensSize.width / 2f - islandSize.width / 2f
    val above = Offset(
        x = centeredX,
        y = lensOffset.y - spacingPx - islandSize.height,
    )
    val below = Offset(
        x = centeredX,
        y = lensOffset.y + lensSize.height + spacingPx,
    )
    return when {
        anchor.vertical < 0 -> listOf(above)
        anchor.vertical > 0 -> listOf(below)
        else -> listOf(above, below)
    }
}

private fun calculateLensCandidateOffset(
    target: Offset,
    lensSize: IntSize,
    cursorRadiusX: Float,
    cursorRadiusY: Float,
    spacingPx: Float,
    anchor: ColorEyedropperLensAnchor,
): Offset {
    if (anchor.horizontal != 0 && anchor.vertical != 0) {
        val diagonalUnit = 1f / sqrt(2f)
        val lensRadiusX = lensSize.width / 2f
        val lensRadiusY = lensSize.height / 2f
        return Offset(
            x = target.x +
                anchor.horizontal * (cursorRadiusX + spacingPx + lensRadiusX) * diagonalUnit -
                lensRadiusX,
            y = target.y +
                anchor.vertical * (cursorRadiusY + spacingPx + lensRadiusY) * diagonalUnit -
                lensRadiusY,
        )
    }

    val x = when (anchor.horizontal) {
        -1 -> target.x - cursorRadiusX - spacingPx - lensSize.width
        1 -> target.x + cursorRadiusX + spacingPx
        else -> target.x - lensSize.width / 2f
    }
    val y = when (anchor.vertical) {
        -1 -> target.y - cursorRadiusY - spacingPx - lensSize.height
        1 -> target.y + cursorRadiusY + spacingPx
        else -> target.y - lensSize.height / 2f
    }
    return Offset(x = x, y = y)
}

private fun Offset.fits(
    width: Int,
    height: Int,
    containerSize: IntSize,
    safeInsets: ColorEyedropperSafeInsets,
): Boolean =
    x >= safeInsets.left &&
        y >= safeInsets.top &&
        x + width <= containerSize.width - safeInsets.right &&
        y + height <= containerSize.height - safeInsets.bottom

private fun Offset.clampTo(
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
): IntOffset =
    IntOffset(
        x = x.coerceIn(minX, maxX).roundToInt(),
        y = y.coerceIn(minY, maxY).roundToInt(),
    )

private fun Offset.round(): IntOffset =
    IntOffset(
        x = x.roundToInt(),
        y = y.roundToInt(),
    )

private fun Float.toColorEyedropperSafeInsets(): ColorEyedropperSafeInsets =
    ColorEyedropperSafeInsets(
        left = this,
        top = this,
        right = this,
        bottom = this,
    )
