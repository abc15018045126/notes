package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * This event happens when {@link CodeEditor#setText(CharSequence)} is called or
 * user edited the displaying content.
 * <p>
 * Note that you should not update the content at this time. Otherwise, there might be some
 * exceptions causing the editor framework to crash. If you do need to update the content, you should
 * post your actions to the main thread so that the user's modification will be successful.
 *
 * @author abc15018045126
 */
class ContentChangeEvent(
    editor: CodeEditor,
    val action: Int,
    val changeStart: CharPosition,
    val changeEnd: CharPosition,
    val changedText: CharSequence,
    val isCausedByUndoManager: Boolean
) : Event(editor) {

    companion object {
        /**
         * Notify that {@link CodeEditor#setText(CharSequence)} is called
         */
        const val ACTION_SET_NEW_TEXT = 1

        /**
         * Notify that user inserted some texts to the content
         */
        const val ACTION_INSERT = 2

        /**
         * Notify that user deleted some texts in the content
         */
        const val ACTION_DELETE = 3
    }
}
