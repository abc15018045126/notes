package io.github.abc15018045126.sora.widget.style

import android.graphics.Canvas
import android.graphics.RectF
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * Class for custom handle style
 *
 * @author abc15018045126
 */
interface SelectionHandleStyle {

    /**
     * Draw a handle to the given canvas and return descriptor of handle.
     *
     * @param canvas     Canvas to draw
     * @param handleType Type of handle being drawn at this position. Value can be [HANDLE_TYPE_INSERT], [HANDLE_TYPE_LEFT], [HANDLE_TYPE_RIGHT] or [HANDLE_TYPE_UNDEFINED]
     * @param x          The x of text position on canvas
     * @param y          The y of row bottom position on canvas
     * @param rowHeight  The height of a single row
     * @param color      The color of handle configured in [EditorColorScheme]
     * @param descriptor The descriptor that should be adjusted
     */
    fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: HandleDescriptor
    )

    fun setAlpha(alpha: Int)

    fun setScale(factor: Float)

    /**
     * The descriptor of a drawn handle on canvas
     */
    class HandleDescriptor {

        /**
         * The position of handle
         */
        @JvmField
        val position = RectF()

        /**
         * The alignment of the handle (of the x coordinate)
         * For example, you can draw handle with align right of the x when you draw the left handle
         *
         * @see ALIGN_CENTER
         * @see ALIGN_LEFT
         * @see ALIGN_RIGHT
         */
        @JvmField
        var alignment = ALIGN_CENTER

        fun set(left: Float, top: Float, right: Float, bottom: Float, alignment: Int) {
            this.alignment = alignment
            position.set(left, top, right, bottom)
        }

        fun setEmpty() {
            position.setEmpty()
            this.alignment = ALIGN_CENTER
        }
    }

    companion object {
        const val HANDLE_TYPE_UNDEFINED = -1
        const val HANDLE_TYPE_INSERT = 0
        const val HANDLE_TYPE_LEFT = 1
        const val HANDLE_TYPE_RIGHT = 2

        const val ALIGN_CENTER = 0
        const val ALIGN_LEFT = 1
        const val ALIGN_RIGHT = 2
    }
}
