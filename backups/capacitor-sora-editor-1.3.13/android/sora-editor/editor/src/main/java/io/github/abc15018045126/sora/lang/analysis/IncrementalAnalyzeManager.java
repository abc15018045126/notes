
package io.github.abc15018045126.sora.lang.analysis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.github.abc15018045126.sora.lang.styling.Span;

/**
 * Interface for line based analyze managers
 *
 * @param <S> State type at line endings
 * @param <T> Token type
 */
public interface IncrementalAnalyzeManager<S, T> extends AnalyzeManager {

    /**
     * Get the initial at document start
     */
    S getInitialState();

    /**
     * Get recorded state for subclass
     */
    LineTokenizeResult<S, T> getState(int line);

    /**
     * Compare the two states.
     * Return true if they equal
     */
    boolean stateEquals(S state, S another);

    /**
     * Tokenize for the given line
     *
     * @param lineIndex -1 for unknown
     */
    LineTokenizeResult<S, T> tokenizeLine(CharSequence line, S state, int lineIndex);

    /**
     * Generate spans for the line
     */
    List<Span> generateSpansForLine(LineTokenizeResult<S, T> tokens);

    /**
     * Called when a State object is to be abandoned
     */
    void onAbandonState(S state);

    /**
     * Called when a State object is to be added
     */
    void onAddState(S state);

    /**
     * Saved state
     */
    class LineTokenizeResult<S_, T_> {

        /**
         * State at line end
         */
        public S_ state;

        /**
         * Tokens on this line
         */
        public List<T_> tokens;

        /**
         * Spans. If spans are generated as well you can directly return them here to avoid
         * {@link #generateSpansForLine(LineTokenizeResult)} calls.
         */
        public List<Span> spans;

        public LineTokenizeResult(@NonNull S_ state, @Nullable List<T_> tokens) {
            this.state = state;
            this.tokens = tokens;
        }

        public LineTokenizeResult(@NonNull S_ state, @Nullable List<T_> tokens, @Nullable List<Span> spans) {
            this.state = state;
            this.tokens = tokens;
            this.spans = spans;
        }

        protected LineTokenizeResult<S_, T_> clearSpans() {
            spans = null;
            return this;
        }

    }


}

