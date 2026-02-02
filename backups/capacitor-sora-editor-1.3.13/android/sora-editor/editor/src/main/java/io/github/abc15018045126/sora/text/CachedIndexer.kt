package io.github.abc15018045126.sora.text

import androidx.annotation.VisibleForTesting
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max

/**
 * Indexer Impl for Content with cache.
 *
 * Range Space of line:
 * [0, columnCount)                            -> Text Content on Line
 * [columnCount, columnCount + lineSepLength)  -> Line Separator for Line
 *
 * Merged Range: [0, columnCount + lineSepLength)
 *
 * Specially, the text end position is valid but not actually readable.
 *
 * @author Rose
 */
class CachedIndexer internal constructor(private val content: Content) : Indexer, ContentListener {
    private val startPosition = CharPosition().toBOF()
    private val endPosition = CharPosition()
    private val cachedPositions = ArrayList<CharPosition>()
    private val thresholdLine = 50
    private var thresholdIndex = 50
    var maxCacheCount = 50
        set(maxSize) {
            field = maxSize
        }

    init {
        updateEnd()
    }

    /**
     * If the querying index is larger than the switch
     * We will add its result to cache
     *
     * @param s Switch
     */
    fun setThresholdIndex(s: Int) {
        thresholdIndex = s
    }

    /**
     * Update the end position
     */
    private fun updateEnd() {
        endPosition.index = content.length
        endPosition.line = content.lineCount - 1
        endPosition.column = content.getColumnCount(endPosition.line)
    }

    /**
     * Get the nearest cache for the given index
     *
     * @param index Querying index
     * @return Nearest cache
     */
    @Synchronized
    private fun findNearestByIndex(index: Int): CharPosition {
        var minDistance = index
        var nearestCharPosition = startPosition
        var targetIndex = 0
        for (i in cachedPositions.indices) {
            val pos = cachedPositions[i]
            val dis = abs(pos.index - index)
            if (dis < minDistance) {
                minDistance = dis
                nearestCharPosition = pos
                targetIndex = i
            }
            if (dis <= thresholdIndex) {
                break
            }
        }
        if (abs(endPosition.index - index) < minDistance) {
            nearestCharPosition = endPosition
        }
        if (nearestCharPosition !== startPosition && nearestCharPosition !== endPosition) {
            Collections.swap(cachedPositions, targetIndex, cachedPositions.size - 1)
        }
        return nearestCharPosition
    }

    /**
     * Get the nearest cache for the given line
     *
     * @param line Querying line
     * @return Nearest cache
     */
    @Synchronized
    private fun findNearestByLine(line: Int): CharPosition {
        var minDistance = line
        var nearestCharPosition = startPosition
        var targetIndex = 0
        for (i in cachedPositions.indices) {
            val pos = cachedPositions[i]
            val dis = abs(pos.line - line)
            if (dis < minDistance) {
                minDistance = dis
                nearestCharPosition = pos
                targetIndex = i
            }
            if (minDistance <= thresholdLine) {
                break
            }
        }
        if (abs(endPosition.line - line) < minDistance) {
            nearestCharPosition = endPosition
        }
        if (nearestCharPosition !== startPosition && nearestCharPosition !== endPosition) {
            Collections.swap(cachedPositions, targetIndex, cachedPositions.size - 1)
        }
        return nearestCharPosition
    }

    /**
     * From the given position to find forward in text
     *
     * @param start Given position
     * @param index Querying index
     */
    @VisibleForTesting
    fun findIndexForward(start: CharPosition, index: Int, dest: CharPosition) {
        if (start.index > index) {
            throw IllegalArgumentException("Unable to find backward from method findIndexForward()")
        }
        var workLine = start.line
        var workColumn = start.column
        var workIndex = start.index
        //Move the column to the line end
        run {
            val addition = max(content.getLineSeparatorUnsafe(workLine).getLength() - 1, 0)
            val column = content.getColumnCountUnsafe(workLine) + addition
            workIndex += column - workColumn
            workColumn = column
        }
        while (workIndex < index) {
            workLine++
            val line = content.getLineUnsafe(workLine)
            val addition = max(line.lineSeparator.getLength() - 1, 0)
            workColumn = line.length + addition
            workIndex += workColumn + 1
        }
        if (workIndex > index) {
            workColumn -= workIndex - index
        }
        dest.column = workColumn
        dest.line = workLine
        dest.index = index
    }

    /**
     * From the given position to find backward in text
     *
     * @param start Given position
     * @param index Querying index
     */
    @VisibleForTesting
    fun findIndexBackward(start: CharPosition, index: Int, dest: CharPosition) {
        if (start.index < index) {
            throw IllegalArgumentException("Unable to find forward from method findIndexBackward()")
        }
        var workLine = start.line
        var workColumn = start.column
        var workIndex = start.index
        while (workIndex > index) {
            workIndex -= workColumn + 1
            workLine--
            if (workLine != -1) {
                val line = content.getLineUnsafe(workLine)
                val addition = max(line.lineSeparator.getLength() - 1, 0)
                workColumn = line.length + addition
            } else {
                // Reached the start of text,we have to use findIndexForward() as this method can not handle it
                findIndexForward(startPosition, index, dest)
                return
            }
        }
        val dColumn = index - workIndex
        if (dColumn > 0) {
            workLine++
            workColumn = dColumn - 1
        }
        dest.column = workColumn
        dest.line = workLine
        dest.index = index
    }

