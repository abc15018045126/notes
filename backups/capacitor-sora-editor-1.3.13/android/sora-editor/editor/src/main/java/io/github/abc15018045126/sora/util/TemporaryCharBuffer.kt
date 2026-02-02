package io.github.abc15018045126.sora.util

object TemporaryCharBuffer {

    private var sTemp: CharArray? = null

    @JvmStatic
    fun obtain(len: Int): CharArray {
        var buf: CharArray?

        synchronized(TemporaryCharBuffer::class.java) {
            buf = sTemp
            sTemp = null
        }

        if (buf == null || buf!!.size < len) {
            buf = CharArray(len)
        }

        return buf!!
    }

    @JvmStatic
    fun recycle(temp: CharArray) {
        if (temp.size > 1000) return

        synchronized(TemporaryCharBuffer::class.java) {
            sTemp = temp
        }
    }
}
