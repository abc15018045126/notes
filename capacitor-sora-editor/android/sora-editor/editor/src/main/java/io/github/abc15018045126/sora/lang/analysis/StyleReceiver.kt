package io.github.abc15018045126.sora.lang.analysis

import io.github.abc15018045126.sora.lang.brackets.BracketsProvider
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.styling.Styles

/**
 * A [StyleReceiver] receives spans and other styles from analyzers.
 *
 * The implementations of the class must make sure its code can be safely run. For example, update
 * UI by posting its actions to UI thread, but not here.
 *
 * Also, the implementations of the class should pay attention to concurrent invocations due not to
 * corrupt the information it maintains.
 *
 * @author abc15018045126
 */
interface StyleReceiver {

    /**
     * Send the styles to the receiver. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     * the sourceManager fail
     */
    fun setStyles(sourceManager: AnalyzeManager, styles: Styles?)

    /**
     * Send the styles to the receiver. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     * the sourceManager fail
     * @param action Sometimes you may need to synchronize your action in main thread. This ensures the given action is executed
     * on main thread before the style updates.
     */
    fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?)

    /**
     * Notify the receiver the given styles object is updated, and line range is given by `range`
     *
     * @param sourceManager Source AnalyzeManager. The receiver may ignore the request if some checks on
     * the sourceManager fail
     * @param styles        The Styles object previously set by [setStyles]
     * @param range         The line range of this update
     */
    fun updateStyles(sourceManager: AnalyzeManager, styles: Styles, range: StyleUpdateRange) {
        setStyles(sourceManager, styles)
    }

    /**
     * Specify new diagnostics. You can call it in any thread.
     * The implementation of this method should make sure that concurrent invocations to it are safe.
     */
    fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?)

    /**
     * Set new provider for brackets highlighting
     */
    fun updateBracketProvider(sourceManager: AnalyzeManager, provider: BracketsProvider?)

}
