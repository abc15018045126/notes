package io.github.abc15018045126.sora.widget

import android.content.res.Resources
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.graphics.RectUtils
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.StylesUtils
import io.github.abc15018045126.sora.lang.styling.line.LineSideIcon
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.Numbers
import io.github.abc15018045126.sora.widget.component.Magnifier
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle
import java.util.Objects
import kotlin.jvm.functions.Function7
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun interface EditorMotionEventConstructor {
    fun create(
        editor: CodeEditor,
        pos: CharPosition,
        event: MotionEvent,
        span: Span?,
        spanRange: TextRange?,
        motionRegion: Int,
        motionBound: Int
    ): EditorMotionEvent
}

/**
 * Handles touch events of editor
 *
 * @author abc15018045126
 */
class EditorTouchEventHandler(@NonNull private val editor: CodeEditor) :
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
    ScaleGestureDetector.OnScaleGestureListener {

    private val scroller: EditorScroller = EditorScroller(editor)
    private val insertHandle: SelectionHandle = SelectionHandle(BOTH)
    @JvmField
    internal var editorMagnifier: Magnifier = Magnifier(editor)
    @JvmField
    internal var selHandleType: Int = -1
    @JvmField
    internal var selHandleMoving: Boolean = false
    @JvmField
    internal var motionX: Float = 0f
    @JvmField
    internal var motionY: Float = 0f
    @JvmField
    internal var glowTopOrBottom: Boolean = false //true for bottom
    @JvmField
    internal var glowLeftOrRight: Boolean = false //true for right
    @JvmField
    @get:JvmName("isScaling")
    var isScaling: Boolean = false
    @JvmField
    internal var scaleMaxSize: Float
    @JvmField
    internal var scaleMinSize: Float
    private var textSizeStart: Float = 0f
    private var timeLastScroll: Long = 0
    private var timeLastSetSelection: Long = 0
    private var holdingScrollbarVertical: Boolean = false
    private var holdingScrollbarHorizontal: Boolean = false
    private var thumbDownY: Float = 0f
    private var thumbDownX: Float = 0f
    private var leftHandle: SelectionHandle = SelectionHandle(LEFT)
    private var rightHandle: SelectionHandle = SelectionHandle(RIGHT)
    private var edgeFieldSize: Float
    private var edgeFlags: Int = 0
    private val touchSlop: Int
    private var thumbMotionRecord: MotionEvent? = null
    private var mouseDownX: Float = 0f
    private var mouseDownY: Float = 0f
    private var mouseDownButtonState: Int = 0
    private var lastTimeMousePrimaryClickUp: Long = 0
    private var mouseDoubleClick: Boolean = false
    internal var lastContextClickPosition: PointF? = null
    @JvmField
    internal var mouseClick: Boolean = false
    @JvmField
    internal var mouseCanMoveText: Boolean = false
    @JvmField
    internal var draggingSelection: CharPosition? = null

    /* dragging selection fields */
    private var dragSelectActive: Boolean = false
    private var dragSelectStarted: Boolean = false
    private var dragSelectInitialCharIndex: Int = -1
    private var dragSelectInitialLeftIndex: Int = -1
    private var dragSelectInitialRightIndex: Int = -1
    private var dragSelectLastDragIndex: Int = -1

    init {
        edgeFieldSize = editor.dpUnit * 18
        scaleMaxSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            26f,
            Resources.getSystem().displayMetrics
        )
        scaleMinSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            8f,
            Resources.getSystem().displayMetrics
        )
        val config = ViewConfiguration.get(editor.context)
        touchSlop = config.scaledTouchSlop
    }

    fun hasAnyHeldHandle(): Boolean {
        return selHandleType != -1
    }

    fun isHandleMoving(): Boolean {
        return selHandleMoving
    }

    /**
     * Whether we should draw scroll bars
     *
     * @return whether draw scroll bars
     */
    fun shouldDrawScrollBarForTouch(): Boolean {
        return System.currentTimeMillis() - timeLastScroll < EditorTouchEventHandler.HIDE_DELAY + EditorTouchEventHandler.SCROLLBAR_FADE_ANIMATION_TIME || holdingScrollbarVertical || holdingScrollbarHorizontal
    }

    fun getScrollBarFadeOutPercentageForTouch(): Float {
        val now = System.currentTimeMillis()
        if (now - timeLastScroll < EditorTouchEventHandler.HIDE_DELAY || holdingScrollbarVertical || holdingScrollbarHorizontal) {
            return 0f
        } else if (now - timeLastScroll >= EditorTouchEventHandler.HIDE_DELAY && now - timeLastScroll < EditorTouchEventHandler.HIDE_DELAY + EditorTouchEventHandler.SCROLLBAR_FADE_ANIMATION_TIME) {
            editor.postInvalidateOnAnimation()
            return (now - timeLastScroll - EditorTouchEventHandler.HIDE_DELAY).toFloat() / EditorTouchEventHandler.SCROLLBAR_FADE_ANIMATION_TIME
        }
        return 1f
    }

    /**
     * Hide the insert handle at once
     */
    fun hideInsertHandle() {
        if (!shouldDrawInsertHandle()) {
            return
        }
        timeLastSetSelection = 0
        editor.invalidate()
    }

    /**
     * Whether the vertical scroll bar is touched
     *
     * @return Whether touched
     */
    fun holdVerticalScrollBar(): Boolean {
        return holdingScrollbarVertical
    }

    /**
     * Whether the horizontal scroll bar is touched
     *
     * @return Whether touched
     */
    fun holdHorizontalScrollBar(): Boolean {
        return holdingScrollbarHorizontal
    }

    /**
     * Whether insert handle is touched
     *
     * @return Whether touched
     */
    fun holdInsertHandle(): Boolean {
        return selHandleType == BOTH
    }

    /**
     * Whether the editor should draw insert handler
     *
     * @return Whether to draw
     */
    fun shouldDrawInsertHandle(): Boolean {
        return System.currentTimeMillis() - timeLastSetSelection < HIDE_DELAY_HANDLE || holdInsertHandle()
    }

    /**
     * Notify the editor later to hide scroll bars
     */
    fun notifyScrolled() {
        timeLastScroll = System.currentTimeMillis()
        val scrollNotifier = Runnable {
            if (System.currentTimeMillis() - timeLastScroll >= HIDE_DELAY) {
                editor.invalidate()
            }
        }
        io.github.abc15018045126.sora.util.EditorHandler.postDelayed(scrollNotifier, HIDE_DELAY.toLong())
    }



    /**
     * Notify the editor later to hide insert handle
     */
    fun notifyLater() {
        timeLastSetSelection = System.currentTimeMillis()
        val invalidateNotifier = Runnable {
            if (System.currentTimeMillis() - timeLastSetSelection >= HIDE_DELAY_HANDLE) {
                editor.invalidate()
            }
        }
        io.github.abc15018045126.sora.util.EditorHandler.postDelayed({
            if (editor.isReleased) return@postDelayed
            invalidateNotifier.run()
        }, HIDE_DELAY_HANDLE.toLong())
    }


    /**
     * Called by editor
     * Whether this class is handling motions by user
     *
     * @return Whether handling
     */
    fun handlingMotions(): Boolean {
        return holdHorizontalScrollBar() || holdVerticalScrollBar() || hasAnyHeldHandle()
    }

    /**
     * Get scroller for editor
     *
     * @return Scroller using
     */
    fun getScroller(): EditorScroller {
        return scroller
    }

    /**
     * Reset states of handler
     */
    fun reset() {
        scroller.startScroll(0, 0, 0, 0, 0)
        reset2()
    }

    /**
     * Reset states of handler, except scrolling state
     */
    fun reset2() {
        holdingScrollbarHorizontal = false
        holdingScrollbarVertical = false
        selHandleType = -1
        finishDragSelect()
        dismissMagnifier()
    }

    private fun getHandleDescriptorByType(type: Int): SelectionHandleStyle.HandleDescriptor? {
        return when (type) {
            BOTH -> editor.insertHandleDescriptor
            LEFT -> editor.leftHandleDescriptor
            RIGHT -> editor.rightHandleDescriptor
            else -> null
        }
    }

    fun updateMagnifier(e: MotionEvent) {
        if (edgeFlags != 0 || !hasAnyHeldHandle() || !editorMagnifier.isEnabled) {
            dismissMagnifier()
            return
        }
        // A handle is already held
        val desc = getHandleDescriptorByType(selHandleType) ?: return
        val pos = desc.position

        val height = pos.height()
        val x: Int
        val y: Int
        if (editor.isStickyTextSelection) {
            x = min(e.x.toInt(), pos.right.toInt())
            y = (pos.top - height / 2).toInt()
        } else {
            x = e.x.toInt()
            y = (e.y - height / 2 - editor.rowHeight).toInt()
        }
        editorMagnifier.show(x, y)
    }

    fun dismissMagnifier() {
        editorMagnifier.dismiss()
    }

    private fun beginDragSelect(line: Int, column: Int) {
        if (!editor.props!!.dragSelectAfterLongPress) {

            return
        }
        val text = editor.text
        dragSelectInitialCharIndex = text.getCharIndex(line, column)
        val cursor = editor.cursor!!

        dragSelectInitialLeftIndex = text.getCharIndex(cursor.leftLine, cursor.leftColumn)
        dragSelectInitialRightIndex = text.getCharIndex(cursor.rightLine, cursor.rightColumn)
        dragSelectLastDragIndex = dragSelectInitialCharIndex
        dragSelectActive = true
        dragSelectStarted = false
    }


    fun isDragSelecting(): Boolean {
        return dragSelectActive
    }

    private fun updateDragSelectMagnifier(e: MotionEvent) {
        if (!editor.props!!.dragSelectAfterLongPress ||

            edgeFlags != 0 || !editorMagnifier.isEnabled || !dragSelectStarted
        ) {
            dismissMagnifier()
            return
        }
        if (!editorMagnifier.isShowing()) {
            val dx = e.x - thumbDownX
            val dy = e.y - thumbDownY
            if (sqrt((dx * dx + dy * dy).toDouble()) < EditorTouchEventHandler.MAGNIFIER_TOUCH_SLOP) {
                return
            }
        }
        val x = e.x.toInt()
        val y = (e.y - editor.rowHeight).toInt()
        editorMagnifier.show(x, y)
    }

    private fun handleDragSelect(e: MotionEvent, fromEdgeScroll: Boolean): Boolean {
        if (!editor.props!!.dragSelectAfterLongPress || !dragSelectActive) {

            return false
        }
        val text = editor.text
        if (text.length == 0) {
            return true
        }
        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        val currentIndex = text.getCharIndex(line, column)
        if (!dragSelectStarted) {
            if (currentIndex == dragSelectInitialCharIndex) {
                if (!fromEdgeScroll)
                    scrollIfThumbReachesEdge(e)
                return true
            }
            dragSelectStarted = true
        }
        if (currentIndex == dragSelectLastDragIndex) {
            updateDragSelectMagnifier(e)
            if (!fromEdgeScroll)
                scrollIfThumbReachesEdge(e)
            return true
        }
        var anchorIndex = if (currentIndex <= dragSelectInitialCharIndex) dragSelectInitialRightIndex else dragSelectInitialLeftIndex
        anchorIndex = Numbers.coerceIn(anchorIndex, 0, text.length)
        val startIndex = min(anchorIndex, currentIndex)
        val endIndex = max(anchorIndex, currentIndex)
        val indexer = text.indexer
        if (startIndex == endIndex) {
            val pos = indexer.getCharPosition(startIndex)
            editor.setSelection(pos.line, pos.column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
        } else {
            val startPos = indexer.getCharPosition(startIndex)
            val endPos = indexer.getCharPosition(endIndex)
            editor.setSelectionRegion(startPos.line, startPos.column, endPos.line, endPos.column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
        }
        dragSelectLastDragIndex = currentIndex
        updateDragSelectMagnifier(e)
        if (!fromEdgeScroll)
            scrollIfThumbReachesEdge(e)
        return true
    }

    private fun finishDragSelect() {
        val startedBefore = dragSelectStarted
        dragSelectActive = false
        dragSelectStarted = false
        dragSelectInitialCharIndex = -1
        dragSelectInitialLeftIndex = -1
        dragSelectInitialRightIndex = -1
        dragSelectLastDragIndex = -1
        if (startedBefore) {
            editor.dispatchEvent(DragSelectStopEvent(editor))
        }
    }

    /**
     * Handle events apart from detectors
     *
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        motionY = e.y
        motionX = e.x
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                finishDragSelect()
                thumbDownY = e.y
                thumbDownX = e.x
                holdingScrollbarHorizontal = false
                holdingScrollbarVertical = false
                var rect = editor.renderer.verticalScrollBarRect
                if (RectUtils.contains(rect, e.x, e.y, editor.dpUnit * 10)) {
                    holdingScrollbarVertical = true
                }
                rect = editor.renderer.horizontalScrollBarRect
                if (rect.contains(e.x, e.y)) {
                    holdingScrollbarHorizontal = true
                }
                if (holdingScrollbarVertical || holdingScrollbarHorizontal) {
                    if (holdingScrollbarVertical && holdingScrollbarHorizontal) {
                        holdingScrollbarHorizontal = false
                    }
                    editor.invalidate()
                } else {
                    val allowedDistance = editor.dpUnit * 7
                    if (shouldDrawInsertHandle() && RectUtils.almostContains(editor.insertHandleDescriptor!!.position, e.x, e.y, allowedDistance)) {

                        selHandleType = BOTH
                    }
                    val left = RectUtils.almostContains(editor.leftHandleDescriptor!!.position, e.x, e.y, allowedDistance)

                    val right = RectUtils.almostContains(editor.rightHandleDescriptor!!.position, e.x, e.y, allowedDistance)

                    if (left) {
                        selHandleType = LEFT
                    } else if (right) {
                        selHandleType = RIGHT
                    }
                    if (selHandleType != -1) {
                        selHandleMoving = false
                        dispatchHandleStateChange(selHandleType, true)
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (holdingScrollbarVertical) {
                    val movedDis = e.y - thumbDownY
                    thumbDownY = e.y
                    val all = editor.scrollMaxY.toFloat()
                    val dy = movedDis / (editor.height - editor.renderer.verticalScrollBarRect.height()) * all
                    scrollBy(0f, dy)
                    return true
                }
                if (holdingScrollbarHorizontal) {
                    val movedDis = e.x - thumbDownX
                    thumbDownX = e.x
                    val all = (editor.scrollMaxX + editor.width).toFloat()
                    val dx: Float
                    if (editor.renderer.horizontalScrollBarRect.width() <= 60 * editor.dpUnit) {
                        dx = movedDis / (editor.width - editor.renderer.horizontalScrollBarRect.width()) * all
                    } else {
                        dx = movedDis / editor.width * all
                    }
                    scrollBy(dx, 0f)
                    return true
                }
                if (handleDragSelect(e, false)) {
                    return true
                }
                if (!selHandleMoving && (abs(e.x - thumbDownX) > touchSlop || abs(e.y - thumbDownY) > touchSlop)) {
                    selHandleMoving = true
                }
                if (selHandleMoving && handleSelectionChange(e)) {
                    if (editorMagnifier.isShowing() || sqrt(((e.x - thumbDownX) * (e.x - thumbDownX) +
                                (e.y - thumbDownY) * (e.y - thumbDownY)).toDouble()) >= EditorTouchEventHandler.MAGNIFIER_TOUCH_SLOP) {
                        updateMagnifier(e)
                    }
                    editor.invalidate()
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (holdingScrollbarVertical || holdingScrollbarHorizontal) {
                    holdingScrollbarVertical = false
                    holdingScrollbarHorizontal = false
                    timeLastScroll = System.currentTimeMillis()
                    notifyScrolled()
                }
                finishDragSelect()
                if (selHandleType != -1) {
                    dispatchHandleStateChange(selHandleType, false)
                    if (selHandleType == BOTH)
                        notifyLater()
                    selHandleType = -1
                }
                editor.invalidate()
                stopEdgeScroll()
                dismissMagnifier()
            }
        }
        return false
    }

    private fun shouldForwardToTouch(): Boolean {
        return holdingScrollbarHorizontal || holdingScrollbarVertical
    }

    /**
     * Entry for mouse motion events
     */
    fun onMouseEvent(event: MotionEvent): Boolean {
        if (editor.isFormatting) {
            resetMouse()
            return false
        }

        if (shouldForwardToTouch()) {
            return onTouchEvent(event)
        }
        lastContextClickPosition = null
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mouseDownX = event.x
                mouseDownY = event.y
                mouseDownButtonState = event.buttonState
                mouseClick = true
                if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                    if (onTouchEvent(event) && shouldForwardToTouch()) {
                        return true
                    }
                    if (SystemClock.uptimeMillis() - lastTimeMousePrimaryClickUp < ViewConfiguration.getDoubleTapTimeout()) {
                        mouseDoubleClick = true
                        onDoubleTap(event)
                        return true
                    }
                    val pos = editor.getPointPositionOnScreen(mouseDownX, mouseDownY)
                    val line = IntPair.getFirst(pos)
                    val column = IntPair.getSecond(pos)
                    val charPos = editor.text.indexer.getCharPosition(line, column)
                    mouseCanMoveText = if (editor.isTextSelected && editor.cursorRange.isPositionInside(charPos) && editor.isScreenPointOnText(mouseDownX, mouseDownY)) {
                        true
                    } else {
                        editor.setSelection(line, column, SelectionChangeEvent.CAUSE_MOUSE_INPUT)
                        editor.requestFocus()
                        false
                    }
                    draggingSelection = charPos
                    editor.postInvalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mouseDoubleClick) {
                    return true
                }
                if (abs(event.x - mouseDownX) > touchSlop || abs(event.y - mouseDownY) > touchSlop) {
                    mouseClick = false
                }
                if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                    val pos = editor.getPointPositionOnScreen(event.x, event.y)
                    val line = IntPair.getFirst(pos)
                    val column = IntPair.getSecond(pos)
                    val charPos = editor.text.indexer.getCharPosition(line, column)
                    if (!mouseClick && !mouseCanMoveText) {
                        val anchor = editor.selectionAnchor!!
                        editor.setSelectionRegion(anchor.line, anchor.column, line, column, SelectionChangeEvent.CAUSE_MOUSE_INPUT)

                    }
                    draggingSelection = charPos
                    editor.postInvalidate()
                    scrollIfThumbReachesEdge(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (event.eventTime - event.downTime > ViewConfiguration.getTapTimeout() * 2f) {
                    mouseClick = false
                }
                if (!mouseDoubleClick) {
                    if (mouseCanMoveText && !mouseClick && (mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                        val pos = editor.getPointPositionOnScreen(event.x, event.y)
                        val line = IntPair.getFirst(pos)
                        val column = IntPair.getSecond(pos)
                        val dest = editor.text.indexer.getCharPosition(line, column)
                        val curRange = editor.cursorRange
                        if (!curRange.isPositionInside(dest) && (editor.getKeyMetaStates().isCtrlPressed || curRange.end != dest)) {
                            val length = curRange.endIndex - curRange.startIndex
                            val insIndex = if (editor.getKeyMetaStates().isCtrlPressed) dest.index else if (dest.index < curRange.startIndex) dest.index else dest.index - length
                            val text = editor.text
                            val insText = text.substring(curRange.startIndex, curRange.endIndex)
                            val insPos: CharPosition
                            if (editor.getKeyMetaStates().isCtrlPressed) {
                                text.insert(dest.line, dest.column, insText)
                                insPos = dest
                            } else {
                                text.beginBatchEdit()
                                editor.deleteText()
                                insPos = text.indexer.getCharPosition(insIndex)
                                text.insert(insPos.line, insPos.column, insText)
                                text.endBatchEdit()
                            }
                            val endPos = text.indexer.getCharPosition(insIndex + length)
                            editor.setSelectionRegion(insPos.line, insPos.column, endPos.line, endPos.column, SelectionChangeEvent.CAUSE_MOUSE_INPUT)
                        }
                    }
                    if (mouseClick) {
                        if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                            onSingleTapUp(event)
                            lastTimeMousePrimaryClickUp = event.eventTime
                        } else if ((mouseDownButtonState and MotionEvent.BUTTON_SECONDARY) != 0) {
                            onContextClick(event)
                        }
                    }
                }
                resetMouse()
                stopEdgeScroll()
            }
            MotionEvent.ACTION_CANCEL -> {
                resetMouse()
                stopEdgeScroll()
            }
        }
        return true
    }

    /**
     * Reset mouse handling state
     */
    fun resetMouse() {
        mouseDownX = 0f
        mouseDownY = 0f
        mouseClick = false
        mouseCanMoveText = false
        draggingSelection = null
        if (mouseDoubleClick) {
            mouseDoubleClick = false
            lastTimeMousePrimaryClickUp = 0L
        }
    }

    /**
     * Context click
     */
    fun onContextClick(event: MotionEvent) {
        lastContextClickPosition = PointF(event.x, event.y)
        if ((dispatchEditorMotionEvent(::ContextClickEvent, null, event) and InterceptTarget.TARGET_EDITOR) != 0) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            editor.performContextClick(event.x, event.y)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            editor.performContextClick()
        }

        if (editor.props!!.mouseContextMenu) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                editor.showContextMenu(event.x, event.y)
            } else {
                editor.showContextMenu()
            }
        }
    }

    @Nullable
    fun getLastContextClickPosition(): PointF? {
        return lastContextClickPosition
    }

    private fun dispatchHandleStateChange(type: Int, held: Boolean) {
        editor.dispatchEvent(HandleStateChangeEvent(editor, type, held))
    }

    fun dispatchEditorMotionEvent(
        constructor: EditorMotionEventConstructor,
        @Nullable pos: CharPosition?,
        @NonNull event: MotionEvent
    ): Int {
        val region = editor.resolveTouchRegion(event)
        return dispatchEditorMotionEvent(constructor, pos, event, IntPair.getFirst(region), IntPair.getSecond(region))
    }

    fun dispatchEditorMotionEvent(
        constructor: EditorMotionEventConstructor,
        @Nullable pos: CharPosition?,
        @NonNull event: MotionEvent,
        motionRegion: Int,
        motionBound: Int
    ): Int {
        var currentPos = pos
        if (currentPos == null) {
            val pt = editor.getPointPositionOnScreen(event.x, event.y)
            currentPos = editor.text.indexer.getCharPosition(IntPair.getFirst(pt), IntPair.getSecond(pt))
        }
        val styles = editor.styles
        val text = editor.text
        val span = StylesUtils.getSpanForPosition(styles, currentPos)
        val nextSpan = StylesUtils.getFollowingSpanForPosition(styles, currentPos)
        var range: TextRange? = null
        if (span != null) {
            val startPos = text.indexer.getCharPosition(currentPos.line, Numbers.coerceIn(span.column, 0, text.getColumnCount(currentPos.line)))
            val endPos = if (nextSpan != null)
                text.indexer.getCharPosition(currentPos.line, Numbers.coerceIn(nextSpan.column, 0, text.getColumnCount(currentPos.line)))
            else text.indexer.getCharPosition(currentPos.line, text.getColumnCount(currentPos.line))
            range = TextRange(startPos, endPos)
        }
        return editor.dispatchEvent(constructor.create(editor, currentPos, event, span, range, motionRegion, motionBound))
    }

    private fun handleSelectionChange(e: MotionEvent): Boolean {
        when (selHandleType) {
            BOTH -> {
                insertHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }
            LEFT -> {
                editor.selectionAnchor = editor.cursor!!.right()

                leftHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }
            RIGHT -> {
                editor.selectionAnchor = editor.cursor!!.left()

                rightHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }
        }
        return false
    }

    private fun handleSelectionChange2(e: MotionEvent) {
        when (selHandleType) {
            BOTH -> insertHandle.applyPosition(e)
            LEFT -> leftHandle.applyPosition(e)
            RIGHT -> rightHandle.applyPosition(e)
        }
    }

    private fun computeEdgeFlags(x: Float, y: Float): Int {
        var flags = 0
        if (x < edgeFieldSize) {
            flags = flags or LEFT_EDGE
        }
        if (y < edgeFieldSize) {
            flags = flags or TOP_EDGE
        }
        if (x > editor.width - edgeFieldSize) {
            flags = flags or RIGHT_EDGE
        }
        if (y > editor.height - edgeFieldSize) {
            flags = flags or BOTTOM_EDGE
        }
        return flags
    }

    fun scrollIfThumbReachesEdge(@Nullable e: MotionEvent?) {
        scrollIfReachesEdge(e, 0f, 0f)
    }

    fun scrollIfReachesEdge(@Nullable e: MotionEvent?, x: Float, y: Float) {
        var currentX = x
        var currentY = y
        if (e != null) {
            currentX = e.x
            currentY = e.y
        }
        val flag = computeEdgeFlags(currentX, currentY)
        if (flag != 0) {
            val oldFlags = edgeFlags
            edgeFlags = flag
            thumbMotionRecord = if (e == null) null else MotionEvent.obtain(e)
            if (oldFlags == 0) {
                val initialDelta = (8 * editor.dpUnit).toInt()
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (editor.isReleased) return@post
                    EdgeScrollRunnable(initialDelta).run()
                }

            }
        } else {
            stopEdgeScroll()
        }
    }

    private fun isSameSign(a: Float, b: Float): Boolean {
        if (abs(a) < 1e-5f || abs(b) < 1e-5f) {
            return false
        }
        return (a < 0 && b < 0) || (a > 0 && b > 0)
    }

    fun stopEdgeScroll() {
        edgeFlags = 0
    }

    @JvmOverloads
    fun scrollBy(distanceX: Float, distanceY: Float, smooth: Boolean = false) {
        var endX = scroller.getCurrX() + distanceX.toInt()
        var endY = scroller.getCurrY() + distanceY.toInt()
        endX = max(endX, 0)
        endY = max(endY, 0)
        endY = min(endY, editor.scrollMaxY)
        endX = min(endX, editor.scrollMaxX)
        editor.dispatchEvent(
            ScrollEvent(
                editor, scroller.getCurrX(),
                scroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG
            )
        )
        if (smooth) {
            scroller.startScroll(
                scroller.getCurrX(),
                scroller.getCurrY(),
                endX - scroller.getCurrX(),
                endY - scroller.getCurrY()
            )
        } else {
            scroller.startScroll(
                scroller.getCurrX(),
                scroller.getCurrY(),
                endX - scroller.getCurrX(),
                endY - scroller.getCurrY(), 0
            )
            scroller.abortAnimation()
        }
        editor.invalidate()
    }

    fun getTouchedHandleType(): Int {
        return selHandleType
    }

    override fun onSingleTapUp(@NonNull e: MotionEvent): Boolean {
        scroller.forceFinished(true)
        if (editor.isFormatting) {
            return true
        }

        val resolved = editor.resolveTouchRegion(e)
        val region = IntPair.getFirst(resolved)
        val regionBound = IntPair.getSecond(resolved)
        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        editor.performClick()
        if (region == REGION_SIDE_ICON) {
            val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
            var row = (e.y + editor.offsetX).toInt() / editor.rowHeight
            row = max(0, min(row, layout.rowCount - 1))
            val inf = layout.getRowAt(row)

            if (inf.isLeadingRow) {
                val style = editor.renderer.getLineStyle(inf.lineIndex, LineSideIcon::class.java)
                if (style != null) {
                    if ((editor.dispatchEvent(SideIconClickEvent(editor, style)) and InterceptTarget.TARGET_EDITOR) != 0) {
                        return true
                    }
                }
            }
        }
        val position = editor.text.indexer.getCharPosition(line, column)
        if ((dispatchEditorMotionEvent(::ClickEvent, position, e, region, regionBound) and InterceptTarget.TARGET_EDITOR) != 0) {
            return true
        }
        editor.showSoftInput()
        notifyLater()
        val lnAction = editor.props!!.actionWhenLineNumberClicked

        if (region == REGION_TEXT) {
            if (editor.isInLongSelect) {
                val cursor = editor.cursor!!
                editor.setSelectionRegion(cursor.leftLine, cursor.leftColumn, line, column, false, SelectionChangeEvent.CAUSE_TAP)

                editor.endLongSelect()
            } else {
                editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP)
            }
        } else if (region == REGION_LINE_NUMBER) {
            when (lnAction) {
                DirectAccessProps.LN_ACTION_SELECT_LINE -> editor.setSelectionRegion(line, 0, line, editor.text.getColumnCount(line), false, SelectionChangeEvent.CAUSE_TAP)
                DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP)
                DirectAccessProps.LN_ACTION_NOTHING -> {}
                else -> {}
            }
        }
        return true
    }

    override fun onLongPress(@NonNull e: MotionEvent) {
        scroller.forceFinished(true)
        editor.releaseEdgeEffects()
        if (editor.isFormatting) {
            return
        }

        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        if ((dispatchEditorMotionEvent(::LongPressEvent, editor.text.indexer.getCharPosition(line, column), e) and InterceptTarget.TARGET_EDITOR) != 0) {
            return
        }
        if ((!editor.props!!.reselectOnLongPress && editor.cursor.isSelected()) || e.pointerCount != 1) {

            return
        }
        editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        editor.selectWord(line, column)
        if (editor.cursor.isSelected()) {
            beginDragSelect(line, column)
        }
    }

    override fun onScroll(e1: MotionEvent?, @NonNull e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        var dx = distanceX
        var dy = distanceY
        if (editor.props!!.singleDirectionDragging) {

            if (abs(dx) > abs(dy)) {
                dy = 0f
            } else {
                dx = 0f
            }
        }
        var endX = scroller.getCurrX() + dx.toInt()
        var endY = scroller.getCurrY() + dy.toInt()
        endX = max(endX, 0)
        endY = max(endY, 0)
        endY = min(endY, editor.scrollMaxY)
        endX = min(endX, editor.scrollMaxX)
        var notifyY = true
        var notifyX = true
        if (!editor.verticalEdgeEffect.isFinished) {
            val displacement = max(0f, min(1f, e2.x / editor.width))
            val distance = (if (glowTopOrBottom) dy else -dy) / editor.measuredHeight
            if (distance > 0) {
                endY = scroller.getCurrY()
                editor.verticalEdgeEffect.onPull(distance, if (!glowTopOrBottom) displacement else 1 - displacement)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val edgeEffect = editor.verticalEdgeEffect
                edgeEffect.onPullDistance(distance, if (!glowTopOrBottom) displacement else 1 - displacement)
                if (edgeEffect.distance != 0f) {
                    endY = scroller.getCurrY()
                }
            } else {
                editor.verticalEdgeEffect.finish()
            }
            notifyY = false
        }
        if (!editor.horizontalEdgeEffect.isFinished) {
            val displacement = max(0f, min(1f, e2.y / editor.height))
            val distance = (if (glowLeftOrRight) dx else -dx) / editor.measuredWidth
            if (distance > 0) {
                endX = scroller.getCurrX()
                editor.horizontalEdgeEffect.onPull(distance, if (!glowLeftOrRight) 1 - displacement else displacement)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val edgeEffect = editor.horizontalEdgeEffect
                edgeEffect.onPullDistance(distance, if (!glowLeftOrRight) 1 - displacement else displacement)
                if (edgeEffect.distance != 0f) {
                    endX = scroller.getCurrX()
                }
            } else {
                editor.horizontalEdgeEffect.finish()
            }
            notifyX = false
        }
        scroller.startScroll(
            scroller.getCurrX(),
            scroller.getCurrY(),
            endX - scroller.getCurrX(),
            endY - scroller.getCurrY(), 0
        )
        val minOverPull = 2f
        if (notifyY && scroller.getCurrY().toFloat() + dy < -minOverPull) {
            editor.verticalEdgeEffect.onPull(-dy / editor.measuredHeight, max(0f, min(1f, e2.x / editor.width)))
            glowTopOrBottom = false
        }
        if (notifyY && scroller.getCurrY().toFloat() + dy > editor.scrollMaxY.toFloat() + minOverPull) {
            editor.verticalEdgeEffect.onPull(dy / editor.measuredHeight, max(0f, min(1f, e2.x / editor.width)))
            glowTopOrBottom = true
        }
        if (notifyX && scroller.getCurrX().toFloat() + dx < -minOverPull) {
            editor.horizontalEdgeEffect.onPull(-dx / editor.measuredWidth, max(0f, min(1f, e2.y / editor.height)))
            glowLeftOrRight = false
        }
        if (notifyX && scroller.getCurrX().toFloat() + dx > editor.scrollMaxX.toFloat() + minOverPull) {
            editor.horizontalEdgeEffect.onPull(dx / editor.measuredWidth, max(0f, min(1f, e2.y / editor.height)))
            glowLeftOrRight = true
        }
        editor.invalidate()
        editor.dispatchEvent(
            ScrollEvent(
                editor, scroller.getCurrX(),
                scroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG
            )
        )
        return true
    }

    override fun onFling(e1: MotionEvent?, @NonNull e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        var vx = velocityX
        var vy = velocityY
        if (editor.props!!.singleDirectionFling) {

            if (abs(vx) > abs(vy)) {
                vy = 0f
            } else {
                vx = 0f
            }
        }
        if (!editor.props!!.scrollFling) {
            return false
        }

        // If we do not finish it here, it can produce a high speed and cause the final scroll range to be broken, even a NaN for velocity
        scroller.forceFinished(true)
        scroller.fling(
            scroller.getCurrX(),
            scroller.getCurrY(),
            (-vx).toInt(),
            (-vy).toInt(),
            0,
            editor.scrollMaxX,
            0,
            editor.scrollMaxY,
            if (editor.props!!.overScrollEnabled && !editor.isWordwrap) (20 * editor.dpUnit).toInt() else 0,
            if (editor.props!!.overScrollEnabled) (20 * editor.dpUnit).toInt() else 0
        )

        val minVe = editor.dpUnit * 2000
        if (abs(vx) >= minVe || abs(vy) >= minVe) {
            notifyScrolled()
        }
        editor.releaseEdgeEffects()
        editor.dispatchEvent(
            ScrollEvent(
                editor, scroller.getCurrX(),
                scroller.getCurrY(), scroller.getFinalX(), scroller.getFinalY(), ScrollEvent.CAUSE_USER_FLING
            )
        )
        editor.postInvalidateOnAnimation()
        return false
    }

    override fun onScale(@NonNull detector: ScaleGestureDetector): Boolean {
        if (editor.isFormatting) {
            return true
        }
        if (editor.isScalable) {
            val newSize = editor.textSizePx * detector.scaleFactor
            if (newSize < scaleMinSize || newSize > scaleMaxSize) {
                return true
            }
            val focusX = detector.focusX
            val focusY = detector.focusY
            val originHeight = editor.rowHeight
            editor.setTextSizePxDirect(newSize)
            val heightFactor = editor.rowHeight.toFloat() / originHeight
            var afterScrollY = (scroller.getCurrY().toFloat() + focusY) * heightFactor - focusY
            var afterScrollX = (scroller.getCurrX().toFloat() + focusX) * detector.scaleFactor - focusX
            afterScrollX = max(0f, min(afterScrollX, editor.scrollMaxX.toFloat()))
            afterScrollY = max(0f, min(afterScrollY, editor.scrollMaxY.toFloat()))
            editor.dispatchEvent(
                ScrollEvent(
                    editor, scroller.getCurrX(),
                    scroller.getCurrY(), afterScrollX.toInt(), afterScrollY.toInt(), ScrollEvent.CAUSE_SCALE_TEXT
                )
            )
            scroller.startScroll(afterScrollX.toInt(), afterScrollY.toInt(), 0, 0, 0)
            scroller.abortAnimation()
            isScaling = true
            editor.invalidate()
            return true
        }
        return false
    }

    override fun onScaleBegin(@NonNull detector: ScaleGestureDetector): Boolean {
        scroller.forceFinished(true)
        textSizeStart = editor.textSizePx
        return editor.isScalable && !editor.isFormatting && !hasAnyHeldHandle()
    }


    @JvmField
    internal var memoryPosition: Long = 0
    @JvmField
    internal var positionNotApplied: Boolean = false
    @JvmField
    internal var focusY: Float = 0f

    override fun onScaleEnd(@NonNull detector: ScaleGestureDetector) {
        isScaling = false
        if (textSizeStart == editor.textSizePx) {
            return
        }
        editor.renderer.forcedRecreateLayout = true
        if (editor.isWordwrap) {
            focusY = detector.focusY
            memoryPosition = editor.getPointPositionOnScreen(detector.focusX, detector.focusY)
            positionNotApplied = true
        } else {
            positionNotApplied = false
        }
        editor.renderContext!!.invalidateRenderNodes()
        editor.renderer.updateTimestamp()

        editor.invalidate()
    }

    override fun onDown(e: MotionEvent): Boolean {
        return editor.isEnabled
    }

    override fun onShowPress(@NonNull e: MotionEvent) {}

    override fun onSingleTapConfirmed(@NonNull e: MotionEvent): Boolean {
        return true
    }

    override fun onDoubleTap(@NonNull e: MotionEvent): Boolean {
        if (editor.isFormatting) {
            return true
        }

        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        if ((dispatchEditorMotionEvent(::DoubleClickEvent, editor.text.indexer.getCharPosition(line, column), e) and InterceptTarget.TARGET_EDITOR) != 0) {
            return true
        }
        if (editor.cursor.isSelected() || e.pointerCount != 1) {
            return true
        }
        editor.selectWord(line, column)
        return true
    }

    override fun onDoubleTapEvent(@NonNull e: MotionEvent): Boolean {
        return true
    }

    /**
     * This is a helper for EventHandler to control handles
     */
    inner class SelectionHandle(var type: Int) {

        private fun checkNoIntersection(
            one: SelectionHandleStyle.HandleDescriptor,
            another: SelectionHandleStyle.HandleDescriptor
        ): Boolean {
            return !RectF.intersects(one.position, another.position)
        }

        /**
         * Handle the event
         *
         * @param e Event sent by EventHandler
         */
        fun applyPosition(e: MotionEvent) {
            val descriptor = when (type) {
                LEFT -> editor.leftHandleDescriptor!!
                RIGHT -> editor.rightHandleDescriptor!!
                else -> editor.insertHandleDescriptor!!
            }
            val anotherDesc = if (type == LEFT) editor.rightHandleDescriptor!! else editor.leftHandleDescriptor!!

            val targetX = scroller.getCurrX().toFloat() + e.x + (if (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER) descriptor.position.width() else 0f) * (if (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT) 1 else -1)
            val targetY = scroller.getCurrY().toFloat() + e.y - descriptor.position.height()
            val coord = editor.getPointPosition(targetX, targetY)
            val line = IntPair.getFirst(coord)
            if (line >= 0 && line < editor.lineCount) {
                val column = IntPair.getSecond(coord)
                val cursor = editor.cursor!!
                val lastLine = if (type == RIGHT) cursor.rightLine else cursor.leftLine
                val lastColumn = if (type == RIGHT) cursor.rightColumn else cursor.leftColumn
                val anotherLine = if (type != RIGHT) cursor.rightLine else cursor.leftLine
                val anotherColumn = if (type != RIGHT) cursor.rightColumn else cursor.leftColumn


                if ((line != lastLine || column != lastColumn) && (type == BOTH || (line != anotherLine || column != anotherColumn))) {
                    when (type) {
                        BOTH -> {
                            editor.cancelAnimation()
                            editor.setSelection(line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
                        }
                        RIGHT -> {
                            if (anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                //Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandleStateChange(this@EditorTouchEventHandler.selHandleType, false)
                                    this@EditorTouchEventHandler.selHandleType = LEFT
                                    dispatchHandleStateChange(this@EditorTouchEventHandler.selHandleType, true)
                                    this.type = LEFT
                                    leftHandle.type = RIGHT
                                    val tmp = rightHandle
                                    rightHandle = leftHandle
                                    leftHandle = tmp
                                    editor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
                                }
                            } else {
                                editor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
                            }
                        }
                        LEFT -> {
                            if (anotherLine < line || (anotherLine == line && anotherColumn < column)) {
                                //Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandleStateChange(this@EditorTouchEventHandler.selHandleType, false)
                                    this@EditorTouchEventHandler.selHandleType = RIGHT
                                    dispatchHandleStateChange(this@EditorTouchEventHandler.selHandleType, true)
                                    this.type = RIGHT
                                    rightHandle.type = LEFT
                                    val tmp = rightHandle
                                    rightHandle = leftHandle
                                    leftHandle = tmp
                                    editor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
                                }
                            } else {
                                editor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE)
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Runnable for controlling auto-scrolling when thumb reaches the edges of editor
     */
    private inner class EdgeScrollRunnable(initDelta: Int) : Runnable {
        private val initialDelta: Float = initDelta.toFloat()
        private var deltaHorizontal: Float = initDelta.toFloat()
        private var deltaVertical: Float = initDelta.toFloat()
        private var lastDx: Float = 0f
        private var lastDy: Float = 0f
        private var factorX: Float = 0f
        private var factorY: Float = 0f
        private var postTimes: Long = 0

        override fun run() {
            var dx = (if ((edgeFlags and LEFT_EDGE) != 0) -deltaHorizontal else 0f) + (if ((edgeFlags and RIGHT_EDGE) != 0) deltaHorizontal else 0f)
            var dy = (if ((edgeFlags and TOP_EDGE) != 0) -deltaVertical else 0f) + (if ((edgeFlags and BOTTOM_EDGE) != 0) deltaVertical else 0f)
            if (dx > 0) {
                // Check whether there is content at right
                val cursor = editor.cursor!!
                val line: Int = if (selHandleType == BOTH || selHandleType == LEFT) {
                    cursor.leftLine
                } else {
                    cursor.rightLine
                }

                val column = editor.text.getColumnCount(line)
                // Do not scroll too far from text region of this line
                val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                val maxOffset = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(line, column)[1] - editor.width * 0.85f

                if (scroller.getCurrX().toFloat() > maxOffset) {
                    dx = 0f
                }
            }
            scrollBy(dx, dy)
            if (editorMagnifier.isShowing()) {
                editorMagnifier.dismiss()
            }

            // Speed up if we are scrolling in the direction
            if (isSameSign(dx, lastDx)) {
                if (factorX < MAX_FACTOR && postTimes % 2 == 0L) {
                    factorX++
                    deltaHorizontal *= INCREASE_FACTOR
                }
            } else {
                // Recover initial speed because direction changed
                deltaHorizontal = initialDelta
                factorX = 0f
            }
            if (isSameSign(dy, lastDy)) {
                if (factorY < MAX_FACTOR && postTimes % 2 == 0L) {
                    factorY++
                    deltaVertical *= INCREASE_FACTOR
                }
            } else {
                deltaVertical = initialDelta
                factorY = 0f
            }
            lastDx = dx
            lastDy = dy

            // Update selection
            thumbMotionRecord?.let {
                if (!handleDragSelect(it, true)) {
                    handleSelectionChange2(it)
                }
            }

            postTimes++
            // Post for animation
            if (edgeFlags != 0) {
                io.github.abc15018045126.sora.util.EditorHandler.postDelayed({
                    if (editor.isReleased) return@postDelayed
                    this.run()
                }, 10)

            }
        }

    }

    companion object {
        const val HIDE_DELAY = 3000
        const val HIDE_DELAY_HANDLE = 3500

        const val SCROLLBAR_FADE_ANIMATION_TIME = 200
        const val MAGNIFIER_TOUCH_SLOP = 4

        const val LEFT_EDGE = 1
        const val RIGHT_EDGE = 1 shl 1
        const val TOP_EDGE = 1 shl 2
        const val BOTTOM_EDGE = 1 shl 3

        const val LEFT = HandleStateChangeEvent.HANDLE_TYPE_LEFT
        const val RIGHT = HandleStateChangeEvent.HANDLE_TYPE_RIGHT
        const val BOTH = HandleStateChangeEvent.HANDLE_TYPE_INSERT

        const val MAX_FACTOR = 32
        const val INCREASE_FACTOR = 1.06f
    }
}
