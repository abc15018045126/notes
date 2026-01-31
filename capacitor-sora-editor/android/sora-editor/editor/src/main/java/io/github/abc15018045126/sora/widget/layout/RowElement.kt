package io.github.abc15018045126.sora.widget.layout

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint

/**
 * Element on a row
 *
 * @author abc15018045126
 */
class RowElement {
    /**
     * Type of element.
     *
     * @see RowElementTypes
     */
    @JvmField
    var type: Int = 0

    /* Fields for type TEXT */

    /**
     * Start column of text
     */
    @JvmField
    var startColumn: Int = 0

    /**
     * End column of text
     */
    @JvmField
    var endColumn: Int = 0

    /**
     * Direction of the text run
     */
    @JvmField
    var isRtlText: Boolean = false

    /* Fields for type INLAY_HINT */

    /**
     * The inlay hint
     */
    @JvmField
    var inlayHint: InlayHint? = null

    /**
     * The expected column position to display after
     */
    @JvmField
    var displayColumnPosition: Int = 0
}
