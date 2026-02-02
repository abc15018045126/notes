package io.github.abc15018045126.sora.widget

import android.widget.OverScroller
import androidx.annotation.NonNull

class EditorScroller(@NonNull private val editor: CodeEditor) {

    private val scroller: OverScroller = OverScroller(editor.context)

    fun setEditorOffsets() {
        editor.scrollX = scroller.currX
        editor.scrollY = scroller.currY
    }

    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        startScroll(startX, startY, dx, dy, editor.props!!.scrollAnimationDurationMs)

    }

    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        scroller.startScroll(startX, startY, dx, dy, duration)
        setEditorOffsets()
    }

    fun forceFinished(finished: Boolean) {
        scroller.forceFinished(finished)
        setEditorOffsets()
    }

    fun abortAnimation() {
        scroller.abortAnimation()
        setEditorOffsets()
    }

    val isFinished: Boolean
        get() = scroller.isFinished

    fun getCurrX(): Int {
        return scroller.currX
    }

    fun getCurrY(): Int {
        return scroller.currY
    }

    fun getFinalX(): Int {
        return scroller.finalX
    }

    fun getFinalY(): Int {
        return scroller.finalY
    }

    fun getStartX(): Int {
        return scroller.startX
    }

    fun getStartY(): Int {
        return scroller.startY
    }

    fun getCurrVelocity(): Float {
        return scroller.currVelocity
    }

    fun computeScrollOffset(): Boolean {
        val computed = scroller.computeScrollOffset()
        if (computed) {
            setEditorOffsets()
        }
        return computed
    }

    fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int, overX: Int, overY: Int
    ) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY)
        setEditorOffsets()
    }

    fun isOverScrolled(): Boolean {
        return scroller.isOverScrolled
    }

    fun getImplScroller(): OverScroller {
        return scroller
    }
}
