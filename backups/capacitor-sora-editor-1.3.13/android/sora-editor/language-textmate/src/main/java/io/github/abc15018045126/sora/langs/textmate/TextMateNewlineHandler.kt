package io.github.abc15018045126.sora.langs.textmate

import android.util.Pair
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandleResult
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler
import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import org.eclipse.tm4e.languageconfiguration.internal.model.CompleteEnterAction
import org.eclipse.tm4e.languageconfiguration.internal.model.EnterAction
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import org.eclipse.tm4e.languageconfiguration.internal.supports.IndentRulesSupport
import org.eclipse.tm4e.languageconfiguration.internal.supports.OnEnterSupport
import org.eclipse.tm4e.languageconfiguration.internal.utils.TextUtils
import java.util.*

class TextMateNewlineHandler(private val language: TextMateLanguage) : NewlineHandler {

    private var enterSupport: OnEnterSupport? = null
    private var indentRulesSupport: IndentRulesSupport? = null
    private var enterAction: CompleteEnterAction? = null
    private var indentForEnter: Pair<String, String>? = null
    var isEnabled = true
    private var languageConfiguration: LanguageConfiguration? = language.languageConfiguration

    init {
        val config = languageConfiguration
        if (config != null) {
            val enterRules = config.onEnterRules
            val brackets = config.brackets
            val indentationRules = config.indentationRules

            if (enterRules != null) {
                enterSupport = OnEnterSupport(brackets, enterRules)
            }
            if (indentationRules != null) {
                indentRulesSupport = IndentRulesSupport(indentationRules)
            }
        }
    }

    override fun matchesRequirement(@NonNull text: Content, @NonNull position: CharPosition, @Nullable style: Styles?): Boolean {
        if (!isEnabled) return false

        enterAction = getEnterAction(text, position)
        indentForEnter = null

        if (enterAction == null) {
            indentForEnter = getIndentForEnter(text, position)
        }

        return enterAction != null || indentForEnter != null
    }

    @NonNull
    override fun handleNewline(
        @NonNull text: Content,
        @NonNull position: CharPosition,
        @Nullable style: Styles?,
        tabSize: Int
    ): NewlineHandleResult {
        val delim = "\n"
        val indentForEnter = this.indentForEnter
        if (indentForEnter != null) {
            val normalIndent = normalizeIndentation(indentForEnter.second)
            return NewlineHandleResult(delim + normalIndent, 0)
        }

        val action = enterAction ?: return NewlineHandleResult("", 0)
        return when (action.indentAction) {
            EnterAction.IndentAction.None, EnterAction.IndentAction.Indent -> {
                val increasedIndent = normalizeIndentation(action.indentation + action.appendText)
                NewlineHandleResult(delim + increasedIndent, 0)
            }
            EnterAction.IndentAction.IndentOutdent -> {
                val normalIndent = normalizeIndentation(action.indentation)
                val increasedIndent = normalizeIndentation(action.indentation + action.appendText)
                val typeText = delim + increasedIndent + delim + normalIndent
                val caretOffset = normalIndent.length + 1
                NewlineHandleResult(typeText, caretOffset)
            }
            EnterAction.IndentAction.Outdent -> {
                val indentation = TextUtils.getIndentationFromWhitespace(action.indentation, language.tabSize, language.useTab())
                val outdentedText = outdentString(normalizeIndentation(indentation + action.appendText))
                val caretOffset = outdentedText.length + 1
                NewlineHandleResult(outdentedText, caretOffset)
            }
            else -> NewlineHandleResult("", 0)
        }
    }

    protected fun getIndentForEnter(text: Content, position: CharPosition): Pair<String, String>? {
        val currentLineText = text.getLineString(position.line)
        val beforeEnterText = currentLineText.substring(0, position.column)
        val afterEnterText = currentLineText.substring(position.column)

        val rulesSupport = indentRulesSupport ?: return null

        val beforeEnterIndent = TextUtils.getLeadingWhitespace(beforeEnterText, 0, beforeEnterText.length)
        val afterEnterAction = getInheritIndentForLine(WrapperContentImp(text, position.line, beforeEnterText), true, position.line + 1)

        if (afterEnterAction == null) {
            return Pair(beforeEnterIndent, beforeEnterIndent)
        }

        var afterEnterIndent = afterEnterAction.indentation
        val indent = if (language.useTab()) "\t" else " ".repeat(language.tabSize)

        if (afterEnterAction.action == EnterAction.IndentAction.Indent) {
            afterEnterIndent = beforeEnterIndent + indent
        }

        if (rulesSupport.shouldDecrease(afterEnterText)) {
            afterEnterIndent = beforeEnterIndent.substring(0, Math.max(0, beforeEnterIndent.length - indent.length))
        }

        return Pair(beforeEnterIndent, afterEnterIndent)
    }

