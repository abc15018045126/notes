
package io.github.abc15018045126.sora.lang.styling;

import androidx.annotation.NonNull;

import java.util.Collection;

import io.github.abc15018045126.sora.lang.styling.span.internal.NoExtSpanImpl;
import io.github.abc15018045126.sora.lang.styling.span.internal.SpanImpl;

/**
 * Factory for {@link Span}
 */
public class SpanFactory {

    private SpanFactory() {

    }

    /**
     * Get an available {@link Span} object from either cache or new instance.
     * The result object will be initialized with the given arguments.
     */
    @NonNull
    public static Span obtain(int column, long style) {
        return SpanImpl.obtain(column, style);
    }

    /**
     * Get an available {@link Span} object from either cache or new instance.
     * The result object will be initialized with the given arguments.
     * <p>
     * Note that the span can not have additional fields beside
     */
    public static Span obtainNoExt(int column, long style) {
        return NoExtSpanImpl.obtain(column, style);
    }

    /**
     * Recycle all spans in the given collection
     */
    public static void recycleAll(@NonNull Collection<Span> spans) {
        for (Span span : spans) {
            if (!span.recycle()) {
                return;
            }
        }
    }

}

