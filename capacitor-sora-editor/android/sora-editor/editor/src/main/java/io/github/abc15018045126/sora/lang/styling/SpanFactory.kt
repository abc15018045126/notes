package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.span.internal.NoExtSpanImpl
import io.github.abc15018045126.sora.lang.styling.span.internal.SpanImpl

/**
 * Factory for [Span]
 */
object SpanFactory {

    /**
     * Get an available [Span] object from either cache or new instance.
     * The result object will be initialized with the given arguments.
     */
    @JvmStatic
    fun obtain(column: Int, style: Long): Span {
        return SpanImpl.obtain(column, style)
    }

    /**
     * Get an available [Span] object from either cache or new instance.
     *
     * Note that the span can not have additional fields beside
     */
    @JvmStatic
    fun obtainNoExt(column: Int, style: Long): Span {
        return NoExtSpanImpl.obtain(column, style)
    }

    /**
     * Recycle all spans in the given collection
     */
    @JvmStatic
    fun recycleAll(spans: Collection<Span>) {
        for (span in spans) {
            if (!span.recycle()) {
                return
            }
        }
    }
}
