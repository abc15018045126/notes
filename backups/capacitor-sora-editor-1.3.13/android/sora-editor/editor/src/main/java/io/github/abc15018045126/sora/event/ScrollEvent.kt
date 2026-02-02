package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Reports a scroll in editor.
 * The scrolling action can either have run or be running when this event is generated and sent.
 * <p>
 * The returned x,y positions are usually positive when over-scrolling is disabled. They represent
 * the left-top position's pixel in editor.
 */
class ScrollEvent @JvmOverloads constructor(
    editor: CodeEditor,
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val cause: Int,
    val flingVelocityX: Float = 0f,
    val flingVelocityY: Float = 0f
) : Event(editor) {

    companion object {
        /**
         * Caused by thumb's exact movements
         */
        const val CAUSE_USER_DRAG = 1

        /**
         * Caused by fling after user's movements
         */
        const val CAUSE_USER_FLING = 2

        /**
         * Caused by calling {@link CodeEditor#ensurePositionVisible(int, int)}.
         * This can happen when this method is manually called or either the user edits the text
         */
        const val CAUSE_MAKE_POSITION_VISIBLE = 3

        /**
         * Caused by the user's thumb reaching the edge of editor viewport, which causes the editor to
         * scroll to move the selection to text currently outside the viewport.
         */
        const val CAUSE_TEXT_SELECTING = 4

        const val CAUSE_SCALE_TEXT = 5
    }
}