    /**
     * From the given position to find forward in text
     *
     * @param start  Given position
     * @param line   Querying line
     * @param column Querying column
     */
    @VisibleForTesting
    fun findLiCoForward(start: CharPosition, line: Int, column: Int, dest: CharPosition) {
        if (start.line > line) {
            throw IllegalArgumentException("can not find backward from findLiCoForward()")
        }
        var workLine = start.line
        var workIndex = start.index
        run {
            //Make index to left of line
            workIndex = workIndex - start.column
        }
        while (workLine < line) {
            val lineObj = content.getLineUnsafe(workLine)
            workIndex += lineObj.length + lineObj.lineSeparator.getLength()
            workLine++
        }
        dest.column = 0
        dest.line = workLine
        dest.index = workIndex
        findInLine(dest, line, column)
    }

    /**
     * From the given position to find backward in text
     *
     * @param start  Given position
     * @param line   Querying line
     * @param column Querying column
     */
    @VisibleForTesting
    fun findLiCoBackward(start: CharPosition, line: Int, column: Int, dest: CharPosition) {
        if (start.line < line) {
            throw IllegalArgumentException("can not find forward from findLiCoBackward()")
        }
        var workLine = start.line
        var workIndex = start.index
        run {
            //Make index to the left of line
            workIndex = workIndex - start.column
        }
        while (workLine > line) {
            val lineObj = content.getLineUnsafe(workLine - 1)
            workIndex -= lineObj.length + lineObj.lineSeparator.getLength()
            workLine--
        }
        dest.column = 0
        dest.line = workLine
        dest.index = workIndex
        findInLine(dest, line, column)
    }

    /**
     * From the given position to find in this line
     *
     * @param pos    Given position
     * @param line   Querying line
     * @param column Querying column
     */
    private fun findInLine(pos: CharPosition, line: Int, column: Int) {
        if (pos.line != line) {
            throw IllegalArgumentException("can not find other lines with findInLine()")
        }
        pos.index = pos.index - pos.column + column
        pos.column = column
    }

    /**
     * Add new cache
     *
     * @param pos New cache
     */
    @Synchronized
    private fun push(pos: CharPosition) {
        if (maxCacheCount <= 0) {
            return
        }
        cachedPositions.add(pos)
        if (cachedPositions.size > maxCacheCount) {
            cachedPositions.removeAt(0)
        }
    }

    override fun getCharIndex(line: Int, column: Int): Int {
        return getCharPosition(line, column).index
    }

    override fun getCharLine(index: Int): Int {
        return getCharPosition(index).line
    }

    override fun getCharColumn(index: Int): Int {
        return getCharPosition(index).column
    }

    override fun getCharPosition(index: Int): CharPosition {
        val pos = CharPosition()
        getCharPosition(index, pos)
        return pos
    }

    override fun getCharPosition(index: Int, dest: CharPosition) {
        content.checkIndex(index, Content.CHECK_TYPE_INDEX)
        content.lock(false)
        try {
            val pos = findNearestByIndex(index)
            if (pos.index == index) {
                dest.set(pos)
            } else if (pos.index < index) {
                findIndexForward(pos, index, dest)
            } else {
                findIndexBackward(pos, index, dest)
            }
            if (abs(index - pos.index) >= thresholdIndex) {
                push(dest.fromThis())
            }
        } finally {
            content.unlock(false)
        }
    }

    override fun getCharPosition(line: Int, column: Int): CharPosition {
        val pos = CharPosition()
        getCharPosition(line, column, pos)
        return pos
    }

    override fun getCharPosition(line: Int, column: Int, dest: CharPosition) {
        content.checkLineAndColumn(line, column, Content.CHECK_TYPE_INDEX)
        content.lock(false)
        try {
            val pos = findNearestByLine(line)
            if (pos.line == line) {
                dest.set(pos)
                if (pos.column == column) {
                    return
                }
                findInLine(dest, line, column)
            } else if (pos.line < line) {
                findLiCoForward(pos, line, column, dest)
            } else {
                findLiCoBackward(pos, line, column, dest)
            }
            if (abs(pos.line - line) > thresholdLine) {
                push(dest.fromThis())
            }
        } finally {
            content.unlock(false)
        }
    }

    @UnsupportedUserUsage
    override fun beforeReplace(content: Content) {
        //Do nothing
    }

    @Synchronized
    @UnsupportedUserUsage
    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        for (pos in cachedPositions) {
            if (pos.line == startLine) {
                if (pos.column >= startColumn) {
                    pos.index += insertedContent.length
                    pos.line += endLine - startLine
                    pos.column = endColumn + pos.column - startColumn
                }
            } else if (pos.line > startLine) {
                pos.index += insertedContent.length
                pos.line += endLine - startLine
            }
        }
        updateEnd()
    }

    @Synchronized
    @UnsupportedUserUsage
    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        val garbage: MutableList<CharPosition> = ArrayList()
        for (pos in cachedPositions) {
            if (pos.line == startLine) {
                if (pos.column >= startColumn) garbage.add(pos)
            } else if (pos.line > startLine) {
                if (pos.line < endLine) {
                    garbage.add(pos)
                } else if (pos.line == endLine) {
                    garbage.add(pos)
                } else {
                    pos.index -= deletedContent.length
                    pos.line -= endLine - startLine
                }
            }
        }
        cachedPositions.removeAll(garbage)
        updateEnd()
    }
}
