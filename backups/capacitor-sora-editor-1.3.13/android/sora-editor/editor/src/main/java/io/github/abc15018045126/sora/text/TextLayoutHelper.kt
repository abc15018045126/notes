package io.github.abc15018045126.sora.text

import android.annotation.SuppressLint
import android.os.Build
import android.text.DynamicLayout
import android.text.Editable
import android.text.Layout
import android.text.Selection
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

/**
 * Helper class for indirectly calling Paint#getTextRunCursor(), which is
 * responsible for cursor controlling.
 *
 * @author abc15018045126
 */
class TextLayoutHelper private constructor() {

    private val text: Editable = Editable.Factory.getInstance().newEditable("")
    private val layout: DynamicLayout

    init {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            layout = DynamicLayout(
                text, TextPaint(), Int.MAX_VALUE / 2,
                Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true
            )
            try {
                @SuppressLint("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
                val field = Layout::class.java.getDeclaredField("mTextDir")
                field.isAccessible = true
                field.set(layout, TextDirectionHeuristics.FIRSTSTRONG_LTR)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            layout = DynamicLayout.Builder.obtain(text, TextPaint(), Int.MAX_VALUE / 2)
                .setIncludePad(true)
                .setLineSpacing(0f, 0f)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
        }
    }

    /**
     * Get cursor position after moving left
     */
    fun getCurPosLeft(offset: Int, s: CharSequence): Int {
        val left = max(0, offset - CHAR_FACTOR)
        var index = offset - left
        text.append(s, left, min(s.length, offset + CHAR_FACTOR + 1))
        index = min(index, text.length)
        Selection.setSelection(text, index)
        try {
            Selection.moveLeft(text, layout)
            index = Selection.getSelectionStart(text)
        } finally {
            text.clear()
            Selection.removeSelection(text)
        }
        return left + index
    }

    /**
     * Get cursor position after moving right
     */
    fun getCurPosRight(offset: Int, s: CharSequence): Int {
        val left = max(0, offset - CHAR_FACTOR)
        var index = offset - left
        text.append(s, left, min(s.length, offset + CHAR_FACTOR + 1))
        index = min(index, text.length)
        Selection.setSelection(text, index)
        try {
            Selection.moveRight(text, layout)
            index = Selection.getSelectionStart(text)
        } finally {
            text.clear()
            Selection.removeSelection(text)
        }
        return left + index
    }

    companion object {
        private val sLocal = ThreadLocal<TextLayoutHelper>()
        private const val CHAR_FACTOR = 64

        /**
         * Get TextLayoutHelper for current thread
         */
        @JvmStatic
        fun get(): TextLayoutHelper {
            var v = sLocal.get()
            if (v == null) {
                v = TextLayoutHelper()
                sLocal.set(v)
            }
            return v!!
        }
    }
}
