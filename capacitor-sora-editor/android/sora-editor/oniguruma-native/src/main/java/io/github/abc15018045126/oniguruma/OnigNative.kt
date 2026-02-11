package io.github.abc15018045126.oniguruma

import java.nio.charset.StandardCharsets

/**
 * Oniguruma native function bridge.
 * Use with carefulness. Dirty pointer may cause a direct crash of your app.
 *
 * @author abc15018045126
 */
object OnigNative {

    init {
        System.loadLibrary("oniguruma-binding")
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern    Pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or null if it fails
     */
    @JvmStatic
    fun newRegex(pattern: String, ignoreCase: Boolean): Long {
        return newRegex(pattern.toByteArray(StandardCharsets.UTF_8), ignoreCase)
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern    UTF-8 Bytes of the pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or null if it fails
     */
    @JvmStatic
    fun newRegex(pattern: ByteArray, ignoreCase: Boolean): Long {
        return nCreateRegex(pattern, ignoreCase)
    }

    /**
     * Release a OnigRegex previously created by [newRegex]
     *
     * @param nativePtr Native pointer. Passing null pointer will have no effect.
     */
    @JvmStatic
    external fun releaseRegex(nativePtr: Long)

    /**
     * Search using the given OnigRegex.
     *
     * @param nativePointer OnigRegex pointer from [newRegex]
     * @param cacheKey  The cache key for the source string
     * @param str       String to be search in
     * @param start     Start position in the string (inclusive)
     * @param end       End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integer start and end.
     * Null if the match failed.
     */
    @JvmStatic
    fun regexSearch(nativePointer: Long, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray? {
        if (start > end || start < 0 || end > str.size) {
            throw IndexOutOfBoundsException("start:$start end:$end str.length:${str.size}")
        }
        return nRegexSearch(nativePointer, cacheKey, str, start, end)
    }

    /**
     * Search using the given OnigRegex list
     *
     * @param nativePointers OnigRegex pointers from [newRegex]
     * @param cacheKey   The cache key for the source string
     * @param str        String to be search in
     * @param start      Start position in the string (inclusive)
     * @param end        End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integer start and end.
     * Null if the match failed.
     */
    @JvmStatic
    fun regexSearchBatch(nativePointers: LongArray, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray? {
        if (start > end || start < 0 || end > str.size) {
            throw IndexOutOfBoundsException("start:$start end:$end str.length:${str.size}")
        }
        return nRegexSearchBatch(nativePointers, cacheKey, str, start, end)
    }

    @JvmStatic
    private external fun nCreateRegex(pattern: ByteArray, ignoreCase: Boolean): Long

    @JvmStatic
    private external fun nRegexSearch(nativePtr: Long, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray?

    @JvmStatic
    private external fun nRegexSearchBatch(nativePtrs: LongArray, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray?
}
