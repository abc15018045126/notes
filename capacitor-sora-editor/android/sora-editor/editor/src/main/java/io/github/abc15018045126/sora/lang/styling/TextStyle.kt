package io.github.abc15018045126.sora.lang.styling

/**
 * Utility class for text style related operations
 *
 * @author abc15018045126
 */
object TextStyle {

    const val COLOR_ID_BIT_COUNT = 19
    const val FOREGROUND_BITS = (1L shl COLOR_ID_BIT_COUNT) - 1
    const val BACKGROUND_BITS = FOREGROUND_BITS shl COLOR_ID_BIT_COUNT

    /**
     * Bold text style
     */
    const val BOLD_BIT = 1L shl COLOR_ID_BIT_COUNT * 2

    /**
     * Italic text style
     */
    const val ITALICS_BIT = BOLD_BIT shl 1

    /**
     * Show a strikethrough
     */
    const val STRIKETHROUGH_BIT = ITALICS_BIT shl 1

    /**
     * Edit texts in the region will not cause auto-completion to work
     */
    const val NO_COMPLETION_BIT = STRIKETHROUGH_BIT shl 1

    /**
     * Convenient method
     *
     * @see .makeStyle
     */
    @JvmStatic
    fun makeStyle(foregroundColorId: Int): Long {
        checkColorId(foregroundColorId)
        return foregroundColorId.toLong()
    }

    /**
     * Convenient method
     *
     * @see .makeStyle
     */
    @JvmStatic
    fun makeStyle(foregroundColorId: Int, noCompletion: Boolean): Long {
        checkColorId(foregroundColorId)
        return foregroundColorId.toLong() or if (noCompletion) NO_COMPLETION_BIT else 0
    }

    /**
     * Convenient method
     *
     * @see .makeStyle
     */
    @JvmStatic
    fun makeStyle(
        foregroundColorId: Int, backgroundColorId: Int, bold: Boolean,
        italic: Boolean, strikeThrough: Boolean
    ): Long {
        return makeStyle(foregroundColorId, backgroundColorId, bold, italic, strikeThrough, false)
    }

    /**
     * Make a TextStyle with the given style arguments
     *
     * Note: colorId must be less than 20 bits
     *
     * @see .BOLD_BIT
     * @see .ITALICS_BIT
     * @see .STRIKETHROUGH_BIT
     * @see .NO_COMPLETION_BIT
     */
    @JvmStatic
    fun makeStyle(
        foregroundColorId: Int, backgroundColorId: Int, bold: Boolean,
        italic: Boolean, strikeThrough: Boolean, noCompletion: Boolean
    ): Long {
        checkColorId(foregroundColorId)
        checkColorId(backgroundColorId)
        return foregroundColorId.toLong() +
                (backgroundColorId.toLong() shl COLOR_ID_BIT_COUNT) or
                (if (bold) BOLD_BIT else 0) or
                (if (italic) ITALICS_BIT else 0) or
                (if (strikeThrough) STRIKETHROUGH_BIT else 0) or
                (if (noCompletion) NO_COMPLETION_BIT else 0)
    }

    @JvmStatic
    fun getForegroundColorId(style: Long): Int {
        return (style and FOREGROUND_BITS).toInt()
    }

    @JvmStatic
    fun getBackgroundColorId(style: Long): Int {
        return ((style and BACKGROUND_BITS) shr COLOR_ID_BIT_COUNT).toInt()
    }

    @JvmStatic
    fun isBold(style: Long): Boolean {
        return style and BOLD_BIT != 0L
    }

    @JvmStatic
    fun isItalics(style: Long): Boolean {
        return style and ITALICS_BIT != 0L
    }

    @JvmStatic
    fun isStrikeThrough(style: Long): Boolean {
        return style and STRIKETHROUGH_BIT != 0L
    }

    @JvmStatic
    fun isNoCompletion(style: Long): Boolean {
        return style and NO_COMPLETION_BIT != 0L
    }

    @JvmStatic
    fun getStyleBits(style: Long): Long {
        return style and (BOLD_BIT + ITALICS_BIT + STRIKETHROUGH_BIT)
    }

    @JvmStatic
    fun checkColorId(colorId: Int) {
        if (colorId > (1 shl COLOR_ID_BIT_COUNT) - 1 || colorId < 0) {
            throw IllegalArgumentException("color id must be positive and bit count is less than $COLOR_ID_BIT_COUNT")
        }
    }

}
