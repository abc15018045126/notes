package io.github.abc15018045126.sora.langs.textmate.registry.model

import org.eclipse.tm4e.core.registry.IGrammarSource

interface GrammarDefinition {
    val name: String
    val languageConfiguration: String?
    val scopeName: String?
    val embeddedLanguages: Map<String, String>
        get() = emptyMap()
    val grammar: IGrammarSource
}
