package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import kotlin.math.max
import kotlin.math.min

@UnsupportedUserUsage
class ComposingText {

    @JvmField
    var startIndex: Int = 0
    @JvmField
    var endIndex: Int = 0
    @JvmField
    var preSetComposing: Boolean = false

    fun set(start: Int, end: Int) {
        this.startIndex = start
        this.endIndex = end
    }

    fun adjustLength(length: Int) {
        this.endIndex = startIndex + length
    }

    fun reset() {
        this.endIndex = -1
        this.startIndex = -1
        preSetComposing = false
    }

    fun isComposing(): Boolean {
        return preSetComposing || (startIndex >= 0 && endIndex >= 0)
    }

    fun shiftOnInsert(insertStart: Int, insertEnd: Int) {
        val length = insertEnd - insertStart
        if (startIndex <= insertStart && endIndex >= insertStart) {
            endIndex += length
        }
        // Type 2, text is inserted before a diagnostic
        if (startIndex > insertStart) {
            startIndex += length
            endIndex += length
        }
    }

    fun shiftOnDelete(deleteStart: Int, deleteEnd: Int) {
        val length = deleteEnd - deleteStart
        // Compute cross length
        val sharedStart = max(deleteStart, startIndex)
        val sharedEnd = min(deleteEnd, endIndex)
        if (sharedEnd <= sharedStart) {
            // No shared region
            if (startIndex >= deleteEnd) {
                // Shift left
                startIndex -= length
                endIndex -= length
            }
        } else {
            // Has shared region
            val sharedLength = sharedEnd - sharedStart
            endIndex -= sharedLength
            if (startIndex > deleteStart) {
                // Shift left
                val shiftLeftCount = startIndex - deleteStart
                startIndex -= shiftLeftCount
                endIndex -= shiftLeftCount
            }
        }
    }
}
