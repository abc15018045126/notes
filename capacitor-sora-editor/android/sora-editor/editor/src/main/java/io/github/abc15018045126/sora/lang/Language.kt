package io.github.abc15018045126.sora.lang

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.completion.CompletionCancelledException
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.widget.SymbolPairMatch

/**
 * Language for editor
 *
 * A Language helps editor to highlight text and provide auto-completion.
 * Implement this interface when you want to add new language support for editor.
 *
 * **NOTE:** A language must not be single instance.
 * One language instance should always serve for only one editor.
 * It means that you should not give one language object to other editor instances
 * after it has been applied to one editor.
 *
 * @author abc15018045126
 */
interface Language {

    /**
     * Get [AnalyzeManager] of the language.
     * This is called from time to time by the editor. Cache your instance please.
     */
    val analyzeManager: AnalyzeManager

    /**
     * Get the interruption level for auto-completion.
     *
     * @see INTERRUPTION_LEVEL_STRONG
     * @see INTERRUPTION_LEVEL_SLIGHT
     * @see INTERRUPTION_LEVEL_NONE
     */
    val interruptionLevel: Int

    /**
     * Request to auto-complete the code at the given `position`.
     * This is called in a worker thread other than UI thread.
     *
     * @param content        Read-only reference of content
     * @param position       The position for auto-complete
     * @param publisher      The publisher used to update items
     * @param extraArguments Arguments set by [io.github.abc15018045126.sora.widget.CodeEditor.setText]
     * @throws io.github.abc15018045126.sora.lang.completion.CompletionCancelledException This thread can be abandoned
     * by the editor framework because the auto-completion items of
     * this invocation are no longer needed by the user. This can either be thrown
     * by [ContentReference] or [CompletionPublisher].
     * How the exceptions will be thrown is according to
     * your settings: [interruptionLevel]
     * @see ContentReference
     * @see CompletionPublisher
     * @see interruptionLevel
     */
    @WorkerThread
    @Throws(CompletionCancelledException::class)
    fun requireAutoComplete(
        content: ContentReference, position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    )

    /**
     * Get delta indent spaces count.
     *
     * @param content Content of given line.
     * @param line    0-indexed line number. The indentation is applied on line index: `line + 1`.
     * @param column  Column on the given line, where a line separator is inserted.
     * @return Delta count of indent spaces. It can be a negative/positive number or zero.
     */
    @UiThread
    fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int

    /**
     * Get delta indent spaces count.
     *
     * @param content          Content of given line.
     * @param line             0-indexed line number. The indentation is applied on line index: `line + 1`.
     * @param column           Column on the given line, where a line separator is inserted.
     * @param spaceCountOnLine The number of spaces on `line`.
     * @param tabCountOnLine   The number of tabs on `line`.
     * @return Delta count of indent spaces. It can be a negative/positive number or zero.
     */
    @UiThread
    fun getIndentAdvance(
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return getIndentAdvance(content, line, column)
    }

    /**
     * Use tab to format
     */
    @UiThread
    fun useTab(): Boolean


    /**
     * Get the code formatter for the current language.
     * The formatter is expected to be the same one during the lifecycle of a language instance.
     *
     * @return The code formatter for the current language.
     */
    @get:UiThread
    val formatter: Formatter

    /**
     * Returns language specified symbol pairs.
     * The method is called only once when the language is applied.
     */
    @get:UiThread
    val symbolPairs: SymbolPairMatch?

    /**
     * Get newline handlers of this language.
     * This method is called each time the user presses ENTER key.
     *
     * Pay attention to the performance as this method is called frequently
     *
     * @return NewlineHandlers , maybe null
     */
    @get:UiThread
    val newlineHandlers: Array<NewlineHandler>?

    /**
     * Get newline handlers of this language.
     * This method is called each time the user types a single character (or a single code point)
     * and some text is currently selected.
     *
     * Pay attention to the performance as this method is called frequently
     *
     * @return QuickQuoteHandler, maybe null
     */
    @get:UiThread
    val quickQuoteHandler: QuickQuoteHandler?
        get() = null

    /**
     * Destroy this [Language] object.
     *
     * When called, you should stop your resource-taking actions and remove any reference
     * of editor or other objects related to editor (such as references to text in editor) to avoid
     * memory leaks and resource waste.
     */
    @UiThread
    fun destroy()

    companion object {
        /**
         * Set the thread's interrupted flag by calling [Thread.interrupt].
         *
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference] and [CompletionPublisher].
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_STRONG = 0

        /**
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference] and [CompletionPublisher].
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_SLIGHT = 1

        /**
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference]
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_NONE = 2
    }

}
