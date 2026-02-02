package io.github.abc15018045126.sora.text.bidi

import io.github.abc15018045126.sora.util.IntPair

/**
 * Manages directions in a text segment
 *
 * @author abc15018045126
 */
class Directions(private var runs: LongArray, private var length: Int) : IDirections {

    fun setData(runs: LongArray, length: Int) {
        this.runs = runs
        this.length = length
    }

    fun setLength(length: Int) {
        this.length = length
    }

    fun getLength(): Int {
        return length
    }

    override val runCount: Int
        get() = runs.size

    override fun getRunStart(i: Int): Int {
        return IntPair.getFirst(runs[i])
    }

    override fun getRunEnd(i: Int): Int {
        return if (i == runs.size - 1) length else getRunStart(i + 1)
    }

    override fun getRunLevel(i: Int): Int {
        return IntPair.getSecond(runs[i])
    }

    override fun isRunRtl(i: Int): Boolean {
        return (getRunLevel(i) and 1) == 1
    }
}
