package io.github.abc15018045126.sora.graphics

/**
 * Utility for breaking text by grapheme bounds
 *
 * @author abc15018045126
 */
object GraphemeBoundsBreaker {

    /**
     * Find next grapheme break point before the given width
     */
    @JvmStatic
    fun findGraphemeBreakPoint(advances: FloatArray, length: Int, width: Int, start: Int): Int {
        var currentWidth = 0f
        var next = start
        while (next < length) {
            if (advances[next] == 0f) {
                // Not grapheme bound
                next++
                continue
            }
            if (currentWidth + advances[next] > width) {
                break
            }
            currentWidth += advances[next]
            next++
        }
        return next
    }
}
