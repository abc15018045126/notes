package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.TextRow
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Wordwrap layout for editor
 *
 * This layout will not let character displayed outside the editor's width
 *
 * However, using this can be power-costing because we will have to recreate this layout in various
 * conditions, such as when the line number increases and its width grows or when the text size has changed
 *
 * @author Rose
 */
class WordwrapLayout(
    editor: CodeEditor,
    text: Content?,
    private val antiWordBreaking: Boolean,
    private val supportRtlRow: Boolean,
    oldLayout: WordwrapLayout?,
    clearCache: Boolean
) : AbstractLayout(editor, text) {

    private val width: Int
    private val miniGraphWidth: Float
    private var rowTable: MutableList<RowRegion>?

    init {
        rowTable = oldLayout?.rowTable ?: mutableListOf()
        if (clearCache) {
            rowTable?.clear()
        }
        miniGraphWidth = if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
            editor.renderer.miniGraphWidth
        } else {
            0f
        }
        width = (editor.width - editor.measureTextRegionOffset() - editor.extraMarginRight - miniGraphWidth * 2).toInt()
        breakAllLines()
    }

    private fun breakAllLines() {
        val text = this.text ?: return
        val editor = this.editor ?: return
        val taskCount = min(SUBTASK_COUNT, ceil(text.lineCount.toFloat() / MIN_LINE_COUNT_FOR_SUBTASK).toInt())
        val sizeEachTask = text.lineCount / taskCount
        val monitor = TaskMonitor(taskCount, object : TaskMonitor.Callback {
            override fun onCompleted(results: Array<Any?>, cancelledCount: Int) {
                val currentEditor = this@WordwrapLayout.editor
                if (currentEditor != null) {
                    val r2 = results.filterIsInstance<WordwrapResult>().sorted()
                    currentEditor.postInLifecycle {
                        if (this@WordwrapLayout.editor !== currentEditor) {
                            return@postInLifecycle
                        }
                        val rt = rowTable ?: mutableListOf<RowRegion>().also { rowTable = it }
                        rt.clear()
                        for (result in r2) {
                            rt.addAll(result.regions)
                        }
                        updateYOffsets(0)
                        currentEditor.setLayoutBusy(false)
                        currentEditor.eventHandler.scrollBy(0f, 0f)
                    }
                }
            }
        })
        editor.setLayoutBusy(true)
        for (i in 0 until taskCount) {
            val start = sizeEachTask * i
            val end = if (i + 1 == taskCount) text.lineCount - 1 else sizeEachTask * (i + 1) - 1
            submitTask(WordwrapAnalyzeTask(monitor, i, start, end))
        }
    }

    private fun findRow(line: Int): Int {
        val rt = rowTable ?: return 0
        var left = 0
        var right = rt.size - 1
        while (left <= right) {
            val mid = (left + right) / 2
            val value = rt[mid].line
            if (value < line) {
                left = mid + 1
            } else if (value > line) {
                right = mid - 1
            } else {
                left = mid
                break
            }
        }
        var index = min(max(0, left), rt.size - 1)
        if (index < 0) return 0
        while (index > 0 && rt[index].startColumn > 0) {
            index--
        }
        return index
    }

    fun findRow(line: Int, column: Int): Int {
        val rt = rowTable ?: return 0
        var row = findRow(line)
        while (row + 1 < rt.size && rt[row].endColumn <= column && rt[row + 1].line == line) {
            row++
        }
        return row
    }

    private fun breakLines(startLine: Int, endLine: Int) {
        val rt = rowTable ?: return
        var insertPosition = 0
        while (insertPosition < rt.size) {
            if (rt[insertPosition].line < startLine) {
                insertPosition++
            } else {
                break
            }
        }
        while (insertPosition < rt.size) {
            val line = rt[insertPosition].line
            if (line in startLine..endLine) {
                rt.removeAt(insertPosition)
            } else {
                break
            }
        }
        val newRegions = mutableListOf<RowRegion>()
        for (i in startLine..endLine) {
            text?.getLine(i)?.let { line ->
                newRegions.addAll(breakLine(i, line, null))
            }
        }
        rt.addAll(insertPosition, newRegions)
        updateYOffsets(insertPosition)
    }

    private fun updateYOffsets(startRow: Int) {
        val rt = rowTable ?: return
        if (rt.isEmpty()) return
        var y = if (startRow > 0) rt[startRow - 1].let { it.yOffset + it.height } else 0
        for (i in startRow until rt.size) {
            val region = rt[i]
            region.yOffset = y
            y += region.height
        }
    }

    private fun breakLine(line: Int, sequence: ContentLine, paint: Paint?): List<RowRegion> {
        val editor = this.editor ?: return emptyList()
        val p = paint ?: Paint(editor.isRenderFunctionCharacters).apply {
            set(editor.textPaint)
        }
        val tr = TextRow()
        val directions = text?.getLineDirections(line) ?: return emptyList()
        tr.set(
            sequence,
            0,
            sequence.length,
            S_SPANS_FOR_WORDWRAP,
            getInlayHints(line),
            directions,
            p,
            null,
            editor.renderer.createTextRowParams()
        )

        var isRtlBased = false
        if (supportRtlRow && sequence.mayNeedBidi()) {
            var minRunLevel = Int.MAX_VALUE
            for (i in 0 until directions.runCount) {
                minRunLevel = min(minRunLevel, directions.getRunLevel(i))
            }
            if ((minRunLevel and 1) != 0) {
                isRtlBased = true
            }
        }

        val rows = tr.breakText(width, antiWordBreaking)
        val results = ArrayList<RowRegion>()
        for (i in rows.indices) {
            val row = rows[i]
            val isTrailing = i == rows.size - 1
            val h = if (isTrailing) editor.logicalRowHeight else editor.wrapRowHeight
            results.add(
                RowRegion(
                    line,
                    row.startColumn,
                    row.endColumn,
                    row.inlayHints,
                    row.rowWidth,
                    isRtlBased,
                    h
                )
            )
        }
        return results
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        val rt = rowTable ?: return
        val delta = endLine - startLine
        if (delta != 0) {
            for (row in findRow(startLine + 1) until rt.size) {
                rt[row].line += delta
            }
        }
        breakLines(startLine, endLine)
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        val rt = rowTable ?: return
        val delta = endLine - startLine
        if (delta != 0) {
            var startRow = findRow(startLine)
            while (startRow < rt.size) {
                val line = rt[startRow].line
                if (line in startLine..endLine) {
                    rt.removeAt(startRow)
                } else {
                    break
                }
            }
            for (row in findRow(startLine) until rt.size) {
                val region = rt[row]
                if (region.line >= endLine) {
                    region.line -= delta
                }
            }
        }
        breakLines(startLine, startLine)
    }

    override fun destroyLayout() {
        super.destroyLayout()
        rowTable = null
    }

    override fun getRowAt(rowIndex: Int): Row {
        val rt = rowTable ?: return Row().apply {
            lineIndex = rowIndex
            isLeadingRow = true
            isTrailingRow = true
            endColumn = text?.getColumnCount(rowIndex) ?: 0
            inlayHints = getInlayHints(rowIndex)
        }
        if (rt.isEmpty()) {
            return Row().apply {
                lineIndex = rowIndex
                isLeadingRow = true
                isTrailingRow = true
                endColumn = text?.getColumnCount(rowIndex) ?: 0
                inlayHints = getInlayHints(rowIndex)
            }
        }
        val region = rt[rowIndex]
        val isLeadingRow = rowIndex <= 0 || rt[rowIndex - 1].line != region.line
        val isTrailingRow = rowIndex + 1 >= rt.size || rt[rowIndex + 1].line != region.line
        return region.toRow(isLeadingRow, isTrailingRow, width.toFloat())
    }

    override fun getLineNumberForRow(row: Int): Int {
        val rt = rowTable ?: return max(0, min(row, (text?.lineCount ?: 1) - 1))
        if (rt.isEmpty()) {
            return max(0, min(row, (text?.lineCount ?: 1) - 1))
        }
        return if (row >= rt.size) rt.last().line else rt[row].line
    }

    override fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator {
        val rt = rowTable
        return if (rt == null || rt.isEmpty()) {
            LineBreakLayout.LineBreakLayoutRowItr(this, text!!, initialRow, preloadedLines)
        } else {
            WordwrapLayoutRowItr(initialRow)
        }
    }

    override fun getUpPosition(line: Int, column: Int): Long {
        val rt = rowTable
        if (rt == null || rt.isEmpty()) {
            if (line - 1 < 0) return IntPair.pack(0, 0)
            val cColumn = text?.getColumnCount(line - 1) ?: 0
            return IntPair.pack(line - 1, if (column > cColumn) cColumn else column)
        }
        val row = findRow(line, column)
        if (row > 0) {
            val offset = column - rt[row].startColumn
            val lastRow = rt[row - 1]
            val maxOffset = lastRow.endColumn - lastRow.startColumn
            return IntPair.pack(lastRow.line, lastRow.startColumn + min(offset, maxOffset))
        }
        return IntPair.pack(0, 0)
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val rt = rowTable
        if (rt == null || rt.isEmpty()) {
            val cLine = text?.lineCount ?: 1
            if (line + 1 >= cLine) return IntPair.pack(line, text?.getColumnCount(line) ?: 0)
            val cColumn = text?.getColumnCount(line + 1) ?: 0
            return IntPair.pack(line + 1, if (column > cColumn) cColumn else column)
        }
        val row = findRow(line, column)
        if (row + 1 < rt.size) {
            val offset = column - rt[row].startColumn
            val nextRow = rt[row + 1]
            val maxOffset = nextRow.endColumn - nextRow.startColumn
            return IntPair.pack(nextRow.line, nextRow.startColumn + min(offset, maxOffset))
        }
        return IntPair.pack(line, text?.getColumnCount(line) ?: 0)
    }

    override val layoutWidth: Int
        get() = 0

    override val layoutHeight: Int
        get() {
            val rt = rowTable
            if (rt == null || rt.isEmpty()) {
                return (editor?.logicalRowHeight ?: 0) * (text?.lineCount ?: 0)
            }
            val last = rt.last()
            return last.yOffset + last.height
        }

    override fun getRowTop(row: Int): Int {
        val rt = rowTable
        if (rt == null || rt.isEmpty()) return row * (editor?.logicalRowHeight ?: 0)
        return rt[row].yOffset
    }

    override fun getRowBottom(row: Int): Int {
        val rt = rowTable
        if (rt == null || rt.isEmpty()) return (row + 1) * (editor?.logicalRowHeight ?: 0)
        val region = rt[row]
        return region.yOffset + region.height
    }

    override fun getRowIndexForY(y: Float): Int {
        val rt = rowTable ?: return (y / (editor?.logicalRowHeight ?: 1)).toInt()
        if (rt.isEmpty()) return (y / (editor?.logicalRowHeight ?: 1)).toInt()
        var left = 0
        var right = rt.size - 1
        while (left <= right) {
            val mid = (left + right) / 2
            val region = rt[mid]
            if (y < region.yOffset) {
                right = mid - 1
            } else if (y >= region.yOffset + region.height) {
                left = mid + 1
            } else {
                return mid
            }
        }
        return max(0, min(rt.size - 1, left))
    }

    override fun getRowIndexForPosition(index: Int): Int {
        val editor = this.editor ?: return 0
        val pos = editor.text.indexer.getCharPosition(index)
        val line = pos.line
        val rt = rowTable ?: return line
        if (rt.isEmpty()) return line
        val column = pos.column
        var row = findRow(line)
        if (row < rt.size) {
            var region = rt[row]
            if (region.line != line) return 0
            while (region.startColumn < column && row + 1 < rt.size) {
                row++
                region = rt[row]
                if (region.line != line || region.startColumn > column) {
                    row--
                    break
                }
            }
            return row
        }
        return 0
    }

    override fun invalidateLines(range: StyleUpdateRange) {
        val text = this.text ?: return
        val itr = range.lineIndexIterator(text.lineCount - 1)
        while (itr.hasNext()) {
            val line = itr.nextInt()
            breakLines(line, line)
        }
    }

    override fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long {
        val editor = this.editor ?: return 0
        val rt = rowTable
        if (rt == null || rt.isEmpty()) {
            val text = this.text ?: return 0
            val lineCount = text.lineCount
            val line = min(lineCount - 1, max((yOffset / editor.rowHeight).toInt(), 0))
            val tr = editor.renderer.createTextRow(line)
            return IntPair.pack(line, tr.getIndexForCursorOffset(xOffset))
        }
        var row = getRowIndexForY(yOffset)
        row = max(0, min(row, rt.size - 1))
        val region = rt[row]
        var x = xOffset
        if (region.startColumn != 0) {
            x -= miniGraphWidth
        }
        x -= region.getRenderTranslateX(width.toFloat())
        val tr = editor.renderer.createTextRow(row)
        return IntPair.pack(region.line, tr.getIndexForCursorOffset(x))
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        var dest = array ?: FloatArray(2)
        val editor = this.editor ?: return dest
        val rt = rowTable
        if (rt == null || rt.isEmpty()) {
            dest[0] = editor.getRowBottom(line).toFloat()
            val tr = editor.renderer.createTextRow(line)
            dest[1] = tr.getCursorOffsetForIndex(column)
            return dest
        }
        var row = findRow(line)
        if (row < rt.size) {
            var region = rt[row]
            if (region.line != line) {
                dest[1] = 0f
                dest[0] = 0f
                return dest
            }
            while (region.startColumn < column && row + 1 < rt.size) {
                row++
                region = rt[row]
                if (region.line != line || region.startColumn > column) {
                    row--
                    region = rt[row]
                    break
                }
            }
            dest[0] = editor.getRowBottom(row).toFloat()
            val tr = editor.renderer.createTextRow(row)
            dest[1] = tr.getCursorOffsetForIndex(column)
            if (region.startColumn != 0) {
                dest[1] += miniGraphWidth
            }
            dest[1] += region.getRenderTranslateX(width.toFloat())
        } else {
            dest[1] = 0f
            dest[0] = 0f
        }
        return dest
    }

    override fun getRowCountForLine(line: Int): Int {
        val rt = rowTable ?: return 1
        if (rt.isEmpty()) return 1
        var row = findRow(line)
        var count = 0
        while (row < rt.size && rt[row].line == line) {
            count++
            row++
        }
        return count
    }

    fun getSoftBreaksForLine(line: Int): List<Int> {
        val rt = rowTable ?: return emptyList()
        if (rt.isEmpty()) return emptyList()
        var row = findRow(line)
        val list = mutableListOf<Int>()
        while (row < rt.size && rt[row].line == line) {
            val column = rt[row].startColumn
            if (column != 0) {
                list.add(column)
            }
            row++
        }
        return list
    }

    override val rowCount: Int
        get() {
            val rt = rowTable
            return if (rt == null || rt.isEmpty()) {
                text?.lineCount ?: 0
            } else {
                rt.size
            }
        }

    class RowRegion(
        var line: Int,
        val startColumn: Int,
        val endColumn: Int,
        var inlayHints: List<InlayHint>?,
        var rowWidth: Float,
        var displayFromRight: Boolean,
        var height: Int
    ) {
        var yOffset: Int = 0

        fun toRow(isLeadingRow: Boolean, isTrailingRow: Boolean, layoutWidth: Float): Row {
            return Row().apply {
                this.isLeadingRow = isLeadingRow
                this.isTrailingRow = isTrailingRow
                this.startColumn = this@RowRegion.startColumn
                this.endColumn = this@RowRegion.endColumn
                this.lineIndex = this@RowRegion.line
                this.inlayHints = this@RowRegion.inlayHints ?: emptyList()
                this.renderTranslateX = getRenderTranslateX(layoutWidth)
            }
        }

        fun getRenderTranslateX(layoutWidth: Float): Float {
            return if (displayFromRight && layoutWidth > rowWidth) layoutWidth - rowWidth else 0f
        }

        override fun toString(): String {
            return "RowRegion(startColumn=$startColumn, endColumn=$endColumn, line=$line)"
        }
    }

    private class WordwrapResult(val index: Int, val regions: List<RowRegion>) : Comparable<WordwrapResult> {
        override fun compareTo(other: WordwrapResult): Int {
            return index.compareTo(other.index)
        }
    }

    inner class WordwrapLayoutRowItr(initialRow: Int) : RowIterator {
        private val result = Row()
        private val initRow = initialRow
        private var currentRow = initialRow

        override fun next(): Row {
            val rt = rowTable ?: throw NoSuchElementException()
            if (!hasNext()) throw NoSuchElementException()
            val region = rt[currentRow]
            result.apply {
                lineIndex = region.line
                startColumn = region.startColumn
                endColumn = region.endColumn
                inlayHints = region.inlayHints ?: emptyList()
                isLeadingRow = currentRow <= 0 || rt[currentRow - 1].line != region.line
                isTrailingRow = currentRow + 1 >= rt.size || rt[currentRow + 1].line != region.line
                renderTranslateX = region.getRenderTranslateX(width.toFloat())
            }
            currentRow++
            return result
        }

        override fun hasNext(): Boolean {
            val rt = rowTable ?: return false
            return currentRow in 0 until rt.size
        }

        override fun reset() {
            currentRow = initRow
        }
    }

    private inner class WordwrapAnalyzeTask(monitor: TaskMonitor, val id: Int, val start: Int, val end: Int) :
        LayoutTask<WordwrapResult>(monitor) {
        private val paint: Paint = Paint(editor?.isRenderFunctionCharacters ?: false).apply {
            set(editor?.textPaint)
            onAttributeUpdate()
        }

        override fun compute(): WordwrapResult {
            val list = mutableListOf<RowRegion>()
            text?.runReadActionsOnLines(start, end, object : Content.ContentLineConsumer2 {
                override fun accept(index: Int, line: ContentLine, abortFlag: Content.ContentLineConsumer2.AbortFlag) {
                    list.addAll(breakLine(index, line, paint))
                    if (!shouldRun()) {
                        abortFlag.set = true
                    }
                }
            })
            return WordwrapResult(id, list)
        }
    }

    companion object {
        private val S_SPANS_FOR_WORDWRAP: List<Span> = listOf(
            SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 0, true, true, false))
        )
    }
}
