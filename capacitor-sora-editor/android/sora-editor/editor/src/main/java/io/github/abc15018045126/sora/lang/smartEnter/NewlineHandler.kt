package io.github.abc15018045126.sora.lang.smartEnter

import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content

/**
 * Perform text processing when user enters '\n' and selection size is 0
 */
interface NewlineHandler {

    /**
     * Checks whether the given input matches the requirement to invoke this handler
     *
     * @param text     Current text in editor
     * @param position The position of cursor
     * @param style    Current code styles
     * @return Whether this handler should be called
     */
    fun matchesRequirement(text: Content, position: CharPosition, style: Styles?): Boolean

    /**
     * Handle newline and return processed content to insert
     *
     * @param text     Current text in editor
     * @param position The position of cursor
     * @param style    Current code styles
     * @return Actual content to insert
     */
    fun handleNewline(text: Content, position: CharPosition, style: Styles?, tabSize: Int): NewlineHandleResult

}
