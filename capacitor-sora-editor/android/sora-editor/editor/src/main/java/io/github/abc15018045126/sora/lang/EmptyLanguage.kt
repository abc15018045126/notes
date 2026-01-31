package io.github.abc15018045126.sora.lang

import android.os.Bundle
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler
import io.github.abc15018045126.sora.lang.util.BaseAnalyzeManager
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.widget.SymbolPairMatch

/**
 * Empty language
 *
 * @author abc15018045126
 */
open class EmptyLanguage : Language {

    override val formatter: Formatter
        get() = EmptyFormatter.INSTANCE

    override val symbolPairs: SymbolPairMatch
        get() = EMPTY_SYMBOL_PAIRS

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
    }

    override val interruptionLevel: Int
        get() = Language.INTERRUPTION_LEVEL_STRONG

    override val newlineHandlers: Array<NewlineHandler>?
        get() = emptyArray()

    override val analyzeManager: AnalyzeManager
        get() = EmptyAnalyzeManager.INSTANCE

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return 0
    }

    override val quickQuoteHandler: QuickQuoteHandler?
        get() = null

    override fun destroy() {}

    override fun useTab(): Boolean {
        return false
    }

    class EmptyFormatter : Formatter {
        override fun format(text: Content, cursorRange: TextRange) {}

        override fun formatRegion(text: Content, rangeToFormat: TextRange, cursorRange: TextRange) {}

        override fun setReceiver(receiver: Formatter.FormatResultReceiver?) {}

        override fun isRunning(): Boolean {
            return false
        }

        override fun destroy() {}

        companion object {
            @JvmField
            val INSTANCE = EmptyFormatter()
        }
    }

    class EmptyAnalyzeManager : BaseAnalyzeManager() {
        override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {}

        override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {}

        override fun rerun() {}

        companion object {
            @JvmField
            val INSTANCE = EmptyAnalyzeManager()
        }
    }

    companion object {
        @JvmField
        val EMPTY_SYMBOL_PAIRS = SymbolPairMatch()
    }
}
