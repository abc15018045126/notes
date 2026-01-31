package io.github.abc15018045126.sora.lang.util

import io.github.abc15018045126.sora.lang.styling.EmptyReader
import io.github.abc15018045126.sora.lang.styling.Spans
import io.github.abc15018045126.sora.text.CharPosition

/**
 * [Spans] implementation that always returns [EmptyReader] for reading spans.
 * Line count is automatically adjusted as content changes.
 *
 * @author abc15018045126
 */
class PlainTextSpans(private var lineCount: Int) : Spans {

    fun setLineCount(lineCount: Int) {
        this.lineCount = lineCount
    }

    override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        lineCount += end.line - start.line
    }

    override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        lineCount -= end.line - start.line
    }

    override fun read(): Spans.Reader {
        return EmptyReader.INSTANCE
    }

    override fun supportsModify(): Boolean {
        return false
    }

    override fun modify(): Spans.Modifier {
        throw UnsupportedOperationException()
    }

    override fun getLineCount(): Int {
        return lineCount
    }
}
