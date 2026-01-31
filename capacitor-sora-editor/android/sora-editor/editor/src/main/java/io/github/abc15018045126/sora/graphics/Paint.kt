package io.github.abc15018045126.sora.graphics

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.CallSuper
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.FunctionCharacters

open class Paint @JvmOverloads constructor(
    var isRenderFunctionCharacters: Boolean = false
) : android.graphics.Paint() {

    var spaceWidth: Float = 0f
        private set

    init {
        spaceWidth = measureText(" ")
    }

    open fun onAttributeUpdate() {
        spaceWidth = measureText(" ")
    }

    fun setTypefaceWrapped(typeface: Typeface?) {
        super.setTypeface(typeface)
        onAttributeUpdate()
    }

    fun setTextSizeWrapped(textSize: Float) {
        super.setTextSize(textSize)
        onAttributeUpdate()
    }

    fun setFontFeatureSettingsWrapped(settings: String?) {
        super.setFontFeatureSettings(settings)
        onAttributeUpdate()
    }

    @CallSuper
    override fun setLetterSpacing(letterSpacing: Float) {
        super.setLetterSpacing(letterSpacing)
        onAttributeUpdate()
    }

    @SuppressLint("NewApi")
    fun myGetTextRunAdvances(
        chars: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        advances: FloatArray?,
        advancesIndex: Int
    ): Float {
        var advance = getTextRunAdvances(
            chars,
            index,
            count,
            contextIndex,
            contextCount,
            isRtl,
            advances,
            advancesIndex
        )
        if (isRenderFunctionCharacters) {
            for (i in 0 until count) {
                val ch = chars[index + i]
                if (FunctionCharacters.isEditorFunctionChar(ch)) {
                    val width = measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                    if (advances != null) {
                        advance -= advances[advancesIndex + i]
                        advances[advancesIndex + i] = width
                    } else {
                        advance -= measureText(ch.toString())
                    }
                    advance += width
                }
            }
        }
        return advance
    }

    /**
     * Get the advance of text with the context positions related to shaping the characters
     */
    fun measureTextRunAdvance(
        text: CharArray,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean
    ): Float {
        return myGetTextRunAdvances(
            text,
            start,
            end - start,
            contextStart,
            contextEnd - contextStart,
            isRtl,
            null,
            0
        )
    }

    /**
     * Find offset for a certain advance returned by [.measureTextRunAdvance]
     */
    fun findOffsetByRunAdvance(
        text: ContentLine, intStart: Int, end: Int,
        contextStart: Int, contextEnd: Int, isRtl: Boolean,
        advance: Float
    ): Int {
        var start = intStart
        if (isRenderFunctionCharacters) {
            var lastEnd = start
            var current = 0f
            val textChars = text.backingCharArray
            for (i in start until end) {
                val ch = textChars[i]
                if (FunctionCharacters.isEditorFunctionChar(ch)) {
                    val result = if (lastEnd == i) i else breakTextImpl(
                        text,
                        lastEnd,
                        i,
                        contextStart,
                        contextEnd,
                        isRtl,
                        advance - current
                    )
                    if (result < i) {
                        return result
                    }
                    current += measureTextRunAdvance(
                        textChars,
                        lastEnd,
                        i,
                        contextStart,
                        contextEnd,
                        isRtl
                    )
                    current += measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                    if (current >= advance) {
                        return i
                    }
                    lastEnd = i + 1
                }
            }
            if (lastEnd < end) {
                return breakTextImpl(
                    text,
                    lastEnd,
                    end,
                    contextStart,
                    contextEnd,
                    isRtl,
                    advance - current
                )
            }
            return end
        } else {
            return breakTextImpl(text, start, end, contextStart, contextEnd, isRtl, advance)
        }
    }

    private fun breakTextImpl(
        text: ContentLine,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advance: Float
    ): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getOffsetForAdvance(
                text.backingCharArray,
                start,
                end,
                contextStart,
                contextEnd,
                isRtl,
                advance
            )
        } else {
            start + breakText(text.backingCharArray, start, end - start, advance, null)
        }
    }
}
