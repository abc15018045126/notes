package io.github.abc15018045126.sora.graphics

import android.graphics.Canvas
import android.graphics.Paint

class BufferedDrawPoints {

    private var pointCount: Int = 0
    private var points: FloatArray = FloatArray(128)
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    fun drawPoint(cx: Float, cy: Float) {
        // Check buffer size and grow
        if (points.size < (pointCount + 1) * 2) {
            val newBuffer = FloatArray(points.size shl 1)
            System.arraycopy(points, 0, newBuffer, 0, pointCount * 2)
            points = newBuffer
        }
        points[pointCount * 2] = cx + offsetX
        points[pointCount * 2 + 1] = cy + offsetY
        pointCount++
    }

    fun setOffsets(x: Float, y: Float) {
        this.offsetX = x
        this.offsetY = y
    }

    fun commitPoints(canvas: Canvas, paint: Paint) {
        if (pointCount == 0) {
            return
        }
        canvas.drawPoints(points, 0, pointCount * 2, paint)
        pointCount = 0
    }
}
