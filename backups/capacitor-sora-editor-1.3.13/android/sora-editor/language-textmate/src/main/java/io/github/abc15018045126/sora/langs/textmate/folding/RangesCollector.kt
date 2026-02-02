package io.github.abc15018045126.sora.langs.textmate.folding

import android.util.SparseIntArray
import io.github.abc15018045126.sora.text.Content

class RangesCollector(/*int tabSize*/) {
    private val _startIndexes: SparseIntArray = SparseIntArray()
    private val _endIndexes: SparseIntArray = SparseIntArray()
    private var _length: Int = 0

    fun insertFirst(startLineNumber: Int, endLineNumber: Int, indent: Int) {
        if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
            return
        }
        val index = this._length
        this._startIndexes.put(index, startLineNumber)
        this._endIndexes.put(index, endLineNumber)
        this._length++
    }

    @Throws(Exception::class)
    fun toIndentRanges(model: Content?): FoldingRegions {
        return FoldingRegions(_startIndexes, _endIndexes)
    }
}
