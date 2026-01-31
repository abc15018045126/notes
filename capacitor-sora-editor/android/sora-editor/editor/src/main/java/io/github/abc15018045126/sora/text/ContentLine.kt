package io.github.abc15018045126.sora.text

import android.text.GetChars
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.text.bidi.BidiRequirementChecker
import io.github.abc15018045126.sora.text.bidi.TextBidi
import io.github.abc15018045126.sora.util.ShareableData
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ContentLine] represents a single line of text in the editor.
 * It provides efficient text manipulation methods and supports RTL/Bidi content.
 */
class ContentLine : CharSequence, GetChars, BidiRequirementChecker, ShareableData<ContentLine> {

    internal var value: CharArray
    override var length: Int = 0
        internal set

    internal var rtlAffectingCount: Int = 0
    private var _lineSeparator: LineSeparator? = null
    var lineSeparator: LineSeparator?
        get() = _lineSeparator
        set(value) {
            _lineSeparator = value
        }
    
    val lineSeparatorSafe: LineSeparator
        get() = _lineSeparator ?: LineSeparator.NONE

    private var refCount: AtomicInteger? = null

    constructor() : this(true)

    constructor(text: CharSequence?) : this(true) {
        insert(0, text)
    }

    constructor(src: ContentLine) : this(src.length + 16) {
        length = src.length
        rtlAffectingCount = src.rtlAffectingCount
        _lineSeparator = src.lineSeparator
        System.arraycopy(src.value, 0, value, 0, length)
    }

    constructor(size: Int) {
        length = 0
        value = CharArray(size)
    }

    private constructor(initialize: Boolean) {
        if (initialize) {
            length = 0
            value = CharArray(32)
        } else {
            // Used by subSequence or copy to delay initialization of value or use provided array
            value = CharArray(0)
        }
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index > length) {
            throw StringIndexOutOfBoundsException("index = $index, length = $length")
        }
    }

    private fun ensureCapacity(capacity: Int) {
        if (value.size < capacity) {
            val newLength = if (value.size * 2 < capacity) capacity + 2 else value.size * 2
            val newValue = CharArray(newLength)
            System.arraycopy(value, 0, newValue, 0, length)
            value = newValue
        }
    }

    fun insert(dstOffset: Int, s: CharSequence?): ContentLine {
        val str = s ?: "null"
        return this.insert(dstOffset, str, 0, str.length)
    }

    fun insert(dstOffset: Int, s: CharSequence?, start: Int, end: Int): ContentLine {
        val str = s ?: "null"
        if (dstOffset < 0 || dstOffset > this.length) {
            throw IndexOutOfBoundsException("dstOffset $dstOffset")
        }
        if (start < 0 || end < 0 || start > end || end > str.length) {
            throw IndexOutOfBoundsException("start $start, end $end, s.length() ${str.length}")
        }
        val len = end - start
        ensureCapacity(length + len)
        System.arraycopy(value, dstOffset, value, dstOffset + len, length - dstOffset)
        var currentOffset = dstOffset
        for (i in start until end) {
            val ch = str[i]
            value[currentOffset++] = ch
            if (TextBidi.couldAffectRtl(ch)) {
                rtlAffectingCount++
            }
        }
        length += len
        return this
    }

    fun insert(offset: Int, c: Char): ContentLine {
        ensureCapacity(length + 1)
        if (offset < length) {
            System.arraycopy(value, offset, value, offset + 1, length - offset)
        }
        if (TextBidi.couldAffectRtl(c)) {
            rtlAffectingCount++
        }
        value[offset] = c
        length += 1
        return this
    }

    fun delete(start: Int, end: Int): ContentLine {
        var e = end
        if (start < 0) {
            throw StringIndexOutOfBoundsException(start)
        }
        if (e > length) {
            e = length
        }
        if (start > e) {
            throw StringIndexOutOfBoundsException()
        }
        val len = e - start
        if (len > 0) {
            for (i in start until e) {
                if (TextBidi.couldAffectRtl(value[i])) {
                    rtlAffectingCount--
                }
            }
            System.arraycopy(value, start + len, value, start, length - e)
            length -= len
        }
        return this
    }

    override fun mayNeedBidi(): Boolean {
        return rtlAffectingCount > 0
    }

    fun append(text: CharSequence): ContentLine {
        return this.insert(length, text)
    }

    @UnsupportedUserUsage
    override fun get(index: Int): Char {
        if (index >= length) {
            val sep = lineSeparatorSafe
            return if (sep.length > 0) {
                sep.content[index - length]
            } else {
                '\n'
            }
        }
        return value[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): ContentLine {
        checkIndex(startIndex)
        checkIndex(endIndex)
        if (endIndex < startIndex) {
            throw StringIndexOutOfBoundsException("start is greater than end")
        }
        val subLen = endIndex - startIndex
        val newValue = CharArray(subLen + 16)
        System.arraycopy(value, startIndex, newValue, 0, subLen)
        val res = ContentLine(false)
        res.value = newValue
        res.length = subLen

        if (rtlAffectingCount > 0) {
            for (i in 0 until res.length) {
                if (TextBidi.couldAffectRtl(newValue[i])) {
                    res.rtlAffectingCount++
                }
            }
        }
        return res
    }

    fun appendTo(sb: StringBuilder) {
        sb.append(value, 0, length)
    }

    override fun toString(): String {
        return String(value, 0, length)
    }

    fun toStringWithNewline(): String {
        if (value.size == length) {
            ensureCapacity(length + 1)
        }
        value[length] = '\n'
        return String(value, 0, length + 1)
    }

    val backingCharArray: CharArray
        get() = value

    override fun getChars(srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int) {
        if (srcBegin < 0) {
            throw StringIndexOutOfBoundsException(srcBegin)
        }
        if (srcEnd < 0 || srcEnd > length) {
            throw StringIndexOutOfBoundsException(srcEnd)
        }
        if (srcBegin > srcEnd) {
            throw StringIndexOutOfBoundsException("srcBegin > srcEnd")
        }
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin)
    }

    fun copy(): ContentLine {
        val clone = ContentLine(false)
        clone.length = length
        clone.value = CharArray(value.size)
        System.arraycopy(value, 0, clone.value, 0, length)
        clone.rtlAffectingCount = rtlAffectingCount
        clone._lineSeparator = _lineSeparator
        return clone
    }

    override fun retain() {
        if (refCount == null) {
            refCount = AtomicInteger(2)
            return
        }
        refCount!!.incrementAndGet()
    }

    override fun release() {
        if (refCount == null) {
            return
        }
        val count = refCount!!.decrementAndGet()
        if (count < 0) {
            throw IllegalStateException("illegal operation. There is no active owner")
        }
    }

    override fun isMutable(): Boolean {
        return refCount == null || refCount!!.get() == 1
    }

    override fun toMutable(): ContentLine {
        return if (isMutable()) this else copy()
    }
}
