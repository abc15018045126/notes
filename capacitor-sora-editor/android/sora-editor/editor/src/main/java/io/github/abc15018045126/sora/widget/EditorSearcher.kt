package io.github.abc15018045126.sora.widget

import android.app.ProgressDialog
import android.widget.Toast
import androidx.annotation.IntRange
import io.github.abc15018045126.sora.I18nConfig
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.event.ContentChangeEvent
import io.github.abc15018045126.sora.event.PublishSearchResultEvent
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.TextUtils
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.LongArrayList
import io.github.abc15018045126.sora.util.regex.RegexBackrefGrammar
import io.github.abc15018045126.sora.util.regex.RegexBackrefHelper
import io.github.abc15018045126.sora.util.regex.RegexBackrefParser
import io.github.abc15018045126.sora.util.regex.RegexBackrefToken
import java.util.regex.Pattern

/**
 * Search text in editor.
 * Note that editor searches text in another thread, so results may not be available immediately. Also,
 * the searcher does not match empty text. For example, you will never match a single empty
 * line by regex '^.*$'. What's more, zero-length pattern is not permitted.
 * The searcher updates its search results automatically when editor text is changed, even after [CodeEditor.setText]
 * is invoked. So be careful that the search result is changing and [PublishSearchResultEvent] is
 * re-triggered when search result is available for changed text.
 *
 * @see PublishSearchResultEvent
 * @see SearchOptions
 * @author abc15018045126
 */
open class EditorSearcher(private val editor: CodeEditor) {

    @JvmField
    internal var currentPattern: String? = null

    @JvmField
    internal var searchOptions: SearchOptions? = null


    @JvmField
    internal var currentThread: Thread? = null


    /**
     * Search results. Note that it is naturally sorted by start index (and also end index).
     * No overlapping region is permitted.
     */
    @JvmField
    internal var lastResults: LongArrayList? = null


    var isCyclicJumping: Boolean = true

