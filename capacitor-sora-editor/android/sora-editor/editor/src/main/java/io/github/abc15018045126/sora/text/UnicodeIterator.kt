package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.ObjectPool

class UnicodeIterator private constructor() {

    private var text: CharSequence? = null
    var codePoint: Int = 0
        private set
    var startIndex: Int = 0
        private set
    var endIndex: Int = 0
        private set
    private var limit: Int = 0

    fun recycle() {
        sPool.recycle(this)
    }

    fun set(text: CharSequence, start: Int, end: Int) {
        if ((start or end or (end - start) or (text.length - end)) < 0) {
            throw IndexOutOfBoundsException()
        }
        this.text = text
        this.endIndex = start
        this.startIndex = this.endIndex
        limit = end
    }

    operator fun hasNext(): Boolean {
        return endIndex < limit
    }

    fun nextCodePoint(): Int {
        startIndex = endIndex
        if (startIndex >= limit) {
            codePoint = 0
        } else {
            endIndex++
            val ch = text!![startIndex]
            if (Character.isHighSurrogate(ch) && endIndex < limit) {
                codePoint = Character.toCodePoint(ch, text!![endIndex])
                endIndex++
            } else {
                codePoint = ch.code
            }
        }
        return codePoint
    }

    companion object {
        private val sPool = object : ObjectPool<UnicodeIterator>() {
            override fun allocateNew(): UnicodeIterator {
                return UnicodeIterator()
            }

            override fun onRecycleObject(recycledObj: UnicodeIterator) {
                recycledObj.text = null
            }
        }

        @JvmStatic
        fun obtain(text: CharSequence, start: Int, end: Int): UnicodeIterator {
            val r = sPool.obtain()
            r.set(text, start, end)
            return r
        }
    }
}
