
package io.github.abc15018045126.sora.langs.textmate.utils

import java.util.regex.MatchResult
import java.util.regex.Matcher

object MatcherUtils {

    @JvmStatic
    fun replaceAll(source: CharSequence, matcher: Matcher, replacer: (MatchResult) -> String): String {
        matcher.reset()
        val sb = StringBuilder()
        var appendPos = 0
        while (matcher.find()) {
            val result = matcher.toMatchResult()
            val replacement = replacer(result)
            sb.append(source, appendPos, result.start())
            sb.append(replacement)
            appendPos = result.end()
        }
        if (sb.isEmpty()) {
            // no match
            return source.toString()
        }
        sb.append(source, appendPos, source.length)
        return sb.toString()
    }

}
