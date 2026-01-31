package io.github.abc15018045126.sora.lang.completion.snippet.parser

import android.util.SparseArray

class CodeSnippetTokenizer(private val value: String) {

    private var index: Int = 0
    private var length: Int = 0
    private var token: TokenType = TokenType.EOF

    init {
        index = 0
        length = 0
    }

    fun nextToken(): Token {
        token = nextTokenInternal()
        return Token(index, length, token)
    }

    private fun nextTokenInternal(): TokenType {
        index += length
        length = 0
        if (index >= value.length) {
            return TokenType.EOF
        }
        var ch = value[index]

        val staticType = staticTypes.get(ch.code)
        if (staticType != null) {
            length = 1
            return staticType
        }

        if (isDigitChar(ch)) {
            length = 1
            while (index + length < value.length && isDigitChar(value[index + length])) {
                length++
            }
            return TokenType.Int
        }

        if (isVariableChar(ch)) {
            length = 1
            while (index + length < value.length && run {
                    ch = value[index + length]
                    isVariableChar(ch) || isDigitChar(ch)
                }) {
                length++
            }
            return TokenType.VariableName
        }

        while (index + length < value.length && !isDigitChar(ch) && !isVariableChar(ch) && staticTypes.get(ch.code) == null) {
            length++
            if (index + length < value.length)
                ch = value[index + length]
        }
        return TokenType.Format
    }

    fun moveTo(index: Int) {
        this.index = index
        length = 0
    }

    fun getToken(): TokenType {
        return token
    }

    fun getTokenText(): String {
        return value.substring(index, index + length)
    }

    fun getTokenLength(): Int {
        return length
    }

    fun getTokenStartIndex(): Int {
        return index
    }

    fun getTokenEndIndex(): Int {
        return index + length
    }

    companion object {
        private val staticTypes = SparseArray<TokenType>()

        init {
            for (value in TokenType.values()) {
                if (value.targetCharacter != '\u0000') {
                    staticTypes.put(value.targetCharacter.code, value)
                }
            }
        }

        private fun isDigitChar(ch: Char): Boolean {
            return Character.isDigit(ch)
        }

        private fun isVariableChar(ch: Char): Boolean {
            return (ch in 'a'..'z') ||
                    (ch in 'A'..'Z') ||
                    ch == '_'
        }
    }
}
