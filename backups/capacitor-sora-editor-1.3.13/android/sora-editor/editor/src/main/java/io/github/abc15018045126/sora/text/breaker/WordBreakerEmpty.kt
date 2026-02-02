package io.github.abc15018045126.sora.text.breaker

class WordBreakerEmpty private constructor() : WordBreaker {

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        return end
    }

    companion object {
        @JvmField
        val INSTANCE: WordBreaker = WordBreakerEmpty()
    }
}
