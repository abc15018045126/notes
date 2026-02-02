
package io.github.abc15018045126.sora.langs.textmate.utils

import java.util.regex.Pattern

object StringUtils {

    @JvmStatic
    fun checkSurrogate(text: String): Boolean {
        for (i in 0 until text.length) {
            if (Character.isSurrogate(text[i])) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun convertUnicodeOffsetToUtf16(text: String, offset: Int, hasSurrogate: Boolean): Int {
        if (hasSurrogate) {
            var j = 0
            var i = 0
            while (i < text.length) {
                if (j == offset) {
                    return i
                }
                val ch = text[i]
                if (Character.isHighSurrogate(ch) && i + 1 < text.length && Character.isLowSurrogate(text[i + 1])) {
                    i++
                }
                j++
                i++
            }
        }
        return offset
    }


    private val MATCH_PATTERN = Pattern.compile(".*/|\\..*")

    @JvmStatic
    fun getFileNameWithoutExtension(filePath: String): String {
        return MATCH_PATTERN.matcher(filePath).replaceAll("")

    }
}
