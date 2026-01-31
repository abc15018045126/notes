package io.github.abc15018045126.sora.lang.format

import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.TextRange

/**
 * Format content for editor
 */
interface Formatter {

    /**
     * Format the given content from [cursorRange] position
     *
     * Format the content directly, and call [FormatResultReceiver] to receive the formatted content from the editor when the formatting is complete
     *
     * @param text        the content to format, but not the original Content in editor
     * @param cursorRange the positions of cursor. Start and end position may be the same.
     */
    fun format(text: Content, cursorRange: TextRange)

    /**
     * Format the given content from [rangeToFormat] position
     *
     * Format the content directly, and call [FormatResultReceiver] to receive the formatted content from the editor when the formatting is complete
     *
     * @param text          the content to format, but not the original Content in editor
     * @param rangeToFormat the range in text to be formatted
     * @param cursorRange   the positions of cursor. Start and end position may be the same.
     */
    fun formatRegion(text: Content, rangeToFormat: TextRange, cursorRange: TextRange)

    /**
     * Set the result receiver
     */
    fun setReceiver(receiver: FormatResultReceiver?)

    /**
     * Whether the current formatter is running
     */
    fun isRunning(): Boolean

    /**
     * Destroy the formatter. Release any resources held.
     * Make sure that you will not call the receiver anymore.
     */
    fun destroy()

    /**
     * Cancel last task if it is still running. Do not send success/failure to editor for last task.
     */
    fun cancel() {
        // Default implementation does nothing
    }

    interface FormatResultReceiver {
        /**
         * Called when the formatting is completed
         *
         * @param applyContent the formatted **full** text
         * @param cursorRange  The range of cursor after formatting. You may pass null for unspecified.
         *                     Also, the start and end of the range may be the same position.
         */
        fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?)

        /**
         * Called when the formatting is failed
         *
         * @param throwable the throwable that caused formatting failed
         */
        fun onFormatFail(throwable: Throwable?)
    }
}
