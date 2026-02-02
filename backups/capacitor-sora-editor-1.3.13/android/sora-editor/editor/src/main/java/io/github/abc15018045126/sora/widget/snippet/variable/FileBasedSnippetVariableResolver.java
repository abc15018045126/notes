
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

public abstract class FileBasedSnippetVariableResolver implements ISnippetVariableResolver {

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{
                "TM_FILENAME", "TM_FILENAME_BASE", "TM_DIRECTORY", "TM_FILEPATH", "RELATIVE_PATH"
        };
    }

}

