package io.github.abc15018045126.sora.util

import kotlin.math.min

/**
 * This class provides region division and iteration with several {@link RegionProvider}s.
 *
 * @author abc15018045126
 */
open class RegionIterator(
    /**
     * Get length of the full region.
     */
    val max: Int,
    vararg providers: RegionProvider
) {

    private val providers: Array<out RegionProvider> = providers
    private val pointers: IntArray = IntArray(providers.size)
    private val pointerStates: BooleanArray = BooleanArray(providers.size)
    protected var start: Int = 0
    protected var end: Int = 0

    /**
     * Move to next region
     */
    fun nextRegion() {
        start = end
        var minNext = max
        for (i in providers.indices) {
            var next = max
            if (pointers[i] < providers[i].pointCount) {
                val value = providers[i].getPointAt(pointers[i])
                if (value <= max) {
                    next = value
                }
            }
            minNext = min(next, minNext)
        }
        end = minNext
        for (i in providers.indices) {
            if (pointers[i] < providers[i].pointCount && providers[i].getPointAt(pointers[i]) == minNext) {
                pointers[i]++
                pointerStates[i] = true
            } else {
                pointerStates[i] = false
            }
        }
    }

    /**
     * Check if we can move to next region
     */
    fun hasNextRegion(): Boolean {
        return end < max
    }

    /**
     * Get current index of dividing points in provider with given index {@code i}
     * @param i Index of provider
     * @return Current index of regions in that provider
     */
    fun getPointer(i: Int): Int {
        return pointers[i]
    }

    /**
     * Get the source index of dividing points in provider with given index {@code i}.
     * Source index is the index of dividing point that leads to current region.
     * @param i Index of provider
     */
    fun getRegionSourcePointer(i: Int): Int {
        val pointerValue = if (pointers[i] < providers[i].pointCount) providers[i].getPointAt(i) else max
        return if (end <= pointerValue && pointerValue < max || pointerStates[i]) pointers[i] - 1 else pointers[i]
    }

    fun getPointerValue(i: Int, j: Int): Int {
        val provider = providers[i]
        if (j < 0) {
            return 0
        }
        if (j >= provider.pointCount) {
            return max
        }
        val value = provider.getPointAt(j)
        return min(value, max)
    }

    /**
     * Get start index of the region
     * Note that this is inclusive.
     */
    fun getStartIndex(): Int {
        return start
    }

    /**
     * Get end index of the region.
     * Note that this is exclusive.
     */
    fun getEndIndex(): Int {
        return min(end, max)
    }

    /**
     * RegionProvider provides dividing points for {@link RegionIterator}. Note that the returned
     * sequence must follow a ascent order.
     */
    interface RegionProvider {

        /**
         * Get count of dividing points
         */
        val pointCount: Int

        /**
         * Get get dividing point at given index
         * @param index Index of point
         * @return Dividing index in region
         */
        fun getPointAt(index: Int): Int
    }
}
