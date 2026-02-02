package io.github.abc15018045126.sora.text

/**
 * Line separator types
 *
 * @author abc15018045126
 */
enum class LineSeparator(private val str: String) {
    /**
     * No separator. Used internally
     */
    NONE(""),
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    private val chars: CharArray = str.toCharArray()

    /**
     * Get the text of this separator
     */
    fun getContent(): String = str

    /**
     * Get text length of this separator
     */
    fun getLength(): Int = str.length

    /**
     * Get a char array containing the line separator. The char array should not be modified.
     */
    fun getChars(): CharArray = chars

    companion object {
        /**
         * Get target line separator from a line separator string.
         *
         * @param str line separator string
         * @throws IllegalArgumentException if the given str is not a line separator
         */
        @JvmStatic
        fun fromSeparatorString(str: String): LineSeparator {
            return when (str) {
                "\r" -> CR
                "\n" -> LF
                "\r\n" -> CRLF
                "" -> NONE
                else -> throw IllegalArgumentException("unknown line separator type")
            }
        }

        /**
         * Get target line separator from a line separator string.
         *
         * @param text  the whole text
         * @param start start index of the line separator
         * @param end   end index of the line separator
         * @throws IllegalArgumentException if the given str is not a line separator
         */
        @JvmStatic
        fun fromSeparatorString(text: CharSequence, start: Int, end: Int): LineSeparator {
            if (end == start) {
                return NONE
            }
            if (end - start == 1) {
                val ch = text[start]
                if (ch == '\r') return CR
                if (ch == '\n') return LF
            }
            if (end - start == 2 && text[start] == '\r' && text[start + 1] == '\n') {
                return CRLF
            }
            throw IllegalArgumentException("unknown line separator type")
        }
    }
}
