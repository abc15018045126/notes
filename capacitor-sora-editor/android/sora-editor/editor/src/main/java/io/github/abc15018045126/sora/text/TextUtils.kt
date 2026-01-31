package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for texts
 */
object TextUtils {

    /**
     * Counts the number of whitespaces at the start of the given {@link CharSequence}.
     *
     * @param text     The text to count the spaces in.
     * @return A long packed with the number of spaces and tabs at the start of the line.
     * Use {@link IntPair#getFirst(long)} to get the number of spaces and {@link IntPair#getSecond(long)}
     * for the number of tabs.
     */
    @JvmStatic
    fun countLeadingSpacesAndTabs(text: CharSequence): Long {
        var p = 0
        var spaces = 0
        var tabs = 0
        while (p < text.length) {
            val c = text[p]
            if (!isWhitespace(c)) {
                break
            }
            if (c == '\t') {
                tabs += 1
            } else {
                spaces += 1
            }
            ++p
        }

        return IntPair.pack(spaces, tabs)
    }

    /**
     * Compute leading space count
     *
     * @param tabWidth Tab is considered in {@code tabWidth} spaces
     */
    @JvmStatic
    fun countLeadingSpaceCount(text: CharSequence, tabWidth: Int): Int {
        val result = countLeadingSpacesAndTabs(text)
        return IntPair.getFirst(result) + tabWidth * IntPair.getSecond(result)
    }

    /**
     * Create indent space
     *
     * @return Generated space string
     */
    @JvmStatic
    fun createIndent(indentSize: Int, tabWidth: Int, useTab: Boolean): String {
        val size = max(0, indentSize)
        val tab: Int
        val space: Int
        if (useTab) {
            tab = size / tabWidth
            space = size % tabWidth
        } else {
            tab = 0
            space = size
        }
        val s = StringBuilder()
        for (i in 0 until tab) {
            s.append('\t')
        }
        for (i in 0 until space) {
            s.append(' ')
        }
        return s.toString()
    }

    @JvmStatic
    fun indexOf(text: CharSequence, pattern: CharSequence, ignoreCase: Boolean, fromIndex: Int): Int {
        val max = text.length - pattern.length
        val len = pattern.length
        label@ for (i in fromIndex..max) {
            // Compare
            for (j in 0 until len) {
                val s = text[i + j]
                val p = pattern[j]
                if (!(s == p || ignoreCase && s.lowercaseChar() == p.lowercaseChar())) {
                    continue@label
                }
            }
            return i
        }
        return -1
    }

    @JvmStatic
    fun lastIndexOf(text: CharSequence, pattern: CharSequence, ignoreCase: Boolean, fromIndex: Int): Int {
        val len = pattern.length
        val startIndex = min(fromIndex, text.length - len)
        label@ for (i in startIndex downTo 0) {
            // Compare
            for (j in 0 until len) {
                val s = text[i + j]
                val p = pattern[j]
                if (!(s == p || ignoreCase && s.lowercaseChar() == p.lowercaseChar())) {
                    continue@label
                }
            }
            return i
        }
        return -1
    }

    @JvmStatic
    fun startsWith(text: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Boolean {
        if (text.length < pattern.length) {
            return false
        }
        val len = pattern.length
        for (i in 0 until len) {
            val s = text[i]
            val p = pattern[i]
            if (!(s == p || ignoreCase && s.lowercaseChar() == p.lowercaseChar())) {
                return false
            }
        }
        return true
    }

    private fun isWhitespace(ch: Char): Boolean {
        return ch == '\t' || ch == ' '
    }

    @JvmStatic
    fun padStart(src: String, padChar: Char, length: Int): String {
        if (src.length >= length) {
            return src
        }
        val sb = StringBuilder(length)
        for (i in 0 until length - src.length) {
            sb.append(padChar)
        }
        sb.append(src)
        return sb.toString()
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    @JvmStatic
    fun findLeadingAndTrailingWhitespacePos(line: ContentLine): Long {
        return findLeadingAndTrailingWhitespacePos(line, 0, line.length)
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line  The line to search
     * @param start Range start (inclusive)
     * @param end   Range end (exclusive)
     */
    @JvmStatic
    fun findLeadingAndTrailingWhitespacePos(line: ContentLine, start: Int, end: Int): Long {
        val buffer = line.backingCharArray
        var leading = start
        var trailing = end
        while (leading < end && isWhitespace(buffer[leading])) {
            leading++
        }
        // Skip for space-filled line
        if (leading != end) {
            while (trailing > 0 && isWhitespace(buffer[trailing - 1])) {
                trailing--
            }
        }
        return IntPair.pack(leading, trailing)
    }
}
