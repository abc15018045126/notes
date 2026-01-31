package io.github.abc15018045126.sora.util.regex

import java.util.regex.Matcher

class RegexBackrefToken(
    val isReference: Boolean,
    val text: String?,
    val group: Int
) {
    fun getReplacementText(matcher: Matcher): String? {
        if (isReference) {
            return matcher.group(group)
        }
        return text
    }
}
