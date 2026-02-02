package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Notify that snippet controller state is changed.
 * <br/>
 * If action is {@link #ACTION_START} and any event receiver intercepts editor, the snippet edit will
 * stop before moving to any tab stop. And consequently, a {@link SnippetEvent} with action {@link #ACTION_STOP}
 * will be broadcast immediately.
 * <br/>
 * There is at least one tab stop in the list when action is {@link #ACTION_START} or {@link #ACTION_SHIFT}.
 * But no tab stop is left there when action is {@link #ACTION_STOP}. The last tab stop is where the selection
 * will be placed when the snippet is finished normally.
 *
 * @author abc15018045126
 */
class SnippetEvent(
    editor: CodeEditor,
    val action: Int,
    val currentTabStop: Int,
    val totalTabStop: Int
) : Event(editor) {

    companion object {
        /**
         * Called before controller shifts to any tab stop
         */
        const val ACTION_START = 1

        /**
         * Called when controller shifted to a tab stop
         */
        const val ACTION_SHIFT = 2

        /**
         * Called when controller <strong>has exited</strong> a snippet
         */
        const val ACTION_STOP = 3
    }
}
