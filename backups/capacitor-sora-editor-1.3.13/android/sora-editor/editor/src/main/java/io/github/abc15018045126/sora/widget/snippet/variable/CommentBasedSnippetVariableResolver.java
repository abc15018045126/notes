
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CommentBasedSnippetVariableResolver implements ISnippetVariableResolver {

    private String[] commentTokens;

    public CommentBasedSnippetVariableResolver() {
        this(null);
    }

    /**
     * For example, Java language: {@code new String[]{ "//", "/*", "*\/" }
     */
    public CommentBasedSnippetVariableResolver(@Nullable String[] commentTokenStrings) {
        setCommentTokens(commentTokenStrings);
    }

    public void setCommentTokens(@Nullable String[] commentTokens) {
        this.commentTokens = commentTokens;
    }

    public String[] getCommentTokens() {
        return commentTokens;
    }

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{
                "LINE_COMMENT", "BLOCK_COMMENT_START", "BLOCK_COMMENT_END"
        };
    }

    @NonNull
    @Override
    public String resolve(@NonNull String name) {
        if (commentTokens == null || commentTokens.length != 3) {
            throw new IllegalStateException("language comment style is not configured properly");
        }
        switch (name) {
            case "LINE_COMMENT":
                return commentTokens[0];
            case "BLOCK_COMMENT_START":
                return commentTokens[1];
            case "BLOCK_COMMENT_END":
                return commentTokens[2];
        }
        throw new IllegalArgumentException("Unsupported variable name:" + name);
    }
}

