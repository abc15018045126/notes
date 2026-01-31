package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.QuickQuoteHandler
import io.github.abc15018045126.sora.text.ContentReference

/**
 * Helper class for better Android API compatibility
 *
 * @author abc15018045126
 */
internal object LanguageHelper {

    @JvmStatic
    fun getQuickQuoteHandler(language: Language): QuickQuoteHandler? {
        return try {
            language.quickQuoteHandler
        } catch (e: AbstractMethodError) {
            null
        }
    }

    @JvmStatic
    fun getIndentAdvance(
        language: Language,
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return try {
            language.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine)
        } catch (e: AbstractMethodError) {
            language.getIndentAdvance(content, line, column)
        }
    }
}
