package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.EditorSearcher

/**
 * This event happens when text is edited by the user, or the user click the view to change the
 * position of selection. Even when the actual values of CharPosition are not changed, you may receive the event.
 * <p>
 * Note that you should not change returned CharPosition objects because they are shared in an event
 * dispatch.
 */
class SelectionChangeEvent(
    editor: CodeEditor,
    val oldLeft: CharPosition?,
    val oldRight: CharPosition?,
    val cause: Int
) : Event(editor) {

    val left: CharPosition = editor.text.cursor.left()
    val right: CharPosition = editor.text.cursor.right()

    /**
     * Checks whether text is selected
     */
    val isSelected: Boolean
        get() = left.index != right.index

    companion object {
        /**
         * Unknown cause
         */
        const val CAUSE_UNKNOWN = 0

        /**
         * Selection change caused by text modifications
         */
        const val CAUSE_TEXT_MODIFICATION = 1

        /**
         * Set selection by handle
         */
        const val CAUSE_SELECTION_HANDLE = 2

        /**
         * Set selection by single tap
         */
        const val CAUSE_TAP = 3

        /**
         * Set selection because of {@link android.view.inputmethod.InputConnection#setSelection(int, int)}
         */
        const val CAUSE_IME = 4

        /**
         * Long press
         */
        const val CAUSE_LONG_PRESS = 5

        /**
         * Search text by {@link EditorSearcher}
         */
        const val CAUSE_SEARCH = 6

        /**
         * From keyboard or direct method invocation to change selection
         */
        const val CAUSE_KEYBOARD_OR_CODE = 7

        /**
         * From mouse
         */
        const val CAUSE_MOUSE_INPUT = 8

        /**
         * Caused by a dead key press
         */
        const val CAUSE_DEAD_KEYS = 9
    }
}
