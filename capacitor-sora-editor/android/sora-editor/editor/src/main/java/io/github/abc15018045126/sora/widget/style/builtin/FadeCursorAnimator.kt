package io.github.abc15018045126.sora.widget.style.builtin

import android.animation.Animator
import android.animation.ValueAnimator
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.style.CursorAnimator

import io.github.abc15018045126.sora.widget.layout.Layout

/**
 * Fade-in/Fade-out cursor animation
 *
 * @author Dmitry Rubtsov
 */
class FadeCursorAnimator(private val editor: CodeEditor) : CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 200

    private var fadeInAnimator: ValueAnimator = ValueAnimator()
    private var fadeOutAnimator: ValueAnimator = ValueAnimator()

    private var phaseEnded = false
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
        return fadeInAnimator.isRunning || fadeOutAnimator.isRunning
    }

    override fun cancel() {
        fadeOutAnimator.cancel()
        fadeInAnimator.cancel()
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
        fadeOutAnimator.removeAllUpdateListeners()
        fadeInAnimator.removeAllUpdateListeners()

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

        fadeOutAnimator = ValueAnimator.ofInt(255, 0)
        fadeOutAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationCancel(animator: Animator) {
            }

            override fun onAnimationRepeat(animator: Animator) {
            }

            override fun onAnimationStart(animator: Animator) {
                phaseEnded = false
            }

            override fun onAnimationEnd(animator: Animator) {
                phaseEnded = true
            }
        })
        fadeOutAnimator.addUpdateListener(this)
        fadeOutAnimator.duration = duration

        fadeInAnimator = ValueAnimator.ofInt(0, 255)
        fadeInAnimator.addUpdateListener(this)
        fadeInAnimator.startDelay = duration
        fadeInAnimator.duration = duration
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        fadeOutAnimator.start()
        fadeInAnimator.start()
        lastAnimateTime = System.currentTimeMillis()
    }

    override fun animatedX(): Float {
        if (phaseEnded || editor.insertHandleDescriptor?.position?.isEmpty == true) {
            return endX
        }
        return startX
    }


    override fun animatedY(): Float {
        if (phaseEnded || editor.insertHandleDescriptor?.position?.isEmpty == true) {
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
        editor.handleStyle?.setAlpha(animation.animatedValue as Int)
        editor.postInvalidateOnAnimation()
    }

}
