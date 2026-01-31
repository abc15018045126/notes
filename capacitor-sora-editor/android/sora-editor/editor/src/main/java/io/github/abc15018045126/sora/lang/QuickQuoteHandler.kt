package io.github.abc15018045126.sora.lang

import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.TextRange

interface QuickQuoteHandler {

    /**
     * Checks whether the given input matches the requirement to invoke this handler
     *
     * @param candidateCharacter The character going to be inserted. Length can be 1 or 2.
     * @param text               Current text in editor
     * @param cursor             The range of cursor
     * @param style              Current code styles
     * @return Whether this handler consumed the event
     */
    fun onHandleTyping(
        candidateCharacter: String,
        text: Content,
        cursor: TextRange,
        style: Styles?
    ): HandleResult

    class HandleResult(
        private var consumed: Boolean,
        private var newCursorRange: TextRange?
    ) {
        fun isConsumed(): Boolean = consumed
        
        fun setConsumed(consumed: Boolean) {
            this.consumed = consumed
        }
        
        fun getNewCursorRange(): TextRange? = newCursorRange
        
        fun setNewCursorRange(newCursorRange: TextRange?) {
            this.newCursorRange = newCursorRange
        }
        
        companion object {
            @JvmField
            val NOT_CONSUMED = HandleResult(false, null)
        }
    }
}
