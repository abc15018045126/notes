
package io.github.abc15018045126.sora.langs.textmate.registry.provider;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@FunctionalInterface
public interface FileResolver {
    @Nullable
    InputStream resolveStreamByPath(String path);

    default void dispose() {

    }

    FileResolver DEFAULT = path -> {
        var file = new File(path);
        if (file.isFile()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }

    };
}

