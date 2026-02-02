
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

import java.util.Random;
import java.util.UUID;

public class RandomBasedSnippetVariableResolver implements ISnippetVariableResolver {

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{
                "RANDOM", "RANDOM_HEX", "UUID"
        };
    }

    @NonNull
    @Override
    public String resolve(@NonNull String name) {
        switch (name) {
            case "RANDOM":
                return Integer.toString(new Random().nextInt());
            case "RANDOM_HEX":
                return Integer.toString(new Random().nextInt(), 16);
            case "UUID":
                return UUID.randomUUID().toString();
        }
        throw new IllegalArgumentException("Unsupported variable name:" + name);
    }
}

