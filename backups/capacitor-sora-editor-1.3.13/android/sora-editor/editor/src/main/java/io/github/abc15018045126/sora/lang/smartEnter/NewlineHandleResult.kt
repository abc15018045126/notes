package io.github.abc15018045126.sora.lang.smartEnter

class NewlineHandleResult(
    /**
     * Text to insert
     */
    @JvmField val text: CharSequence,
    /**
     * Count to shift left from the end of {@link NewlineHandleResult#text}
     */
    @JvmField val shiftLeft: Int
) {
    init {
        if (shiftLeft < 0 || shiftLeft > text.length) {
            throw IllegalArgumentException("invalid shiftLeft")
        }
    }
}