    private fun getInheritIndentForLine(wrapperContent: WrapperContent, honorIntentionalIndent: Boolean, line: Int): InheritIndentResult? {
        if (line <= 0) return InheritIndentResult("", null)

        val precedingUnIgnoredLine = getPrecedingValidLine(wrapperContent, line)
        if (precedingUnIgnoredLine <= -1) return null

        val rulesSupport = indentRulesSupport ?: return null
        val precedingUnIgnoredLineContent = wrapperContent.getLineContent(precedingUnIgnoredLine)

        if (rulesSupport.shouldIncrease(precedingUnIgnoredLineContent) || rulesSupport.shouldIndentNextLine(precedingUnIgnoredLineContent)) {
            return InheritIndentResult(TextUtils.getLeadingWhitespace(precedingUnIgnoredLineContent), EnterAction.IndentAction.Indent, precedingUnIgnoredLine)
        } else if (rulesSupport.shouldDecrease(precedingUnIgnoredLineContent)) {
            return InheritIndentResult(TextUtils.getLeadingWhitespace(precedingUnIgnoredLineContent), null, precedingUnIgnoredLine)
        } else {
            if (precedingUnIgnoredLine == 0) {
                return InheritIndentResult(TextUtils.getLeadingWhitespace(wrapperContent.getLineContent(precedingUnIgnoredLine)), null, precedingUnIgnoredLine)
            }

            val previousLine = precedingUnIgnoredLine - 1
            val previousLineIndentMetadata = rulesSupport.getIndentMetadata(wrapperContent.getLineContent(previousLine))

            if (((previousLineIndentMetadata and (IndentRulesSupport.IndentConsts.INCREASE_MASK or IndentRulesSupport.IndentConsts.DECREASE_MASK)) == 0) &&
                (previousLineIndentMetadata and IndentRulesSupport.IndentConsts.INDENT_NEXTLINE_MASK) == 0 && previousLineIndentMetadata > 0
            ) {
                var stopLine = 0
                for (i in previousLine - 1 downTo 1) {
                    if (rulesSupport.shouldIndentNextLine(wrapperContent.getLineContent(i))) continue
                    stopLine = i
                    break
                }
                return InheritIndentResult(TextUtils.getLeadingWhitespace(wrapperContent.getLineContent(stopLine + 1)), null, stopLine + 1)
            }

            if (honorIntentionalIndent) {
                return InheritIndentResult(TextUtils.getLeadingWhitespace(wrapperContent.getLineContent(precedingUnIgnoredLine)), null, precedingUnIgnoredLine)
            }

            for (i in precedingUnIgnoredLine downTo 1) {
                val lineContent = wrapperContent.getLineContent(i)
                if (rulesSupport.shouldIncrease(lineContent)) {
                    return InheritIndentResult(TextUtils.getLeadingWhitespace(lineContent), EnterAction.IndentAction.Indent, i)
                } else if (rulesSupport.shouldIndentNextLine(lineContent)) {
                    var stopLine = 0
                    for (j in i - 1 downTo 1) {
                        if (rulesSupport.shouldIndentNextLine(wrapperContent.getLineContent(i))) continue
                        stopLine = j
                        break
                    }
                    return InheritIndentResult(TextUtils.getLeadingWhitespace(wrapperContent.getLineContent(stopLine + 1)), null, stopLine + 1)
                } else if (rulesSupport.shouldDecrease(lineContent)) {
                    return InheritIndentResult(TextUtils.getLeadingWhitespace(lineContent), null, i)
                }
            }
            return InheritIndentResult(TextUtils.getLeadingWhitespace(wrapperContent.getLineContent(1)), null, 1)
        }
    }

    internal fun getPrecedingValidLine(content: WrapperContent, lineNumber: Int): Int {
        val rulesSupport = indentRulesSupport ?: return -1
        if (lineNumber > 0) {
            for (lastLineNumber in lineNumber - 1 downTo 0) {
                val text = content.getLineContent(lastLineNumber)
                if (rulesSupport.shouldIgnore(text) || text.isEmpty()) continue
                return lastLineNumber
            }
        }
        return -1
    }

    @Nullable
    fun getEnterAction(content: Content, position: CharPosition): CompleteEnterAction? {
        var indentation = TextUtils.getLinePrefixingWhitespaceAtPosition(content, position)
        val onEnterSupport = enterSupport ?: return null

        val scopedLineText = content.getLineString(position.line)
        val beforeEnterText = scopedLineText.substring(0, position.column)
        val afterEnterText = scopedLineText.substring(position.column)

        var previousLineText = ""
        if (position.line > 0) {
            previousLineText = content.getLineString(position.line - 1)
        }

        val enterResult: EnterAction = try {
            onEnterSupport.onEnter(previousLineText, beforeEnterText, afterEnterText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return null

        val indentAction = enterResult.indentAction
        var appendText = enterResult.appendText
        val removeText = enterResult.removeText

        if (appendText == null) {
            appendText = if (indentAction == EnterAction.IndentAction.Indent || indentAction == EnterAction.IndentAction.IndentOutdent) {
                "\t"
            } else {
                ""
            }
        } else if (indentAction == EnterAction.IndentAction.Indent) {
            appendText = "\t" + appendText
        }

        if (removeText != null) {
            indentation = indentation.substring(0, indentation.length - removeText)
        }

        return CompleteEnterAction(indentAction, appendText, removeText, indentation)
    }

    private fun outdentString(str: String): String {
        if (str.startsWith("\t")) return str.substring(1)

        if (!language.useTab()) {
            val spaces = " ".repeat(language.tabSize)
            if (str.startsWith(spaces)) return str.substring(spaces.length)
        }
        return str
    }

    private fun normalizeIndentation(str: String): String {
        return TextUtils.normalizeIndentation(str, language.tabSize, !language.useTab())
    }

    internal class InheritIndentResult(
        var indentation: String,
        var action: EnterAction.IndentAction?,
        var line: Int = 0
    )

    private open class WrapperContentImp(
        private val content: Content,
        private val line: Int,
        private val currentLineContent: String
    ) : WrapperContent {
        override fun getOrigin(): Content = content
        override fun getLineContent(line: Int): String {
            return if (line == this.line) currentLineContent else content.getLineString(line)
        }
    }

    internal interface WrapperContent {
        fun getOrigin(): Content
        fun getLineContent(line: Int): String
    }
}
