package io.github.abc15018045126.sora.text

/**
 * A range made up of two {@link CharPosition} objects.
 *
 * @author abc15018045126
 */
class TextRange(
    @JvmField var start: CharPosition,
    @JvmField var end: CharPosition
) {

    fun getStart(): CharPosition {
        return start
    }

    fun setStart(start: CharPosition) {
        this.start = start
    }

    fun getEnd(): CharPosition {
        return end
    }

    fun setEnd(end: CharPosition) {
        this.end = end
    }

    val startIndex: Int
        get() = start.index

    val endIndex: Int
        get() = end.index

    /**
     * Check if the given position is inside the range
     */
    fun isPositionInside(pos: CharPosition): Boolean {
        return pos.index >= start.index && pos.index < end.index
    }

    override fun toString(): String {
        return "TextRange{" +
                "start=" + start +
                ", end=" + end +
                '}'
    }
}
