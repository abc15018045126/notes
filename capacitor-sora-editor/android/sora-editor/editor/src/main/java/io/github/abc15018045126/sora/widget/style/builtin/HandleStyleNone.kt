package io.github.abc15018045126.sora.widget.style.builtin

import android.graphics.Canvas
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle

/**
 * A handle style that draws nothing.
 */
class HandleStyleNone : SelectionHandleStyle {

    override fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: SelectionHandleStyle.HandleDescriptor
    ) {
        descriptor.setEmpty()
    }

    override fun setAlpha(alpha: Int) {
        // Ignored
    }

    override fun setScale(factor: Float) {
        // Ignored
    }
}
