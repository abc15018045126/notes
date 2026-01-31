package io.github.abc15018045126.sora.lang.completion.snippet.parser

import io.github.abc15018045126.sora.lang.completion.snippet.CodeSnippet
import io.github.abc15018045126.sora.lang.completion.snippet.ConditionalFormat
import io.github.abc15018045126.sora.lang.completion.snippet.FormatString
import io.github.abc15018045126.sora.lang.completion.snippet.NextUpperCaseFormat
import io.github.abc15018045126.sora.lang.completion.snippet.NoFormat
import io.github.abc15018045126.sora.lang.completion.snippet.PlaceHolderElement
import io.github.abc15018045126.sora.lang.completion.snippet.PlaceholderDefinition
import io.github.abc15018045126.sora.lang.completion.snippet.PlainPlaceholderElement
import io.github.abc15018045126.sora.lang.completion.snippet.Transform
import io.github.abc15018045126.sora.lang.completion.snippet.VariableItem
import java.util.ArrayList
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class CodeSnippetParser private constructor(
    private val src: String,
    definitions: MutableList<PlaceholderDefinition>
) {
    private val builder: CodeSnippet.Builder = CodeSnippet.Builder(definitions)
    private val tokenizer: CodeSnippetTokenizer = CodeSnippetTokenizer(src)
    private lateinit var token: Token

    private fun next() {
        if (token.type == TokenType.EOF) {
            return
        }
        token = tokenizer.nextToken()
    }

    private fun accept(type: TokenType): Boolean {
        if (token.type == type) {
            next()
            return true
        }
        return false
    }

    private fun _accept(type: TokenType): String? {
        if (token.type == type) {
            val text = src.substring(token.index, token.index + token.length)
            next()
            return text
        }
        return null
    }

    private fun accept(vararg types: TokenType): Boolean {
        for (type in types) {
            if (token.type == type) {
                next()
                return true
            }
        }
        return false
    }

    private fun _accept(vararg types: TokenType): String? {
        if (types.isEmpty()) {
            val text = src.substring(token.index, token.index + token.length)
            next()
            return text
        }
        for (type in types) {
            if (token.type == type) {
                val text = src.substring(token.index, token.index + token.length)
                next()
                return text
            }
        }
        return null
    }

    private fun backTo(token: Token) {
        tokenizer.moveTo(token.index + token.length)
        this.token = token
    }

    private fun parse() {
        token = tokenizer.nextToken()
        while (parseInternal()) {
            //empty
        }
    }

    private fun parseInternal(): Boolean {
        return parseEscaped() ||
                parseTabStopOrVariableName() ||
                parseComplexVariable() ||
                parseComplexPlaceholder() ||
                parseInterpolatedShell() ||
                parseAnything()
    }

    private fun parseEscaped(): Boolean {
        if (accept(TokenType.Backslash)) {
            var escaped = _accept(TokenType.CurlyClose, TokenType.Dollar, TokenType.Backslash, TokenType.Backtick)
            if (escaped == null) {
                escaped = "\\"
            }
            builder.addPlainText(escaped)
            return true
        }
        return false
    }

    private fun parseInterpolatedShell(): Boolean {
        val backup = token
        if (accept(TokenType.Backtick)) {
            val sb = StringBuilder()
            while (!accept(TokenType.Backtick)) {
                if (accept(TokenType.Backslash)) {
                    if (accept(TokenType.Backtick)) {
                        sb.append('`')
                    } else {
                        sb.append('\\')
                    }
                } else if (token.type == TokenType.EOF) {
                    backTo(backup)
                    return false
                } else {
                    sb.append(_accept())
                }
            }
            builder.addInterpolatedShell(sb.toString())
            return true
        }
        backTo(token)
        return false
    }

    private fun parseTabStopOrVariableName(): Boolean {
        val backup = token
        if (accept(TokenType.Dollar)) {
            var text: String?
            if (_accept(TokenType.Int).also { text = it } != null) {
                builder.addPlaceholder(text!!.toInt())
            } else if (_accept(TokenType.VariableName).also { text = it } != null) {
                builder.addVariable(text!!, null as String?)
            } else {
                backTo(backup)
                return false
            }
            return true
        }
        return false
    }

    private fun parseComplexVariable(): Boolean {
        val variable = _parseComplexVariable()
        if (variable != null) {
            builder.addVariable(variable)
        }
        return variable != null
    }

    private fun _parseComplexVariable(): VariableItem? {
        val backup = token
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen)) {
            val text = _accept(TokenType.VariableName)
            if (text != null) {
            val variableName = text
            var defaultValue: String? = null
            if (accept(TokenType.Colon)) {
                // ${name:xxx}
                val sb = StringBuilder()
                while (!accept(TokenType.CurlyClose)) {
                    if (accept(TokenType.Backslash)) {
                        val escaped = _accept(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose)
                        if (escaped != null) {
                            sb.append(escaped)
                        } else {
                            sb.append('\\')
                        }
                    } else if (token.type == TokenType.EOF) {
                        backTo(backup)
                        return null
                    } else {
                        sb.append(src, token.index, token.index + token.length)
                        next()
                    }
                }
                return VariableItem(-1, variableName, sb.toString())
            } else if (accept(TokenType.Forwardslash)) {
                // ${name/regexp/format/options}
                val transform = Transform()
                if (parseTransform(transform)) {
                    return VariableItem(-1, variableName, null, transform)
                }
                backTo(backup)
                return null
            } else if (accept(TokenType.CurlyClose)) {
                // ${name}
                return VariableItem(-1, variableName, "")
            } else {
                // missing token
                backTo(backup)
                return null
            }
            }
        }
        backTo(backup)
        return null
    }

    private fun parseComplexPlaceholder(): Boolean {
        val backup = token
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen)) {
            val text = _accept(TokenType.Int)
            if (text != null) {
            val idText = text
            if (accept(TokenType.Colon)) {
                // ${1:xxx}
                val elements = ArrayList<PlaceHolderElement>()
                while (!accept(TokenType.CurlyClose)) {
                    if (accept(TokenType.Backslash)) {
                        val escaped = _accept(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose)
                        val t: String
                        if (escaped != null) {
                            t = escaped
                        } else {
                            t = "\\"
                        }
                        appendPlaceholderElement(elements, t)
                    } else if (token.type == TokenType.EOF) {
                        backTo(backup)
                        return false
                    } else {
                        val v = parseSimpleVariableName()
                        if (v != null) {
                            elements.add(VariableItem(token.index, v, ""))
                            continue
                        }

                        val vi = _parseComplexVariable()
                        if (vi != null) {
                            vi.setIndex(token.index)
                            elements.add(vi)
                            continue
                        }

                        val t = src.substring(token.index, token.index + token.length)
                        appendPlaceholderElement(elements, t)
                        next()
                    }
                }
                val id = idText.toInt()
                builder.addComplexPlaceholder(id, elements)
            } else if (accept(TokenType.Pipe)) {
                // ${1|one,two,three|}
                val choices = ArrayList<String>()
                while (true) {
                    if (parseChoiceElement(choices)) {
                        if (accept(TokenType.Comma)) {
                            continue
                        }
                        if (accept(TokenType.Pipe) && accept(TokenType.CurlyClose)) {
                            builder.addPlaceholder(idText.toInt(), choices)
                            return true
                        }
                    }

                    backTo(backup)
                    return false
                }
            } else if (accept(TokenType.Forwardslash)) {
                // ${1/regexp/format/options}
                val transform = Transform()
                if (parseTransform(transform)) {
                    builder.addPlaceholder(idText.toInt(), transform)
                    return true
                }
                backTo(backup)
                return false
            } else if (accept(TokenType.CurlyClose)) {
                // ${1}
                builder.addPlaceholder(idText.toInt())
            } else {
                // missing token
                backTo(backup)
                return false
            }
            return true
            }
        }
        backTo(backup)
        return false
    }

    private fun parseSimpleVariableName(): String? {
        val backup = token
        if (accept(TokenType.Dollar)) {
            // Check for : $VARIABLE_NAME
            val v = _accept(TokenType.VariableName)
            if (v != null) {
                return v
            }
        }
        backTo(backup)
        return null
    }

    private fun parseChoiceElement(choices: MutableList<String>): Boolean {
        val backup = token
        val sb = StringBuilder()
        var text: String?
        while (token.type != TokenType.Comma && token.type != TokenType.Pipe) {
            if (accept(TokenType.Backslash)) {
                if (_accept(TokenType.Pipe, TokenType.Comma, TokenType.Backslash).also { text = it } != null) {
                    sb.append(text)
                } else {
                    sb.append('\\')
                }
            } else if (token.type != TokenType.EOF) {
                sb.append(_accept())
            } else {
                backTo(backup)
                return false
            }
        }
        if (sb.isEmpty()) {
            backTo(backup)
            return false
        }
        choices.add(sb.toString())
        return true
    }

    private fun parseTransform(transform: Transform): Boolean {
        // ...<regex>/<format>/<options>}
        val backup = token

        // (1) /regex
        val regexValue = StringBuilder()
        while (!accept(TokenType.Forwardslash)) {
            if (accept(TokenType.Backslash)) {
                if (accept(TokenType.Forwardslash)) {
                    regexValue.append('/')
                } else {
                    regexValue.append('\\')
                }
                continue
            }

            if (token.type != TokenType.EOF) {
                regexValue.append(_accept())
                continue
            }

            return false
        }

        // (2) /format
        val list = ArrayList<FormatString>()
        while (!accept(TokenType.Forwardslash)) {
            if (accept(TokenType.Backslash)) {
                var escaped: String?
                if (_accept(TokenType.Backslash, TokenType.Forwardslash).also { escaped = it } != null) {
                    list.add(NoFormat(escaped!!))
                } else if (_accept(TokenType.VariableName).also { escaped = it } != null) {
                    if ("u" == escaped) {
                        list.add(NextUpperCaseFormat())
                    } else {
                        list.add(NoFormat("\\$escaped"))
                    }
                } else {
                    list.add(NoFormat("\\"))
                }
                continue
            }

            if (parseFormatString(list) || parseAnything(list)) {
                continue
            }
            return false
        }

        // (3) /option
        val regexOptions = StringBuilder()
        while (!accept(TokenType.CurlyClose)) {
            if (token.type != TokenType.EOF) {
                regexOptions.append(_accept())
                continue
            }
            return false
        }

        try {
            var options = 0
            if (regexOptions.indexOf("i") != -1) {
                options = options or Pattern.CASE_INSENSITIVE
            }
            if (regexOptions.indexOf("m") != -1) {
                options = options or Pattern.MULTILINE
            }
            transform.globalMode = (regexOptions.indexOf("g") != -1)
            transform.regexp = Pattern.compile(regexValue.toString(), options)
        } catch (e: PatternSyntaxException) {
            backTo(backup)
            return false
        }
        transform.format = list
        return true
    }

    private fun parseFormatString(formatStrings: MutableList<FormatString>): Boolean {
        val backup = token
        if (!accept(TokenType.Dollar)) {
            return false
        }
        val complex = accept(TokenType.CurlyOpen)
        var text: String?
        if (_accept(TokenType.Int).also { text = it } == null) {
            backTo(backup)
            return false
        }
        val group = text!!.toInt()
        val format = ConditionalFormat()
        format.group = group
        if (complex) {
            if (accept(TokenType.Colon)) {
                if (accept(TokenType.Forwardslash)) {
                    // ${1:/upcase}
                    if (_accept(TokenType.VariableName).also { text = it } != null && accept(TokenType.CurlyClose)) {
                        format.shorthand = text
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.Plus)) {
                    // ${1:+<if>}
                    val ifValue = until(TokenType.CurlyClose)
                    if (ifValue != null) {
                        accept(TokenType.CurlyClose)
                        format.ifValue = ifValue
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.Dash)) {
                    val elseValue = until(TokenType.CurlyClose)
                    if (elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.QuestionMark)) {
                    val ifValue = until(TokenType.Colon)
                    accept(TokenType.Colon)
                    val elseValue = until(TokenType.CurlyClose)
                    if (ifValue != null && elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.ifValue = ifValue
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                } else {
                    val elseValue = until(TokenType.CurlyClose)
                    if (elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                }
            }
            backTo(backup)
            return false
        } else {
            // $1
            formatStrings.add(format)
            return true
        }
    }

    private fun parseAnything(formatStrings: MutableList<FormatString>): Boolean {
        return if (token.type == TokenType.EOF) {
            false
        } else {
            formatStrings.add(NoFormat(_accept()!!))
            true
        }
    }

    private fun parseAnything(): Boolean {
        return if (token.type == TokenType.EOF) {
            false
        } else {
            builder.addPlainText(_accept()!!)
            true
        }
    }

    private fun until(type: TokenType): String? {
        val backup = token
        val sb = StringBuilder()
        while (token.type != type) {
            if (token.type == TokenType.EOF) {
                backTo(backup)
                return null
            } else {
                var text: String?
                if (accept(TokenType.Backslash)) {
                    if (_accept(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose).also { text = it } != null) {
                        sb.append(text)
                    } else {
                        backTo(backup)
                        return null
                    }
                } else {
                    sb.append(_accept())
                }
            }
        }
        return sb.toString()
    }

    companion object {
        private fun appendPlaceholderElement(elements: ArrayList<PlaceHolderElement>, t: String) {
            if (elements.isNotEmpty()) {
                if (elements[elements.size - 1] is PlainPlaceholderElement) {
                    // merge with the last plain placeholder element
                    val plain = elements[elements.size - 1] as PlainPlaceholderElement
                    plain.text = plain.text + t
                    return
                }
            }
            elements.add(PlainPlaceholderElement(t))
        }

        @JvmStatic
        fun parse(snippet: String): CodeSnippet {
            return parse(snippet, ArrayList())
        }

        @JvmStatic
        fun parse(snippet: String, definitions: MutableList<PlaceholderDefinition>): CodeSnippet {
            val parser = CodeSnippetParser(snippet, definitions)
            parser.parse()
            return parser.builder.build()
        }
    }
}
