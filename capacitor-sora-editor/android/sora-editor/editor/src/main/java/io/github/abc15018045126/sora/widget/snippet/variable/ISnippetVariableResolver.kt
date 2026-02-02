package io.github.abc15018045126.sora.widget.snippet.variable

/**
 * Interface for resolving code snippet variables
 */
interface ISnippetVariableResolver {

    /**
     * Resolve the given variable name. Caller should ensure that the given variable name is
     * supported by this resolver.
     *
     * @return A non-empty string
     */
    fun resolve(name: String): String

    /**
     * Get variable names supported by this resolver
     */
    fun getResolvableNames(): Array<String>

}
