package io.github.abc15018045126.sora.lang.completion.snippet.parser

data class Token(
    @JvmField var index: Int,
    @JvmField var length: Int,
    @JvmField var type: TokenType
)
