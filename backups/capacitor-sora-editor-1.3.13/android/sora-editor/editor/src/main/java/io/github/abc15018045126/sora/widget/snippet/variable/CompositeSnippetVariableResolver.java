
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CompositeSnippetVariableResolver implements ISnippetVariableResolver {

    private final Map<String, ISnippetVariableResolver> resolverMap;

    public CompositeSnippetVariableResolver() {
        resolverMap = new HashMap<>();
    }

    public void addResolver(@NonNull ISnippetVariableResolver resolver) {
        if (resolver instanceof CompositeSnippetVariableResolver) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(resolver, "resolver must not be null");
        for (var name : resolver.getResolvableNames()) {
            resolverMap.put(name, resolver);
        }
    }

    public void removeResolver(@NonNull ISnippetVariableResolver resolver) {
        for (var name : resolver.getResolvableNames()) {
            if (resolverMap.get(name) == resolver) {
                resolverMap.remove(name);
            }
        }
    }

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[0];
    }

    public boolean canResolve(@NonNull String name) {
        return resolverMap.containsKey(name);
    }

    @NonNull
    @Override
    public String resolve(@NonNull String name) {
        var resolver = resolverMap.get(name);
        if (resolver != null) {
            return resolver.resolve(name);
        }
        return "";
    }

}

