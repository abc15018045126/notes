package io.github.abc15018045126.sora.lang.brackets

import io.github.abc15018045126.sora.text.Content

/**
 * Interface for providing paired brackets
 *
 * @author abc15018045126
 */
interface BracketsProvider {

    /**
     * Get left and right brackets position in text
     *
     * @param text  The text in editor
     * @param index Index of cursor in text
     * @return Paired positions or null if not matched
     */
    fun getPairedBracketAt(text: Content, index: Int): PairedBracket?

}
