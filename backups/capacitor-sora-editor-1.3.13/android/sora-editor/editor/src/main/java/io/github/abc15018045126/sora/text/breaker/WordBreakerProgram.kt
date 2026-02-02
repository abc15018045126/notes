package io.github.abc15018045126.sora.text.breaker

import io.github.abc15018045126.sora.text.ContentLine

class WordBreakerProgram(text: ContentLine) : WordBreakerIcu(text) {

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        val icuResult = super.getOptimizedBreakPoint(start, end)
        if (icuResult != end || end <= start || /* end > start */ Character.isWhitespace(chars[end - 1])) {
            return icuResult
        }
        // Add extra opportunities for dots
        var index = end - 1
        while (index > start) {
            if (chars[index] == '.' && index - 1 >= start && !Character.isDigit(chars[index - 1])) {
                // Break after this dot
                return index + 1
            }
            index--
        }
        return end
    }
}
