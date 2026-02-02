package io.github.abc15018045126.sora.event

import android.view.InputDevice
import android.view.MotionEvent
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.REGION_DIVIDER
import io.github.abc15018045126.sora.widget.REGION_DIVIDER_MARGIN
import io.github.abc15018045126.sora.widget.REGION_LINE_NUMBER
import io.github.abc15018045126.sora.widget.REGION_OUTBOUND
import io.github.abc15018045126.sora.widget.REGION_SIDE_ICON
import io.github.abc15018045126.sora.widget.REGION_TEXT
import io.github.abc15018045126.sora.widget.IN_BOUND as WIDGET_IN_BOUND
import io.github.abc15018045126.sora.widget.OUT_BOUND as WIDGET_OUT_BOUND

/**
 * Base class for click events
 *
 * @author abc15018045126
 * @see ClickEvent
 * @see DoubleClickEvent
 * @see LongPressEvent
 * @see ContextClickEvent
 * @see HoverEvent
 */
abstract class EditorMotionEvent(
    editor: CodeEditor,
    private val pos: CharPosition,
    private val event: MotionEvent,
    val span: Span?,
    val spanRange: TextRange?,
    val motionRegion: Int,
    val motionBound: Int
) : Event(editor) {

    companion object {
        /**
         * Motion occurred outside of editor.
         */
        const val REGION_OUTBOUND = io.github.abc15018045126.sora.widget.REGION_OUTBOUND

        /**
         * Motion occurred in line number region.
         */
        const val REGION_LINE_NUMBER = io.github.abc15018045126.sora.widget.REGION_LINE_NUMBER

        /**
         * Motion occurred in side icon region.
         */
        const val REGION_SIDE_ICON = io.github.abc15018045126.sora.widget.REGION_SIDE_ICON

        /**
         * Motion occurred in divider margin region.
         */
        const val REGION_DIVIDER_MARGIN = io.github.abc15018045126.sora.widget.REGION_DIVIDER_MARGIN

        /**
         * Motion occurred in line divider region.
         */
        const val REGION_DIVIDER = io.github.abc15018045126.sora.widget.REGION_DIVIDER

        /**
         * Motion occurred in text region.
         */
        const val REGION_TEXT = io.github.abc15018045126.sora.widget.REGION_TEXT

        /**
         * Motion occurred in editor bounds on the Y-axis.
         */
        const val IN_BOUND = io.github.abc15018045126.sora.widget.IN_BOUND

        /**
         * Motion occurred outside of editor bounds on the Y-axis.
         */
        const val OUT_BOUND = io.github.abc15018045126.sora.widget.OUT_BOUND
    }

    override fun canIntercept(): Boolean {
        return true
    }

    val isFromMouse: Boolean
        get() = event.isFromSource(InputDevice.SOURCE_MOUSE)

    val line: Int
        get() = pos.line

    val column: Int
        get() = pos.column

    val index: Int
        get() = pos.index

    fun getCharPosition(): CharPosition {
        return pos.fromThis()
    }

    val x: Float
        get() = event.x

    val y: Float
        get() = event.y

    /**
     * Get original event object from Android framework
     */
    val causingEvent: MotionEvent
        get() = event
}
