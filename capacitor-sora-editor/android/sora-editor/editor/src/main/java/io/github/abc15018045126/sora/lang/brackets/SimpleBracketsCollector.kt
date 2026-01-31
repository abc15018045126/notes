package io.github.abc15018045126.sora.lang.brackets

import android.util.SparseIntArray
import io.github.abc15018045126.sora.text.Content

/**
 * Collect brackets for simple languages. Not very effective. Not thread-safe.
 *
 * @author abc15018045126
 */
class SimpleBracketsCollector : BracketsProvider {

    private val mapping = SparseIntArray()

    /**
     * Add new pair
     */
    fun add(start: Int, end: Int) {
        // add 1 to avoid zeros
        mapping.put(start + 1, end + 1)
        mapping.put(end + 1, start + 1)
    }

    /**
     * Remove all pairs
     */
    fun clear() {
        mapping.clear()
    }

    private fun getForIndex(index: Int): PairedBracket? {
        var another = mapping.get(index + 1) - 1
        var currentIndex = index
        if (another > currentIndex) {
            val tmp = currentIndex
            currentIndex = another
            another = tmp
        }
        return if (another != -1) {
            PairedBracket(leftIndex = currentIndex, rightIndex = another)
        } else {
            null
        }
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        var res = if (index - 1 >= 0) getForIndex(index - 1) else null
        if (res == null) {
            res = getForIndex(index)
        }
        return res
    }
}
