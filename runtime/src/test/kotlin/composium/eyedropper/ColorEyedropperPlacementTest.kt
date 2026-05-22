package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorEyedropperPlacementTest {

    @Test
    fun placesLensAboveCursorWhenItFits() {
        val result = calculateColorEyedropperLensPlacement(
            target = Offset(150f, 160f),
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.Top, result.anchor)
        assertEquals(IntOffset(x = 110, y = 52), result.offset)
    }

    @Test
    fun placesLensBelowCursorNearTopEdge() {
        val result = calculateColorEyedropperLensPlacement(
            target = Offset(150f, 30f),
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.Bottom, result.anchor)
        assertEquals(IntOffset(x = 110, y = 58), result.offset)
    }

    @Test
    fun usesTopEndDiagonalWhenCenteredTopWouldOverflowStartEdge() {
        val result = calculateColorEyedropperLensPlacement(
            target = Offset(22f, 160f),
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.TopEnd, result.anchor)
        assertEquals(IntOffset(x = 30, y = 72), result.offset)
    }

    @Test
    fun keepsDiagonalLensAtConfiguredCircularGapFromCursor() {
        val target = Offset(22f, 160f)
        val lensSize = IntSize(width = 80, height = 80)
        val cursorSize = IntSize(width = 32, height = 32)
        val spacing = 12f

        val result = calculateColorEyedropperLensPlacement(
            target = target,
            containerSize = IntSize(width = 300, height = 320),
            lensSize = lensSize,
            cursorSize = cursorSize,
            spacingPx = spacing,
            safePaddingPx = 8f,
        )

        val lensRadius = lensSize.width / 2.0
        val cursorRadius = cursorSize.width / 2.0
        val lensCenterX = result.offset.x + lensRadius
        val lensCenterY = result.offset.y + lensRadius
        val centerDistance = sqrt(
            (lensCenterX - target.x) * (lensCenterX - target.x) +
                (lensCenterY - target.y) * (lensCenterY - target.y),
        )
        val circularGap = centerDistance - lensRadius - cursorRadius

        assertEquals(ColorEyedropperLensAnchor.TopEnd, result.anchor)
        assertEquals(spacing.toDouble(), circularGap, absoluteTolerance = 0.75)
    }

    @Test
    fun usesTopStartDiagonalWhenCenteredTopWouldOverflowEndEdge() {
        val result = calculateColorEyedropperLensPlacement(
            target = Offset(278f, 160f),
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.TopStart, result.anchor)
        assertEquals(IntOffset(x = 190, y = 72), result.offset)
    }

    @Test
    fun clampsLensInsideTinyContainerWhenNoAnchorFullyFits() {
        val result = calculateColorEyedropperLensPlacement(
            target = Offset(35f, 35f),
            containerSize = IntSize(width = 90, height = 90),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.Top, result.anchor)
        assertEquals(IntOffset(x = 8, y = 8), result.offset)
    }

    @Test
    fun placesIslandAboveLensWhenLensIsAboveCursor() {
        val lensPlacement = ColorEyedropperLensPlacement(
            offset = IntOffset(x = 110, y = 92),
            anchor = ColorEyedropperLensAnchor.Top,
        )

        val result = calculateColorEyedropperIslandOffset(
            lensPlacement = lensPlacement,
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            islandSize = IntSize(width = 130, height = 42),
            spacingPx = 8f,
            safePaddingPx = 8f,
        )

        assertEquals(IntOffset(x = 85, y = 42), result)
    }

    @Test
    fun placesIslandBelowLensWhenLensIsBelowCursor() {
        val lensPlacement = ColorEyedropperLensPlacement(
            offset = IntOffset(x = 110, y = 58),
            anchor = ColorEyedropperLensAnchor.Bottom,
        )

        val result = calculateColorEyedropperIslandOffset(
            lensPlacement = lensPlacement,
            containerSize = IntSize(width = 300, height = 320),
            lensSize = IntSize(width = 80, height = 80),
            islandSize = IntSize(width = 130, height = 42),
            spacingPx = 8f,
            safePaddingPx = 8f,
        )

        assertEquals(IntOffset(x = 85, y = 146), result)
    }

    @Test
    fun movesLensAndIslandTogetherWhenTopBundleDoesNotFit() {
        val result = calculateColorEyedropperOverlayPlacement(
            target = Offset(115f, 145f),
            containerSize = IntSize(width = 230, height = 400),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            islandSize = IntSize(width = 120, height = 42),
            lensSpacingPx = 12f,
            islandSpacingPx = 8f,
            safePaddingPx = 8f,
        )

        assertEquals(ColorEyedropperLensAnchor.Bottom, result.lensPlacement.anchor)
        assertEquals(IntOffset(x = 75, y = 173), result.lensPlacement.offset)
        assertEquals(IntOffset(x = 55, y = 261), result.islandOffset)
    }

    @Test
    fun placesSideLensIslandBelowWhenAboveWouldOverlapSafeTop() {
        val result = calculateColorEyedropperOverlayPlacement(
            target = Offset(120f, 150f),
            containerSize = IntSize(width = 300, height = 300),
            lensSize = IntSize(width = 80, height = 80),
            cursorSize = IntSize(width = 32, height = 32),
            islandSize = IntSize(width = 120, height = 42),
            lensSpacingPx = 12f,
            islandSpacingPx = 8f,
            safeInsets = ColorEyedropperSafeInsets(
                left = 8f,
                top = 100f,
                right = 8f,
                bottom = 8f,
            ),
        )

        assertEquals(ColorEyedropperLensAnchor.End, result.lensPlacement.anchor)
        assertEquals(IntOffset(x = 148, y = 110), result.lensPlacement.offset)
        assertEquals(IntOffset(x = 128, y = 198), result.islandOffset)
    }

    @Test
    fun defaultCloudIsConfiguredSmallerThanLens() {
        val metrics = ColorEyedropperDefaults.metrics

        assertEquals(true, metrics.islandMinWidth < metrics.lensDiameter)
    }
}
