package io.github.abc15018045126.sora.widget.rendering

/**
 * Holds some shared constants in editor graphics properties
 *
 * @author abc15018045126
 */
object RenderingConstants {
    /**
     * Text skew X applied in editor
     */
    const val TEXT_SKEW_X = -0.2f

    /**
     * Edge radius multiplier for editor round rectangles
     */
    const val ROUND_RECT_FACTOR = 0.13f

    /**
     * Edge radius multiplier for editor bubbles
     */
    const val ROUND_BUBBLE_FACTOR = 0.5f

    /**
     * Shadow radius maximum for pinned line number, in dp unit
     */
    const val DIVIDER_SHADOW_MAX_RADIUS_DIP = 8f

    /**
     * Circle radius multiplier for non-printable character placeholders
     */
    const val NON_PRINTABLE_CIRCLE_RADIUS_FACTOR = 0.125f

    /**
     * Width for scrollbars, in dp unit
     */
    const val SCROLLBAR_WIDTH_DIP = 10f

    /**
     * Min length for scrollbars, in dp unit
     */
    const val SCROLLBAR_LENGTH_MIN_DIP = 60f

    /**
     * Underline width multiplier for matching delimiters
     */
    const val MATCHING_DELIMITERS_UNDERLINE_WIDTH_FACTOR = 0.1f

    /**
     * Underline width multiplier for normal texts
     */
    const val TEXT_UNDERLINE_WIDTH_FACTOR = 0.06f
}
