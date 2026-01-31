package io.github.abc15018045126.sora.widget.layout

import android.view.View
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.SingleCharacterWidths
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.MutableInt
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ViewMeasureHelper {
    /**
     * Get desired view size for the given arguments
     */
    @JvmStatic
    fun getDesiredSize(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        gutterSize: Float,
        rowHeight: Float,
        wordwrap: Boolean,
        tabSize: Int,
        text: Content,
        paint: Paint
    ): Long {
        var widthMS = widthMeasureSpec
        var heightMS = heightMeasureSpec
        val widthMode = View.MeasureSpec.getMode(widthMS)
        val heightMode = View.MeasureSpec.getMode(heightMS)
        val maxSize = 0X3FFFFFFF
        val maxWidth: Int = if (widthMode == View.MeasureSpec.UNSPECIFIED) {
            maxSize
        } else {
            View.MeasureSpec.getSize(widthMS)
        }
        val maxHeight: Int = if (heightMode == View.MeasureSpec.UNSPECIFIED) {
            maxSize
        } else {
            View.MeasureSpec.getSize(heightMS)
        }
        val measurer = SingleCharacterWidths(tabSize)
        if (wordwrap) {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                val lines = if (heightMode != View.MeasureSpec.EXACTLY) IntArray(text.lineCount) else null
                val lineMaxSize = MutableInt(0)
                text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { index, line, _ ->
                    val measured = ceil(
                        measurer.measureText(
                            line.backingCharArray,
                            0,
                            line.length,
                            paint
                        ).toDouble()
                    ).toInt()
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured
                    }
                    if (lines != null) {
                        lines[index] = measured
                    }
                })
                val width = min(maxWidth.toDouble(), (lineMaxSize.value + gutterSize).toDouble())
                    .toInt()
                widthMS = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                if (lines != null) {
                    val rowCount = MutableInt(0)
                    val availableSize = (width - gutterSize).toInt()
                    if (availableSize <= 0) {
                        rowCount.value = text.length
                    } else {
                        for (i in lines.indices) {
                            rowCount.value += max(
                                1.0,
                                ceil(1.0 * lines[i] / availableSize)
                            ).toInt()
                        }
                    }
                    val height = min((rowHeight * rowCount.value).toInt(), maxHeight)
                    heightMS = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                }
            } else {
                if (heightMode != View.MeasureSpec.EXACTLY) {
                    val rowCount = MutableInt(0)
                    val availableSize = (maxWidth - gutterSize).toInt()
                    if (availableSize <= 0) {
                        rowCount.value = text.length
                    } else {
                        text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { _, line, _ ->
                            val measured = ceil(
                                measurer.measureText(
                                    line.backingCharArray,
                                    0,
                                    line.length,
                                    paint
                                ).toDouble()
                            ).toInt()
                            rowCount.value += max(
                                1.0,
                                ceil(1.0 * measured / availableSize)
                            ).toInt()
                        })
                    }
                    val height = min((rowHeight * rowCount.value).toInt(), maxHeight)
                    heightMS = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                }
            }
        } else {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                val lineMaxSize = MutableInt(0)
                text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { _, line, _ ->
                    val measured = ceil(
                        measurer.measureText(
                            line.backingCharArray,
                            0,
                            line.length,
                            paint
                        ).toDouble()
                    ).toInt()
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured
                    }
                })
                val width = min((lineMaxSize.value + gutterSize).toInt(), maxWidth)
                widthMS = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            }
            if (heightMode != View.MeasureSpec.EXACTLY) {
                val height = min(maxHeight, (rowHeight * text.lineCount).toInt())
                heightMS = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            }
        }
        return IntPair.pack(widthMS, heightMS)
    }
}
