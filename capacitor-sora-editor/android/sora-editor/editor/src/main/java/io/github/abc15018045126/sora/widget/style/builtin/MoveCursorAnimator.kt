package io.github.abc15018045126.sora.widget.style.builtin

import android.animation.ValueAnimator
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.style.CursorAnimator

import io.github.abc15018045126.sora.widget.layout.Layout

/**
 * Default cursor animation implementation
 *
 * @author abc15018045126
 */
class MoveCursorAnimator(private val editor: CodeEditor) : CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 120
    private var animatorX: ValueAnimator = ValueAnimator()
    private var animatorY: ValueAnimator = ValueAnimator()
    private var animatorBgBottom: ValueAnimator = ValueAnimator()
    private var animatorBackground: ValueAnimator = ValueAnimator()
    private var startX = 0f
    private var startY = 0f
    private var startSize = 0f
    private var startBottom = 0f
    private var lastAnimateTime: Long = 0

    private fun getHeightOfRows(rowCount: Int): Int {
        return editor.rowHeight * rowCount
    }

    override fun markStartPos() {
        val line = editor.cursor!!.leftLine
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val pos = layout.getCharLayoutOffset(line, editor.cursor!!.leftColumn)
        startX = editor.measureTextRegionOffset() + pos[1]
        startY = pos[0] - minusHeight()
        startSize = getHeightOfRows(layout.getRowCountForLine(line)).toFloat()
        startBottom = layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]
    }

    override fun isRunning(): Boolean {
        return animatorX.isRunning || animatorY.isRunning || animatorBackground.isRunning || animatorBgBottom.isRunning
    }

    override fun cancel() {
        animatorX.cancel()
        animatorY.cancel()
        animatorBackground.cancel()
        animatorBgBottom.cancel()
    }

    private fun minusHeight(): Float {
        return if (editor.props!!.textBackgroundWrapTextOnly) editor.lineSpacingPixels / 2f else 0f
    }

    override fun markEndPos() {
        if (!editor.isCursorAnimationEnabled) {
            return
        }
        if (isRunning()) {
            startX = animatedX()
            startY = animatedY()
            startSize = animatorBackground.animatedValue as Float
            startBottom = animatorBgBottom.animatedValue as Float
            cancel()
        }
        if (System.currentTimeMillis() - lastAnimateTime < 100) {
            return
        }
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val line = editor.cursor!!.leftLine
        animatorX.removeAllUpdateListeners()
        val pos = layout.getCharLayoutOffset(editor.cursor!!.leftLine, editor.cursor!!.leftColumn)

        animatorX = ValueAnimator.ofFloat(startX, pos[1] + editor.measureTextRegionOffset())
        animatorY = ValueAnimator.ofFloat(startY, pos[0] - minusHeight())

        animatorBackground = ValueAnimator.ofFloat(
            startSize,
            getHeightOfRows(layout.getRowCountForLine(editor.cursor!!.leftLine)).toFloat()
        )
        animatorBgBottom = ValueAnimator.ofFloat(
            startBottom,
            layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]
        )

        animatorX.addUpdateListener(this)

        animatorX.duration = duration
        animatorY.duration = duration
        animatorBackground.duration = duration
        animatorBgBottom.duration = duration
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        animatorX.start()
        animatorY.start()
        animatorBackground.start()
        animatorBgBottom.start()

        lastAnimateTime = System.currentTimeMillis()
    }

    override fun animatedX(): Float {
        return animatorX.animatedValue as Float
    }

    override fun animatedY(): Float {
        return animatorY.animatedValue as Float
    }

    override fun animatedLineHeight(): Float {
        return animatorBackground.animatedValue as Float
    }

    override fun animatedLineBottom(): Float {
        return animatorBgBottom.animatedValue as Float
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        editor.postInvalidateOnAnimation()
    }
}
