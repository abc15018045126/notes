package io.github.abc15018045126.sora.langs.textmate

import org.eclipse.tm4e.core.grammar.IStateStack
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult

class MyState(
    @JvmField var tokenizeState: IStateStack?,
    @JvmField var foldingCache: OnigResult?,
    @JvmField var indent: Int,
    @JvmField var identifiers: List<String>?
)