    init {
        this.editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            if (hasQuery()) {
                executeMatch()
            }
        }
    }

    /**
     * Search text with the given pattern and options. If you use [SearchOptions.TYPE_REGULAR_EXPRESSION],
     * the pattern will be your regular expression.
     *
     * [stopSearch] should be called if you want to stop, instead of invoking this method with nulls.
     *
     * Note that, the result is not immediately available because we search texts in another thread to
     * avoid lags in main thread. If you want to be notified when the results is available, refer to
     * [PublishSearchResultEvent]. Also be careful that, the event is also triggered when [stopSearch]
     * is called.
     * @throws IllegalArgumentException if pattern length is zero
     * @throws java.util.regex.PatternSyntaxException if pattern is invalid when regex is enabled.
     */
    fun search(pattern: String, options: SearchOptions) {
        if (pattern.isEmpty()) {
            throw IllegalArgumentException("pattern length must be > 0")
        }
        if (options.type == SearchOptions.TYPE_REGULAR_EXPRESSION) {
            // Pre-check
            Pattern.compile(pattern)
        }
        currentPattern = pattern
        searchOptions = options
        executeMatch()
        editor.postInvalidate()
    }

    /**
     * Execute current match task. Cancel any previous tasks.
     */
    private fun executeMatch() {
        if (currentThread != null && currentThread!!.isAlive) {
            currentThread!!.interrupt()
        }
        val options = searchOptions
        val pattern = currentPattern
        if (options != null && pattern != null) {
            val runnable = SearchRunnable(editor.text, options, pattern)
            currentThread = Thread(runnable)
            currentThread!!.start()
        }
    }

    /**
     * Stop searching.
     */
    fun stopSearch() {
        if (currentThread != null && currentThread!!.isAlive) {
            currentThread!!.interrupt()
        }
        currentThread = null
        lastResults = null
        currentPattern = null
        searchOptions = null
        editor.dispatchEvent(PublishSearchResultEvent(editor))
    }

    /**
     * Check if any search is in progress
     */
    fun hasQuery(): Boolean {
        return currentPattern != null
    }

    private fun checkState() {
        if (!hasQuery()) {
            throw IllegalStateException("pattern not set")
        }
    }

    /**
     * Find current selected region in search results and return the index in search result.
     * Or `-1` if result is not available or the current selected region is not in result.
     * @throws IllegalStateException if no search is in progress
     */
    val currentMatchedPositionIndex: Int
        get() {
            checkState()
            val cur = editor.cursor!!

            if (!cur.isSelected()) {
                return -1
            }
            val left = cur.left
            val right = cur.right

            if (isResultValid()) {
                val res = lastResults ?: return -1
                val packed = IntPair.pack(left, right)
                val index = res.lowerBound(packed)
                if (index < res.size && res.get(index) == packed) {
                    return index
                }
            }
            return -1
        }

    /**
     * Get item count of search result. Or `0` if result is not available or no item is found.
     * @throws IllegalStateException if no search is in progress
     */
    val matchedPositionCount: Int
        get() {
            checkState()
            if (!isResultValid()) {
                return 0
            }
            val result = lastResults
            return result?.size ?: 0
        }

    /**
     * Goto next matched position based on cursor position.
     * @see isCyclicJumping
     * @return if any jumping action is performed
     * @throws IllegalStateException if no search is in progress
     */
    fun gotoNext(): Boolean {
        checkState()
        if (isResultValid()) {
            val res = lastResults ?: return false
            val right = editor.cursor!!.right

            var index = res.lowerBoundByFirst(right)
            if (index == res.size && isCyclicJumping) {
                index = 0
            }
            if (index < res.size) {
                val data = res.get(index)
                val start = IntPair.getFirst(data)
                val pos1 = editor.text.indexer.getCharPosition(start)
                val pos2 = editor.text.indexer.getCharPosition(IntPair.getSecond(data))
                editor.setSelectionRegion(
                    pos1.line,
                    pos1.column,
                    pos2.line,
                    pos2.column,
                    SelectionChangeEvent.CAUSE_SEARCH
                )
                return true
            }
        }
        return false
    }

    /**
     * Goto last matched position based on cursor position.
     * @see isCyclicJumping
     * @return if any jumping action is performed
     * @throws IllegalStateException if no search is in progress
     */
    fun gotoPrevious(): Boolean {
        checkState()
        if (isResultValid()) {
            val res = lastResults
            if (res == null || res.size == 0) {
                return false
            }
            val left = editor.cursor!!.left

            var index = res.lowerBoundByFirst(left)
            if (index == res.size || IntPair.getFirst(res.get(index)) >= left) {
                index--
            }
            if (index < 0 && isCyclicJumping) {
                index = res.size - 1
            }
            if (index in 0 until res.size) {
                val data = res.get(index)
                val end = IntPair.getSecond(data)
                val pos1 = editor.text.indexer.getCharPosition(IntPair.getFirst(data))
                val pos2 = editor.text.indexer.getCharPosition(end)
                editor.setSelectionRegion(
                    pos1.line,
                    pos1.column,
                    pos2.line,
                    pos2.column,
                    SelectionChangeEvent.CAUSE_SEARCH
                )
                return true
            }
        }
        return false
    }

    /**
     * Check if selected region is exactly a search result
     * @throws IllegalStateException if no search is in progress
     */
    fun isMatchedPositionSelected(): Boolean {
        return currentMatchedPositionIndex > -1
    }

    /**
     * Replace currently selected region if the region is exactly a match of searching pattern.
     * Otherwise, attempt to jump to next matched position.
     *
     * @param replacement The text for replacement
     * @throws IllegalStateException if no search is in progress
     */
    fun replaceCurrentMatch(replacement: String) {
        if (!editor.isEditable) {
            return
        }
        if (isMatchedPositionSelected()) {
            if (replacement.isEmpty()) {
                editor.deleteText()
            } else {
                var finalReplacement = replacement
                val options = searchOptions
                if (options?.type == SearchOptions.TYPE_REGULAR_EXPRESSION &&
                    options.regexBackrefGrammar != null
                ) {
                    val cursor = editor.cursor!!
                    val currentText = editor.text.substring(cursor.left, cursor.right)

                    val pattern = Pattern.compile(
                        currentPattern!!,
                        (if (options.caseInsensitive) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    val matcher = pattern.matcher(currentText)
                    if (!matcher.find()) {
                        return
                    }
                    finalReplacement = RegexBackrefHelper.computeReplacement(
                        matcher,
                        options.regexBackrefGrammar,
                        replacement
                    )
                }
                editor.commitText(finalReplacement, false, false)
            }
        } else {
            gotoNext()
        }
    }

    /**
     * Replace all matched position. Note that after invoking this, a blocking [ProgressDialog]
     * is shown until the action is done (either succeeded or failed).
     * @param replacement The text for replacement
     * @throws IllegalStateException if no search is in progress
     */
    @JvmOverloads
    fun replaceAll(replacement: String, whenSucceeded: Runnable? = null) {
        if (!editor.isEditable) {
            return
        }
        checkState()
        if (!isResultValid()) {
            Toast.makeText(
                editor.context,
                I18nConfig.getResourceId(R.string.sora_editor_editor_search_busy),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val context = editor.context
        val dialog = ProgressDialog.show(
            context,
            I18nConfig.getString(context, R.string.sora_editor_replaceAll),
            I18nConfig.getString(context, R.string.sora_editor_editor_search_replacing),
            true,
            false
        )
        val res = lastResults!!
        val options = searchOptions!!
        val patternStr = currentPattern!!
        Thread {
            try {
                val sb = editor.text.toStringBuilder()
                val backrefGrammar = options.regexBackrefGrammar
                if (options.type == SearchOptions.TYPE_REGULAR_EXPRESSION && backrefGrammar != null) {
                    val regex = Pattern.compile(
                        patternStr,
                        (if (options.caseInsensitive) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    var matcher: java.util.regex.Matcher? = null
                    var tokens: List<RegexBackrefToken>? = null
                    var delta = 0
                    val text = sb.toString()
                    for (i in 0 until res.size) {
                        val region = res.get(i)
                        val start = IntPair.getFirst(region)
                        val end = IntPair.getSecond(region)
                        val regionText = text.substring(start, end)
                        if (matcher == null) {
                            matcher = regex.matcher(regionText)
                        } else {
                            matcher.reset(regionText)
                        }
                        if (!matcher.find()) {
                            continue
                        }
                        if (tokens == null) {
                            tokens = RegexBackrefParser(backrefGrammar).parse(
                                replacement,
                                matcher.groupCount()
                            )
                        }
                        val computedReplacement = RegexBackrefHelper.computeReplacement(matcher, tokens)
                        val newLength = computedReplacement.length
                        val oldLength = end - start
                        sb.replace(start + delta, end + delta, computedReplacement)
                        delta += newLength - oldLength
                    }
                } else {
                    val newLength = replacement.length
                    var delta = 0
                    for (i in 0 until res.size) {
                        val region = res.get(i)
                        val start = IntPair.getFirst(region)
                        val end = IntPair.getSecond(region)
                        val oldLength = end - start
                        sb.replace(start + delta, end + delta, replacement)
                        delta += newLength - oldLength
                    }
                }
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (editor.isReleased) return@post
                    val pos = editor.cursor!!.left()

                    editor.text.replace(
                        0,
                        0,
                        editor.lineCount - 1,
                        editor.text.getColumnCount(editor.lineCount - 1),
                        sb
                    )

                    editor.setSelectionAround(pos.line, pos.column)
                    dialog.dismiss()

                    whenSucceeded?.run()
                }
            } catch (e: Exception) {
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (editor.isReleased) return@post
                    Toast.makeText(editor.context, "Replace failed:$e", Toast.LENGTH_SHORT).show()

                    dialog.dismiss()
                }
            }
        }.start()
    }

    internal fun isResultValid(): Boolean {
        return currentThread == null || !currentThread!!.isAlive
    }


    /**
     * Search options for [EditorSearcher.search]
     */
    class SearchOptions @JvmOverloads constructor(
        @field:IntRange(from = 1, to = 3) @get:IntRange(from = 1, to = 3) val type: Int,
        val caseInsensitive: Boolean,
        val regexBackrefGrammar: RegexBackrefGrammar? = null
    ) {
        companion object {
            /**
             * Normal text searching
             */
            const val TYPE_NORMAL = 1

            /**
             * Text searching by whole word
             */
            const val TYPE_WHOLE_WORD = 2

            /**
             * Use regular expression for text searching
             */
            const val TYPE_REGULAR_EXPRESSION = 3
        }

        constructor(caseInsensitive: Boolean, useRegex: Boolean) : this(
            if (useRegex) TYPE_REGULAR_EXPRESSION else TYPE_NORMAL,
            caseInsensitive
        )

        init {
            if (type < 1 || type > 3) {
                throw IllegalArgumentException("invalid type")
            }
        }
    }

    /**
     * Run for regex matching
     */
    private inner class SearchRunnable(
        content: Content,
        private val options: SearchOptions,
        private val pattern: String
    ) : Runnable {
        private val text: StringBuilder = content.toStringBuilder()
        private var localThread: Thread? = null

        private fun checkNotCancelled(): Boolean {
            return currentThread == localThread && !Thread.interrupted()
        }

        override fun run() {
            localThread = Thread.currentThread()
            val results = LongArrayList()
            val textLength = text.length
            val ignoreCase = options.caseInsensitive
            var patternStr = this.pattern
            when (options.type) {
                SearchOptions.TYPE_NORMAL -> {
                    var nextStart = 0
                    val patternLength = patternStr.length
                    while (nextStart != -1 && nextStart < textLength && checkNotCancelled()) {
                        nextStart = TextUtils.indexOf(text, patternStr, ignoreCase, nextStart)
                        if (nextStart != -1) {
                            results.add(IntPair.pack(nextStart, nextStart + patternLength))
                            nextStart += patternLength
                        }
                    }
                }
                SearchOptions.TYPE_WHOLE_WORD -> {
                    patternStr = "\\b" + Pattern.quote(patternStr) + "\\b"
                    // fall-through
                    val regex = Pattern.compile(
                        patternStr,
                        (if (ignoreCase) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    val stringText = text.toString()
                    val matcher = regex.matcher(stringText)
                    while (matcher.find() && checkNotCancelled()) {
                        results.add(IntPair.pack(matcher.start(), matcher.end()))
                        if (matcher.end() == stringText.length) {
                            break
                        }
                    }
                }
                SearchOptions.TYPE_REGULAR_EXPRESSION -> {
                    val regex = Pattern.compile(
                        patternStr,
                        (if (ignoreCase) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    val stringText = text.toString()
                    val matcher = regex.matcher(stringText)
                    while (matcher.find() && checkNotCancelled()) {
                        results.add(IntPair.pack(matcher.start(), matcher.end()))
                        if (matcher.end() == stringText.length) {
                            break
                        }
                    }
                }
            }
            if (checkNotCancelled()) {
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (editor.isReleased) return@post
                    if (currentThread == localThread) {
                        lastResults = results
                        editor.invalidate()
                        editor.dispatchEvent(PublishSearchResultEvent(editor))
                        currentThread = null
                    }
                }

            }
        }
    }
}
