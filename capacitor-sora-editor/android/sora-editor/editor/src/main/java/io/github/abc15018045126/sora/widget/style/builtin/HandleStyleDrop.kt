package io.github.abc15018045126.sora.widget.style.builtin

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.Companion.ALIGN_CENTER

open class HandleStyleDrop(context: Context) : SelectionHandleStyle {

    private val drawable: Drawable = context.getDrawable(R.drawable.ic_sora_handle_drop)!!.mutate()
    private val width: Int
    private val height: Int
    private var lastColor = 0

    private var alpha = 255
    private var scaleFactor = 1.0f

    init {
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f,
            context.resources.displayMetrics
        ).toInt()
        height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            30f,
            context.resources.displayMetrics
        ).toInt()
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
        if (lastColor != color) {
            lastColor = color
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        val left = (x - (width * scaleFactor) / 2).toInt()
        val top = y.toInt()
        val right = (x + (width * scaleFactor) / 2).toInt()
        val bottom = (y + height * scaleFactor).toInt()
        drawable.setBounds(left, top, right, bottom)
        drawable.alpha = alpha
        drawable.draw(canvas)
        descriptor.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), ALIGN_CENTER)
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
    }

    override fun setScale(factor: Float) {
        this.scaleFactor = factor
    }
}
