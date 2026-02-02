package io.github.abc15018045126.sora.langs.textmate

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.NonNull
import io.github.abc15018045126.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.abc15018045126.sora.lang.brackets.BracketsProvider
import io.github.abc15018045126.sora.lang.brackets.OnlineBracketsMatcher
import io.github.abc15018045126.sora.lang.completion.IdentifierAutoComplete
import io.github.abc15018045126.sora.lang.styling.CodeBlock
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.analysis.IncrementalAnalyzeManager
import io.github.abc15018045126.sora.lang.styling.color.ConstColor
import io.github.abc15018045126.sora.langs.textmate.folding.FoldingHelper
import io.github.abc15018045126.sora.langs.textmate.folding.IndentRange
import io.github.abc15018045126.sora.langs.textmate.registry.ThemeRegistry
import io.github.abc15018045126.sora.langs.textmate.registry.model.ThemeModel
import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.util.MyCharacter
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.EncodedTokenAttributes
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult
import org.eclipse.tm4e.core.internal.oniguruma.OnigString
import org.eclipse.tm4e.core.internal.oniguruma.Oniguruma
import org.eclipse.tm4e.core.internal.theme.FontStyle
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.time.Duration
import java.util.*

class TextMateAnalyzer(
    private val language: TextMateLanguage,
    private val grammar: IGrammar,
    languageConfiguration: LanguageConfiguration?,
    private val themeRegistry: ThemeRegistry
) : AsyncIncrementalAnalyzeManager<MyState?, Span?>(), FoldingHelper, ThemeRegistry.ThemeChangeListener {

    private var theme: Theme = themeRegistry.currentThemeModel.theme!!
    private val configuration: LanguageConfiguration? = languageConfiguration
    private var cachedRegExp: OnigRegExp? = null
    private var foldingOffside = false
    private var bracketsProvider: BracketsProvider? = null
    val syncIdentifiers = IdentifierAutoComplete.SyncIdentifiers()

    init {
        if (!themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this)
        }

        if (languageConfiguration != null) {
            val pairs = languageConfiguration.brackets
            if (pairs != null && pairs.isNotEmpty()) {
                var size = pairs.size
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        size--
                    }
                }
                val pairArr = CharArray(size * 2)
                var i = 0
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        continue
                    }
                    pairArr[i * 2] = pair.open[0]
                    pairArr[i * 2 + 1] = pair.close[0]
                    i++
                }
                bracketsProvider = OnlineBracketsMatcher(pairArr, 100000)
            }
        }

        createFoldingExp()
    }

    private fun createFoldingExp() {
        if (configuration == null) {
            return
        }
        val markers = configuration.folding ?: return
        foldingOffside = markers.offSide
        cachedRegExp = Oniguruma.newRegex("(" + markers.markersStart + ")|(?:" + markers.markersEnd + ")")
    }

    override fun getInitialState(): MyState? = null

    override fun stateEquals(state: MyState?, another: MyState?): Boolean {
        if (state == null && another == null) {
            return true
        }
        return if (state != null && another != null) {
            Objects.equals(state.tokenizeState, another.tokenizeState)
        } else false
    }

    override fun getIndentFor(line: Int): Int {
        return getState(line).state?.indent ?: 0
    }

    override fun getResultFor(line: Int): OnigResult? {
        return getState(line).state?.foldingCache
    }

    override fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock?> {
        val list = ArrayList<CodeBlock?>()
        analyzeCodeBlocks(text, list, delegate)
        if (delegate.isNotCancelled) {
            withReceiver { r -> r.updateBracketProvider(this, bracketsProvider) }
        }
        return list
    }

    fun analyzeCodeBlocks(model: Content, blocks: ArrayList<CodeBlock?>, delegate: CodeBlockAnalyzeDelegate) {
        if (cachedRegExp == null) {
            return
        }
        try {
            val foldingRegions = IndentRange.computeRanges(
                model,
                language.tabSize,
                foldingOffside,
                this,
                cachedRegExp,
                delegate
            )
            blocks.ensureCapacity(foldingRegions.length())
            for (i in 0 until foldingRegions.length()) {
                if (!delegate.isNotCancelled) break
                val startLine = foldingRegions.getStartLineNumber(i)
                val endLine = foldingRegions.getEndLineNumber(i)
                if (startLine != endLine) {
                    val codeBlock = CodeBlock()
                    codeBlock.toBottomOfEndLine = true
                    codeBlock.startLine = startLine
                    codeBlock.endLine = endLine

                    val length = model.getColumnCount(startLine)
                    val chars = model.getLine(startLine).backingCharArray

                    codeBlock.startColumn = IndentRange.computeStartColumn(chars, length, language.tabSize)
                    codeBlock.endColumn = codeBlock.startColumn
                    blocks.add(codeBlock)
                }
            }
            Collections.sort(blocks, CodeBlock.COMPARATOR_END)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        managedStyles.setIndentCountMode(true)
    }

    @SuppressLint("NewApi")
    @Synchronized
    override fun tokenizeLine(lineC: CharSequence, state: MyState?, lineIndex: Int): IncrementalAnalyzeManager.LineTokenizeResult<MyState?, Span?> {
        val line = if (lineC is ContentLine) lineC.toStringWithNewline() else lineC.toString()
        val tokens = ArrayList<Span?>()
        val surrogate = StringUtils.checkSurrogate(line)
        val lineTokens = grammar.tokenizeLine2(line, state?.tokenizeState, Duration.ofSeconds(2L))
        val tokensLength = lineTokens.tokens.size / 2
        val identifiers = if (language.createIdentifiers) ArrayList<String>() else null
        
        for (i in 0 until tokensLength) {
            val startIndex = StringUtils.convertUnicodeOffsetToUtf16(line, lineTokens.tokens[2 * i], surrogate)
            if (i == 0 && startIndex != 0) {
                tokens.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
            }
            val metadata = lineTokens.tokens[2 * i + 1]
            val foreground = EncodedTokenAttributes.getForeground(metadata)
            val fontStyle = EncodedTokenAttributes.getFontStyle(metadata)
            val tokenType = EncodedTokenAttributes.getTokenType(metadata)
            
            if (language.createIdentifiers && tokenType == StandardTokenType.Other) {
                val end = if (i + 1 == tokensLength) lineC.length 
                          else StringUtils.convertUnicodeOffsetToUtf16(line, lineTokens.tokens[2 * (i + 1)], surrogate)
                if (end > startIndex && MyCharacter.isJavaIdentifierStart(line[startIndex])) {
                    var flag = true
                    for (j in startIndex + 1 until end) {
                        if (!MyCharacter.isJavaIdentifierPart(line[j])) {
                            flag = false
                            break
                        }
                    }
                    if (flag) {
                        identifiers?.add(line.substring(startIndex, end))
                    }
                }
            }
            
            val span = SpanFactory.obtainNoExt(
                startIndex, TextStyle.makeStyle(
                    foreground + 255, 
                    0, 
                    (fontStyle and FontStyle.Bold) != 0, 
                    (fontStyle and FontStyle.Italic) != 0, 
                    false
                ).toLong()
            )

            span.setExtra(tokenType)

            if ((fontStyle and FontStyle.Underline) != 0) {
                val color = theme.getColor(foreground)
                if (color != null) {
                    span.underlineColor = ConstColor(Color.parseColor(color))
                }
            }

            tokens.add(span)
        }
        
        val backingChars = (lineC as? ContentLine)?.backingCharArray
        val indent = if (backingChars != null) {
            IndentRange.computeIndentLevel(backingChars, line.length - 1, language.tabSize)
        } else 0

        return IncrementalAnalyzeManager.LineTokenizeResult(
            MyState(
                lineTokens.ruleStack,
                cachedRegExp?.search(OnigString.of(line), 0),
                indent,
                identifiers
            ), 
            null, 
            tokens
        )
    }

    override fun onAddState(state: MyState?) {
        super.onAddState(state)
        if (state != null && language.createIdentifiers) {
            state.identifiers?.forEach { syncIdentifiers.identifierIncrease(it) }
        }
    }

    override fun onAbandonState(state: MyState?) {
        super.onAbandonState(state)
        if (state != null && language.createIdentifiers) {
            state.identifiers?.forEach { syncIdentifiers.identifierDecrease(it) }
        }
    }

    override fun reset(@NonNull content: ContentReference, @NonNull extraArguments: Bundle) {
        super.reset(content, extraArguments)
        syncIdentifiers.clear()
    }

    override fun destroy() {
        super.destroy()
        themeRegistry.removeListener(this)
    }

    override fun generateSpansForLine(tokens: IncrementalAnalyzeManager.LineTokenizeResult<MyState?, Span?>?): List<Span?>? {
        return null
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        this.theme = newTheme.theme!!
    }
}
