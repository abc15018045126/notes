
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

public abstract class WorkspaceBasedSnippetVariableResolver implements ISnippetVariableResolver {

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{
                "WORKSPACE_NAME", "WORKSPACE_FOLDER"
        };
    }

}

