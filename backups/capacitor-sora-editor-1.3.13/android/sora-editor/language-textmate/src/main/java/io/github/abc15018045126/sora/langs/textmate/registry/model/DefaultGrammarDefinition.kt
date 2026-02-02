package io.github.abc15018045126.sora.langs.textmate.registry.model

import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils
import org.eclipse.tm4e.core.registry.IGrammarSource

class DefaultGrammarDefinition private constructor(
    override val name: String,
    override val scopeName: String?,
    override val grammar: IGrammarSource,
    override val languageConfiguration: String?,
    private val _embeddedLanguages: Map<String, String>? = null
) : GrammarDefinition {

    override val embeddedLanguages: Map<String, String>
        get() = _embeddedLanguages ?: emptyMap()

    fun withEmbeddedLanguages(embeddedLanguages: Map<String, String>?): GrammarDefinition {
        if (embeddedLanguages == null) {
            return this
        }
        return DefaultGrammarDefinition(
            this.name, this.scopeName,
            this.grammar, this.languageConfiguration,
            embeddedLanguages
        )
    }

    companion object {
        @JvmStatic
        fun withGrammarSource(grammarSource: IGrammarSource): DefaultGrammarDefinition {
            val languageNameByPath = StringUtils.getFileNameWithoutExtension(grammarSource.filePath)
            return withGrammarSource(grammarSource, languageNameByPath, "source.$languageNameByPath")
        }

        @JvmStatic
        fun withLanguageConfiguration(
            grammarSource: IGrammarSource,
            languageConfigurationPath: String
        ): DefaultGrammarDefinition {
            val languageNameByPath = StringUtils.getFileNameWithoutExtension(grammarSource.filePath)
            return withLanguageConfiguration(
                grammarSource,
                languageConfigurationPath,
                languageNameByPath,
                "source.$languageNameByPath"
            )
        }

        @JvmStatic
        fun withLanguageConfiguration(
            grammarSource: IGrammarSource,
            languageConfigurationPath: String?,
            languageName: String,
            scopeName: String?
        ): DefaultGrammarDefinition {
            return DefaultGrammarDefinition(languageName, scopeName, grammarSource, languageConfigurationPath)
        }

        @JvmStatic
        fun withGrammarSource(
            grammarSource: IGrammarSource,
            languageName: String,
            scopeName: String?
        ): DefaultGrammarDefinition {
            return DefaultGrammarDefinition(languageName, scopeName, grammarSource, null)
        }
    }
}
