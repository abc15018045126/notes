package io.github.abc15018045126.sora.langs.textmate

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.WorkerThread
import io.github.abc15018045126.sora.lang.EmptyLanguage
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.completion.CompletionHelper
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher
import io.github.abc15018045126.sora.lang.completion.IdentifierAutoComplete
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler
import io.github.abc15018045126.sora.langs.textmate.registry.GrammarRegistry
import io.github.abc15018045126.sora.langs.textmate.registry.ThemeRegistry
import io.github.abc15018045126.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.abc15018045126.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.util.MyCharacter
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.io.Reader

class TextMateLanguage protected constructor(
    grammar: IGrammar,
    var languageConfiguration: LanguageConfiguration?,
    var grammarRegistry: GrammarRegistry,
    var themeRegistry: ThemeRegistry,
    val createIdentifiers: Boolean
) : EmptyLanguage() {

    var tabSize = 4
    private var useTab = false

    private val autoComplete = IdentifierAutoComplete()
    var isAutoCompleteEnabled = true
    
    var textMateAnalyzer: TextMateAnalyzer? = null
    private var _newlineHandlers: Array<NewlineHandler>? = null
    var symbolPairMatch: TextMateSymbolPairMatch
    private var newlineHandler: TextMateNewlineHandler? = null

    init {
        symbolPairMatch = TextMateSymbolPairMatch(this)
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    @WorkerThread
    @Deprecated("Use ThemeRegistry.setTheme")
    @Throws(Exception::class)
    fun updateTheme(theme: IThemeSource?) {
        theme?.let { themeRegistry.loadTheme(it) }
    }

    private fun createAnalyzerAndNewlineHandler(grammar: IGrammar, languageConfiguration: LanguageConfiguration?) {
        textMateAnalyzer?.let {
            it.setReceiver(null)
            it.destroy()
        }
        try {
            textMateAnalyzer = TextMateAnalyzer(this, grammar, languageConfiguration, themeRegistry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.languageConfiguration = languageConfiguration
        val handler = TextMateNewlineHandler(this)
        newlineHandler = handler
        _newlineHandlers = arrayOf(handler)
        if (languageConfiguration != null) {
            symbolPairMatch.updatePair()
        }
    }

    fun updateLanguage(scopeName: String) {
        val grammar = grammarRegistry.findGrammar(scopeName)
            ?: throw IllegalArgumentException("Grammar $scopeName not found")
        val configuration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
        createAnalyzerAndNewlineHandler(grammar, configuration)
    }

    fun updateLanguage(grammarDefinition: GrammarDefinition) {
        val grammar = grammarRegistry.loadGrammar(grammarDefinition)
            ?: throw IllegalArgumentException("Grammar ${grammarDefinition.scopeName} not found")
        val configuration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
        createAnalyzerAndNewlineHandler(grammar, configuration)
    }

    override fun getAnalyzeManager(): AnalyzeManager {
        return textMateAnalyzer ?: EmptyAnalyzeManager.INSTANCE
    }

    override fun destroy() {
        super.destroy()
    }

    override fun useTab(): Boolean = useTab

    fun setUseTab(useTab: Boolean) {
        this.useTab = useTab
    }

    fun getNewlineHandler(): TextMateNewlineHandler? = newlineHandler

    override fun getSymbolPairs(): TextMateSymbolPairMatch = symbolPairMatch

    override fun getNewlineHandlers(): Array<NewlineHandler>? = _newlineHandlers

    override fun requireAutoComplete(
        @NonNull content: ContentReference,
        @NonNull position: CharPosition,
        @NonNull publisher: CompletionPublisher,
        @NonNull extraArguments: Bundle
    ) {
        if (!isAutoCompleteEnabled) return
        val prefix = CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart)
        val idt = textMateAnalyzer?.syncIdentifiers
        autoComplete.requireAutoComplete(content, position, prefix, publisher, idt)
    }

    fun getAutoCompleter(): IdentifierAutoComplete = autoComplete

    fun setCompleterKeywords(keywords: Array<String>) {
        autoComplete.setKeywords(keywords, false)
    }

    companion object {
        @Deprecated("Use ThemeRegistry")
        @JvmStatic
        fun prepareLoad(grammarSource: IGrammarSource, languageConfiguration: Reader?, themeSource: IThemeSource?): IGrammar {
            val definition = DefaultGrammarDefinition.withGrammarSource(
                grammarSource,
                StringUtils.getFileNameWithoutExtension(grammarSource.filePath),
                null
            )
            val registry = GrammarRegistry.getInstance()
            val grammar = registry.loadGrammar(definition)
            if (languageConfiguration != null) {
                val config = LanguageConfiguration.load(languageConfiguration)
                if (config != null) {
                    registry.languageConfigurationToGrammar(config, grammar)
                }
            }
            val tRegistry = ThemeRegistry.getInstance()
            try {
                themeSource?.let { tRegistry.loadTheme(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return grammar
        }

        @Deprecated("Use scope name")
        @JvmStatic
        fun create(grammarSource: IGrammarSource, languageConfiguration: Reader?, themeSource: IThemeSource?): TextMateLanguage {
            val grammar = prepareLoad(grammarSource, languageConfiguration, themeSource)
            return create(grammar.scopeName, true)
        }

        @Deprecated("Use scope name")
        @JvmStatic
        fun create(grammarSource: IGrammarSource, themeSource: IThemeSource?): TextMateLanguage {
            val grammar = prepareLoad(grammarSource, null, themeSource)
            return create(grammar.scopeName, true)
        }

        @JvmStatic
        fun create(languageScopeName: String, autoCompleteEnabled: Boolean): TextMateLanguage {
            return create(languageScopeName, GrammarRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(languageScopeName: String, grammarRegistry: GrammarRegistry, autoCompleteEnabled: Boolean): TextMateLanguage {
            return create(languageScopeName, grammarRegistry, ThemeRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(
            languageScopeName: String,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.findGrammar(languageScopeName)
                ?: throw IllegalArgumentException("Language with $languageScopeName scope name not found")
            val configuration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
            return TextMateLanguage(grammar, configuration, grammarRegistry, themeRegistry, autoCompleteEnabled)
        }

        @JvmStatic
        fun create(grammarDefinition: GrammarDefinition, autoCompleteEnabled: Boolean): TextMateLanguage {
            return create(grammarDefinition, GrammarRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            return create(grammarDefinition, grammarRegistry, ThemeRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.loadGrammar(grammarDefinition)
                ?: throw IllegalArgumentException("Language with ${grammarDefinition.scopeName} scope name not found")
            val configuration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
            return TextMateLanguage(grammar, configuration, grammarRegistry, themeRegistry, autoCompleteEnabled)
        }
    }
}
