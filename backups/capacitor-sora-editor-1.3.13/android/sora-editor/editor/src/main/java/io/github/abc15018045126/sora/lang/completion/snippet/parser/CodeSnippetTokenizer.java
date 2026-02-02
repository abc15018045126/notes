
package io.github.abc15018045126.sora.lang.completion.snippet.parser;

import android.util.SparseArray;

import java.util.Objects;

public class CodeSnippetTokenizer {

    private int index;
    private int length;
    private final String value;
    private TokenType token = TokenType.EOF;

    public CodeSnippetTokenizer(String input) {
        index = length = 0;
        this.value = Objects.requireNonNull(input);
    }

    public Token nextToken() {
        token = nextTokenInternal();
        return new Token(index, length, token);
    }

    private TokenType nextTokenInternal() {
        index += length;
        length = 0;
        if (index >= value.length()) {
            return TokenType.EOF;
        }
        char ch = value.charAt(index);

        var staticType = staticTypes.get(ch);
        if (staticType != null) {
            length = 1;
            return staticType;
        }

        if (isDigitChar(ch)) {
            length = 1;
            while (index + length < value.length() && isDigitChar(value.charAt(index + length))) {
                length++;
            }
            return TokenType.Int;
        }

        if (isVariableChar(ch)) {
            length = 1;
            while (index + length < value.length() && (isVariableChar(ch = value.charAt(index + length)) || isDigitChar(ch))) {
                length++;
            }
            return TokenType.VariableName;
        }

        while (index + length < value.length() && !isDigitChar(ch) && !isVariableChar(ch) && staticTypes.get(ch) == null) {
            length++;
            if (index + length < value.length())
                ch = value.charAt(index + length);
        }
        return TokenType.Format;
    }

    public void moveTo(int index) {
        this.index = index;
        length = 0;
    }

    public TokenType getToken() {
        return token;
    }

    public String getTokenText() {
        return value.substring(index, index + length);
    }

    public int getTokenLength() {
        return length;
    }

    public int getTokenStartIndex() {
        return index;
    }

    public int getTokenEndIndex() {
        return index + length;
    }

    private static SparseArray<TokenType> staticTypes = new SparseArray<>();

    static {
        for (TokenType value : TokenType.values()) {
            if (value.getTargetCharacter() != '\0') {
                staticTypes.put(value.getTargetCharacter(), value);
            }
        }
    }

    private static boolean isDigitChar(char ch) {
        return Character.isDigit(ch);
    }

    private static boolean isVariableChar(char ch) {
        return (ch >= 'a' && ch <= 'z') ||
                (ch >= 'A' && ch <= 'Z') ||
                ch == '_';
    }

}

