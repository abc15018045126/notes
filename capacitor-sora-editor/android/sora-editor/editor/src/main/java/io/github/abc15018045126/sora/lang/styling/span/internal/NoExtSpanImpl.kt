package io.github.abc15018045126.sora.lang.styling.span.internal

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanPool
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.span.SpanExt
import java.util.*

/**
 * Span without SpanExt support.
 *
 * @author abc15018045126
 */
class NoExtSpanImpl : Span {

    override var column: Int = 0
    override var style: Long = 0L
    override var extra: Any? = null

    internal constructor()

    internal constructor(column: Int, style: Long) {
        this.column = column
        this.style = style
    }

    override var underlineColor: ResolvableColor?
        get() = null
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun setSpanExt(extType: Int, ext: SpanExt?) {
        throw UnsupportedOperationException()
    }

    override fun hasSpanExt(extType: Int): Boolean {
        return false
    }

    override fun <T> getSpanExt(extType: Int): T? {
        return null
    }

    override fun removeAllSpanExt() {
    }

    override fun reset() {
        column = 0
        style = 0L
        extra = null
    }

    override fun copy(): Span {
        val span = NoExtSpanImpl(column, style)
        span.extra = extra
        return span
    }

    override fun recycle(): Boolean {
        reset()
        return pool.offer(this)
    }

    override fun toString(): String {
        return "NoExtSpanImpl(column=$column, style=$style, extra=$extra)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NoExtSpanImpl
        return column == that.column && style == that.style && extra == that.extra
    }

    override fun hashCode(): Int {
        return Objects.hash(column, style, extra)
    }

    companion object {
        @JvmStatic
        private val pool = SpanPool { c, s -> NoExtSpanImpl(c, s) }

        @JvmStatic
        fun obtain(column: Int, style: Long): NoExtSpanImpl {
            return pool.obtain(column, style)
        }
    }
}
