package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * An Event object describes an event of editor.
 * It includes several attributes such as time and the editor object.
 * Subclasses of Event will define their own fields or methods.
 *
 * @author abc15018045126
 */
abstract class Event @JvmOverloads constructor(
    val editor: CodeEditor,
    open val eventTime: Long = System.currentTimeMillis()
) {
    var interceptTargets: Int = 0
        private set

    /**
     * Check whether this event can be intercepted (so that the event is not sent to other
     * receivers after being intercepted)
     * Intercept-able events:
     *
     * @see LongPressEvent
     * @see ClickEvent
     * @see DoubleClickEvent
     * @see EditorKeyEvent
     */
    open fun canIntercept(): Boolean {
        return false
    }

    /**
     * Intercept the event for all targets.
     * <p>
     * Make sure {@link #canIntercept()} returns true. Otherwise, an {@link UnsupportedOperationException}
     * will be thrown.
     *
     * @see InterceptTarget
     */
    fun intercept() {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = InterceptTarget.TARGET_EDITOR or InterceptTarget.TARGET_RECEIVERS
    }

    /**
     * Intercept the event for some targets
     *
     * @param targets Masks for target types
     * @see InterceptTarget
     */
    fun intercept(targets: Int) {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = targets
    }

    /**
     * Check whether this event is intercepted for some types of targets
     *
     * @see #getInterceptTargets()
     */
    fun isIntercepted(): Boolean {
        return interceptTargets != 0
    }
}
