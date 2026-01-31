package io.github.abc15018045126.sora.text.breaker

import io.github.abc15018045126.sora.text.CharSequenceIterator
import io.github.abc15018045126.sora.text.ContentLine
import java.text.BreakIterator
import kotlin.math.max
import kotlin.math.min

open class WordBreakerIcu(text: ContentLine) : WordBreaker {

    protected val wrappingIterator: BreakIterator
    protected val chars: CharArray

    init {
        this.chars = text.backingCharArray
        val textIterator = CharSequenceIterator(text)
        wrappingIterator = BreakIterator.getLineInstance()
        wrappingIterator.setText(textIterator)
    }

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        var resultEnd = end
        // Merging trailing whitespaces is not supported by editor, so force to break here
        if (resultEnd > 0 && !Character.isWhitespace(chars[resultEnd - 1]) && !wrappingIterator.isBoundary(resultEnd)) {
            // Break text at last boundary
            val lastBoundary = wrappingIterator.preceding(resultEnd)
            if (lastBoundary != BreakIterator.DONE) {
                val suggestedNext = max(start, min(resultEnd, lastBoundary))
                if (suggestedNext > start) {
                    resultEnd = suggestedNext
                }
            }
        }
        return resultEnd
    }
}
