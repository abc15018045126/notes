package io.github.abc15018045126.sora.util.regex

import java.util.ArrayList

class RegexBackrefParser(private val grammar: RegexBackrefGrammar) {

    fun parse(pattern: String, groupCount: Int): List<RegexBackrefToken> {
        val pat = pattern + '\u0000' // add an extra char to truncate trailing backref
        val result = ArrayList<RegexBackrefToken>()
        val escapeChar = grammar.escapeChar
        val backrefChar = grammar.backrefStartChar
        var index = 0
        val len = pat.length
        // State 0: Text
        // State 1: Right after escape character
        // State 2: Require first digit for backref
        // State 3: Scan reset digits for backref
        var state = 0
        var textStart = 0
        var currentGroup: Long = 0
        while (index < len) {
            val ch = pat[index]
            when (state) {
                0 -> {
                    if (ch == escapeChar) {
                        result.add(RegexBackrefToken(false, pat.substring(textStart, index), -1))
                        state = 1
                    } else if (ch == backrefChar) {
                        result.add(RegexBackrefToken(false, pat.substring(textStart, index), -1))
                        state = 2
                    }
                }
                1 -> {
                    if (ch == escapeChar || ch == backrefChar) {
                        result.add(RegexBackrefToken(false, ch.toString(), -1))
                    } else {
                        result.add(RegexBackrefToken(false, pat.substring(index - 1, index + 1), -1))
                    }
                    state = 0
                    textStart = index + 1
                }
                2 -> {
                    if (ch in '0'..'9') {
                        currentGroup = (ch - '0').toLong()
                        if (currentGroup <= groupCount) {
                            state = 3 // scan rest digits
                        } else {
                            // not backref, fallback to plain text
                            textStart = index - 1
                            index--
                            state = 0
                        }
                    } else {
                        // not backref, fallback to plain text
                        textStart = index - 1
                        index--
                        state = 0
                    }
                }
                3 -> {
                    if (ch in '0'..'9') {
                        val newGroup = currentGroup * 10 + (ch - '0')
                        if (newGroup <= groupCount) {
                            currentGroup = newGroup
                        } else {
                            result.add(RegexBackrefToken(true, null, currentGroup.toInt()))
                            textStart = index
                            state = 0
                        }
                    } else {
                        result.add(RegexBackrefToken(true, null, currentGroup.toInt()))
                        textStart = index
                        state = 0
                    }
                }
            }

            index++
        }

        if (state != 0) {
            throw IllegalArgumentException("illegal backref expression")
        } else {
            result.add(RegexBackrefToken(false, pat.substring(textStart, len - 1), -1))
        }

        return result
    }
}
