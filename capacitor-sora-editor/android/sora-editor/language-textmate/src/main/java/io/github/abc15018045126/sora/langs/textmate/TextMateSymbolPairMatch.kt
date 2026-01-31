package io.github.abc15018045126.sora.langs.textmate

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.SymbolPairMatch
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.languageconfiguration.internal.model.AutoClosingPairConditional
import java.util.*

class TextMateSymbolPairMatch(private val language: TextMateLanguage) : SymbolPairMatch(DefaultSymbolPairs()) {

    private var enabled = true

    init {
        updatePair()
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            removeAllPairs()
        } else {
            updatePair()
        }
    }

    fun updatePair() {
        if (!enabled) return

        val config = language.languageConfiguration ?: return

        removeAllPairs()

        val surroundingPairs = config.surroundingPairs
        val autoClosingPairs = config.autoClosingPairs
        val mergePairs = ArrayList<AutoClosingPairConditional>()

        if (autoClosingPairs != null) {
            mergePairs.addAll(autoClosingPairs)
        }

        if (surroundingPairs != null) {
            for (surroundingPair in surroundingPairs) {
                val newPair = AutoClosingPairConditional(
                    surroundingPair.open, surroundingPair.close,
                    SURROUNDING_PAIR_FLAG_LIST
                )
                mergePairs.add(newPair)
            }
        }

        for (pair in mergePairs) {
            putPair(pair.open, SymbolPair(pair.open, pair.close, SymbolPairExImpl(pair)))
        }
    }

    private class SymbolPairExImpl(pair: AutoClosingPairConditional) : SymbolPair.SymbolPairEx {
        private var notInTokenTypeArray: IntArray? = null
        private var isSurroundingPair = false

        init {
            val notInList = pair.notIn
            if (notInList == null || notInList.isEmpty()) {
                notInTokenTypeArray = null
            } else {
                val mutableNotInList = ArrayList(notInList)
                if (mutableNotInList.contains(SURROUNDING_PAIR_FLAG)) {
                    isSurroundingPair = true
                    if (mutableNotInList.size == 1) {
                        notInTokenTypeArray = null
                    } else {
                        mutableNotInList.remove(SURROUNDING_PAIR_FLAG)
                    }
                }
                
                if (!isSurroundingPair || mutableNotInList.isNotEmpty()) {
                    notInTokenTypeArray = IntArray(mutableNotInList.size)
                    for (i in mutableNotInList.indices) {
                        val notInValue = mutableNotInList[i].lowercase()
                        var notInTokenType = StandardTokenType.String
                        when (notInValue) {
                            "string" -> notInTokenType = StandardTokenType.String
                            "comment" -> notInTokenType = StandardTokenType.Comment
                            "regex" -> notInTokenType = StandardTokenType.RegEx
                        }
                        notInTokenTypeArray!![i] = notInTokenType
                    }
                    notInTokenTypeArray!!.sort()
                }
            }
        }

        override fun shouldReplace(editor: CodeEditor, contentLine: ContentLine, leftColumn: Int): Boolean {
            if (editor.cursor.isSelected()) {
                return isSurroundingPair
            }
            if (isSurroundingPair) {
                return false
            }

            val array = notInTokenTypeArray ?: return true

            val cursor = editor.cursor
            val currentLine = cursor.leftLine
            val currentColumn = cursor.leftColumn

            val spansOnCurrentLine = editor.getSpansForLine(currentLine) ?: return true
            val currentSpan = binarySearchSpan(spansOnCurrentLine, currentColumn) ?: return true
            val extra = currentSpan.extra

            if (extra is Int) {
                val index = Arrays.binarySearch(array, extra)
                return index < 0
            }

            return true
        }

        private fun checkIndex(index: Int, max: Int): Int {
            return Math.max(Math.min(index, max), 0)
        }

        private fun binarySearchSpan(spanList: List<Span>, column: Int): Span? {
            if (spanList.isEmpty()) return null
            var start = 0
            var end = spanList.size - 1
            val size = spanList.size - 1

            while (start <= end) {
                val middle = (start + end) / 2
                val currentSpan = spanList[middle]
                if (currentSpan.column == column) {
                    return currentSpan
                }

                if (currentSpan.column < column) {
                    val nextSpan = spanList[checkIndex(middle + 1, size)]
                    if (nextSpan.column > column) {
                        return currentSpan
                    }
                    start++
                } else {
                    val previousSpan = spanList[checkIndex(middle - 1, size)]
                    if (previousSpan.column < column) {
                        return currentSpan
                    }
                    end--
                }
            }
            return spanList[Math.min(start, size)]
        }

        override fun shouldDoAutoSurround(content: Content): Boolean {
            return isSurroundingPair && content.getCursor().isSelected()
        }
    }

    companion object {
        private const val SURROUNDING_PAIR_FLAG = "surroundingPair"
        private val SURROUNDING_PAIR_FLAG_LIST = listOf(SURROUNDING_PAIR_FLAG)
    }
}
