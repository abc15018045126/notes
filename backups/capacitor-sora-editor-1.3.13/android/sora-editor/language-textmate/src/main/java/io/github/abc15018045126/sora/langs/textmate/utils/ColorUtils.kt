
package io.github.abc15018045126.sora.langs.textmate.utils

import android.graphics.Color

object ColorUtils {

    /**
     * This method is used to convert common web-standard color formats,
     * primarily RGBA (including RGB), into the ARGB integer format widely
     * used on the Android platform.
     *
     *
     * The web/VSCode color theme often uses the RGBA hex format (#RRGGBBAA),
     * where the Alpha channel is placed at the end. Android, however, uses
     * the ARGB integer format, where Alpha is the most significant byte
     * (#AARRGGBB). This method handles the necessary byte swapping for
     * an 8-character hex string.
     *
     *
     * @param colorString The color string to be parsed. Supported formats are:
     *
     *  * Color names (e.g., "red", "blue"), which are passed to Color.parseColor.
     *  * Standard 6-digit hex (#RRGGBB).
     *  * Web 8-digit hex (#RRGGBBAA).
     *
     * @return The 32-bit integer color in ARGB format.
     * @throws IllegalArgumentException if the hex string length is not 7 or 9 (including '#').
     */
    @JvmStatic
    fun parseRGBAToARGB(colorString: String): Int {
        if (colorString[0] != '#') {
            // See https://android.googlesource.com/platform/frameworks/base/+/876dbfb/graphics/java/android/graphics/Color.java#157
            // For non-hex strings (e.g., color names, rgb functions), rely on Android's built-in parser.
            return Color.parseColor(colorString)
        }
        // Use a long to avoid rollovers on #ffXXXXXX
        var color = colorString.substring(1).toLong(16)
        if (colorString.length == 7) {
            // Set the alpha value
            color = color or -0x1000000L

            // RGB has no alpha; the format is current;
            return color.toInt()
        } else if (colorString.length != 9) {
            throw IllegalArgumentException("Unknown color")
        }


        val r = (color shr 24).toInt() and 0xFF
        val g = (color shr 16).toInt() and 0xFF
        val b = (color shr 8).toInt() and 0xFF
        val a = (color and 0xFF).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
