package io.github.abc15018045126.sora.util

import java.util.Arrays

/**
 * @author Rose
 * Get whether Identifier part/start quickly
 */
object MyCharacter {

    /**
     * Compressed bit set for isJavaIdentifierStart()
     */
    private var bitsIsStart: IntArray? = null

    /**
     * Compressed bit set for isJavaIdentifierPart()
     */
    private var bitsIsPart: IntArray? = null

    init {
        initMapInternal()
    }

    /**
     * Get bit in compressed bit set
     *
     * @param values   Compressed bit set
     * @param bitIndex Target index
     * @return Boolean value at the index
     */
    private fun get(values: IntArray?, bitIndex: Int): Boolean {
        return (values!![bitIndex / 32] and (1 shl (bitIndex % 32))) != 0
    }

    /**
     * Make the given position's bit true
     *
     * @param values   Compressed bit set
     * @param bitIndex Index of bit
     */
    private fun set(values: IntArray?, bitIndex: Int) {
        values!![bitIndex / 32] = values[bitIndex / 32] or (1 shl (bitIndex % 32))
    }

    /**
     * Init maps
     *
     * @deprecated The class will be initialized automatically
     */
    @Deprecated("The class will be initialized automatically")
    @JvmStatic
    fun initMap() {
        // Empty
    }

    /**
     * Init maps
     */
    private fun initMapInternal() {
        if (bitsIsStart != null) {
            return
        }
        bitsIsPart = IntArray(2048)
        bitsIsStart = IntArray(2048)
        Arrays.fill(bitsIsPart, 0)
        Arrays.fill(bitsIsStart, 0)
        for (i in 0..65535) {
            if (Character.isJavaIdentifierPart(i.toChar())) {
                set(bitsIsPart, i)
            }
            if (Character.isJavaIdentifierStart(i.toChar())) {
                set(bitsIsStart, i)
            }
        }
    }

    /**
     * @param key Character
     * @return Whether a identifier part
     * @see Character.isJavaIdentifierPart
     */
    @JvmStatic
    fun isJavaIdentifierPart(key: Char): Boolean {
        return get(bitsIsPart, key.code)
    }

    /**
     * @param key Character
     * @return Whether a identifier start
     * @see Character.isJavaIdentifierStart
     */
    @JvmStatic
    fun isJavaIdentifierStart(key: Char): Boolean {
        return get(bitsIsStart, key.code)
    }

    @JvmStatic
    fun couldBeEmoji(cp: Int): Boolean {
        return cp in 0x1F000..0x1FAFF
    }

    @JvmStatic
    fun isFitzpatrick(cp: Int): Boolean {
        return cp in 0x1F3FB..0x1F3FF
    }

    @JvmStatic
    fun isZWJ(cp: Int): Boolean {
        return cp == 0x200D
    }

    @JvmStatic
    fun isZWNJ(cp: Int): Boolean {
        return cp == 0x200C
    }

    @JvmStatic
    fun isVariationSelector(cp: Int): Boolean {
        return cp == 0xFE0E || cp == 0xFE0F
    }

    @JvmStatic
    fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }
}
