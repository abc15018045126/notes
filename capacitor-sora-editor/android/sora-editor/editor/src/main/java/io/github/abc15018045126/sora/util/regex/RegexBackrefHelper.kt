package io.github.abc15018045126.sora.util.regex

import java.util.regex.Matcher

object RegexBackrefHelper {

    @JvmStatic
    fun computeReplacement(matcher: Matcher, grammar: RegexBackrefGrammar, replacementPattern: String): String {
        val parser = RegexBackrefParser(grammar)
        val tokens = parser.parse(replacementPattern, matcher.groupCount())
        return computeReplacement(matcher, tokens)
    }

    @JvmStatic
    fun computeReplacement(matcher: Matcher, tokens: List<RegexBackrefToken>): String {
        val sb = StringBuilder()
        for (token in tokens) {
            if (token.isReference) {
                val text = matcher.group(token.group)
                sb.append(text ?: "")
            } else {
                sb.append(token.text)
            }
        }
        return sb.toString()
    }
}
