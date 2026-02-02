
package io.github.abc15018045126.sora.widget.snippet.variable;

import android.content.ClipboardManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClipboardBasedSnippetVariableResolver implements ISnippetVariableResolver {

    private final ClipboardManager clipboardManager;

    public ClipboardBasedSnippetVariableResolver(@Nullable ClipboardManager clipboardManager) {
        this.clipboardManager = clipboardManager;
    }

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{"CLIPBOARD"};
    }

    @NonNull
    @Override
    public String resolve(@NonNull String name) {
        if ("CLIPBOARD".equals(name)) {
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                var clip = clipboardManager.getPrimaryClip();
                if (clip == null || clip.getItemCount() == 0) {
                    return "";
                }
                return clip.getItemAt(0).getText().toString();
            }
            return "";
        }
        throw new IllegalArgumentException("Unsupported variable name:" + name);
    }
}

