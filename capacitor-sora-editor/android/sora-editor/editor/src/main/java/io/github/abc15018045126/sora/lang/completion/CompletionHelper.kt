package io.github.abc15018045126.sora.lang.completion

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.widget.component.EditorAutoCompletion

/**
 * Helper class for completion
 *
 * @author abc15018045126
 */
object CompletionHelper {

    /**
     * Searches backward on the line, with the given checker to check chars.
     * Returns the longest text that matches the requirement
     */
    @JvmStatic
    fun computePrefix(ref: ContentReference, pos: CharPosition, checker: PrefixChecker): String {
        var begin = pos.column
        val line = ref.getLine(pos.line)
        while (begin > 0) {
            if (!checker.check(line[begin - 1])) {
                break
            }
            begin--
        }
        return line.substring(begin, pos.column)
    }

    /**
     * Check whether the thread is abandoned by editor.
     * Return true if it is cancelled by editor.
     */
    @JvmStatic
    fun checkCancelled(): Boolean {
        val thread = Thread.currentThread()
        return if (thread is EditorAutoCompletion.CompletionThread) {
            thread.isCancelled
        } else {
            false
        }
    }

    fun interface PrefixChecker {
        fun check(ch: Char): Boolean
    }
}
