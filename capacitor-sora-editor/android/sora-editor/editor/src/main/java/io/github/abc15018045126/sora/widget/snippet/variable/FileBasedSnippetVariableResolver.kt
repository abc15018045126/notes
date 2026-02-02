package io.github.abc15018045126.sora.widget.snippet.variable

abstract class FileBasedSnippetVariableResolver : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "TM_FILENAME", "TM_FILENAME_BASE", "TM_DIRECTORY", "TM_FILEPATH", "RELATIVE_PATH"
        )
    }

}
