package io.github.abc15018045126.sora.lang.brackets

import io.github.abc15018045126.sora.text.Content

/**
 * Compute paired bracket when queried
 *
 * @author abc15018045126
 */
class OnlineBracketsMatcher(
    private val pairs: CharArray,
    private val limit: Int
) : BracketsProvider {

    init {
        if ((pairs.size and 1) != 0) {
            throw IllegalArgumentException("pairs must have even length")
        }
    }

    private fun findIndex(ch: Char): Int {
        for (i in pairs.indices) {
            if (ch == pairs[i]) {
                return i
            }
        }
        return -1
    }

    private fun tryComputePaired(text: Content, index: Int): PairedBracket? {
        val a = text[index]
        val symbolIndex = findIndex(a)
        if (symbolIndex != -1) {
            val b = pairs[symbolIndex xor 1]
            var stack = 0
            if ((symbolIndex and 1) == 0) {
                // Find forward
                var i = index + 1
                while (i < text.length && i - index < limit) {
                    val ch = text[i]
                    if (ch == b) {
                        if (stack <= 0) {
                            return PairedBracket(leftIndex = index, rightIndex = i)
                        } else {
                            stack--
                        }
                    } else if (ch == a) {
                        stack++
                    }
                    i++
                }
            } else {
                // Find backward
                var i = index - 1
                while (i >= 0 && index - i < limit) {
                    val ch = text[i]
                    if (ch == b) {
                        if (stack <= 0) {
                            return PairedBracket(leftIndex = i, rightIndex = index)
                        } else {
                            stack--
                        }
                    } else if (ch == a) {
                        stack++
                    }
                    i--
                }
            }
        }
        return null
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        var pairedBracket: PairedBracket? = null
        if (index > 0) {
            pairedBracket = tryComputePaired(text, index - 1)
        }
        if (pairedBracket == null && index < text.length) {
            pairedBracket = tryComputePaired(text, index)
        }
        return pairedBracket
    }
}
