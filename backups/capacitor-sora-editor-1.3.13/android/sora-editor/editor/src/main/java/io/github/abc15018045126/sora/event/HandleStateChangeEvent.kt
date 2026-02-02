package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Notifies a selection handle's touch state has changed
 *
 * @author abc15018045126
 */
class HandleStateChangeEvent(
    editor: CodeEditor,
    val handleType: Int,
    val isHeld: Boolean
) : Event(editor) {

    companion object {
        const val HANDLE_TYPE_INSERT = 0
        const val HANDLE_TYPE_LEFT = 1
        const val HANDLE_TYPE_RIGHT = 2
    }
}
