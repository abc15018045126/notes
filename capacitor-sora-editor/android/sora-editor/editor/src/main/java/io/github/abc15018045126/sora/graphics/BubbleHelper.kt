package io.github.abc15018045126.sora.graphics

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sqrt

/**
 * Helper class for building a bubble rect
 *
 * @author abc15018045126
 */
object BubbleHelper {

    private val tempMatrix = Matrix()

    /**
     * Build a bubble into the given Path object. Old content in given Path is cleared.
     * @param path target Path object
     * @param bounds The bounds for the bubble
     */
    @JvmStatic
    fun buildBubblePath(path: Path, bounds: RectF) {
        path.reset()

        var width = bounds.width()
        val height = bounds.height()
        val r = height / 2
        val sqrt2 = sqrt(2.0).toFloat()
        // Ensure we are convex.
        width = maxOf(r + sqrt2 * r, width)
        pathArcTo(path, r, r, r, 90f, 180f)
        val o1X = width - sqrt2 * r
        pathArcTo(path, o1X, r, r, -90f, 45f)
        val r2 = r / 5
        val o2X = width - sqrt2 * r2
        pathArcTo(path, o2X, r, r2, -45f, 90f)
        pathArcTo(path, o1X, r, r, 45f, 45f)
        path.close()

        tempMatrix.reset()
        tempMatrix.postTranslate(bounds.left, bounds.top)
        path.transform(tempMatrix)
    }

    private fun pathArcTo(
        path: Path,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        path.arcTo(
            centerX - radius, centerY - radius, centerX + radius, centerY + radius,
            startAngle, sweepAngle, false
        )
    }
}
