package io.github.abc15018045126.sora.lang.styling.span.internal

import androidx.collection.MutableIntObjectMap
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanPool
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.span.SpanExt
import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs
import java.util.*

class SpanImpl : Span {

    override var column: Int = 0
    override var style: Long = 0L
    override var extra: Any? = null
    private var extMap: MutableIntObjectMap<SpanExt>? = null

    internal constructor()

    internal constructor(column: Int, style: Long) {
        this.column = column
        this.style = style
    }

    override var underlineColor: ResolvableColor?
        get() = getSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR)
        set(value) = setSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR, value)

    override fun setSpanExt(extType: Int, ext: SpanExt?) {
        if (!SpanExtAttrs.checkType(extType, ext)) {
            throw IllegalArgumentException("type mismatch: extType $extType and extObj $ext")
        }
        if (ext == null) {
            extMap?.remove(extType)
            return
        }
        if (extMap == null) {
            extMap = MutableIntObjectMap()
        }
        extMap?.set(extType, ext)
    }

    override fun hasSpanExt(extType: Int): Boolean {
        return getSpanExt<Any>(extType) != null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getSpanExt(extType: Int): T? {
        return extMap?.get(extType) as T?
    }

    override fun removeAllSpanExt() {
        extMap?.clear()
    }

    override fun reset() {
        column = 0
        style = 0L
        extra = null
        removeAllSpanExt()
    }

    override fun copy(): Span {
        val span = SpanImpl()
        span.column = column
        span.style = style
        extMap?.let {
            span.extMap = MutableIntObjectMap()
            span.extMap?.putAll(it)
        }
        return span
    }

    override fun recycle(): Boolean {
        reset()
        return pool.offer(this)
    }

    override fun toString(): String {
        return "SpanImpl(column=$column, style=$style, extra=$extra, extMap=$extMap)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val span = other as SpanImpl
        return column == span.column && style == span.style && extMap == span.extMap
    }

    override fun hashCode(): Int {
        return Objects.hash(column, style, extMap)
    }

    companion object {
        @JvmStatic
        private val pool = SpanPool { c, s -> SpanImpl(c, s) }

        @JvmStatic
        fun obtain(column: Int, style: Long): SpanImpl {
            return pool.obtain(column, style)
        }
    }
}
