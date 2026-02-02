
package io.github.abc15018045126.sora.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.Language;
import io.github.abc15018045126.sora.lang.QuickQuoteHandler;
import io.github.abc15018045126.sora.text.ContentReference;

/**
 * Helper class for better Android API compatibility
 *
 * @author abc15018045126
 */
class LanguageHelper {

    @Nullable
    public static QuickQuoteHandler getQuickQuoteHandler(@NonNull Language language) {
        try {
            return language.getQuickQuoteHandler();
        } catch (AbstractMethodError e) {
            return null;
        }
    }

    public static int getIndentAdvance(
            @NonNull Language language,
            @NonNull ContentReference content,
            int line,
            int column,
            int spaceCountOnLine,
            int tabCountOnLine
    ) {
        try {
            return language.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine);
        } catch (AbstractMethodError e) {
            return language.getIndentAdvance(content, line, column);
        }
    }


}

