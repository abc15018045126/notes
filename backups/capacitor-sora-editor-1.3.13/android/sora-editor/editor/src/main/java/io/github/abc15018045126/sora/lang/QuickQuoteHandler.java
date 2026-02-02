
package io.github.abc15018045126.sora.lang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.styling.Styles;
import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.text.TextRange;

public interface QuickQuoteHandler {

    /**
     * Checks whether the given input matches the requirement to invoke this handler
     *
     * @param candidateCharacter The character going to be inserted. Length can be 1 or 2.
     * @param text               Current text in editor
     * @param cursor             The range of cursor
     * @param style              Current code styles
     * @return Whether this handler consumed the event
     */
    @NonNull
    HandleResult onHandleTyping(@NonNull String candidateCharacter, @NonNull Content text, @NonNull TextRange cursor, @Nullable Styles style);

    class HandleResult {

        public final static HandleResult NOT_CONSUMED = new HandleResult(false, null);

        private boolean consumed;

        private TextRange newCursorRange;

        public HandleResult(boolean consumed, TextRange newCursorRange) {
            this.consumed = consumed;
            this.newCursorRange = newCursorRange;
        }

        public boolean isConsumed() {
            return consumed;
        }

        public void setConsumed(boolean consumed) {
            this.consumed = consumed;
        }

        public TextRange getNewCursorRange() {
            return newCursorRange;
        }

        public void setNewCursorRange(TextRange newCursorRange) {
            this.newCursorRange = newCursorRange;
        }
    }

}

