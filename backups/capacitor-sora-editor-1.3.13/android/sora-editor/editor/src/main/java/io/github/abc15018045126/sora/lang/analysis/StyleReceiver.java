
package io.github.abc15018045126.sora.lang.analysis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.brackets.BracketsProvider;
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.abc15018045126.sora.lang.styling.Styles;

/**
 * A {@link StyleReceiver} receives spans and other styles from analyzers.
 * <p>
 * The implementations of the class must make sure its code can be safely run. For example, update
 * UI by posting its actions to UI thread, but not here.
 * <p>
 * Also, the implementations of the class should pay attention to concurrent invocations due not to
 * corrupt the information it maintains.
 *
 * @author abc15018045126
 */
public interface StyleReceiver {

    /**
     * Send the styles to the receiver. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     *                      the sourceManager fail
     */
    void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles);

    /**
     * Send the styles to the receiver. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     *                      the sourceManager fail
     * @param action Sometimes you may need to synchronize your action in main thread. This ensures the given action is executed
     *               on main thread before the style updates.
     */
    void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles, @Nullable Runnable action);

    /**
     * Notify the receiver the given styles object is updated, and line range is given by {@code range}
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     *                      the sourceManager fail
     * @param styles        The Styles object previously set by {@link #setStyles(AnalyzeManager, Styles)}
     * @param range         The line range of this update
     */
    default void updateStyles(@NonNull AnalyzeManager sourceManager, @NonNull Styles styles, @NonNull StyleUpdateRange range) {
        setStyles(sourceManager, styles);
    }

    /**
     * Specify new diagnostics. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     */
    void setDiagnostics(@NonNull AnalyzeManager sourceManager, @Nullable DiagnosticsContainer diagnostics);

    /**
     * Set new provider for brackets highlighting
     */
    void updateBracketProvider(@NonNull AnalyzeManager sourceManager, @Nullable BracketsProvider provider);

}

