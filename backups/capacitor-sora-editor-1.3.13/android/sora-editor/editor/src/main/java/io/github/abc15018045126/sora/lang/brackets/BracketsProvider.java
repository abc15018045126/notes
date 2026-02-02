
package io.github.abc15018045126.sora.lang.brackets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.text.Content;

/**
 * Interface for providing paired brackets
 *
 * @author abc15018045126
 */
public interface BracketsProvider {

    /**
     * Get left and right brackets position in text
     *
     * @param text  The text in editor
     * @param index Index of cursor in text
     * @return Paired positions or null if not matched
     */
    @Nullable
    PairedBracket getPairedBracketAt(@NonNull Content text, int index);

}

