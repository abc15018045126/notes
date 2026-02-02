package io.github.abc15018045126.sora.text

import android.text.GetChars
import java.nio.CharBuffer

/**
 * Wrapper for char array. Make char array work as a char sequence.
 *
 * @author abc15018045126
 */
class CharArrayWrapper(
    private val data: CharArray,
    private val offset: Int,
    private var count: Int
) : CharSequence, GetChars {

    constructor(array: CharArray, dataCount: Int) : this(array, 0, dataCount)

    fun setDataCount(count: Int) {
        this.count = count
    }

    override val length: Int
        get() = count

    override fun get(index: Int): Char {
        return data[offset + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return CharBuffer.wrap(data, offset + startIndex, endIndex - startIndex)
    }

    override fun getChars(start: Int, end: Int, dest: CharArray, destOffset: Int) {
        if (end > count) {
            throw StringIndexOutOfBoundsException()
        }
        System.arraycopy(data, start + this.offset, dest, destOffset, end - start)
    }
}
