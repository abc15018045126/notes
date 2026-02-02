package io.github.abc15018045126.sora.lang.styling

import androidx.annotation.NonNull
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import java.util.*

/**
 * Store spans by mapping.
 *
 * @see Builder
 */
class MappedSpans private constructor(@NonNull private val spanMap: MutableList<MutableList<Span>>) : Spans {

    override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineInsert(spanMap, startLine, startColumn, endColumn)
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineInsert(spanMap, startLine, startColumn, endLine, endColumn)
        }
    }

    override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineDelete(spanMap, startLine, startColumn, endColumn)
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineDelete(spanMap, startLine, startColumn, endLine, endColumn)
        }
    }

    override fun read(): Spans.Reader {
        return MappedSpansAccessor()
    }

    override fun supportsModify(): Boolean {
        return true
    }

    override fun modify(): Spans.Modifier {
        return MappedSpansAccessor()
    }

    override fun getLineCount(): Int {
        return spanMap.size
    }

    /**
     * Allow you to build a span map linearly.
     */
    class Builder @JvmOverloads constructor(lineCapacity: Int = 128) {
        private val spans: MutableList<MutableList<Span>> = ArrayList(lineCapacity)
        private var last: Span? = null

        /**
         * Add a new span if required.
         *
         * If no special style is specified, you can use colorId as style long integer
         *
         * @param spanLine Line
         * @param column   Column
         * @param style    Style of text
         */
        fun addIfNeeded(spanLine: Int, column: Int, style: Long) {
            val currentLast = last
            if (currentLast != null && currentLast.style == style) {
                return
            }
            add(spanLine, SpanFactory.obtainNoExt(column, style))
        }

        /**
         * Add a span directly
         *
         * Note: the line should always >= the line of span last committed
         *
         * If two spans are on the same line, you must add them in order by their column
         *
         * @param spanLine The line position of span
         * @param span     The span
         */
        fun add(spanLine: Int, span: Span) {
            var mapLine = spans.size - 1
            if (spanLine == mapLine) {
                spans[spanLine].add(span)
            } else if (spanLine > mapLine) {
                var extendedSpan = last
                if (extendedSpan == null) {
                    extendedSpan = SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
                }
                while (mapLine < spanLine) {
                    val lineSpans = mutableListOf<Span>()
                    lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                    spans.add(lineSpans)
                    mapLine++
                }
                val lineSpans = spans[spanLine]
                if (span.column == 0) {
                    lineSpans.clear()
                }
                lineSpans.add(span)
            } else {
                throw IllegalStateException("Invalid position")
            }
            last = span
        }

        /**
         * This method must be called when whole text is analyzed.
         * **Note that it is not the line count but line index!**
         *
         * @param line The line is the line last of text
         */
        fun determine(line: Int) {
            var mapLine = spans.size - 1
            var extendedSpan = last
            if (extendedSpan == null) {
                extendedSpan = SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
            }
            while (mapLine < line) {
                val lineSpans = mutableListOf<Span>()
                lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                spans.add(lineSpans)
                mapLine++
            }
        }

        /**
         * Ensure the list not empty
         */
        fun addNormalIfNull() {
            if (spans.isEmpty()) {
                val spanList = mutableListOf<Span>()
                spanList.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
                spans.add(spanList)
            }
        }

        fun build(): MappedSpans {
            return MappedSpans(spans)
        }
    }

    private inner class MappedSpansAccessor : Spans.Reader, Spans.Modifier {
        private var span: MutableList<Span>? = null

        private fun checkLine() {
            if (span == null) {
                throw IllegalStateException("line must be set first")
            }
        }

        override fun moveToLine(line: Int) {
            if (line == -1) {
                span = null
                return
            }
            span = spanMap[line]
        }

        override fun getSpanCount(): Int {
            checkLine()
            return span!!.size
        }

        override fun getSpanAt(index: Int): Span {
            checkLine()
            return span!![index]
        }

        override fun getSpansOnLine(line: Int): List<Span> {
            return Collections.unmodifiableList(spanMap[line])
        }

        override fun setSpansOnLine(line: Int, spans: List<Span>) {
            val lastLine = spanMap[spanMap.size - 1]
            val extend = lastLine[lastLine.size - 1]
            while (spanMap.size <= line) {
                val list = mutableListOf<Span>()
                list.add(copyAndSetColumn(extend, 0))
                spanMap.add(list)
            }
            spanMap[line] = ArrayList(spans)
        }

        override fun addLineAt(line: Int, spans: List<Span>) {
            spanMap.add(line, ArrayList(spans))
        }

        override fun deleteLineAt(line: Int) {
            spanMap.removeAt(line)
        }
    }

    companion object {
        @JvmStatic
        private fun copyAndSetColumn(s: Span, column: Int): Span {
            val span = s.copy()
            span.column = column
            return span
        }
    }
}
