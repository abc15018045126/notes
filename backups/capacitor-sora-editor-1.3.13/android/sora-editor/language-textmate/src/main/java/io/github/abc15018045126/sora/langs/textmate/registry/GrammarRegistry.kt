package io.github.abc15018045126.sora.langs.textmate.registry

import android.util.Pair
import io.github.abc15018045126.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder
import io.github.abc15018045126.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.abc15018045126.sora.langs.textmate.registry.model.ThemeModel
import io.github.abc15018045126.sora.langs.textmate.registry.reader.LanguageDefinitionReader
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.registry.Registry
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.io.InputStreamReader

class GrammarRegistry {

    private var registry: Registry? = Registry()
    private var parent: GrammarRegistry? = null

    private val languageConfigurationMap = LinkedHashMap<String, LanguageConfiguration>()
    private val scopeName2GrammarId = LinkedHashMap<String, Int>()
    private val grammarFileName2ScopeName = LinkedHashMap<String, String>()
    private val scopeName2GrammarDefinition = LinkedHashMap<String, GrammarDefinition>()

    constructor()

    constructor(parent: GrammarRegistry) {
        this.parent = parent
    }

    private fun initThemeListener() {
        val themeRegistry = ThemeRegistry.getInstance()
        val themeChangeListener = ThemeRegistry.ThemeChangeListener { newTheme ->
            try {
                setTheme(newTheme)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        if (!themeRegistry.hasListener(themeChangeListener)) {
            themeRegistry.addListener(themeChangeListener)
        }
    }

    @JvmOverloads
    fun findGrammar(scopeName: String, findInParent: Boolean = true): IGrammar? {
        val grammar = registry?.grammarForScopeName(scopeName)
        if (grammar != null) {
            return grammar
        }
        if (!findInParent) {
            return null
        }
        return parent?.findGrammar(scopeName, true)
    }

    @Deprecated("The grammar file and language configuration file should in most cases be on local file")
    @Synchronized
    fun languageConfigurationToGrammar(languageConfiguration: LanguageConfiguration, grammar: IGrammar) {
        languageConfigurationMap[grammar.scopeName] = languageConfiguration
    }

    @JvmOverloads
    fun findLanguageConfiguration(scopeName: String, findInParent: Boolean = true): LanguageConfiguration? {
        val languageConfiguration = languageConfigurationMap[scopeName]
        if (languageConfiguration != null) {
            return languageConfiguration
        }
        if (!findInParent) {
            return null
        }
        return parent?.findLanguageConfiguration(scopeName, true)
    }

    fun loadLanguageAndLanguageConfiguration(grammarDefinition: GrammarDefinition): Pair<IGrammar, LanguageConfiguration> {
        val grammar = loadGrammar(grammarDefinition)
        val languageConfiguration = findLanguageConfiguration(grammar.scopeName, false)
            ?: throw IllegalStateException("Language configuration not found for ${grammar.scopeName}")
        return Pair.create(grammar, languageConfiguration)
    }

    fun loadGrammars(builder: LanguageDefinitionListBuilder): List<IGrammar> {
        return loadGrammars(builder.build())
    }

    fun loadGrammars(list: List<GrammarDefinition>): List<IGrammar> {
        prepareLoadGrammars(list)
        return list.map { loadGrammar(it) }
    }

    fun loadGrammars(jsonPath: String): List<IGrammar> {
        return loadGrammars(LanguageDefinitionReader.read(jsonPath))
    }

    @Synchronized
    fun loadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val languageName = grammarDefinition.name
        val scopeName = grammarDefinition.scopeName
        if (grammarFileName2ScopeName.containsKey(languageName) && scopeName != null) {
            val loaded = registry?.grammarForScopeName(scopeName)
            if (loaded != null) return loaded
        }

        val grammar = doLoadGrammar(grammarDefinition)
        if (scopeName != null) {
            grammarFileName2ScopeName[languageName] = scopeName
            scopeName2GrammarDefinition[grammar.scopeName] = grammarDefinition
        }
        return grammar
    }

    @Synchronized
    private fun doLoadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val languageConfigurationPath = grammarDefinition.languageConfiguration
        if (languageConfigurationPath != null) {
            val languageConfigurationStream = FileProviderRegistry.getInstance()
                .tryGetInputStream(languageConfigurationPath)
            if (languageConfigurationStream != null) {
                val languageConfiguration = LanguageConfiguration.load(
                    InputStreamReader(languageConfigurationStream)
                )
                if (languageConfiguration != null) {
                    languageConfigurationMap[grammarDefinition.scopeName!!] = languageConfiguration
                }
            }
        }

        val reg = registry ?: throw IllegalStateException("Registry is disposed")
        val grammar: IGrammar
        if (grammarDefinition.embeddedLanguages.isEmpty()) {
            grammar = reg.addGrammar(grammarDefinition.grammar)
        } else {
            grammar = reg.addGrammar(
                grammarDefinition.grammar,
                null,
                getOrPullGrammarId(grammarDefinition.scopeName!!),
                findGrammarIds(grammarDefinition.embeddedLanguages)
            )
        }

        val targetScopeName = grammarDefinition.scopeName
        if (targetScopeName != null && grammar.scopeName != targetScopeName) {
            throw IllegalStateException(
                "The scope name loaded by the grammar file does not match the declared scope name, it should be $targetScopeName instead of ${grammar.scopeName}"
            )
        }
        return grammar
    }

    private fun prepareLoadGrammars(grammarDefinitions: List<GrammarDefinition>) {
        for (grammar in grammarDefinitions) {
            grammar.scopeName?.let { getOrPullGrammarId(it) }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun setTheme(themeModel: ThemeModel) {
        val reg = registry ?: throw IllegalStateException("Registry is disposed")
        if (!themeModel.isLoaded) {
            themeModel.load(reg.colorMap)
        }
        reg.setTheme(themeModel.theme)
    }

    @Synchronized
    private fun getOrPullGrammarId(scopeName: String): Int {
        var id = scopeName2GrammarId[scopeName]
        if (id == null) {
            id = scopeName2GrammarId.size + 2
        }
        scopeName2GrammarId[scopeName] = id
        return id
    }

    @Synchronized
    private fun findGrammarIds(scopeName2LanguageName: Map<String, String>): Map<String, Int> {
        val result = HashMap<String, Int>()
        for ((key, value) in scopeName2LanguageName) {
            result[key] = getOrPullGrammarId(getGrammarScopeName(value))
        }
        return result
    }

    private fun getGrammarScopeName(name: String): String {
        if (scopeName2GrammarDefinition.containsKey(name)) {
            return name
        }
        val grammarName = grammarFileName2ScopeName[name]
        return grammarName ?: name
    }

    @Synchronized
    @JvmOverloads
    fun dispose(closeParent: Boolean = false) {
        if (registry == null) {
            return
        }
        registry = null
        grammarFileName2ScopeName.clear()
        languageConfigurationMap.clear()
        scopeName2GrammarId.clear()
        scopeName2GrammarDefinition.clear()

        if (parent != null && closeParent) {
            parent?.dispose(true)
        }
    }

    companion object {
        @get:Synchronized
        private var internalInstance: GrammarRegistry? = null
            get() {
                if (field == null) {
                    field = GrammarRegistry()
                    field!!.initThemeListener()
                }
                return field
            }

        @JvmStatic
        fun getInstance(): GrammarRegistry = internalInstance!!
    }
}
