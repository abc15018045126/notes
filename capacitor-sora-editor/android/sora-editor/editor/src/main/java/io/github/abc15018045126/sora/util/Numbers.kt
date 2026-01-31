package io.github.abc15018045126.sora.util

import kotlin.math.max
import kotlin.math.min

/**
 * From Java java.lang.Integer
 */
object Numbers {

    val DigitTens: CharArray = charArrayOf(
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
    )

    val DigitOnes: CharArray = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    @JvmStatic
    fun stringSize(x: Int): Int {
        var xx = x
        var d = 1
        if (xx >= 0) {
            d = 0
            xx = -xx
        }
        var p = -10
        for (i in 1..9) {
            if (xx > p)
                return i + d
            p = 10 * p
        }
        return 10 + d
    }

    @JvmStatic
    fun getChars(i: Int, index: Int, buf: CharArray) {
        var ii = i
        var q: Int
        var r: Int
        var charPos = index

        val negative = ii < 0
        if (!negative) {
            ii = -ii
        }

        // Generate two digits per iteration
        while (ii <= -100) {
            q = ii / 100
            r = (q * 100) - ii
            ii = q
            buf[--charPos] = DigitOnes[r]
            buf[--charPos] = DigitTens[r]
        }

        // We know there are at most two digits left at this point.
        buf[--charPos] = DigitOnes[-ii]
        if (ii < -9) {
            buf[--charPos] = DigitTens[-ii]
        }

        if (negative) {
            buf[--charPos] = '-'
        }
    }

    /**
     * Clear flag in flags
     * The flag must be power of two
     *
     * @param flags Flags to filter
     * @param flag  The flag to clear
     * @return Cleared flags
     */
    @JvmStatic
    fun clearBit(flags: Int, flag: Int): Int {
        return if ((flags and flag) != 0) flags xor flag else flags
    }

    @JvmStatic
    fun clearBits(flags: Int, bitsToClear: Int): Int {
        val mask = bitsToClear.inv()
        return flags and mask
    }

    @JvmStatic
    fun coerceIn(value: Int, min: Int, max: Int): Int {
        return max(min.toDouble(), min(max.toDouble(), value.toDouble())).toInt()
    }
}
