package io.github.abc15018045126.sora.text.bidi

import android.text.TextUtils
import java.text.Bidi
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.TemporaryCharBuffer

/**
 * Text bidirectional utils. Some codes are from AOSP
 *
 * @author abc15018045126
 */
object TextBidi {

    /**
     * Compute text directions for the given text
     */
    @JvmStatic
    fun getDirections(text: CharSequence): Directions {
        val len = text.length
        if (doesNotNeedBidi(text)) {
            return Directions(longArrayOf(IntPair.pack(0, 0)), len)
        }
        val chars = TemporaryCharBuffer.obtain(len)
        TextUtils.getChars(text, 0, len, chars, 0)
        val bidi = Bidi(chars, 0, null, 0, text.length, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
        val runs = LongArray(bidi.runCount)
        for (i in runs.indices) {
            runs[i] = IntPair.pack(bidi.getRunStart(i), bidi.getRunLevel(i))
        }
        TemporaryCharBuffer.recycle(chars)
        return Directions(runs, len)
    }

    @JvmStatic
    fun couldAffectRtl(c: Char): Boolean {
        val i = c.code
        return (i in 0x0590..0x08FF) ||  // RTL scripts
                i == 0x200E ||  // Bidi format character
                i == 0x200F ||  // Bidi format character
                (i in 0x202A..0x202E) ||  // Bidi format characters
                (i in 0x2066..0x2069) ||  // Bidi format characters
                (i in 0xD800..0xDFFF) ||  // Surrogate pairs
                (i in 0xFB1D..0xFDFF) ||  // Hebrew and Arabic presentation forms
                (i in 0xFE70..0xFEFE)    // Arabic presentation forms
    }

    /**
     * Returns true if there is no character present that may potentially affect RTL layout.
     * Since this calls couldAffectRtl() above, it's also quite conservative, in the way that
     * it may return 'false' (needs bidi) although careful consideration may tell us it should
     * return 'true' (does not need bidi).
     */
    @JvmStatic
    fun doesNotNeedBidi(text: CharSequence): Boolean {
        if (text is BidiRequirementChecker) {
            return !text.mayNeedBidi()
        }
        val len = text.length
        for (i in 0 until len) {
            if (couldAffectRtl(text[i])) {
                return false
            }
        }
        return true
    }
}
