package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.color.ConstColor
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.span.SpanExt

/**
 * Span describes the appearance and other attributes of text segment
 *
 * @author abc15018045126
 */
interface Span {

    /**
     * Set column of this span
     */
    var column: Int

    fun shiftColumnBy(deltaColumn: Int) {
        column += deltaColumn
    }

    /**
     * Set style of the span
     *
     * @see TextStyle
     */
    var style: Long

    /**
     * Get foreground color ID from style
     */
    val foregroundColorId: Int
        get() = TextStyle.getForegroundColorId(style)

    /**
     * Get background color ID from style
     */
    val backgroundColorId: Int
        get() = TextStyle.getBackgroundColorId(style)

    /**
     * Get bits of other text styles that affects measuring
     */
    val styleBits: Long
        get() = TextStyle.getStyleBits(style)

    /**
     * Set underline color of span. `0` for no underline.
     * **This is not color ID**
     */
    fun setUnderlineColor(color: Int) {
        if (color == 0) {
            underlineColor = null
            return
        }
        underlineColor = ConstColor(color)
    }

    /**
     * Set underline color with a [ResolvableColor] to resolve colors when the span is rendered.
     * Null for no underline.
     */
    var underlineColor: ResolvableColor?

    /**
     * Extra data for language internal use
     */
    var extra: Any?

    /**
     * Set extended attribute of this span. The type of `ext` is checked whether it is compatible
     * with the given `extType`.
     *
     * @param extType Type of extension, from [io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs]
     * @param ext     The data to set. Use null to unset.
     */
    fun setSpanExt(extType: Int, ext: SpanExt?)

    /**
     * Check if certain extended attribute is set
     */
    fun hasSpanExt(extType: Int): Boolean

    /**
     * Get extended attribute of given type. If it is unset, null is returned.
     */
    fun <T> getSpanExt(extType: Int): T?

    /**
     * Remove all [SpanExt]s
     */
    fun removeAllSpanExt()

    /**
     * Reset all properties of this span, including column, style and ext.
     */
    fun reset()

    /**
     * Create a new span with the same attributes. The new span can be safely modified with affecting
     * the original span.
     *
     * Note that [SpanExt] objects are **shared**s by the old span and new span instance.
     *
     * @return new span with the same attribute
     */
    fun copy(): Span

    /**
     * Recycle this span to pool for later use. After calling this method, you should not
     * make any access to this [Span] instance. And all attributes of this span are reset.
     *
     * Note that no matter whether the span is added to pool, it will be reset.
     *
     * @return if the span is actually added to the pool.
     */
    fun recycle(): Boolean

    companion object {
        /**
         * Get an available [Span] object from either cache or new instance.
         * The result object will be initialized with the given arguments.
         */
        @JvmStatic
        fun obtain(column: Int, style: Long): Span {
            return SpanFactory.obtain(column, style)
        }

        /**
         * Recycle all spans in the given collection
         */
        @JvmStatic
        fun recycleAll(spans: Collection<Span>) {
            SpanFactory.recycleAll(spans)
        }
    }

}
