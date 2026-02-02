package io.github.abc15018045126.sora.text.bidi

import io.github.abc15018045126.sora.util.IntPair
import java.text.Bidi

/**
 * Helper class for reordering logical text runs to visual runs.
 *
 * @author abc15018045126
 */
class VisualDirections(dirs: Directions) : IDirections {
    private val runs: Array<RunInfo?> // Bidi.reorderVisually takes Object[]

    private class RunInfo(var range: Long, var level: Int)

    init {
        val runCount = dirs.runCount
        runs = arrayOfNulls(runCount)
        val paramLevels = ByteArray(runCount)
        for (i in 0 until runCount) {
            paramLevels[i] = dirs.getRunLevel(i).toByte()
            runs[i] = RunInfo(IntPair.pack(dirs.getRunStart(i), dirs.getRunEnd(i)), dirs.getRunLevel(i))
        }
        Bidi.reorderVisually(paramLevels, 0, runs, 0, runCount)
    }

    override val runCount: Int
        get() = runs.size

    override fun getRunStart(i: Int): Int {
        return IntPair.getFirst(runs[i]!!.range)
    }

    override fun getRunEnd(i: Int): Int {
        return IntPair.getSecond(runs[i]!!.range)
    }

    override fun getRunLevel(i: Int): Int {
        return runs[i]!!.level
    }

    override fun isRunRtl(i: Int): Boolean {
        return (getRunLevel(i) and 1) != 0
    }
}
