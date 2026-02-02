package io.github.abc15018045126.sora.widget.style

/**
 * Interface for provide various cursor animations
 *
 * @author abc15018045126, Dmitry Rubtsov
 */
interface CursorAnimator {

    /**
     * Mark the current cursor position as animation start position
     */
    fun markStartPos()

    /**
     * Mark the current cursor position as animation end position
     */
    fun markEndPos()

    /**
     * Start animation
     */
    fun start()

    /**
     * Cancel animation
     */
    fun cancel()

    /**
     * Check whether animation is in process
     */
    fun isRunning(): Boolean

    /**
     * The current x position of cursor in view offset
     */
    fun animatedX(): Float

    /**
     * The current y position of cursor in view offset
     */
    fun animatedY(): Float

    /**
     * Height of current line background
     */
    fun animatedLineHeight(): Float

    /**
     * Bottom Y position in view offset of current line background
     */
    fun animatedLineBottom(): Float
}
