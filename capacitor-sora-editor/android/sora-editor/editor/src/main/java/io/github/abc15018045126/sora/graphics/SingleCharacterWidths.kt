package io.github.abc15018045126.sora.graphics

import android.util.SparseArray
import io.github.abc15018045126.sora.text.CharArrayWrapper
import io.github.abc15018045126.sora.text.FunctionCharacters
import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.min

class SingleCharacterWidths(private val tabWidth: Int) {
    @JvmField
    val widths: FloatArray = FloatArray(10)
    
    @JvmField
    val codePointWidths: SparseArray<Float> = SparseArray()
    
    @JvmField
    val buffer: CharArray = CharArray(10)
    
    private val cache: FloatArray = FloatArray(65536)
    
    var isHandleFunctionCharacters: Boolean = false

    /**
     * Clear caches of font
     */
    fun clearCache() {
        Arrays.fill(cache, 0f)
        codePointWidths.clear()
    }

    /**
     * Measure a single character
     */
    fun measureChar(ch: Char, p: Paint): Float {
        var char = ch
        var rate = 1
        if (char == '\t') {
            char = ' '
            rate = tabWidth
        }
        var width = cache[char.code]
        if (width == 0f) {
            buffer[0] = char
            width = p.measureText(buffer, 0, 1)
            cache[char.code] = width
        }
        return width * rate
    }

    /**
     * Measure a single character
     * @param cp Code Point
     */
    fun measureCodePoint(cp: Int, p: Paint): Float {
        if (cp <= 65535) {
            return measureChar(cp.toChar(), p)
        }
        var width = codePointWidths[cp]
        if (width == null) {
            val count = Character.toChars(cp, buffer, 0)
            width = p.measureText(buffer, 0, count)
            codePointWidths.put(cp, width)
        }
        return width!!
    }

    /*
     * Measure text
     */
    fun measureText(chars: CharArray, start: Int, end: Int, p: Paint): Float {
        return measureText(CharArrayWrapper(chars, chars.size), start, end, p)
    }

    fun measureText(str: CharSequence, p: Paint): Float {
        return measureText(str, 0, str.length, p)
    }

    /**
     * Measure text
     */
    fun measureText(str: CharSequence, start: Int, end: Int, p: Paint): Float {
        var width: Long = 0
        var i = start
        while (i < end) {
            val ch = str[i]
            if (isEmoji(ch)) {
                if (i + 4 <= end) {
                    p.getTextWidths(str, i, i + 4, widths)
                    if (widths[0] > 0f && widths[1] == 0f && widths[2] == 0f && widths[3] == 0f) {
                        i += 3
                        width += ceil((widths[0] * PRECISION)).toLong()
                        i++
                        continue
                    }
                }
                val commitEnd = min(end.toDouble(), (i + 2).toDouble()).toInt()
                val len = commitEnd - i
                for (j in 0 until len) {
                    buffer[j] = str[i + j]
                }
                width += ceil((p.measureText(buffer, 0, len) * PRECISION)).toLong()
                i += len - 1
            } else if (isHandleFunctionCharacters && FunctionCharacters.isEditorFunctionChar(ch)) {
                val name = FunctionCharacters.getNameForFunctionCharacter(ch)
                for (j in 0 until name.length) {
                    width += ceil((measureChar(name[j], p) * PRECISION)).toLong()
                }
            } else {
                width += ceil((measureChar(ch, p) * PRECISION)).toLong()
            }
            i++
        }
        return width.toFloat() / PRECISION
    }

    companion object {
        /**
         * Floating-point precision steps.
         *
         *
         * Introduced to avoid accumulated floating-point errors.
         */
        private const val PRECISION = 1000L

        @JvmStatic
        fun isEmoji(ch: Char): Boolean {
            val code = ch.code
            return code == 0xd83c || code == 0xd83d || code == 0xd83e
        }
    }
}
