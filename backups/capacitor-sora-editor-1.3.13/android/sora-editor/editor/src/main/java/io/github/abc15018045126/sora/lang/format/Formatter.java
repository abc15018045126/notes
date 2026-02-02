
package io.github.abc15018045126.sora.lang.format;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.text.TextRange;

/**
 * Format content for editor
 */
public interface Formatter {


    /**
     * Format the given content from {@code start} position to {@code end} position
     * <p>
     * Format the content directly, and call {@link FormatResultReceiver} to receive the formatted content from the editor when the formatting is complete
     * *
     *
     * @param text        the content to format, but not the original Content in editor
     * @param cursorRange the positions of cursor. Start and end position may be the same.
     */
    void format(@NonNull Content text, @NonNull TextRange cursorRange);

    /**
     * Format the given content from {@code start} position to {@code end} position
     * <p>
     * Format the content directly, and call {@link FormatResultReceiver} to receive the formatted content from the editor when the formatting is complete
     *
     * @param text          the content to format, but not the original Content in editor
     * @param rangeToFormat the range in text to be formatted
     * @param cursorRange   the positions of cursor. Start and end position may be the same.
     */
    void formatRegion(@NonNull Content text, @NonNull TextRange rangeToFormat, @NonNull TextRange cursorRange);

    /**
     * Set the result receiver
     */
    void setReceiver(@Nullable FormatResultReceiver receiver);

    /**
     * Whether the current formatter is running
     */
    boolean isRunning();

    /**
     * Destroy the formatter. Release any resources held.
     * Make sure that you will not call the receiver anymore.
     */
    void destroy();

    /**
     * Cancel last task if it is still running. Do not send success/failure to editor for last task.
     */
    default void cancel() {

    }


    interface FormatResultReceiver {
        /**
         * Called when the formatting is completed
         *
         * @param applyContent the formatted <strong>full</strong> text
         * @param cursorRange  The range of cursor after formatting. You may pass null for unspecified.
         *                     Also, the start and end of the range may be the same position.
         */
        void onFormatSucceed(@NonNull CharSequence applyContent, @Nullable TextRange cursorRange);

        /**
         * Called when the formatting is failed
         *
         * @param throwable the throwable that caused formatting failed
         */
        void onFormatFail(@Nullable Throwable throwable);

    }

}

