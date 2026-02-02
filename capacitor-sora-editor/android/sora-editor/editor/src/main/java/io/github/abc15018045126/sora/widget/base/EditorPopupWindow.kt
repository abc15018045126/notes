package io.github.abc15018045126.sora.widget.base

import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import io.github.abc15018045126.sora.event.EventReceiver
import io.github.abc15018045126.sora.event.ScrollEvent
import io.github.abc15018045126.sora.widget.CodeEditor
import kotlin.math.abs

/**
 * Base class for all editor popup windows.
 */
open class EditorPopupWindow(open val editor: CodeEditor, val features: Int) {

    companion object {
        /**
         * Update the position of this window when user scrolls the editor
         */
        const val FEATURE_SCROLL_AS_CONTENT = 1

        /**
         * Allow the window to be displayed outside the view's rectangle.
         * Otherwise, the window's size will be adjusted to force it to display in the view.
         * If the space can't display it, it will get hidden.
         */
        const val FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED = 1 shl 1

        /**
         * Hide this window when the user scrolls fast. Such as the selection handle
         * is currently near the edge of screen.
         */
        const val FEATURE_HIDE_WHEN_FAST_SCROLL = 1 shl 2

        /**
         * Dismiss the window if it covers the current caret.
         */
        const val FEATURE_DISMISS_WHEN_OBSCURING_CURSOR = 1 shl 3
    }

    val popup = PopupWindow()
    private val locationBuffer = IntArray(2)
    private val scrollListener: EventReceiver<ScrollEvent>
    private val editorLayoutChangeListener: View.OnLayoutChangeListener
    private var registerFlag = false
    private var registered = false
    private var layoutChangeListenerRegistered = false
    private var parentView: View = editor
    
    private var offsetX = 0f
    private var offsetY = 0f
    private var windowX = 0f
    private var windowY = 0f
    
    var width = 0
        protected set
    var height = 0
        protected set

    init {
        popup.elevation = editor.dpUnit * 8
        editorLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (isShowing) {
                applyWindowAttributes(false)
            }
        }
        scrollListener = EventReceiver { event, unsubscribe ->
            if (!registerFlag) {
                unsubscribe.unsubscribe()
                registered = false
                return@EventReceiver
            }
            when (event.cause) {
                ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE,
                ScrollEvent.CAUSE_TEXT_SELECTING,
                ScrollEvent.CAUSE_USER_FLING,
                ScrollEvent.CAUSE_SCALE_TEXT -> {
                    if (isFeatureEnabled(FEATURE_HIDE_WHEN_FAST_SCROLL) &&
                        (abs(event.endX - event.startX) > 80 || abs(event.endY - event.startY) > 80)
                    ) {
                        if (isShowing) {
                            dismiss()
                            return@EventReceiver
                        }
                    }
                }
            }
            if (isFeatureEnabled(FEATURE_SCROLL_AS_CONTENT)) {
                applyWindowAttributes(false)
            }
        }
        register()
    }

    /**
     * Checks whether a single feature is enabled
     */
    fun isFeatureEnabled(feature: Int): Boolean {
        if (Integer.bitCount(feature) != 1) {
            throw IllegalArgumentException("Not a valid feature integer")
        }
        return (features and feature) != 0
    }

    fun register() {
        if (!registered) {
            editor.subscribeEvent(ScrollEvent::class.java, scrollListener)
            registered = true
        }
        if (isFeatureEnabled(FEATURE_DISMISS_WHEN_OBSCURING_CURSOR) && !layoutChangeListenerRegistered) {
            editor.addOnLayoutChangeListener(editorLayoutChangeListener)
            layoutChangeListenerRegistered = true
        }
        registerFlag = true
    }

    fun unregister() {
        registerFlag = false
        if (layoutChangeListenerRegistered) {
            editor.removeOnLayoutChangeListener(editorLayoutChangeListener)
            layoutChangeListenerRegistered = false
        }
    }

    open val isShowing: Boolean
        get() = popup.isShowing

    /**
     * @see [PopupWindow.setContentView]
     */
    open fun setContentView(view: View) {
        popup.contentView = view
    }

    private fun wrapHorizontal(horizontal: Float): Float = 
        horizontal.coerceIn(0f, editor.width.toFloat())

    private fun wrapVertical(vertical: Float): Float = 
        vertical.coerceIn(0f, editor.height.toFloat())

    private fun applyWindowAttributes(show: Boolean) {
        if (!show && !isShowing) {
            return
        }
        val autoScroll = isFeatureEnabled(FEATURE_SCROLL_AS_CONTENT)
        var left = if (autoScroll) (windowX - editor.offsetX) else (windowX - offsetX)
        var top = if (autoScroll) (windowY - editor.offsetY) else (windowY - offsetY)
        var right = left + width
        var bottom = top + height
        
        if (!isFeatureEnabled(FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED)) {
            val finalLeft = wrapHorizontal(left)
            val finalRight = wrapHorizontal(right)
            val finalTop = wrapVertical(top)
            val finalBottom = wrapVertical(bottom)
            
            if (finalTop >= finalBottom || finalLeft >= finalRight) {
                dismiss()
                return
            }
            left = finalLeft
            right = finalRight
            top = finalTop
            bottom = finalBottom
        }
        
        if (isCursorObscured(left, top, right, bottom)) {
            dismiss()
            return
        }
        
        editor.getLocationInWindow(locationBuffer)
        val w = (right - left).toInt()
        val h = (bottom - top).toInt()
        val finalX = (left + locationBuffer[0]).toInt()
        val finalY = (top + locationBuffer[1]).toInt()
        
        if (popup.isShowing) {
            popup.update(finalX, finalY, w, h)
        } else if (show) {
            popup.width = w
            popup.height = h
            popup.showAtLocation(parentView, Gravity.START or Gravity.TOP, finalX, finalY)
        }
    }

    open fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        applyWindowAttributes(false)
    }

    open fun setLocation(x: Float, y: Float) {
        windowX = x
        windowY = y
        offsetY = editor.offsetY.toFloat()
        offsetX = editor.offsetX.toFloat()
        applyWindowAttributes(false)
    }

    open fun setLocationAbsolutely(x: Float, y: Float) {
        setLocation(x + editor.offsetX, y + editor.offsetY)
    }

    private fun isCursorObscured(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        if (!isFeatureEnabled(FEATURE_DISMISS_WHEN_OBSCURING_CURSOR)) {
            return false
        }
        return try {
            val cursor = editor.cursor ?: return false
            val line = cursor.leftLine
            val column = cursor.leftColumn
            val cursorLeft = editor.getCharOffsetX(line, column)
            val cursorTop = editor.getCharOffsetY(line, column)
            if (cursorLeft.isNaN() || cursorTop.isNaN()) {
                return false
            }
            val cursorRight = cursorLeft + Math.max(1f, editor.insertSelectionWidth)
            val cursorBottom = cursorTop + editor.rowHeight
            cursorLeft < right && cursorRight > left && cursorTop < bottom && cursorBottom > top
        } catch (ignored: Throwable) {
            false
        }
    }

    open fun show() {
        if (isShowing) {
            return
        }
        applyWindowAttributes(true)
    }

    open fun dismiss() {
        if (isShowing) {
            popup.dismiss()
        }
    }

    fun getParentView(): View = parentView

    fun setParentView(view: View) {
        parentView = view
    }
}
