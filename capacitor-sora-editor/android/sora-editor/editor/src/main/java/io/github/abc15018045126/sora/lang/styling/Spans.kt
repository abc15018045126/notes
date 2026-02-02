package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * Spans object saves spans in editor.
 */
interface Spans {

    /**
     * Adjust spans on insert.
     * Must be implemented.
     */
    fun adjustOnInsert(start: CharPosition, end: CharPosition)

    /**
     * Adjust spans on delete.
     * Must be implemented.
     */
    fun adjustOnDelete(start: CharPosition, end: CharPosition)

    /**
     * Read spans.
     * Must be implemented.
     */
    fun read(): Reader

    /**
     * Check whether the class supports [modify]
     */
    fun supportsModify(): Boolean

    /**
     * Modify the content.
     *
     * Optional to implement.
     */
    fun modify(): Modifier

    /**
     * Get line count of the spans
     */
    fun getLineCount(): Int

    /**
     * Reader reads the spans in a [Spans] object.
     */
    interface Reader {

        /**
         * Start reading the spans on the given line.
         * You may prepare some data here if the actual spans are not stored by [Span] objects.
         *
         * line may be -1 to release the reader.
         */
        fun moveToLine(line: Int)

        /**
         * Get span count on current line
         */
        fun getSpanCount(): Int

        /**
         * Get span at position [index].
         * The result object is read-only. Callers should not modify this object.
         */
        fun getSpanAt(index: Int): Span

        /**
         * Get all spans on the given line. This ignores the line argument set by [Reader.moveToLine]
         * The list contains at least 1 span. And the result list is unmodifiable.
         */
        fun getSpansOnLine(line: Int): List<Span>
    }

    /**
     * Modifier updates the spans in a [Spans] object.
     */
    interface Modifier {

        /**
         * Set the line's spans to the new ones. The given [spans] list should not be stored,
         * but the content of it can be copied.
         *
         * If the line index exceeds the current capacity, implementation of this should expand the capacity
         * without throwing an exception. Set spans of the filled lines to color [EditorColorScheme.TEXT_NORMAL]
         * or extends previous styles.
         */
        fun setSpansOnLine(line: Int, spans: List<out Span>)

        /**
         * Add a line at the given position.
         * The given [spans] list should not be stored,
         * but the content of it can be copied.
         */
        fun addLineAt(line: Int, spans: List<out Span>)

        /**
         * Remove a line
         */
        fun deleteLineAt(line: Int)
    }
}
