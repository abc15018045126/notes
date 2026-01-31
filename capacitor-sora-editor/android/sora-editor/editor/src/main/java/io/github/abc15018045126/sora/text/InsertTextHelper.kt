package io.github.abc15018045126.sora.text

internal class InsertTextHelper {

    private var text: CharSequence? = null
    var index: Int = 0
        private set
    var indexNext: Int = 0
        private set
    private var length: Int = 0

    private fun init(text: CharSequence) {
        this.text = text
        index = -1
        indexNext = 0
        length = text.length
    }

    fun forward(): Int {
        this.index = indexNext
        if (index == length) {
            return TYPE_EOF
        }
        var ch = text!![index]
        when (ch) {
            '\n' -> {
                indexNext = index + 1
                return TYPE_NEWLINE
            }
            '\r' -> {
                if (index + 1 < length && text!![index + 1] == '\n') {
                    indexNext = index + 2
                } else {
                    indexNext = index + 1
                }
                return TYPE_NEWLINE
            }
            else -> {
                indexNext = index + 1
                while (indexNext < length) {
                    ch = text!![indexNext]
                    if (ch == '\n' || ch == '\r') {
                        break
                    }
                    indexNext++
                }
                return TYPE_LINE_CONTENT
            }
        }
    }

    fun recycle() {
        synchronized(InsertTextHelper::class.java) {
            for (i in sCached.indices) {
                if (sCached[i] == null) {
                    sCached[i] = this
                    reset()
                    break
                }
            }
        }
    }

    fun reset() {
        text = null
        index = 0
        length = 0
    }

    companion object {
        private val sCached = arrayOfNulls<InsertTextHelper>(8)
        const val TYPE_LINE_CONTENT: Int = 0
        const val TYPE_NEWLINE: Int = 1
        const val TYPE_EOF: Int = 2

        @Synchronized
        private fun obtain(): InsertTextHelper {
            for (i in sCached.indices) {
                if (sCached[i] != null) {
                    val cache = sCached[i]
                    sCached[i] = null
                    return cache!!
                }
            }
            return InsertTextHelper()
        }

        @JvmStatic
        fun forInsertion(text: CharSequence): InsertTextHelper {
            val o = obtain()
            o.init(text)
            return o
        }
    }
}
