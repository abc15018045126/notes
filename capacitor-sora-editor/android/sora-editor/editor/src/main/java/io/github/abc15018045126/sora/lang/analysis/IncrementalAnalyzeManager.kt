package io.github.abc15018045126.sora.lang.analysis

import io.github.abc15018045126.sora.lang.styling.Span

/**
 * Interface for line based analyze managers
 *
 * @param <S> State type at line endings
 * @param <T> Token type
 */
interface IncrementalAnalyzeManager<S, T> : AnalyzeManager {

    /**
     * Get the initial at document start
     */
    val initialState: S

    /**
     * Get recorded state for subclass
     */
    fun getState(line: Int): LineTokenizeResult<S, T>?

    /**
     * Compare the two states.
     * Return true if they equal
     */
    fun stateEquals(state: S, another: S): Boolean

    /**
     * Tokenize for the given line
     *
     * @param lineIndex -1 for unknown
     */
    fun tokenizeLine(line: CharSequence, state: S, lineIndex: Int): LineTokenizeResult<S, T>

    /**
     * Generate spans for the line
     */
    fun generateSpansForLine(tokens: LineTokenizeResult<S, T>): List<Span>?

    /**
     * Called when a State object is to be abandoned
     */
    fun onAbandonState(state: S)

    /**
     * Called when a State object is to be added
     */
    fun onAddState(state: S)

    /**
     * Saved state
     */
    class LineTokenizeResult<S_, T_> {

        /**
         * State at line end
         */
        @JvmField
        var state: S_

        /**
         * Tokens on this line
         */
        @JvmField
        var tokens: List<T_>?

        /**
         * Spans. If spans are generated as well you can directly return them here to avoid
         * [generateSpansForLine] calls.
         */
        @JvmField
        var spans: List<Span>? = null

        constructor(state: S_, tokens: List<T_>?) {
            this.state = state
            this.tokens = tokens
        }

        constructor(state: S_, tokens: List<T_>?, spans: List<Span>?) {
            this.state = state
            this.tokens = tokens
            this.spans = spans
        }

        fun clearSpans(): LineTokenizeResult<S_, T_> {
            spans = null
            return this
        }

    }

}
