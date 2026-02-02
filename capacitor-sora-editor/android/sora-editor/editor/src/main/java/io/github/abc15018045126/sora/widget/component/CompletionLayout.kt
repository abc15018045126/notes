package io.github.abc15018045126.sora.widget.component

import android.content.Context
import android.view.View
import android.widget.AdapterView
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * Manages layout of [EditorAutoCompletion]
 * Can be set by [EditorAutoCompletion.setLayout]
 *
 * The implementation of this class must call [EditorAutoCompletion.select] to select the
 * item in completion list when the user clicks one.
 */
interface CompletionLayout {

    /**
     * Color scheme changed
     */
    fun onApplyColorScheme(colorScheme: EditorColorScheme)

    /**
     * Attach the [EditorAutoCompletion].
     * This is called first before other methods are called.
     */
    fun setEditorCompletion(completion: EditorAutoCompletion)

    /**
     * Inflate the layout, return the view root.
     */
    fun inflate(context: Context): View

    /**
     * Get the [AdapterView] to display completion items
     */
    fun getCompletionList(): AdapterView<*>

    /**
     * Set loading state.
     * You may update your layout to show other contents
     */
    fun setLoading(loading: Boolean)

    /**
     * Make the given position visible
     *
     * @param position        Item index
     * @param incrementPixels If you scroll the layout, this is a recommended value of each scroll. [EditorCompletionAdapter.getItemHeight]
     */
    fun ensureListPositionVisible(position: Int, incrementPixels: Int)

    /**
     * Some layout may support to display more animations,
     * this method provides control over the animation of the layout.
     */
    fun setEnabledAnimation(enabledAnimation: Boolean) {
        //ignore
    }
}
