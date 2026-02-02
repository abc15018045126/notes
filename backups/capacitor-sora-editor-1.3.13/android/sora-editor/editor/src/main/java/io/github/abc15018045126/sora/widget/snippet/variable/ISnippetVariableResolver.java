
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

/**
 * Interface for resolving code snippet variables
 */
public interface ISnippetVariableResolver {

    /**
     * Resolve the given variable name. Caller should ensure that the given variable name is
     * supported by this resolver.
     *
     * @return A non-empty string
     */
    @NonNull
    String resolve(@NonNull String name);

    /**
     * Get variable names supported by this resolver
     */
    @NonNull
    String[] getResolvableNames();

}

