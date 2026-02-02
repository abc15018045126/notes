
package io.github.abc15018045126.sora.lang.smartEnter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.styling.Styles;
import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.Content;

/**
 * Perform text processing when user enters '\n' and selection size is 0
 */
public interface NewlineHandler {

    /**
     * Checks whether the given input matches the requirement to invoke this handler
     *
     * @param text     Current text in editor
     * @param position The position of cursor
     * @param style    Current code styles
     * @return Whether this handler should be called
     */
    boolean matchesRequirement(@NonNull Content text, @NonNull CharPosition position, @Nullable Styles style);

    /**
     * Handle newline and return processed content to insert
     *
     * @param text     Current text in editor
     * @param position The position of cursor
     * @param style    Current code styles
     * @return Actual content to insert
     */
    @NonNull
    NewlineHandleResult handleNewline(@NonNull Content text, @NonNull CharPosition position, @Nullable Styles style, int tabSize);

}

