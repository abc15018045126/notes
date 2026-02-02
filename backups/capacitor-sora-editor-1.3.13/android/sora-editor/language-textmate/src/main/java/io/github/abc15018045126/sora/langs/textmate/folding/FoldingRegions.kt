/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/abc15018045126/sora-editor
 *    Copyright (C) 2020-2024  abc15018045126
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact abc15018045126 by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.abc15018045126.sora.langs.textmate.folding

import android.util.SparseIntArray
import java.util.Stack

class FoldingRegions(private val _startIndexes: SparseIntArray, private val _endIndexes: SparseIntArray) {
    private var _parentsComputed: Boolean = false

    init {
        if (_startIndexes.size() != _endIndexes.size() || _startIndexes.size() > IndentRange.MAX_FOLDING_REGIONS) {
            throw Exception("invalid startIndexes or endIndexes size")
        }
        this._parentsComputed = false
    }

    fun length(): Int {
        return this._startIndexes.size()
    }

    fun getStartLineNumber(index: Int): Int {
        return this._startIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }

    fun getEndLineNumber(index: Int): Int {
        return this._endIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }


    fun toRegion(index: Int): FoldingRegion {
        return FoldingRegion(this, index)
    }

    private fun isInsideLast(parentIndexes: Stack<Int>, startLineNumber: Int, endLineNumber: Int): Boolean {
        val index = parentIndexes[parentIndexes.size - 1]
        return this.getStartLineNumber(index) <= startLineNumber && this.getEndLineNumber(index) >= endLineNumber

    }

    @Throws(Exception::class)
    private fun ensureParentIndices() {
        if (!this._parentsComputed) {
            this._parentsComputed = true
            val parentIndexes = Stack<Int>()
            for (i in 0 until this._startIndexes.size()) {
                val startLineNumber = this._startIndexes.get(i)
                val endLineNumber = this._endIndexes.get(i)
                if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
                    throw Exception("startLineNumber or endLineNumber must not exceed " + IndentRange.MAX_LINE_NUMBER)
                }
                while (parentIndexes.isNotEmpty() && !isInsideLast(parentIndexes, startLineNumber, endLineNumber)) {
                    parentIndexes.pop()
                }
                val parentIndex = if (parentIndexes.isNotEmpty()) parentIndexes[parentIndexes.size - 1] else -1
                parentIndexes.push(i)
                this._startIndexes.put(i, startLineNumber + (parentIndex and 0xFF shl 24))
                this._endIndexes.put(i, endLineNumber + (parentIndex and 0xFF00 shl 16))
            }
        }
    }

    @Throws(Exception::class)
    fun getParentIndex(index: Int): Int {
        this.ensureParentIndices()
        val parent =
            (this._startIndexes.get(index) and IndentRange.MASK_INDENT ushr 24) + (this._endIndexes.get(index) and IndentRange.MASK_INDENT ushr 16)
        return if (parent == IndentRange.MAX_FOLDING_REGIONS) {
            -1
        } else parent
    }
}
