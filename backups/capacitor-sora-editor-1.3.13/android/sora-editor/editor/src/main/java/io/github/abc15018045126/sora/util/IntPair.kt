package io.github.abc15018045126.sora.util

/**
 * Pack two int numbers into a long number, and unpack it.
 * <p>
 * This is effective for passing two primitive 32-bit numbers without creating a new object.
 *
 * @author abc15018045126
 */
object IntPair {

    /**
     * Convert an integer to a long whose binary bits are equal to the given integer
     */
    private fun toUnsignedLong(x: Int): Long {
        return x.toLong() and 0xffffffffL
    }

    /**
     * Pack two int number into a long number
     *
     * @param first  First of pair
     * @param second Second of pair
     * @return Packed value
     */
    @JvmStatic
    fun pack(first: Int, second: Int): Long {
        return (toUnsignedLong(first) shl 32) or toUnsignedLong(second)
    }

    /**
     * Get second of pair
     *
     * @param packedValue Packed value
     * @return Second of pair
     */
    @JvmStatic
    fun getSecond(packedValue: Long): Int {
        return (packedValue and 0xFFFFFFFFL).toInt()
    }

    /**
     * Get first of pair
     *
     * @param packedValue Packed value
     * @return First of pair
     */
    @JvmStatic
    fun getFirst(packedValue: Long): Int {
        return (packedValue ushr 32).toInt()
    }

    /**
     * Pack an int number and a floating-number into a long number
     *
     * @param first  First of pair
     * @param second Second of pair (float)
     * @return Packed value
     */
    @JvmStatic
    fun packIntFloat(first: Int, second: Float): Long {
        return pack(first, java.lang.Float.floatToRawIntBits(second))
    }

    /**
     * Get second of pair, but as a floating number
     *
     * @param packedValue Packed value
     * @return Second of pair
     * @see #packIntFloat(int, float)
     */
    @JvmStatic
    fun getSecondAsFloat(packedValue: Long): Float {
        return java.lang.Float.intBitsToFloat(getSecond(packedValue))
    }
}
