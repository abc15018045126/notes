package io.github.abc15018045126.sora.text

/**
 * A helper class for ITextContent to transform (line,column) and index
 *
 * @author Rose
 */
interface Indexer {

    /**
     * Get the index of (line,column)
     *
     * @param line   The line position of index
     * @param column The column position of index
     * @return Calculated index
     */
    fun getCharIndex(line: Int, column: Int): Int

    /**
     * Get the line position of index
     *
     * @param index The index you want to know its line
     * @return Line position of index
     */
    fun getCharLine(index: Int): Int

    /**
     * Get the column position of index
     *
     * @param index The index you want to know its column
     * @return Column position of index
     */
    fun getCharColumn(index: Int): Int

    /**
     * Get the CharPosition for the given index
     *
     * @param index The index you want to get
     * @return The CharPosition object.
     */
    fun getCharPosition(index: Int): CharPosition

    /**
     * Get the CharPosition for the given (line,column)
     *
     * @param line   The line position you want to get
     * @param column The column position you want to get
     * @return The CharPosition object.
     */
    fun getCharPosition(line: Int, column: Int): CharPosition

    /**
     * @param dest Destination of result
     * @see #getCharPosition(int)
     */
    fun getCharPosition(index: Int, dest: CharPosition)

    /**
     * @param dest Destination of result
     * @see #getCharPosition(int, int)
     */
    fun getCharPosition(line: Int, column: Int, dest: CharPosition)
}
