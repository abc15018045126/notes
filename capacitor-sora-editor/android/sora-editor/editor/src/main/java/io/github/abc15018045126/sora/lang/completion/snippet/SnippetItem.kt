package io.github.abc15018045126.sora.lang.completion.snippet

abstract class SnippetItem : Cloneable {

    var startIndex: Int = 0
        private set
    var endIndex: Int = 0
        private set

    constructor() : this(0)

    constructor(index: Int) : this(index, index)

    constructor(start: Int, end: Int) {
        setIndex(start, end)
    }

    fun setIndex(index: Int) {
        setIndex(index, index)
    }

    fun setIndex(start: Int, end: Int) {
        this.startIndex = start
        this.endIndex = end
    }

    fun shiftIndex(deltaIndex: Int) {
        startIndex += deltaIndex
        endIndex += deltaIndex
    }

    public override abstract fun clone(): SnippetItem
}
