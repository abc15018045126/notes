package io.github.abc15018045126.sora.widget.snippet.variable

abstract class WorkspaceBasedSnippetVariableResolver : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "WORKSPACE_NAME", "WORKSPACE_FOLDER"
        )
    }

}
