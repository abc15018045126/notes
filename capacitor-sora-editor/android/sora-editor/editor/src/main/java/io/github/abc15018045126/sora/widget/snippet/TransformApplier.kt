package io.github.abc15018045126.sora.widget.snippet

import io.github.abc15018045126.sora.lang.completion.snippet.ConditionalFormat
import io.github.abc15018045126.sora.lang.completion.snippet.FormatString
import io.github.abc15018045126.sora.lang.completion.snippet.NextUpperCaseFormat
import io.github.abc15018045126.sora.lang.completion.snippet.NoFormat
import io.github.abc15018045126.sora.lang.completion.snippet.Transform
import io.github.abc15018045126.sora.util.MyCharacter.isAlpha
import java.util.Locale
import java.util.regex.Matcher

/**
 * Utility class for applying [Transform] objects
 *
 * @author abc15018045126
 */
object TransformApplier {

    /**
     * Apply the given [Transform] to the text and return transform result
     *
     * @param text      the text to be transformed. must not be null
     * @param transform the [Transform] object describing how to transform the text, maybe null
     * @return the transformed text
     */
    @JvmStatic
    fun doTransform(text: String, transform: Transform?): String {
        val regexp = transform?.regexp
        val format = transform?.format
        if (transform == null || regexp == null || format == null) {
            return text
        }
        val sb = StringBuilder()
        val matcher = regexp.matcher(text)
        var loopCount = 0
        val limit = if (transform.globalMode) Int.MAX_VALUE else 1
        var nextIndex = 0
        while (loopCount < limit && nextIndex < text.length) {
            if (matcher.find(nextIndex)) {
                val start = matcher.start()
                val end = matcher.end()
                sb.append(text, nextIndex, start)
                sb.append(applySingle(matcher, format))
                nextIndex = end
            } else {
                break
            }
            loopCount++
        }
        if (nextIndex < text.length) {
            sb.append(text, nextIndex, text.length)
        }
        return sb.toString()
    }

    /**
     * Generate text for the given region in Matcher.
     *
     * @param matcher          the Matcher at the requested region
     * @param formatStringList the format descriptors
     * @return generated(transform) text
     */
    private fun applySingle(matcher: Matcher, formatStringList: List<FormatString>): CharSequence {
        val sb = StringBuilder()
        var nextUpperCase = false
        for (formatString in formatStringList) {
            when (formatString) {
                is NoFormat -> {
                    sb.append(applyFirstUpperCase(formatString.text, nextUpperCase))
                }
                is ConditionalFormat -> {
                    val group = matcher.group(formatString.group)
                    if (formatString.shorthand != null) {
                        if (group != null) {
                            when (formatString.shorthand) {
                                "upcase" -> sb.append(applyFirstUpperCase(group.uppercase(Locale.ROOT), nextUpperCase))
                                "lowcase" -> sb.append(applyFirstUpperCase(group.lowercase(Locale.ROOT), nextUpperCase))
                                else -> sb.append(applyFirstUpperCase(group, nextUpperCase))
                            }
                        }
                    } else {
                        val ifValue = formatString.ifValue ?: group
                        val elseValue = formatString.elseValue ?: ""
                        sb.append(applyFirstUpperCase(if (group != null) ifValue else elseValue, nextUpperCase))
                    }
                }
            }
            nextUpperCase = formatString is NextUpperCaseFormat
        }
        return sb
    }

    /**
     * Convenient method for applying upper case of first character only
     */
    private fun applyFirstUpperCase(text: String?, apply: Boolean): String? {
        if (apply && text != null && text.isNotEmpty() && isAlpha(text[0])) {
            return text[0].uppercaseChar().toString() + text.substring(1)
        }
        return text
    }

}
