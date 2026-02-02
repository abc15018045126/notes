package io.github.abc15018045126.sora.util

/**
 * ArrayList for primitive type long
 *
 * @author abc15018045126
 */
class LongArrayList {

    private var data: LongArray = LongArray(64)
    private var length: Int = 0

    /**
     * Add a value at end
     */
    fun add(value: Long) {
        data[length++] = value
        if (data.size == length) {
            val newData = LongArray(length shl 1)
            System.arraycopy(data, 0, newData, 0, length)
            data = newData
        }
    }

    /**
     * Get length of the list
     */
    val size: Int
        get() = length

    fun size(): Int = length

    /**
     * Set element at given index to {@code value}
     * @throws ArrayIndexOutOfBoundsException if index is invalid
     */
    fun set(index: Int, value: Long) {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        data[index] = value
    }

    /**
     * Refers to C++ algorithm lower_bound().
     * Compare by {@link IntPair#getFirst(long)} on each element.
     * <p>
     * Note that, you guarantee the sequence in list is in ascendant order.
     *
     * @param key Target value
     * @return Index of target value, or index of the insertion point (that's the index of first element
     * bigger than {@code key} or array length)
     */
    fun lowerBoundByFirst(key: Int): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = IntPair.getFirst(data[mid])

            if (midVal < key)
                low = mid + 1
            else if (midVal > key)
                high = mid - 1
            else
                return mid // key found
        }
        return low  // key not found.
    }

    fun lowerBound(key: Long): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = data[mid]

            if (midVal < key)
                low = mid + 1
            else if (midVal > key)
                high = mid - 1
            else
                return mid // key found
        }
        return low  // key not found.
    }

    /**
     * Get element at given index
     * @throws ArrayIndexOutOfBoundsException if index is invalid
     */
    fun get(index: Int): Long {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        return data[index]
    }

    fun clear() {
        length = 0
    }
}
