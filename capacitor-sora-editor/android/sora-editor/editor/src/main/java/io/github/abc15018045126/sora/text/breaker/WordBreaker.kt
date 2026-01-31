package io.github.abc15018045126.sora.text.breaker

import io.github.abc15018045126.sora.text.ContentLine

/**
 * Breakpoint optimizer used when breaking text to visual rows
 */
interface WordBreaker {

    fun getOptimizedBreakPoint(start: Int, end: Int): Int

    object Factory {

        @JvmStatic
        fun newInstance(text: ContentLine): WordBreaker {
            return WordBreakerProgram(text)
        }
    }
}
