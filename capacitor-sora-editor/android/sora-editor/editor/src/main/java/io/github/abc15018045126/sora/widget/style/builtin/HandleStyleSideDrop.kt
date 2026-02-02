package io.github.abc15018045126.sora.widget.style.builtin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.ALIGN_LEFT
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.ALIGN_RIGHT
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.HANDLE_TYPE_INSERT
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.HANDLE_TYPE_LEFT
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.HANDLE_TYPE_UNDEFINED

open class HandleStyleSideDrop(context: Context) : HandleStyleDrop(context) {

    private val size: Int
    private val paint: Paint

    init {
        size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            22f,
            context.resources.displayMetrics
        ).toInt()
        paint = Paint()
        paint.isAntiAlias = true
    }

    override fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: SelectionHandleStyle.HandleDescriptor
    ) {
        val radius = size / 2f
        paint.color = color
        if (handleType == HANDLE_TYPE_INSERT || handleType == HANDLE_TYPE_UNDEFINED) {
            super.draw(canvas, handleType, x, y, rowHeight, color, descriptor)
        } else {
            val type = handleType == HANDLE_TYPE_LEFT
            val cx = if (type) x - radius else x + radius
            canvas.drawCircle(cx, y + radius, radius, paint)
            canvas.drawRect(
                if (type) cx else cx - radius,
                y,
                if (type) cx + radius else cx,
                y + radius,
                paint
            )
            descriptor.set(
                cx - radius,
                y,
                cx + radius,
                y + 2 * radius,
                if (type) ALIGN_LEFT else ALIGN_RIGHT
            )
        }
    }
}
