package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair

/**
 * This a data class of a character position in {@link Content}
 *
 * @author abc15018045126
 */
class CharPosition @JvmOverloads constructor(
    /**
     * Get line
     *
     * @return line
     */
    @JvmField var line: Int = 0,
    /**
     * Get column
     *
     * @return column
     */
    @JvmField var column: Int = 0,
    /**
     * Get the index
     *
     * @return index
     */
    @JvmField var index: Int = -1
) {

    fun getLine(): Int = line
    fun getColumn(): Int = column
    fun getIndex(): Int = index

    /**
     * Make this CharPosition zero and return self
     *
     * @return self
     */
    fun toBOF(): CharPosition {
        index = 0
        line = 0
        column = 0
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other is CharPosition) {
            return other.column == column &&
                    other.line == line &&
                    other.index == index
        }
        return false
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + line
        result = 31 * result + column
        return result
    }

    /**
     * Convert {@link CharPosition#line} and {@link CharPosition#column} to a Long number
     * <p>
     * First integer is line and second integer is column
     *
     * @return A Long integer describing the position
     */
    fun toIntPair(): Long {
        return IntPair.pack(line, column)
    }

    /**
     * Make a copy of this CharPosition and return the copy
     *
     * @return New CharPosition including info of this CharPosition
     */
    fun fromThis(): CharPosition {
        val pos = CharPosition()
        pos.set(this)
        return pos
    }

    /**
     * Set this {@link CharPosition} object's data the same as {@code another}
     */
    fun set(another: CharPosition) {
        index = another.index
        line = another.line
        column = another.column
    }

    override fun toString(): String {
        return "CharPosition(line = $line,column = $column,index = $index)"
    }
}
