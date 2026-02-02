package io.github.abc15018045126.sora.widget.snippet.variable

class CommentBasedSnippetVariableResolver(var commentTokens: Array<String>? = null) : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "LINE_COMMENT", "BLOCK_COMMENT_START", "BLOCK_COMMENT_END"
        )
    }

    override fun resolve(name: String): String {
        val tokens = commentTokens
        if (tokens == null || tokens.size != 3) {
            throw IllegalStateException("language comment style is not configured properly")
        }
        return when (name) {
            "LINE_COMMENT" -> tokens[0]
            "BLOCK_COMMENT_START" -> tokens[1]
            "BLOCK_COMMENT_END" -> tokens[2]
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}
