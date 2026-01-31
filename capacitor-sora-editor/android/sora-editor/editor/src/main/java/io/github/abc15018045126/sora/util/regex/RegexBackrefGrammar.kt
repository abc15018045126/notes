package io.github.abc15018045126.sora.util.regex

data class RegexBackrefGrammar(
    @JvmField val backrefStartChar: Char,
    @JvmField val escapeChar: Char
) {
    companion object {
        @JvmField
        val DEFAULT = RegexBackrefGrammar('$', '\\')
    }
}
