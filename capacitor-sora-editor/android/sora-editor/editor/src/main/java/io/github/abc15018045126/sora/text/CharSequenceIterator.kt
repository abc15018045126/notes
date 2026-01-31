package io.github.abc15018045126.sora.text

import java.text.CharacterIterator

/**
 * CharacterIterator implementation
 *
 * @author abc15018045126
 */
class CharSequenceIterator(private val src: CharSequence) : CharacterIterator {

    private var index: Int = 0

    override fun first(): Char {
        index = 0
        return current()
    }

    override fun last(): Char {
        index = src.length - 1
        if (index < 0) {
            index = 0
        }
        return current()
    }

    override fun current(): Char {
        return if (index == endIndex) CharacterIterator.DONE else src[index]
    }

    override fun next(): Char {
        index++
        return current()
    }

    override fun previous(): Char {
        index--
        if (index < 0) {
            index = 0
        }
        return current()
    }

    override fun setIndex(i: Int): Char {
        index = i
        return current()
    }

    override fun getBeginIndex(): Int {
        return 0
    }

    override fun getEndIndex(): Int {
        return src.length
    }

    override fun getIndex(): Int {
        return index
    }

    override fun clone(): Any {
        val another = CharSequenceIterator(src)
        another.index = index
        return another
    }
}
