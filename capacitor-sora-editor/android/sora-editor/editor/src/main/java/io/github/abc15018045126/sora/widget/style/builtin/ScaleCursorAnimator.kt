package io.github.abc15018045126.sora.widget.style.builtin

import android.animation.ValueAnimator
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.style.CursorAnimator

import io.github.abc15018045126.sora.widget.layout.Layout

/**
 * Scale-Up/Scale-Down cursor animation
 *
 * @author Dmitry Rubtsov
 */
class ScaleCursorAnimator(private val editor: CodeEditor) : CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 180

    private var scaleAnimator: ValueAnimator = ValueAnimator()
    private var lastAnimateTime: Long = 0
    private var lineHeight = 0f
    private var lineBottom = 0f
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    override fun markStartPos() {
        val line = editor.cursor!!.leftLine
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        lineHeight = (layout.getRowCountForLine(line) * editor.rowHeight).toFloat()
        lineBottom = layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]

        val pos = layout.getCharLayoutOffset(
            editor.cursor!!.leftLine,
            editor.cursor!!.leftColumn
        )
        startX = pos[1] + editor.measureTextRegionOffset()
        startY = pos[0]
    }


    override fun isRunning(): Boolean {
        return scaleAnimator.isRunning
    }

    override fun cancel() {
        scaleAnimator.cancel()
    }

    override fun markEndPos() {
        if (!editor.isCursorAnimationEnabled) {
            return
        }
        if (isRunning()) {
            cancel()
        }
        if (System.currentTimeMillis() - lastAnimateTime < 100) {
            return
        }
        scaleAnimator.removeAllUpdateListeners()

        val line = editor.cursor!!.leftLine
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        lineHeight = (layout.getRowCountForLine(line) * editor.rowHeight).toFloat()
        lineBottom = layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]

        val pos = layout.getCharLayoutOffset(
            editor.cursor!!.leftLine,
            editor.cursor!!.leftColumn
        )
        endX = pos[1] + editor.measureTextRegionOffset()
        endY = pos[0]


        if (editor.insertHandleDescriptor?.position?.isEmpty == true) {
            scaleAnimator = ValueAnimator.ofFloat(0f, 1.0f)
            scaleAnimator.duration = duration
        } else {
            scaleAnimator = ValueAnimator.ofFloat(1.0f, 0f, 1.0f)
            scaleAnimator.duration = duration * 2
        }
        scaleAnimator.addUpdateListener(this)
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100 || editor.insertHandleDescriptor?.position?.isEmpty == true) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        if (startX == endX && startY == endY && editor.insertHandleDescriptor?.position?.isEmpty == false) {
            return
        }
        scaleAnimator.start()
        lastAnimateTime = System.currentTimeMillis()
    }

    private fun shouldReturnEndValue(): Boolean {
        if (!scaleAnimator.isRunning || editor.insertHandleDescriptor?.position?.isEmpty == true) {
            return true
        }
        return if (scaleAnimator.duration == duration) {
            true
        } else {
            scaleAnimator.currentPlayTime > duration
        }
    }

    override fun animatedX(): Float {
        if (shouldReturnEndValue()) {
            return endX
        }
        return startX
    }

    override fun animatedY(): Float {
        if (shouldReturnEndValue()) {
            return endY
        }
        return startY
    }

    override fun animatedLineHeight(): Float {
        return lineHeight
    }

    override fun animatedLineBottom(): Float {
        return lineBottom
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        editor.handleStyle?.setScale(animation.animatedValue as Float)
        editor.postInvalidateOnAnimation()
    }
}
