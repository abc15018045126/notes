package io.github.abc15018045126.sora.util

object TemporaryFloatBuffer {

    private val sCache = FloatArrayCache()

    @JvmStatic
    fun obtain(len: Int): FloatArray {
        return sCache.obtain(len)
    }

    @JvmStatic
    fun recycle(temp: FloatArray) {
        sCache.recycle(temp)
    }

    class FloatArrayCache {

        private var temp: FloatArray? = null

        fun obtain(len: Int): FloatArray {
            var buf: FloatArray?

            synchronized(this) {
                buf = temp
                temp = null
            }

            if (buf == null || buf!!.size < len) {
                buf = FloatArray(len)
            }

            return buf!!
        }

        fun recycle(temp: FloatArray) {
            if (temp.size > 1000) return

            synchronized(this) {
                this.temp = temp
            }
        }
    }
}
