package io.github.abc15018045126.sora.lang.completion.snippet.parser

enum class TokenType(val targetCharacter: Char = '\u0000') {
    Dollar('$'),
    Colon(':'),
    Comma(','),
    CurlyOpen('{'),
    CurlyClose('}'),
    Backslash('\\'),
    Forwardslash('/'),
    Pipe('|'),
    Int,
    VariableName,
    Format,
    Plus('+'),
    Dash('-'),
    QuestionMark('?'),
    Backtick('`'),
    EOF
}
