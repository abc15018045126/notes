
package io.github.abc15018045126.sora.widget.component;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme;

/**
 * Manages layout of {@link EditorAutoCompletion}
 * Can be set by {@link EditorAutoCompletion#setLayout(CompletionLayout)}
 * <p>
 * The implementation of this class must call {@link EditorAutoCompletion#select(int)} to select the
 * item in completion list when the user clicks one.
 */
@SuppressWarnings("rawtypes")
public interface CompletionLayout {

    /**
     * Color scheme changed
     */
    void onApplyColorScheme(@NonNull EditorColorScheme colorScheme);

    /**
     * Attach the {@link EditorAutoCompletion}.
     * This is called first before other methods are called.
     */
    void setEditorCompletion(@NonNull EditorAutoCompletion completion);

    /**
     * Inflate the layout, return the view root.
     */
    @NonNull
    View inflate(@NonNull Context context);

    /**
     * Get the {@link AdapterView} to display completion items
     */
    @NonNull
    AdapterView getCompletionList();

    /**
     * Set loading state.
     * You may update your layout to show other contents
     */
    void setLoading(boolean loading);

    /**
     * Make the given position visible
     *
     * @param position        Item index
     * @param incrementPixels If you scroll the layout, this is a recommended value of each scroll. {@link EditorCompletionAdapter#getItemHeight()}
     */
    void ensureListPositionVisible(int position, int incrementPixels);

    /**
     * Some layout may support to display more animations,
     * this method provides control over the animation of the layout.
     */
    default void setEnabledAnimation(boolean enabledAnimation) {
        //ignore
    }
}

