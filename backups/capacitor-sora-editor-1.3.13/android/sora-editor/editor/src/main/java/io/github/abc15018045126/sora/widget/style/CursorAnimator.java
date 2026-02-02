
package io.github.abc15018045126.sora.widget.style;

/**
 * Interface for provide various cursor animations
 *
 * @author abc15018045126, Dmitry Rubtsov
 */
public interface CursorAnimator {

    /**
     * Mark the current cursor position as animation start position
     */
    void markStartPos();

    /**
     * Mark the current cursor position as animation end position
     */
    void markEndPos();

    /**
     * Start animation
     */
    void start();

    /**
     * Cancel animation
     */
    void cancel();

    /**
     * Check whether animation is in process
     */
    boolean isRunning();

    /**
     * The current x position of cursor in view offset
     */
    float animatedX();

    /**
     * The current y position of cursor in view offset
     */
    float animatedY();

    /**
     * Height of current line background
     */
    float animatedLineHeight();

    /**
     * Bottom Y position in view offset of current line background
     */
    float animatedLineBottom();
}

