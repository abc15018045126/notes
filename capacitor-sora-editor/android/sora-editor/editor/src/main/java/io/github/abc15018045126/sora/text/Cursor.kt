package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair

/**
 * The [Cursor] position will update automatically when the content has been changed by other ways.
 *
 * @author abc15018045126
 */
class Cursor(private val content: Content) {

    private val indexer: CachedIndexer = CachedIndexer(content)
    private var leftSel: CharPosition = CharPosition().toBOF()
    private var rightSel: CharPosition = CharPosition().toBOF()
    private var cache0: CharPosition? = null
    private var cache1: CharPosition? = null
    private var cache2: CharPosition? = null

    private var selDirection = DIRECTION_NONE

    /**
     * Make left and right cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun set(line: Int, column: Int) {
        setLeft(line, column)
        setRight(line, column)
    }

    /**
     * Make left cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun setLeft(line: Int, column: Int) {
        leftSel = indexer.getCharPosition(line, column).fromThis()
    }

    /**
     * Make right cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun setRight(line: Int, column: Int) {
        rightSel = indexer.getCharPosition(line, column).fromThis()
    }

    /**
     * Get the left cursor line
     *
     * @return line of left cursor
     */
    val leftLine: Int
        get() = leftSel.line

    /**
     * Get the left cursor column
     *
     * @return column of left cursor
     */
    val leftColumn: Int
        get() = leftSel.column

    /**
     * Get the right cursor line
     *
     * @return line of right cursor
     */
    val rightLine: Int
        get() = rightSel.line

    /**
     * Get the right cursor column
     *
     * @return column of right cursor
     */
    val rightColumn: Int
        get() = rightSel.column

    /**
     * Whether the given position is in selected region
     *
     * @param line   The line to query
     * @param column The column to query
     * @return Whether is in selected region
     */
    fun isInSelectedRegion(line: Int, column: Int): Boolean {
        if (line in leftLine..rightLine) {
            var yes = true
            if (line == leftLine) {
                yes = column >= leftColumn
            }
            if (line == rightLine) {
                yes = yes && column < rightColumn
            }
            return yes
        }
        return false
    }

    /**
     * Get the left cursor index
     *
     * @return index of left cursor
     */
    val left: Int
        get() = leftSel.index

    /**
     * Get the right cursor index
     *
     * @return index of right cursor
     */
    val right: Int
        get() = rightSel.index

    /**
     * Notify the Indexer to update its cache for current display position
     *
     * This will make querying actions quicker
     *
     * Especially when the editor user want to set a new cursor position after scrolling long time
     *
     * @param line First visible line
     */
    fun updateCache(line: Int) {
        indexer.getCharIndex(line, 0)
    }

    /**
     * Get the using Indexer object
     *
     * @return Using Indexer
     */
    fun getIndexer(): CachedIndexer {
        return indexer
    }

    /**
     * Get whether text is selected
     *
     * @return Whether selected
     */
    fun isSelected(): Boolean {
        return leftSel.index != rightSel.index
    }

    /**
     * Set current direction of selection.
     */
    fun setSelectionDirection(selDirection: Int) {
        this.selDirection = selDirection
    }

    /**
     * Get current direction of selection
     */
    fun getSelectionDirection(): Int {
        return selDirection
    }

    /**
     * Get position after moving left once
     *
     * @param position A packed pair (line, column) describing the original position
     * @return A packed pair (line, column) describing the result position
     */
    fun getLeftOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val nColumn = TextLayoutHelper.get().getCurPosLeft(column, content.getLine(line))
        return if (nColumn == column && column == 0) {
            if (line == 0) {
                0L
            } else {
                val cColumn = content.getColumnCount(line - 1)
                IntPair.pack(line - 1, cColumn)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }

    /**
     * Get position after moving right once
     *
     * @param position A packed pair (line, column) describing the original position
     * @return A packed pair (line, column) describing the result position
     */
    fun getRightOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val cColumn = content.getColumnCount(line)
        val nColumn = TextLayoutHelper.get().getCurPosRight(column, content.getLine(line))
        return if (nColumn == cColumn && column == nColumn) {
            if (line + 1 == content.lineCount) {
                IntPair.pack(line, cColumn)
            } else {
                IntPair.pack(line + 1, 0)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }

    /**
     * Get copy of left cursor
     */
    fun left(): CharPosition {
        return leftSel.fromThis()
    }

    /**
     * Get copy of right cursor
     */
    fun right(): CharPosition {
        return rightSel.fromThis()
    }

    /**
     * Get current range of cursor. Modifications to the returned object does not affect cursor positions.
     *
     * @return [TextRange] object describing cursor positions
     */
    fun getRange(): TextRange {
        return TextRange(left(), right())
    }

    /**
     * Internal call back before insertion
     *
     * @param startLine   Start line
     * @param startColumn Start column
     */
    internal fun beforeInsert(startLine: Int, startColumn: Int) {
        cache0 = indexer.getCharPosition(startLine, startColumn).fromThis()
    }

    /**
     * Internal call back before deletion
     *
     * @param startLine   Start line
     * @param startColumn Start column
     * @param endLine     End line
     * @param endColumn   End column
     */
    internal fun beforeDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        cache1 = indexer.getCharPosition(startLine, startColumn).fromThis()
        cache2 = indexer.getCharPosition(endLine, endColumn).fromThis()
    }

    /**
     * Internal call back before replace
     */
    internal fun beforeReplace() {
        indexer.beforeReplace(content)
    }

    /**
     * Internal call back after insertion
     *
     * @param startLine       Start line
     * @param startColumn     Start column
     * @param endLine         End line
     * @param endColumn       End column
     * @param insertedContent Inserted content
     */
    internal fun afterInsert(
        startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        insertedContent: CharSequence
    ) {
        indexer.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        val beginIdx = cache0?.index ?: 0
        if (left >= beginIdx) {
            leftSel = indexer.getCharPosition(left + insertedContent.length).fromThis()
        }
        if (right >= beginIdx) {
            rightSel = indexer.getCharPosition(right + insertedContent.length).fromThis()
        }
    }

    /**
     * Internal call back
     *
     * @param startLine      Start line
     * @param startColumn    Start column
     * @param endLine        End line
     * @param endColumn      End column
     * @param deletedContent Deleted content
     */
    internal fun afterDelete(
        startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        deletedContent: CharSequence
    ) {
        indexer.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        val beginIdx = cache1?.index ?: 0
        val endIdx = cache2?.index ?: 0
        if (beginIdx > right) {
            return
        }
        val left = left - Math.max(0, Math.min(left - beginIdx, endIdx - beginIdx))
        val right = right - Math.max(0, Math.min(right - beginIdx, endIdx - beginIdx))
        leftSel = indexer.getCharPosition(left).fromThis()
        rightSel = indexer.getCharPosition(right).fromThis()
    }

    companion object {
        const val DIRECTION_NONE = 0
        const val DIRECTION_LTR = 1
        const val DIRECTION_RTL = 2
    }
}
