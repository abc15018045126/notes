package io.github.abc15018045126.sora.widget.layout

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint

/**
 * This class represents a 'row' in editor.
 * Editor uses this to draw rows
 *
 * @author abc15018045126
 */
class Row {
    /**
     * The index in lines
     * But not row index
     */
    @JvmField
    var lineIndex: Int = 0

    /**
     * Whether this row is the first one of a line.
     * Editor will draw line number to left of this row to indicate this
     */
    @JvmField
    var isLeadingRow: Boolean = false

    /**
     * Whether this row is the last one of a line.
     * Editor will draw soft-wrap or line-break indicator according to this
     */
    @JvmField
    var isTrailingRow: Boolean = false

    /**
     * Start index in target line
     */
    @JvmField
    var startColumn: Int = 0

    /**
     * End index in target line
     */
    @JvmField
    var endColumn: Int = 0

    /**
     * Inlay hints on the row
     */
    @JvmField
    @JvmSuppressWildcards
    var inlayHints: List<InlayHint> = emptyList()

    /**
     * Extra translation when rendering
     */
    @JvmField
    var renderTranslateX: Float = 0f
}
