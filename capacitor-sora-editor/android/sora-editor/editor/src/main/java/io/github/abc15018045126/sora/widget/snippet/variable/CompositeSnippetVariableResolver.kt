package io.github.abc15018045126.sora.widget.snippet.variable

class CompositeSnippetVariableResolver : ISnippetVariableResolver {

    private val resolverMap = HashMap<String, ISnippetVariableResolver>()

    fun addResolver(resolver: ISnippetVariableResolver) {
        if (resolver is CompositeSnippetVariableResolver) {
            throw IllegalArgumentException("Cannot add a CompositeSnippetVariableResolver to another one")
        }
        for (name in resolver.getResolvableNames()) {
            resolverMap[name] = resolver
        }
    }

    fun removeResolver(resolver: ISnippetVariableResolver) {
        for (name in resolver.getResolvableNames()) {
            if (resolverMap[name] === resolver) {
                resolverMap.remove(name)
            }
        }
    }

    override fun getResolvableNames(): Array<String> {
        return emptyArray()
    }

    fun canResolve(name: String): Boolean {
        return resolverMap.containsKey(name)
    }

    override fun resolve(name: String): String {
        return resolverMap[name]?.resolve(name) ?: ""
    }
}
