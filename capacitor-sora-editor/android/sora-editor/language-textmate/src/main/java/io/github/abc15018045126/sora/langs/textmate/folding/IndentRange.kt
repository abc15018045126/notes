package io.github.abc15018045126.sora.langs.textmate.folding

import io.github.abc15018045126.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.abc15018045126.sora.text.Content
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp

object IndentRange {
    const val MAX_LINE_NUMBER = 0xFFFFFF
    const val MAX_FOLDING_REGIONS = 0xFFFF
    const val MASK_INDENT = -0x1000000

    // START sora-editor note
    // Change String to char[] and int
    // END sora-editor note

    @JvmStatic
    fun computeStartColumn(line: CharArray, len: Int, tabSize: Int): Int {
        var column = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                column++
            } else if (chCode == '\t') {
                column += tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            // line only consists of whitespace
            return -1
        }

        return column
    }

    /**
     * @return :
     * - -1 => the line consists of whitespace
     * - otherwise => the indent level is returned value
     */
    @JvmStatic
    fun computeIndentLevel(line: CharArray, len: Int, tabSize: Int): Int {
        var indent = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                indent++
            } else if (chCode == '\t') {
                indent = indent - indent % tabSize + tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            // line only consists of whitespace
            return -1
        }

        return indent
    }

    @Throws(Exception::class)
    @JvmStatic
    fun computeRanges(
        model: Content,
        tabSize: Int,
        offSide: Boolean,
        helper: FoldingHelper,
        pattern: OnigRegExp?,
        delegate: AsyncIncrementalAnalyzeManager<*, *>.CodeBlockAnalyzeDelegate
    ): FoldingRegions {

        val result = RangesCollector(/*tabSize*/)

        val previousRegions = ArrayList<PreviousRegion>()
        var line = model.lineCount + 1
        // sentinel, to make sure there's at least one entry
        previousRegions.add(PreviousRegion(-1, line, line))

        line = model.lineCount - 1
        while (line >= 0 && delegate.isNotCancelled()) {
            val indent =
                helper.getIndentFor(line)//computeIndentLevel(model.getLine(line).getBackingCharArray(), model.getColumnCount(line), tabSize);
            var previous = previousRegions[previousRegions.size - 1]
            if (indent == -1) {
                if (offSide) {
                    // for offSide languages, empty lines are associated to the previous block
                    // note: the next block is already written to the results, so this only
                    // impacts the end position of the block before
                    previous.endAbove = line
                }
                line--
                continue // only whitespace
            }
            val m = if (pattern != null) helper.getResultFor(line) else null
            if (pattern != null && m != null) {
                // folding pattern match
                if (m.count() >= 2) { // start pattern match
                    // discard all regions until the folding pattern
                    var i = previousRegions.size - 1
                    while (i > 0 && previousRegions[i].indent != -2) {
                        i--
                    }
                    if (i > 0) {
                        //??? previousRegions.length = i + 1;
                        previous = previousRegions[i]

                        // new folding range from pattern, includes the end line
                        result.insertFirst(line, previous.line, indent)
                        previous.line = line
                        previous.indent = indent
                        previous.endAbove = line
                        line--
                        continue
                    } else {
                        // no end marker found, treat line as a regular line
                    }
                } else { // end pattern match
                    previousRegions.add(PreviousRegion(-2, line, line))
                    line--
                    continue
                }
            }
            if (previous.indent > indent) {
                // discard all regions with larger indent
                do {
                    previousRegions.removeAt(previousRegions.size - 1)
                    previous = previousRegions[previousRegions.size - 1]
                } while (previous.indent > indent)

                // new folding range
                val endLineNumber = previous.endAbove - 1
                if (endLineNumber - line >= 1) { // needs at east size 1
                    result.insertFirst(line, endLineNumber, indent)
                }
            }
            if (previous.indent == indent) {
                previous.endAbove = line
            } else { // previous.indent < indent
                // new region with a bigger indent
                previousRegions.add(PreviousRegion(indent, line, line))
            }
            line--
        }
        return result.toIndentRanges(model)
    }
}
