package io.github.abc15018045126.sora.graphics

import android.graphics.RectF

object RectUtils {

    @JvmStatic
    fun contains(rect: RectF, x: Float, y: Float, extraXSpace: Float): Boolean {
        return (x >= rect.left - extraXSpace && x <= rect.right + extraXSpace && y >= rect.top && y <= rect.bottom)
    }

    @JvmStatic
    fun almostContains(rect: RectF, x: Float, y: Float, extraSpace: Float): Boolean {
        return (x >= rect.left - extraSpace && x <= rect.right + extraSpace && y >= rect.top - extraSpace && y <= rect.bottom + extraSpace)
    }
}
