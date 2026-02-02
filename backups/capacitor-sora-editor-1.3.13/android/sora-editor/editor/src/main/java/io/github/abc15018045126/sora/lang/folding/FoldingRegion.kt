package io.github.abc15018045126.sora.lang.folding

import io.github.abc15018045126.sora.util.IntPair

/**
 * Indicates a folding region
 */
class FoldingRegion internal constructor(
    private var start: Long,
    private var end: Long
) {
    var isCollapsed: Boolean = false
    private var children: MutableList<FoldingRegion>? = null

    constructor(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) : this(
        IntPair.pack(startLine, startColumn),
        IntPair.pack(endLine, endColumn)
    ) {
        if (startLine > endLine || (startLine == endLine && startColumn > endColumn)) {
            throw IllegalArgumentException("start > end")
        }
    }

    val startLine: Int
        get() = IntPair.getFirst(start)

    val startColumn: Int
        get() = IntPair.getSecond(start)

    val endLine: Int
        get() = IntPair.getFirst(end)

    val endColumn: Int
        get() = IntPair.getSecond(end)

    fun createChild(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): FoldingRegion {
        if (startLine < this.startLine || (startLine == this.startLine && startColumn < this.startColumn)) {
            throw IllegalArgumentException("child start is before parent start")
        }
        if (endLine > this.endLine || (endLine == this.endLine && endColumn > this.endColumn)) {
            throw IllegalArgumentException("child end is beyond parent end")
        }
        val child = FoldingRegion(startLine, startColumn, endLine, endColumn)
        if (children == null) {
            children = ArrayList()
        }
        children!!.add(child)
        return child
    }
}
